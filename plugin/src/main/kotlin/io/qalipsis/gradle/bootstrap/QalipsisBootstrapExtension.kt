package io.qalipsis.gradle.bootstrap

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.util.Properties
import javax.inject.Inject

private const val QALIPSIS_VERSION_KEY = "qalipsis.version"

/**
 * Root extension of QALIPSIS Gradle plugin.
 */
open class QalipsisBootstrapExtension @Inject constructor(private val project: Project, objects: ObjectFactory) {

    internal val version: Property<String> = objects.property(String::class.java)

    internal val plugins: Property<QalipsisPluginsExtension> = objects.property(QalipsisPluginsExtension::class.java)

    init {
        initQalipsisVersion()
    }

    /**
     * Initializes the version of QALIPSIS to use, from different sources, in that order, when set:
     * - constant from file default.properties
     * - environment variables `QALIPSIS_VERSION`
     * - system property `qalipsis.version`
     */
    private fun initQalipsisVersion() {
        val pluginProperties = Properties()
        pluginProperties.load(this::class.java.classLoader.getResourceAsStream("default.properties"))
        version.set(pluginProperties.getProperty(QALIPSIS_VERSION_KEY))

        // Initializes the version of QALIPSIS from the environment properties if set.
        System.getenv()["QALIPSIS_VERSION"]?.takeIf { it.isNotBlank() }?.also { versionFromSystemProperty ->
            version.set(versionFromSystemProperty)
        }

        // Initializes the version of QALIPSIS from the system properties if set.
        if (project.hasProperty(QALIPSIS_VERSION_KEY)) {
            project.property(QALIPSIS_VERSION_KEY).toString()
                .takeIf { it.isNotBlank() }
                ?.also { versionFromSystemProperty ->
                    version.set(versionFromSystemProperty)
                }
        }
    }

    /**
     * Sets the version of QALIPSIS to use.
     */
    fun version(qalipsisVersion: String) {
        this.version.set(qalipsisVersion)
    }

    /**
     * Reads the configured version of QALIPSIS to use.
     */
    fun version() = version.get()

    /**
     * Configures the plugins to enable for the scenario.
     */
    fun plugins(configure: QalipsisPluginsExtension.() -> Unit) {
        if (!plugins.isPresent) {
            plugins.set(QalipsisPluginsExtension())
        }
        plugins.get().configure()
    }

}