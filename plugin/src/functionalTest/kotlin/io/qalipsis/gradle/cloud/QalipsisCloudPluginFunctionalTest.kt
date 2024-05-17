package io.qalipsis.gradle.cloud

import io.kotest.matchers.string.shouldContain
import io.qalipsis.gradle.MockserverBasedTest
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
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
internal class QalipsisCloudPluginFunctionalTest : MockserverBasedTest() {

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
    fun `should list the scenarios and create tasks`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
            qalipsis.cloud.registry.token=My-token
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
                .withHeader(Header("Authorization", "Bearer My-token"))
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

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("tasks", "--all")
        val result = runner.build()

        // Then
        result.output shouldContain """
            Listing the scenarios from the cloud environment
            Scenarios currently present:
              scenario-1: First dummy scenario (version 0.0.1)
              scenario-2: Second dummy scenario (version 0.0.1)
        """.trimIndent()
        assertTrue(result.output.contains("Qalipsis cloud tasks"))
        assertTrue(result.output.contains("qalipsisPublish"))
        assertTrue(result.output.contains("qalipsisCloudDeleteScenarioScenario1"))
        assertTrue(result.output.contains("qalipsisCloudDeleteScenarioScenario2"))
    }

    @Test
    fun `should fail when the qalipsis cloud token property is not configured`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.registry.url=http://localhost:${mockPort}/scenarios
            qalipsis.cloud.registry.list=true
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
                .withStatusCode(400)
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("tasks", "--all")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            The property 'qalipsis.cloud.registry.token' is not set. 
                   1. Create a token on your QALIPSIS GUI or with the API, with the permissions `read:scenario` and `write:scenario`.
                   2. Add its value to your Gradle build as the property `qalipsis.cloud.registry.token`.
       """.trimIndent()
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `should fail when there is an issue with the auth token`(statusCode: Int, @TempDir tempFolder: Path) {
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
            .withArguments()
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
           There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to read scenarios?
        """.trimIndent()

    }

    @ParameterizedTest
    @ValueSource(ints = [502, 504])
    fun `should return the right error message when there is a server error`(
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
                .withStatusCode(statusCode)
                .withBody("Gateway Timeout")
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("tasks", "--all")
        val result = runner.build()

        // Then
        result.output shouldContain """
            The QALIPSIS server is currently not available, and the list of existing scenarios cannot be retrieved.
             Please try again in a moment.
             If the problem persists, please contact the support.
       """.trimIndent()
        assertTrue(result.output.contains("Qalipsis cloud tasks"))
        assertTrue(result.output.contains("qalipsisPublish"))
    }

    @Test
    fun `should return the right error message for when any other error occurs while listing scenarios`(@TempDir tempFolder: Path) {
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
                .withStatusCode(503)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                    {
                        "error": "Service Unavailable", 
                        "message": "Service is temporarily unavailable. Please try again later."
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
            .withArguments("tasks", "--all")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Service is temporarily unavailable. Please try again later.
        """.trimIndent()
    }

}
