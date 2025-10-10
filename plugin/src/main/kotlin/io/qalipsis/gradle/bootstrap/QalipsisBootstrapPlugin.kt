package io.qalipsis.gradle.bootstrap

import io.qalipsis.gradle.bootstrap.tasks.RunQalipsis
import java.net.URI
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

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

        configureKotlin(project)
        configureApplication(project)
        configureDistribution(project)

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

        if (qalipsisVersion.lowercase().endsWith("-snapshot", ignoreCase = true)
            || qalipsisVersion.lowercase().endsWith("-dev", ignoreCase = true)
        ) {
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

    private fun configureApplication(project: Project) {
        project.extensions.configure<JavaApplication>("application") {
            it.mainClass.set("io.qalipsis.runtime.Qalipsis")
        }
    }

    /**
     * Overwrites the default start scripts to use the one from the distribution and add default configuration files.
     */
    private fun configureDistribution(project: Project) {
        project.tasks.withType(CreateStartScripts::class.java).configureEach { startScripts ->
            startScripts.enabled = false
        }

        val createQalipsisStartScripts = project.tasks.register("createQalipsisStartScripts") {
            it.doLast {
                copyDistributionFile(project, source = "config/logback.xml", target = "config/logback.xml")
                copyDistributionFile(project, source = "config/qalipsis.yml", target = "config/qalipsis.yml")
                copyDistributionFile(
                    project,
                    source = "start-scripts/unix.template",
                    target = "scripts/${project.name}",
                    executable = true
                )
                copyDistributionFile(
                    project,
                    source = "start-scripts/windows.template",
                    target = "scripts/${project.name}.bat",
                    executable = true
                )
            }
        }

        project.afterEvaluate {
            val buildDir = project.layout.buildDirectory.get().asFile

            project.tasks.withType(Zip::class.java).filter { it.name.contains("dist", true) }.forEach { zipTask ->
                zipTask.dependsOn(createQalipsisStartScripts)
                // Include config files
                zipTask.from("$buildDir/config") {
                    it.into("${project.name}-${project.version}/config")
                }
            }
        }
    }

    private fun copyDistributionFile(project: Project, source: String, target: String, executable: Boolean = false) {
        val targetFile = project.layout.buildDirectory.file(target).get().asFile
        if (!targetFile.exists()) {
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }
        if (executable) {
            targetFile.setExecutable(true)
        }

        this::class.java.getResourceAsStream("/distribution/$source")!!.use { resource ->
            resource.copyTo(targetFile.outputStream())
        }
    }

    private fun configureKotlin(project: Project) {
        project.extensions.configure(KotlinJvmExtension::class.java) {
            it.jvmToolchain { toolchain ->
                toolchain.languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION.majorVersion))
            }
            it.compilerOptions {
                freeCompilerArgs.add("-Xemit-jvm-type-annotations")
                javaParameters.set(true)
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
