package io.qalipsis.gradle.bootstrap.tasks

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.JavaExec
import javax.inject.Inject

abstract class RunQalipsis @Inject constructor(
    objectFactory: ObjectFactory,
    project: Project
) : JavaExec() {

    /**
     * Contains the list of scenarios to execute.
     */
    private val scenarios = objectFactory.setProperty(String::class.java)

    /**
     * Contains the configuration to pass as arguments.
     */
    private val configuration = objectFactory.mapProperty(String::class.java, Any::class.java)

    init {
        group = "qalipsis"
        description = "Executes one or more scenarios using QALIPSIS"

        maxHeapSize = "256m"
        workingDir = project.projectDir
        // Forces the class to execute.
        mainClass.set("io.qalipsis.runtime.Qalipsis")

        // The compilation should be triggered before the execution.
        project.afterEvaluate{
            dependsOn(project.tasks.getByPath("classes"))
        }
    }

    override fun exec() {
        args(prepareCommandLineArguments())

        val sourceSets =
            (project as org.gradle.api.plugins.ExtensionAware).extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer
        classpath = sourceSets.getByName("main").runtimeClasspath

        super.exec()
    }

    /**
     * Prepares the command line arguments, depending on the provided configuration.
     */
    private fun prepareCommandLineArguments(): MutableList<String> {
        val arguments = mutableListOf("--autostart")
        if (scenarios.isPresent && scenarios.get().isNotEmpty()) {
            arguments += listOf("-s", scenarios.get().joinToString { it.trim() })
        }
        if (configuration.isPresent) {
            configuration.get().forEach { (key, value) ->
                arguments += listOf("-c", "$key=$value")
            }
        }
        project.logger.debug("Executing QALIPSIS: {}", arguments)
        return arguments
    }

    /**
     * Overwrites the selection of scenarios to execute.
     */
    fun scenarios(vararg names: String) {
        scenarios.set(names.toSet())
    }

    /**
     * Overwrites the configuration of the campaign, using key/value pairs.
     * The function expects an even count of arguments to identify the key and value.
     */
    fun configuration(vararg keyAndValues: String) {
        require(keyAndValues.size % 2 == 0) { "An even number of argument should be provided as keys and values" }
        val configurationValues = keyAndValues.toList().windowed(2, 2).associate { (key, value) ->
            key to value
        }.toMap()
        configuration.set(configurationValues)
    }

    /**
     * Overwrites the configuration of the campaign.
     */
    fun configuration(vararg config: Pair<String, String>) {
        configuration.set(config.toMap())
    }

    /**
     * Overwrites the configuration of the campaign.
     */
    fun configuration(config: Map<String, String>) {
        configuration.set(config)
    }
}