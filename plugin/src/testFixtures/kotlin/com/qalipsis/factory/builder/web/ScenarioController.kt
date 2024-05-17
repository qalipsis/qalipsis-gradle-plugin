package com.qalipsis.factory.builder.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.qalipsis.factory.builder.InternalBuildException
import com.qalipsis.factory.builder.model.LogMessage
import com.qalipsis.factory.builder.model.Scenario
import com.qalipsis.factory.builder.model.ScenarioUploadMetadata
import com.qalipsis.factory.builder.service.LogSinksFeedbackEmitter
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Part
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.sse.Event
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Sinks
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import kotlin.concurrent.thread


/**
 * Controller for REST calls to scenario operations
 *
 * @author Luis Silva
 */
@Controller("/scenarios")
internal open class ScenarioController(private val objectMapper: ObjectMapper) {

    @Post(consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_EVENT_STREAM])
    open fun buildScenario(
        @Part("metadata") @NotBlank md: String,
        @Part("archive") archive: CompletedFileUpload
    ): Publisher<Event<LogMessage>> {
        val metadata = objectMapper.readValue<ScenarioUploadMetadata>(md)

        // When there is a dependency with name containing bandwith-error, it generates a bandwith-error.
        if (metadata.dependencies.any { it.name.contains("bandwith-error") }) {
            throw HttpStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "Exceeded limit")
        }
        val response = Sinks.many().unicast().onBackpressureBuffer<LogMessage>()
        val feedbackEmitter = LogSinksFeedbackEmitter(UUID.randomUUID().toString(), response)
        thread {
            feedbackEmitter.info("The archive ${metadata.jarName} has a size of ${archive.size} bytes")
            Thread.sleep(1000)
            feedbackEmitter.info("The list of dependencies is : ${metadata.dependencies}")
            Thread.sleep(1000)
            feedbackEmitter.info("The list of plugins is : ${metadata.plugins}")
            Thread.sleep(1000)

            // When there is a dependency with name containing error, it generates an error.
            if (metadata.dependencies.any { it.name.contains("error") }) {
                feedbackEmitter.error(InternalBuildException("This is an error"))
            } else {
                feedbackEmitter.info("The platform to use is : ${metadata.platformVersion}")
            }
            Thread.sleep(1000)
            feedbackEmitter.info("This is the latest message")
            feedbackEmitter.complete()
        }

        return response.asFlux().map { Event.of(it) }
    }

    @Get
    open fun list(): Collection<Scenario> {
        return listOf(
            Scenario("my-scenario-1", "It does something", "0.1"),
            Scenario("my-scenario-2", version = "0.2")
        )
    }

    @Delete("/{scenarios}")
    @Status(HttpStatus.NO_CONTENT)
    open fun delete(
        scenarios: @NotEmpty Set<@NotBlank String>
    ) {
        if (scenarios.any { it == "bandwith-error" }) {
            throw HttpStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "Exceeded limit")
        }
        logger.info { "Deleting the scenarios $scenarios" }
    }

    private companion object {

        val logger = KotlinLogging.logger { }

    }
}
