package io.qalipsis.gradle.cloud


import io.kotest.matchers.string.shouldContain
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.server.TestEmbeddedServer
import jakarta.inject.Inject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@MicronautTest(startApplication = true, packages = ["com.qalipsis.factory.builder"])
internal open class QalipsisCloudPluginFunctionalTest {

    @Inject
    lateinit var embeddedServer: TestEmbeddedServer

    @Test
    fun `should list the scenarios and create tasks `(@TempDir tempFolder: Path) {
        // Given
        val propertiesFile = tempFolder.resolve("gradle.properties")
        propertiesFile.writeText(
            """
            qalipsis.cloud.builder.url=http://localhost:${embeddedServer.port}/scenarios
            qalipsis.cloud.builder.list=true
            qalipsis.cloud.builder.token=My-token
        """.trimIndent()
        )
        val buildFile = tempFolder.resolve("build.gradle.kts")
        buildFile.writeText(
            """
plugins {
    id("io.qalipsis.cloud")
}

"""
        )

        // When
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(tempFolder.toFile())
            .withDebug(true)
        val result = runner.build()

        // Then
        result.output shouldContain "Using QALIPSIS 0.7.d-SNAPSHOT"
    }

}
