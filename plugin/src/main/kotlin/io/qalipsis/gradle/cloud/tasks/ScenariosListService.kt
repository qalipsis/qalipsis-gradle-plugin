package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.Scenario
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.Request
import org.gradle.api.Project

/**
 * Service in charge of listing the scenarios currently present into QALIPSIS Cloud.
 */
internal class ScenariosListService {

    fun execute(project: Project): Collection<Scenario> {
        val request = Request.Builder()
            .url(Helper.rootEndpoint(project))
            .authenticate(project)
            .build()
        if (project.logger.isDebugEnabled) {
            project.logger.debug("Listing the scenarios from ${request.url}")
        } else {
            project.logger.lifecycle("Listing the scenarios from the cloud environment")
        }

        val scenarios = Helper.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMessage = Helper.jsonMapper.readValue<ErrorMessage>(response.body!!.bytes())
                throw RuntimeException(errorMessage.message)
            }

            Helper.jsonMapper.readValue<Collection<Scenario>>(requireNotNull(response.body?.bytes()) { "The response does not contain any data" })

        }
        project.logger.debug("Retrieved the scenarios ${scenarios.joinToString { it.name }}")
        return scenarios
    }
}