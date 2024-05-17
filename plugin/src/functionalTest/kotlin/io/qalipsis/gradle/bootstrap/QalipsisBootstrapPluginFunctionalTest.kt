package io.qalipsis.gradle.bootstrap

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Path
import kotlin.io.path.writeText

private const val GRADLE_BUILD_FILE = "build.gradle.kts"

internal class QalipsisBootstrapPluginFunctionalTest {

    /**
     * Kotlin snippet that list all the dependencies of a project.
     */
    private val listDependencies =
        this::class.java.getResourceAsStream("/listAllDependencies.kt.txt")!!.reader().readText()

    @Nested
    inner class General {

        @Test
        fun `should load the plugin with the default version`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}

afterEvaluate {
    $listDependencies
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            val output = result.output
            output shouldContain "Using QALIPSIS 0.13.a-SNAPSHOT"
            output shouldContain """
implementation: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-runtime:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-head:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-factory:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
kapt: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
        """.trimIndent()
        }

        @Test
        fun `should load the plugin with the version specified as extension`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}
    
qalipsis {
    version("the-version")
}

$listDependencies
"""
            )

            // When
            val runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempFolder.toFile())
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            result.output shouldContain "Using QALIPSIS the-version"
            result.tasks.filter { it.path.startsWith("qalipsis") }.asClue {
                it shouldHaveSize 0
            }
        }

        @Test
        fun `should load the plugin with the version specified as property`(@TempDir tempFolder: Path) {
            // Given
            val propertiesFile = tempFolder.resolve("gradle.properties")
            propertiesFile.writeText("qalipsis.version=my-version")
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(tempFolder.toFile())
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            result.output shouldContain "Using QALIPSIS my-version"
            result.tasks.filter { it.path.startsWith("qalipsis") }.asClue {
                it shouldHaveSize 0
            }
        }

        @Test
        fun `should load the plugin with dependencies and plugins`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}

qalipsis {
    plugins {
        apacheKafka()
        elasticsearch()
        netty()
    }
}

dependencies {
    // Add your own dependencies.
    implementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    implementation("org.slf4j:slf4j-api:1.7.36")

    runtimeOnly(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
}

afterEvaluate {
$listDependencies
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            val output = result.output
            output shouldContain "Using QALIPSIS 0.13.a-SNAPSHOT"
            output shouldContain """
implementation: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-runtime:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-head:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-factory:<unspecified>:<unspecified>
 - io.netty:netty-transport-native-kqueue:<unspecified>:osx-x86_64
 - org.slf4j:slf4j-api:1.7.36:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-kafka:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-elasticsearch:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-netty:<unspecified>:<unspecified>
kapt: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
runtimeOnly: 
 - io.netty:netty-transport-native-kqueue:<unspecified>:osx-x86_64
        """.trimIndent()
        }
    }

    @Nested
    inner class PluginIntegration {

        @ParameterizedTest
        @CsvSource(
            value = [
                "apacheCassandra,qalipsis-plugin-cassandra",
                "apacheKafka,qalipsis-plugin-kafka",
                "elasticsearch,qalipsis-plugin-elasticsearch",
                "graphite,qalipsis-plugin-graphite",
                "influxDb,qalipsis-plugin-influxdb",
                "jackson,qalipsis-plugin-jackson",
                "jms,qalipsis-plugin-jms",
                "jakartaMessaging,qalipsis-plugin-jakarta-ee-messaging",
                "mail,qalipsis-plugin-mail",
                "mongoDb,qalipsis-plugin-mongodb",
                "netty,qalipsis-plugin-netty",
                "r2dbcJasync,qalipsis-plugin-r2dbc-jasync",
                "rabbitMq,qalipsis-plugin-rabbitmq",
                "redisLettuce,qalipsis-plugin-redis-lettuce",
                "slack,qalipsis-plugin-slack",
                "timescaleDb,qalipsis-plugin-timescaledb",
            ]
        )
        fun `should load the plugins`(pluginExtension: String, pluginDependency: String, @TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}

qalipsis {
    plugins {
        $pluginExtension()
    }
}

afterEvaluate {
$listDependencies
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            val output = result.output
            output shouldContain "Using QALIPSIS 0.13.a-SNAPSHOT"
            output shouldContain """
implementation: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-runtime:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-head:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-factory:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
 - io.qalipsis.plugin:$pluginDependency:<unspecified>:<unspecified>
kapt: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
        """.trimIndent()
        }

        @Test
        fun `should load all the plugins at once`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}

qalipsis {
    plugins {
        apacheCassandra()
        apacheKafka()
        elasticsearch()
        graphite()
        influxDb()
        jackson()
        jms()
        jakartaMessaging()
        mail()
        mongoDb()
        netty()
        r2dbcJasync()
        rabbitMq()
        redisLettuce()
        slack()
        timescaleDb()
    }
}

afterEvaluate {
$listDependencies
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            val output = result.output
            output shouldContain "Using QALIPSIS 0.13.a-SNAPSHOT"
            output shouldContain """
implementation: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-runtime:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-head:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-factory:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-cassandra:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-kafka:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-elasticsearch:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-graphite:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-influxdb:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jackson:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jms:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jakarta-ee-messaging:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-mail:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-mongodb:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-netty:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-r2dbc-jasync:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-rabbitmq:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-redis-lettuce:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-slack:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-timescaledb:<unspecified>:<unspecified>
kapt: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
        """.trimIndent()
        }

        @Test
        fun `should load all the plugins individually`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}

qalipsis {
    plugins {
        all()
    }
}

afterEvaluate {
$listDependencies
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("--stacktrace")
            val result = runner.build()

            // Then
            val output = result.output
            output shouldContain "Using QALIPSIS 0.13.a-SNAPSHOT"
            output shouldContain """
implementation: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-runtime:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-head:<unspecified>:<unspecified>
 - io.qalipsis:qalipsis-factory:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-cassandra:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-kafka:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-elasticsearch:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-graphite:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-influxdb:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jackson:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jms:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-jakarta-ee-messaging:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-mail:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-mongodb:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-netty:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-r2dbc-jasync:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-rabbitmq:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-redis-lettuce:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-slack:<unspecified>:<unspecified>
 - io.qalipsis.plugin:qalipsis-plugin-timescaledb:<unspecified>:<unspecified>
kapt: 
 - io.qalipsis:qalipsis-api-processors:<unspecified>:<unspecified>
 - (platform) io.qalipsis:qalipsis-platform:0.13.a-SNAPSHOT:<unspecified>
        """.trimIndent()
        }
    }

    @Nested
    inner class Tasks {

        @Test
        fun `should run qalipsis without configuration`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
plugins {
    id("io.qalipsis.bootstrap")
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("qalipsisRunAllScenarios", "--debug")
            val result = runner.buildAndFail()

            // Then
            val output = result.output
            output shouldContain "No enabled scenario could be found"
            output shouldContain "Executing QALIPSIS: [--autostart]"
        }


        @Test
        fun `should run qalipsis with configuration`(@TempDir tempFolder: Path) {
            // Given
            val buildFile = tempFolder.resolve(GRADLE_BUILD_FILE)
            buildFile.writeText(
                """
import io.qalipsis.gradle.bootstrap.tasks.RunQalipsis  
                    
plugins {
    id("io.qalipsis.bootstrap")
}
    
tasks {
    named<RunQalipsis>("qalipsisRunAllScenarios") {
        scenarios("scenario-1", "scenario-2")
        configuration(
            "configuration1" to "true",
            "configuration2" to "a-value"
        )
    }
}
"""
            )

            // When
            val runner = GradleRunner.create()
                .withProjectDir(tempFolder.toFile())
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("qalipsisRunAllScenarios", "--debug")
            val result = runner.buildAndFail()

            // Then
            val output = result.output
            output shouldContain "Executing QALIPSIS: [--autostart, -s, scenario-1, scenario-2, -c, configuration1=true, -c, configuration2=a-value]"
        }
    }
}

