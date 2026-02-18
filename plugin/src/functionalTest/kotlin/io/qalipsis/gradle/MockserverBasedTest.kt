package io.qalipsis.gradle

import kotlin.math.pow
import org.testcontainers.mockserver.MockServerContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Returns a containerized instance of MockServer for testing HTTP requests and responses.
 */
@Testcontainers
abstract class MockserverBasedTest {

    companion object {

        @Container
        @JvmStatic
        val container = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))
            .withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory((512 * 1024.0.pow(2)).toLong()).withCpuCount(2)
            }
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withEnv("action.destructive_requires_name", "false")
            .withEnv("xpack.security.enabled", "false")
    }


}