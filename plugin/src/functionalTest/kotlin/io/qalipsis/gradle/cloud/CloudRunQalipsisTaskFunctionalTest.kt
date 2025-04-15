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
import org.mockserver.model.JsonBody
import java.nio.file.Path
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CloudRunQalipsisTaskFunctionalTest : MockserverBasedTest() {

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
    fun `should run a campaign when the cloud run task is called`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.api.url=http://localhost:${mockPort}/api
            qalipsis.cloud.registry.list=false
            qalipsis.cloud.registry.token=my-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Dummy campaign configuration") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer my-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        {
                            "version": "2025-04-28T13:09:43.533Z",
                            "key": "r0g0u1piyI",
                            "creation": "2025-04-28T13:09:43.533Z",
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "scheduledMinions": null,
                            "hardTimeout": "2025-04-28T13:09:44.533Z",
                            "start": null,
                            "end": null,
                            "status": "IN_PROGRESS",
                            "configurerName": null,
                            "aborterName": "qalipsis-ten",
                            "scenarios": [
                               {
                                  "name": "My scenario 1",                        
                                  "minionsCount": 7,
                                  "version": "2025-04-28T13:09:43.533Z"
                               }
                            ],
                            "zones": ["CH", "AS"]
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
            .withArguments("qalipsisRunCampaign")
        val result = runner.build()

        // Then
        result.output shouldContain """
            Task :qalipsisRunCampaign
            Starting a new campaign with name Dummy campaign configuration
            You can retrieve the details of the campaign from the link http://localhost:$mockPort/api/campaigns/r0g0u1piyI
        """.trimIndent()
        assertEquals(TaskOutcome.SUCCESS, result.task(":qalipsisRunCampaign")?.outcome)
    }

    @Test
    fun `should run a campaign when the cloud run task is called with multiple scenarios`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.api.url=http://localhost:${mockPort}/api
            qalipsis.cloud.registry.list=false
            qalipsis.cloud.registry.token=my-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Dummy campaign configuration") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                        scenario("My scenario 2") {
                            minionsCount = 1000
                            zones {
                               "CH" to 25
                               "AS" to 31
                               "US" to 44
                            }
                            profile {
                                regular(
                                    periodInMs = 1000,
                                    minionsCountProLaunch = 100,
                                    totalMinionsCount = 2000,
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer my-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                },
                                "My scenario 2": {
                                    "minionsCount": 1000,
                                    "zones": {
                                        "CH": 25,
                                        "AS": 31,
                                        "US": 44
                                    },
                                    "executionProfile": {
                                        "profile": "REGULAR",
                                        "periodInMs": 1000,
                                        "minionsCountProLaunch": 100,
                                        "totalMinionsCount": 2000,
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        {
                            "version": "2025-04-28T13:09:43.533Z",
                            "key": "r0g0u1piyI",
                            "creation": "2025-04-28T13:09:43.533Z",
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "scheduledMinions": null,
                            "hardTimeout": "2025-04-28T13:09:44.533Z",
                            "start": null,
                            "end": null,
                            "status": "IN_PROGRESS",
                            "configurerName": null,
                            "aborterName": "qalipsis-ten",
                            "scenarios": [
                               {
                                  "name": "My scenario 1",                        
                                  "minionsCount": 7,
                                  "version": "2025-04-28T13:09:43.533Z"
                               },
                               {
                                  "name": "My scenario 2",                        
                                  "minionsCount": 1000,
                                  "version": "2025-04-28T13:09:43.533Z"
                               }
                            ],
                            "zones": ["CH", "AS", "US"]
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
            .withArguments("qalipsisRunCampaign")
        val result = runner.build()

        // Then
        result.output shouldContain """
            Task :qalipsisRunCampaign
            Starting a new campaign with name Dummy campaign configuration
            You can retrieve the details of the campaign from the link http://localhost:$mockPort/api/campaigns/r0g0u1piyI
        """.trimIndent()
        assertEquals(TaskOutcome.SUCCESS, result.task(":qalipsisRunCampaign")?.outcome)
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `should fail when there is an issue with the auth token`(statusCode: Int, @TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
                qalipsis.cloud.api.url=http://localhost:${mockPort}/api
                qalipsis.cloud.registry.list=false
                qalipsis.cloud.registry.token=wrong-token
            """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Dummy campaign configuration") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer wrong-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
        ).respond(
            response()
                .withStatusCode(statusCode)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisRunCampaign")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Execution failed for task ':qalipsisRunCampaign'.
            > java.io.IOException:  There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write campaigns?
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisRunCampaign")?.outcome)
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
                qalipsis.cloud.api.url=http://localhost:${mockPort}/api
                qalipsis.cloud.registry.list=false
                qalipsis.cloud.registry.token=wrong-token
            """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Dummy campaign configuration") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer wrong-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
        ).respond(
            response()
                .withStatusCode(statusCode)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisRunCampaign")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Execution failed for task ':qalipsisRunCampaign'.
            > java.io.IOException:  The QALIPSIS server is currently not available, and the campaign could not be started.
               Please try again in a moment.
               If the problem persists, please contact the support.
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisRunCampaign")?.outcome)
    }

    @Test
    fun `should return the right error message for any other error that occurs in creating campaigns`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
                qalipsis.cloud.api.url=http://localhost:${mockPort}/api
                qalipsis.cloud.registry.list=false
                qalipsis.cloud.registry.token=wrong-token
            """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Dummy campaign configuration") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer wrong-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Dummy campaign configuration",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
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
            .withArguments("qalipsisRunCampaign")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Service is temporarily unavailable. Please try again later.
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisRunCampaign")?.outcome)
    }

    @Test
    fun `should return the right error message when there is a validation error from creating campaigns`(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
                qalipsis.cloud.api.url=http://localhost:${mockPort}/api
                qalipsis.cloud.registry.list=false
                qalipsis.cloud.registry.token=wrong-token
            """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
                import io.qalipsis.gradle.cloud.tasks.CloudRunQalipsis  
                
                plugins {
                    id("io.qalipsis.cloud")
                    java
                }
                tasks.register<CloudRunQalipsis>("qalipsisRunCampaign") {
                    campaign("Du") {
                        speedFactor.set(2.0)
                        startOffsetMs.set(2000)
                        campaignTimeout.set("PT1H30M")
                        hardTimeout.set(false)
                        scenario("My scenario 1") {
                            minionsCount = 7
                            zones {
                               "CH" to 45
                               "AS" to 55
                            }
                            profile {
                                more(
                                    periodMs = 11, 
                                    minionsCountProLaunchAtStart = 23, 
                                    multiplier = 2.0, 
                                    maxMinionsCountProLaunch = 7
                                )
                            }
                        }
                    }
                }
            """.trimIndent()
        )
        mockServerClient.`when`(
            request()
                .withPath("/api/campaigns")
                .withMethod("POST")
                .withHeader(Header("Authorization", "Bearer wrong-token"))
                .withBody(
                    JsonBody.json(
                        """
                            {
                            "name": "Du",
                            "speedFactor": 2.0,
                            "startOffsetMs": 2000,
                            "timeout": "PT1H30M",
                            "hardTimeout": false,
                            "scenarios": {
                                "My scenario 1": {
                                    "minionsCount": 7,
                                    "zones": {
                                        "CH": 45,
                                        "AS": 55
                                    },
                                    "executionProfile": {
                                        "profile": "PROGRESSING_VOLUME",
                                        "periodMs": 11,
                                        "minionsCountProLaunchAtStart": 23,
                                        "multiplier": 2.0,
                                        "maxMinionsCountProLaunch": 7
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                )
        ).respond(
            response()
                .withStatusCode(400)
                .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(
                    """
                        {"errors":[{"property":"campaign.name","message":"size must be between 3 and 300"}]}     
                    """.trimIndent()
                )
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
            .withArguments("qalipsisRunCampaign")
        val result = runner.buildAndFail()

        // Then
        result.output shouldContain """
            Property campaign.name size must be between 3 and 300
        """.trimIndent()
        assertEquals(TaskOutcome.FAILED, result.task(":qalipsisRunCampaign")?.outcome)
    }
}