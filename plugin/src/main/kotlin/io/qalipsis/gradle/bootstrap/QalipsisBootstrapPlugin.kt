package io.qalipsis.gradle.bootstrap

import io.qalipsis.gradle.bootstrap.tasks.RunQalipsis
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

/**
 * Plugin to configure a QALIPSIS Gradle project with the default settings.
 */
internal class QalipsisBootstrapPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("qalipsis", QalipsisBootstrapExtension::class.java)
        project.tasks.create("qalipsisRunAllScenarios", RunQalipsis::class.java)

        project.pluginManager.apply(KotlinPluginWrapper::class.java)
        project.pluginManager.apply(Kapt3GradleSubplugin::class.java)
        project.pluginManager.apply(ApplicationPlugin::class.java)

        configureJava(project)
        configureKotlin(project)

        project.extensions.configure<JavaApplication>("application") {
            it.mainClass.set("io.qalipsis.runtime.Qalipsis")
        }

        project.dependencies.apply {
            add("implementation", "io.qalipsis:qalipsis-api-processors")
            add("kapt", "io.qalipsis:qalipsis-api-processors")

            add("implementation", "io.qalipsis:qalipsis-runtime")
            add("implementation", "io.qalipsis:qalipsis-head")
            add("implementation", "io.qalipsis:qalipsis-factory")
        }

        // Configuration of elements depending on the configuration.
        project.afterEvaluate {
            // Add the maven central repository if not done yet.
            if (!project.repositories.contains(project.repositories.mavenCentral())) {
                project.repositories.add(project.repositories.mavenCentral())
            }
            // Add the Jitpack repository if not done yet.
            if (project.repositories.none { (it as? MavenArtifactRepository)?.url == URI("https://jitpack.io") }) {
                project.repositories.add(project.repositories.maven {
                    it.name = "jitpack"
                    it.setUrl("https://jitpack.io")
                })
            }
            addConfiguredRepositoriesAndPlatform(project, extension)
        }
    }

    private fun addConfiguredRepositoriesAndPlatform(project: Project, extension: QalipsisBootstrapExtension) {
        val qalipsisVersion = extension.version.get()

        project.logger.lifecycle("Using QALIPSIS $qalipsisVersion")

        if (qalipsisVersion.lowercase().endsWith("-snapshot")) {
            // When the version to use is a snapshot, the convenient repository is added.
            project.repositories.add(project.repositories.maven {
                it.name = "qalipsis-oss-snapshots"
                it.setUrl("https://maven.qalipsis.com/repository/oss-snapshots")
                it.content {
                    it.includeGroup("io.qalipsis")
                    it.includeGroup("io.qalipsis.plugin")
                    it.includeGroup("io.qalipsis.gradle")
                }
            })
        }

        project.dependencies.apply {
            add("implementation", platform("io.qalipsis:qalipsis-platform:${qalipsisVersion}"))
            add("kapt", platform("io.qalipsis:qalipsis-platform:${qalipsisVersion}"))

            if (extension.plugins.isPresent) {
                extension.plugins.get().requiredPluginsDependency.forEach { pluginDependency ->
                    add("implementation", pluginDependency)
                }
            }
        }
    }

    private fun configureJava(project: Project) {
        project.extensions.configure<JavaPluginExtension>("java") {
            it.sourceCompatibility = JAVA_VERSION
            it.targetCompatibility = JAVA_VERSION
        }
    }

    private fun configureKotlin(project: Project) {
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            it.kotlinOptions {
                jvmTarget = JAVA_VERSION.majorVersion
                javaParameters = true
            }
        }
        project.extensions.configure(KaptExtensionConfig::class.java) {
            it.correctErrorTypes = true
            it.mapDiagnosticLocations = true
        }
    }

    private companion object {

        /**
         * Default version of Java as source and target.
         */
        val JAVA_VERSION = JavaVersion.VERSION_11

    }
}
