package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.Scenario
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import javax.inject.Inject

/**
 * Task in charge of deleting a unique scenario from QALIPSIS Cloud.
 */
internal abstract class DeleteScenarioTask @Inject constructor(
    private val taskName: String,
    private val scenario: Scenario,
) : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        onlyIf {
            // When the project is offline, the task is disabled.
            !project.gradle.startParameter.isOffline
        }
    }

    override fun getName() = taskName

    override fun getGroup() = "qalipsis cloud"

    @TaskAction
    fun deleteScenario() {
        val request = Request.Builder()
            .url(Helper.buildUrl(project, scenario.name))
            .authenticate(project)
            .delete()
            .build()

        project.logger.lifecycle("Deleting the scenario ${scenario.name} (${scenario.description})")
        Helper.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    in setOf(401, 403) -> {
                        val errorMessage = """
                                    | There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?
                                """.trimMargin()
                        throw IOException(errorMessage)
                    }
                    in setOf(502, 504) -> {
                        val errorMessage = """
                                    | The QALIPSIS server is currently not available, and the scenario could not be deleted.
                                    | Please try again in a moment.
                                    | If the problem persists, please contact the support.
                                """.trimMargin()
                        throw IOException(errorMessage)
                    }
                    else -> {
                        val bodyString = response.body
                        val errorMessage =
                            kotlin.runCatching { Helper.jsonMapper.readValue<ErrorMessage>(bodyString!!.bytes()).message }
                                .getOrDefault(bodyString.toString())
                        throw RuntimeException(errorMessage)
                    }
                }
            }
        }

    }


}