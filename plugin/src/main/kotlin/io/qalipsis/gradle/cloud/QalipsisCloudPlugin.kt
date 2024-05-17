package io.qalipsis.gradle.cloud

import io.qalipsis.gradle.cloud.tasks.PublishScenariosTask
import io.qalipsis.gradle.cloud.tasks.DeleteScenarioTask
import io.qalipsis.gradle.cloud.tasks.Helper
import io.qalipsis.gradle.cloud.tasks.ScenariosListService
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to upload, manage and execute scenarios into QALIPSIS Cloud.
 */
internal class QalipsisCloudPlugin : Plugin<Project> {

    private val scenariosListService = ScenariosListService()

    override fun apply(project: Project) {
        project.tasks.create("qalipsisPublish", PublishScenariosTask::class.java)

        val listScenarios = if (project.hasProperty("qalipsis.cloud.registry.list")) {
            project.property("qalipsis.cloud.registry.list").toString().toBoolean()
        } else {
            true
        }
        if (listScenarios && !project.gradle.startParameter.isOffline) {
            createTasksToDeleteScenarios(project)
        }
    }

    private fun createTasksToDeleteScenarios(project: Project) {
        val scenarios = scenariosListService.execute(project)
        // Displays the scenarios in the console.
        if (scenarios.isNotEmpty()) {
            project.logger.lifecycle(
                """Scenarios currently present:
                |${
                    scenarios.joinToString(separator = "\n  ", prefix = "  ") { scenario ->
                        if (scenario.description == null) {
                            "${scenario.name} (version ${scenario.version})"
                        } else {
                            "${scenario.name}: ${scenario.description} (version ${scenario.version})"
                        }
                    }
                }
            """.trimMargin()
            )
            scenarios.forEach { scenario ->
                // When the scenario name is "my-scenario", the task name is "deleteMyScenario"
                val taskName = "qalipsisCloudDeleteScenario" + Helper.camelCaseScenarioName(scenario.name)
                project.tasks.create(taskName, DeleteScenarioTask::class.java, taskName, scenario)
            }
        }
    }

    internal companion object {

        val DEFAULT_SCENARIOS_API_ROOT_URL = "https://app.qalipsis.io/api/scenarios".toHttpUrl()

    }
}