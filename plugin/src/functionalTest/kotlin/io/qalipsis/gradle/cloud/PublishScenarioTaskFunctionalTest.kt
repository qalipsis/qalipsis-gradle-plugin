package io.qalipsis.gradle.cloud

import io.kotest.matchers.string.shouldContain
import io.qalipsis.gradle.SSETestServer
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.stream.Stream
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PublishScenarioTaskFunctionalTest {

    private val port = SSETestServer.port

    @BeforeAll
    fun init() {
        SSETestServer.start()
    }

    @AfterAll
    fun tearDown() {
        SSETestServer.stop()
    }

    @Test
    fun `should publish scenario when the task is called`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${port}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    id("io.qalipsis.bootstrap")
                    java
                }
            """.trimIndent()
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudPublish")
        val result = runner.build()

        // Then
        result.output shouldContain """
            Task :qalipsisPublish
            Sending the scenario(s) to the cloud
        """.trimIndent()
        result.output shouldContain "[INFO]  $testInstant Successfully published Scenario.. 0"
        result.output shouldContain "[INFO]  $testInstant:01 Successfully published Scenario.. 1"
        result.output shouldContain "[INFO]  $testInstant:02 Successfully published Scenario.. 2"
        result.output shouldContain "[INFO]  $testInstant:03 Successfully published Scenario.. 3"
        result.output shouldContain "[INFO]  $testInstant:04 Successfully published Scenario.. 4"
        result.output shouldContain "[INFO]  $testInstant:05 Successfully published Scenario.. 5"

        result.output shouldContain """
            Scenario(s) now ready for use in the cloud, refresh your project to list them
        """.trimIndent()
        assertEquals(TaskOutcome.SUCCESS, result.task(":qalipsisPublish")?.outcome)
    }

    @Test
    fun `should properly handle failures when server suddenly shuts down while publishing scenarios`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${port}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
            qalipsis.cloud.registry.parameters=type=server-shutdown
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    id("io.qalipsis.bootstrap")
                    java
                }
            """.trimIndent()
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudPublish")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """[INFO]  $testInstant Successfully published Scenario.. 0""".trimIndent()
        result.output shouldContain """[INFO]  $testInstant:01 Successfully published Scenario.. 1""".trimIndent()
        result.output shouldContain """An error occurred while sending the scenario(s) to the cloud server""".trimIndent()
        result.output shouldContain """An error occurred while building your factory on the remote server""".trimIndent()
        result.output shouldContain """Error from the cloud server: null""".trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisPublish")?.outcome)
    }

    @ParameterizedTest
    @MethodSource("failureTypeAndMessage")
    fun `should properly handle 401, 403, 502, 504 types of failures that occur on connecting to the server`(failureCodeAndMessage: Pair<String, String>, @TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${port}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
            qalipsis.cloud.registry.parameters=type=${failureCodeAndMessage.first}
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    id("io.qalipsis.bootstrap")
                    java
                }
            """.trimIndent()
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudPublish")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """An error occurred while sending the scenario(s) to the cloud server""".trimIndent()
        result.output shouldContain """
            ${failureCodeAndMessage.second}
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisPublish")?.outcome)
    }

    @Test
    fun `should properly handle other failures that occur on connecting to the server`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${port}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
            qalipsis.cloud.registry.parameters=type=500-internalServerError
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    id("io.qalipsis.bootstrap")
                    java
                }
            """.trimIndent()
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudPublish")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """An error occurred while sending the scenario(s) to the cloud server""".trimIndent()
        result.output shouldContain """An error occurred while building your factory on the remote server""".trimIndent()
        result.output shouldContain """Internal Server Error: Something went wrong""".trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisPublish")?.outcome)
    }

    companion object {

        private val testInstant = Instant.parse("2025-03-23T08:07:00Z").convertToLocaleTime()

        @JvmStatic
        fun failureTypeAndMessage(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("401-unauthorized" to """
                    java.io.IOException:  There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?
                """.trimIndent()),
                Arguments.of("403-forbidden" to """
                    java.io.IOException:  There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?
                """.trimIndent()),
                Arguments.of("502-badGateway" to """
                    java.io.IOException: The QALIPSIS server is currently not available, please try again in a moment.
                                  If the problem persists, please contact the support.
                """.trimIndent()),
                Arguments.of("504-serviceUnavailable" to """
                    java.io.IOException: The QALIPSIS server is currently not available, please try again in a moment.
                                  If the problem persists, please contact the support.
                """.trimIndent())
            )
        }

        private fun Instant.convertToLocaleTime(): LocalDateTime =
            LocalDateTime.ofInstant(this, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS)
    }

}
