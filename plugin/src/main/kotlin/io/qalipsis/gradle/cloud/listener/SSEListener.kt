package io.qalipsis.gradle.cloud.listener

import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.LogMessage
import io.qalipsis.gradle.cloud.tasks.Helper
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.gradle.api.logging.Logger
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

/**
 * Custom listener class for handling Server-Sent Events (SSE).
 */
class SSEListener(
    private val operationResult: CompletableFuture<Result<Unit>>,
    private val logger: Logger,
) : EventSourceListener() {

    private var errors = mutableListOf<String>()

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        val message = Helper.jsonMapper.readValue<LogMessage>(data)
        when (message.level) {
            "ERROR" -> {
                errors += message.text
                logger.error("[ERROR] {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
            }

            "DEBUG" -> logger.debug("[DEBUG] {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
            else -> logger.lifecycle("[INFO]  {} {}", convertToLocaleTime(message.timestamp), message.text.trim())
        }
    }

    private fun convertToLocaleTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS)

    override fun onOpen(eventSource: EventSource, response: Response) {
        logger.debug("Connected to the cloud server...")
        super.onOpen(eventSource, response)
    }

    override fun onClosed(eventSource: EventSource) {
        if (errors.isEmpty()) {
            operationResult.complete(Result.success(Unit))
        } else {
            operationResult.complete(
                Result.failure(RuntimeException(errors.joinToString(separator = "\n -", prefix = "\n")))
            )
        }

    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        logger.error("An error occurred while sending the scenario(s) to the cloud server")
        response?.let {
            val exception = when (response.code) {
                in setOf(401, 403) -> {
                    val errorMessage = """
                                    | There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?
                                """.trimMargin()
                    IOException(errorMessage)
                }

                in setOf(502, 504) -> {
                    IOException(
                        """
                                    The QALIPSIS server is currently not available, please try again in a moment.
                                    If the problem persists, please contact the support.
                                """.trimIndent()
                    )
                }

                else -> {
                    logger.error("An error occurred while building your factory on the remote server")

                    val body = it.body!!.string()
                    val errorMessage = runCatching {
                        Helper.jsonMapper.readValue<ErrorMessage>(body)
                    }.getOrNull()?.message.takeUnless { it.isNullOrBlank() } ?: body
                    RuntimeException(errorMessage)
                }
            }

            operationResult.complete(Result.failure(exception))
        }
        t?.let {
            logger.error("Error from the cloud server: ${t.message}")
            operationResult.complete(Result.failure(t))
        }
    }
}