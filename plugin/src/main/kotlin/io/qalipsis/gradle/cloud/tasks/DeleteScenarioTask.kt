package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.Scenario
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Task in charge of deleting a unique scenario from QALIPSIS Cloud.
 */
internal abstract class DeleteScenarioTask @Inject constructor(
    private val taskName: String,
    private val scenario: Scenario
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
            .url(Helper.rootEndpoint(project) + scenario.name)
            .authenticate(project)
            .delete()
            .build()

        project.logger.lifecycle("Deleting the scenario ${scenario.name} (${scenario.description})")
        Helper.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMessage = Helper.jsonMapper.readValue<ErrorMessage>(response.body!!.bytes())
                throw RuntimeException(errorMessage.message)
            }
        }

    }


}