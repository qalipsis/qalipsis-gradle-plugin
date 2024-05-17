package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.bootstrap.QalipsisBootstrapExtension
import io.qalipsis.gradle.cloud.model.Dependency
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.LogMessage
import io.qalipsis.gradle.cloud.model.ScenarioUploadMetadata
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.attributes.Category
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

/**
 * Task in charge of pushing the local scenarios to QALIPSIS Cloud.
 */
internal abstract class PublishScenariosTask : DefaultTask() {
    init {
        // FIXME only execute when:
        //  - dependent tasks were updated
        //  - the security token changed.
        //  - the path to the server changed.
        outputs.upToDateWhen {
            false
        }

        onlyIf {
            // When the project is offline, the task is disabled.
            !project.gradle.startParameter.isOffline
        }
        project.afterEvaluate {
            dependsOn(project.tasks.getByPath("jar"))
        }
    }

    override fun getName() = "qalipsisCloudPublish"

    override fun getGroup() = "qalipsis cloud"

    @TaskAction
    fun publishScenarios() {
        // Generated simple archive.
        val jarFile = (project.tasks.findByName("jar") as Jar).archiveFile.get().asFile

        val dependencies = project.configurations
            .filter { it.dependencies.isNotEmpty() }
            .filterNot {
                // All the dependencies for the tests or kotlin-plugin generated configurations are ignored.
                it.name.startsWith("test")
                        || it.name.endsWith("Test")
                        || it.name.startsWith("kotlin")
            }
            .flatMap { configuration ->
                logger.lifecycle("Filtering the ${configuration.dependencies.size} dependencies of ${configuration.name}")
                configuration.dependencies
                    .onEach {
                        logger.lifecycle("Evaluating the dependency $it: ${it.group}:${it.name}:${it.version}")
                    }
                    // Exclude the QALIPSIS dependencies, they are forced by the remote builder and cannot be overwritten
                    // for security reason.
                    .filterNot { it.group?.startsWith("io.qalipsis") == true }
                    .filter { dep ->
                        dep is ExternalDependency
                                && dep.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name !in ACCEPTED_DEPENDENCIES_CATEGORIES
                    }
                    .map { dep ->
                        val (classifier, category) = if (dep is ExternalDependency) {
                            dep.artifacts.joinToString { it.classifier ?: "" }
                                .takeIf { it.isNotBlank() } to dep.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name
                        } else {
                            null to null
                        }
                        Dependency(
                            configuration.name,
                            requireNotNull(dep.group),
                            dep.name,
                            dep.version,
                            classifier,
                            category
                        )

                    }
            }
        val qalipsisExtension = project.extensions.getByType(QalipsisBootstrapExtension::class.java)
        val plugins = if (qalipsisExtension.plugins.isPresent) {
            qalipsisExtension.plugins.get().requiredPlugins
        } else {
            emptyList()
        }

        val metadata = ScenarioUploadMetadata(dependencies, plugins, qalipsisExtension.version(), jarFile.name)
        val jsonizedMetadata = Helper.jsonMapper.writeValueAsString(metadata)
        project.logger.debug("Sending the metadata $jsonizedMetadata")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", jsonizedMetadata)
            .addFormDataPart("archive", jarFile.name, jarFile.asRequestBody("application/java-archive".toMediaType()))
            .build()

        val request = Request.Builder()
            .url(Helper.rootEndpoint(project))
            .authenticate(project)
            .post(requestBody)
            .build()

        project.logger.lifecycle("Sending the scenario(s) to the cloud")
        val operationResult = CompletableFuture<Result<Unit>>()
        try {
            EventSources.createFactory(Helper.httpClient)
                .newEventSource(request = request, listener = SSEListener(operationResult, project.logger))
        } catch (e: Exception) {
            logger.error("Error type: ${e.javaClass}")
            logger.error("Error from the cloud server: ${e.message}")
            operationResult.complete(
                Result.failure(e)
            )
        }

        operationResult.get().getOrThrow()
        logger.lifecycle("Scenario(s) now ready for use in the cloud, refresh your project to list them")
    }

    private class SSEListener(
        private val operationResult: CompletableFuture<Result<Unit>>,
        private val logger: Logger
    ) : EventSourceListener() {

        private var errors = mutableListOf<String>()

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            val message = Helper.jsonMapper.readValue<LogMessage>(data)
            when (message.level) {
                "ERROR" -> {
                    errors += message.text
                    logger.error("[ERROR] {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
                }

                "DEBUG" -> logger.debug("[DEBUG] {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
                else -> logger.lifecycle("[INFO]  {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
            }
        }

        private fun convertToLocaleTime(instant: Instant): LocalDateTime =
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS)

        override fun onOpen(eventSource: EventSource, response: Response) {
            logger.debug("Connected to the cloud server...")
            super.onOpen(eventSource, response)
        }

        override fun onClosed(eventSource: EventSource) {
            if (errors.isEmpty()) {
                operationResult.complete(Result.success(Unit))
            } else {
                operationResult.complete(
                    Result.failure(RuntimeException(errors.joinToString(separator = "\n -", prefix = "\n")))
                )
            }
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            logger.error("An error occurred while sending the scenario(s) to the cloud server")
            response?.let {
                val body = it.body!!.string()
                val errorMessage = runCatching {
                    Helper.jsonMapper.readValue<ErrorMessage>(body)
                }.getOrNull()?.message.takeUnless { it.isNullOrBlank() } ?: body
                logger.error("Error from the cloud server: $errorMessage")
                operationResult.complete(Result.failure(RuntimeException(errorMessage)))
            }
            t?.let {
                logger.error("Error from the cloud server: ${t.message}")
                operationResult.complete(Result.failure(t))
            }
        }
    }

    private companion object {

        /**
         * When a category is set on the dependencies, it is only used when of this type.
         */
        val ACCEPTED_DEPENDENCIES_CATEGORIES = setOf(Category.REGULAR_PLATFORM, Category.ENFORCED_PLATFORM)

    }

}