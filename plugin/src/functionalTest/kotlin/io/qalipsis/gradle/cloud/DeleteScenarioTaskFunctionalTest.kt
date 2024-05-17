package io.qalipsis.gradle.cloud

import io.kotest.matchers.string.shouldContain
import io.qalipsis.gradle.MockserverBasedTest
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockserver.client.MockServerClient
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import java.nio.file.Path
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeleteScenarioTaskFunctionalTest : MockserverBasedTest() {

    private var mockPort: Int = -1

    private lateinit var mockServerClient: MockServerClient

    @BeforeAll
    fun init() {
        mockPort = container.firstMappedPort
        mockServerClient = MockServerClient(container.host, mockPort)
    }

    @AfterEach
    fun resetMockserver() {
        mockServerClient.reset()
    }

    @Test
    fun `delete task should delete the scenario in the server`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=write-scenario-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios")
                .withMethod("GET")
                .withHeader(Header("Authorization", "Bearer write-scenario-token"))
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        [
                          {
                            "name": "scenario-1",
                            "description": "First dummy scenario",
                            "version": "0.0.1"
                          },
                          {
                            "name": "scenario-2",
                            "description": "Second dummy scenario",
                            "version": "0.0.1"
                          }
                        ]
                    """.trimIndent()
                )
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios/scenario-1")
                .withMethod("DELETE")
                .withHeader(Header("Authorization", "Bearer write-scenario-token"))
        ).respond(
            response()
                .withStatusCode(200)
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios/scenario-2")
                .withMethod("DELETE")
                .withHeader(Header("Authorization", "Bearer write-scenario-token"))
        ).respond(
            response()
                .withStatusCode(200)
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudDeleteScenarioScenario1", "qalipsisCloudDeleteScenarioScenario2")
        val result = runner.build()

        // Then
        result.output shouldContain """
            Task :qalipsisCloudDeleteScenarioScenario1
            Deleting the scenario scenario-1 (First dummy scenario)
        """.trimIndent()
        result.output shouldContain """
            Task :qalipsisCloudDeleteScenarioScenario2
            Deleting the scenario scenario-2 (Second dummy scenario)
        """.trimIndent()

        assertEquals(TaskOutcome.SUCCESS, result.task(":qalipsisCloudDeleteScenarioScenario1")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":qalipsisCloudDeleteScenarioScenario2")?.outcome)
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 504])
    fun `delete scenario task should not work when there is a server error and it should return the right error message`(
        statusCode: Int,
        @TempDir tempFolder: Path
    ) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios")
                .withMethod("GET")
                .withHeader(Header("Authorization", "Bearer my-token"))
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        [
                          {
                            "name": "scenario-1",
                            "description": "First dummy scenario",
                            "version": "0.0.1"
                          },
                          {
                            "name": "scenario-2",
                            "description": "Second dummy scenario",
                            "version": "0.0.1"
                          }
                        ]
                    """.trimIndent()
                )
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios/scenario-1")
                .withMethod("DELETE")
                .withHeader(Header("Authorization", "Bearer my-token"))
        ).respond(
            response()
                .withStatusCode(statusCode)
                .withBody("Bad Gateway")
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudDeleteScenarioScenario1")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Task :qalipsisCloudDeleteScenarioScenario1 FAILED
            Deleting the scenario scenario-1 (First dummy scenario)
       """.trimIndent()
        result.output shouldContain """
            java.io.IOException:  The QALIPSIS server is currently not available, and the scenario could not be deleted.
               Please try again in a moment.
               If the problem persists, please contact the support.
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisCloudDeleteScenarioScenario1")?.outcome)
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `delete scenario task should not work when the request contain no auth header`(
        statusCode: Int,
        @TempDir tempFolder: Path
    ) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=my-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios")
                .withMethod("GET")
                .withHeader(Header("Authorization", "Bearer my-token"))
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        [
                          {
                            "name": "scenario-1",
                            "description": "First dummy scenario",
                            "version": "0.0.1"
                          },
                          {
                            "name": "scenario-2",
                            "description": "Second dummy scenario",
                            "version": "0.0.1"
                          }
                        ]
                    """.trimIndent()
                )
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios/scenario-1")
                .withMethod("DELETE")
        ).respond(
            response()
                .withStatusCode(statusCode)
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudDeleteScenarioScenario1")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Task :qalipsisCloudDeleteScenarioScenario1 FAILED
            Deleting the scenario scenario-1 (First dummy scenario)
       """.trimIndent()
        result.output shouldContain """
            There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisCloudDeleteScenarioScenario1")?.outcome)
    }

    @Test
    fun `delete scenario task should return the right error message when any other error occurs`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=write-scenario-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios")
                .withMethod("GET")
                .withHeader(Header("Authorization", "Bearer write-scenario-token"))
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        [
                          {
                            "name": "scenario-1",
                            "description": "First dummy scenario",
                            "version": "0.0.1"
                          },
                          {
                            "name": "scenario-2",
                            "description": "Second dummy scenario",
                            "version": "0.0.1"
                          }
                        ]
                    """.trimIndent()
                )
        )
        mockServerClient.`when`(
            request()
                .withPath("/scenarios/scenario-1")
                .withMethod("DELETE")
                .withHeader(Header("Authorization", "Bearer write-scenario-token"))
        ).respond(
            response()
                .withStatusCode(503)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                    {
                        "error": "Service Unavailable", 
                        "message": "Service is temporarily unavailable at the moment. Please try again later."
                    }
                """.trimIndent()
                )
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisCloudDeleteScenarioScenario1")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Service is temporarily unavailable at the moment. Please try again later.
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisCloudDeleteScenarioScenario1")?.outcome)
    }
}
