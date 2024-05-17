package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.Scenario
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.Request
import org.gradle.api.Project
import java.io.IOException

/**
 * Service in charge of listing the scenarios currently present into QALIPSIS Cloud.
 */
internal class ScenariosListService {

    fun execute(project: Project): Collection<Scenario> {
        val request = Request.Builder()
            .url(Helper.buildUrl(project))
            .authenticate(project)
            .build()
        if (project.logger.isDebugEnabled) {
            project.logger.debug("Listing the scenarios from ${request.url}")
        } else {
            project.logger.lifecycle("Listing the scenarios from the cloud environment")
        }

        val scenarios = Helper.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    in setOf(401, 403) -> {
                        val errorMessage = """
                                    | There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to read scenarios?
                                """.trimMargin()
                        throw IOException(errorMessage)
                    }
                    in setOf(502, 504) -> {
                        project.logger.warn(
                            """
                                    | The QALIPSIS server is currently not available, and the list of existing scenarios cannot be retrieved.
                                    | Please try again in a moment.
                                    | If the problem persists, please contact the support.
                                """.trimMargin()
                        )
                        emptyList()
                    }
                    else -> {
                        val errorMessage = Helper.jsonMapper.readValue<ErrorMessage>(response.body!!.bytes())
                        throw RuntimeException(errorMessage.message)
                    }
                }
            } else {
                Helper.jsonMapper.readValue<Collection<Scenario>>(requireNotNull(response.body?.bytes()) { "The response does not contain any data" })
            }
        }
        project.logger.debug("Retrieved the scenarios ${scenarios.joinToString { it.name }}")
        return scenarios
    }
}