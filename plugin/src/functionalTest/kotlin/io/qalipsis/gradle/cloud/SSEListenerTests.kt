package io.qalipsis.gradle.cloud

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.qalipsis.gradle.cloud.listener.SSEListener
import okhttp3.sse.EventSource
import java.util.concurrent.CompletableFuture
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.ZoneId
import java.util.stream.Stream
import kotlin.test.assertTrue

@TestInstance(Lifecycle.PER_CLASS)
internal class SSEListenerTests {

    @MockK
    lateinit var logger: Logger

    private var operationResult = CompletableFuture<Result<Unit>>()

    private val eventSource = mockk<EventSource>()

    @InjectMockKs
    private lateinit var sseListener: SSEListener

    @BeforeAll
    fun initMocks() {
        MockKAnnotations.init(this)
    }

    @BeforeEach
    fun initializeMocks() {
        operationResult = spyk(CompletableFuture())
        sseListener = spyk(SSEListener(operationResult, logger), recordPrivateCalls = true)
    }

    @Test
    fun `should receive valid events and process them`() {
        // given
        val request = mockk<Request>(relaxed = true)
        val data = """
            {
                "text": "Successfully published Scenario.. 0",
                "timestamp": "${Instant.parse("2025-03-23T08:07:00Z")}",
                "level": "INFO"
            }
        """.trimIndent()
        val data2 = """
            {
                "text": "Successfully published Scenario.. 2",
                "timestamp": "${Instant.parse("2025-03-23T08:07:02Z")}",
                "level": "DEBUG"
            }
        """.trimIndent()
        val data3 = """
            {
                "text": "There was an issue in publishing Scenario.. 3",
                "timestamp": "${Instant.parse("2025-03-23T08:07:03Z")}",
                "level": "ERROR"
            }
        """.trimIndent()
        every { logger.lifecycle(any<String>(), any(), any()) } answers { callOriginal() }
        every { logger.debug(any<String>(), any(), any()) } answers { callOriginal() }
        every { logger.error(any<String>(), any(), any()) } answers { callOriginal() }
        every { eventSource.request() } returns request

        // when
        sseListener.onEvent(eventSource, "", "", data)

        // then
        verify {
            logger.lifecycle(
                "[INFO]  {} {}",
                Instant.parse("2025-03-23T08:07:00Z").atZone(ZoneId.systemDefault()).toLocalDateTime(),
                "Successfully published Scenario.. 0"
            )
        }

        // when
        sseListener.onEvent(eventSource, "", "", data2)

        // then
        verify {
            logger.debug(
                "[DEBUG] {} {}",
                Instant.parse("2025-03-23T08:07:02Z").atZone(ZoneId.systemDefault()).toLocalDateTime(),
                "Successfully published Scenario.. 2"
            )
        }

        // when
        sseListener.onEvent(eventSource, "", "", data3)

        // then
        verify {
            logger.error(
                "[ERROR] {} {}",
                Instant.parse("2025-03-23T08:07:03Z").atZone(ZoneId.systemDefault()).toLocalDateTime(),
                "There was an issue in publishing Scenario.. 3"
            )
        }
    }

    @Test
    fun `onOpen should log event and handle call to super`() {
        // given
        val response = mockk<Response>(relaxed = true)
        every { logger.debug(any<String>()) } answers { callOriginal() }

        // when
        sseListener.onOpen(eventSource, response)

        // then
        verify { logger.debug("Connected to the cloud server...") }
        verify { sseListener.onOpen(eventSource, response) }
    }

    @Test
    fun `should properly handle closing of the listener when there are no errors`() {
        // given

        // when
        sseListener.onClosed(eventSource)

        // then
        verify { operationResult.complete(Result.success(Unit)) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isSuccess }
    }

    @Test
    fun `should properly handle closing of the listener when there are errors`() {
        // given
        val request = mockk<Request>(relaxed = true)
        val data = "{ \"text\": \"There was an issue in publishing Scenario.. 3\"," +
                "\"timestamp\": \"${Instant.parse("2025-03-23T08:07:03Z")}\"," +
                "\"level\": \"ERROR\"}\n\n"
        every { eventSource.request() } returns request
        every { logger.error(any<String>(), any(), any()) } answers { callOriginal() }
        sseListener.onEvent(eventSource, "", "", data)

        // when
        sseListener.onClosed(eventSource)

        // then
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
        assertThat(operationResult.get().exceptionOrNull()?.message)
            .isEqualTo("\nThere was an issue in publishing Scenario.. 3")
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `should properly handle failure scenarios when there is a 401 or 403 failure and the throwable is null`(
        statusCode: Int
    ) {
        // given
        val response = mockk<Response>(relaxed = true)
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns statusCode

        // when
        sseListener.onFailure(eventSource, null, response)

        // then
        assertThat(
            operationResult.get().exceptionOrNull()?.message
        ).isEqualTo(" There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?")
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `should properly handle failure scenarios when there is a 401 or 403 failure and the throwable is not null`(
        statusCode: Int
    ) {
        // given
        val throwable = mockk<Throwable>()
        val response = mockk<Response>(relaxed = true)
        every { throwable.message } answers { "Authentication failed." }
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns statusCode

        // when
        sseListener.onFailure(eventSource, throwable, response)

        // then
        assertThat(
            operationResult.get().exceptionOrNull()?.message
        ).isEqualTo(" There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write scenarios?")
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
        verify { logger.error("Error from the cloud server: Authentication failed.") }
        verify(exactly = 2) { operationResult.complete(withArg { it.isFailure }) }
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 504])
    fun `should properly handle failure scenarios when there is a 502 or 504 failure and the throwable is null`(
        statusCode: Int
    ) {
        // given
        val response = mockk<Response>(relaxed = true)
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns statusCode

        // when
        sseListener.onFailure(eventSource, null, response)

        // then
        assertThat(operationResult.get().exceptionOrNull()?.message)
            .isEqualTo(
                """
                The QALIPSIS server is currently not available, please try again in a moment.
                If the problem persists, please contact the support.
            """.trimIndent()
            )
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 504])
    fun `should properly handle failure scenarios when there is a 502 or 504 failure and the throwable is not null`(
        statusCode: Int
    ) {
        // given
        val throwable = mockk<Throwable>()
        val response = mockk<Response>(relaxed = true)
        every { throwable.message } answers { "Internal server error." }
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns statusCode

        // when
        sseListener.onFailure(eventSource, throwable, response)

        // then
        assertThat(operationResult.get().exceptionOrNull()?.message)
            .isEqualTo(
                """
                The QALIPSIS server is currently not available, please try again in a moment.
                If the problem persists, please contact the support.
            """.trimIndent()
            )
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
        verify { logger.error("Error from the cloud server: Internal server error.") }
        verify(exactly = 2) { operationResult.complete(withArg { it.isFailure }) }
    }

    @ParameterizedTest
    @MethodSource("failureCodeAndMessage")
    fun `should properly handle failure scenarios for failures with a null response`(failureCodeAndMessage: Pair<Int, String>) {
        // given
        val response = mockk<Response>(relaxed = true)
        val throwable = mockk<Throwable>()
        val message = failureCodeAndMessage.second
        every { throwable.message } answers { message }
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns failureCodeAndMessage.first

        // when
        sseListener.onFailure(eventSource, throwable, null)

        // then
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
        verify { logger.error("Error from the cloud server: $message") }

        // when
        sseListener.onFailure(eventSource, null, null)

        // then
        verify { logger.error(ERROR_ON_SCENARIO_END) }
    }

    @ParameterizedTest
    @MethodSource("otherFailuresCodeAndMessage")
    fun `should properly handle failure cases for their status codes when there is no throwable`(failureCodeAndMessage: Pair<Int, String>) {
        // given
        val response = mockk<Response>(relaxed = true)
        val body = failureCodeAndMessage.second
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns failureCodeAndMessage.first
        every { response.body } returns body.toResponseBody(null)

        // when
        sseListener.onFailure(eventSource, null, response)

        // then
        assertThat(operationResult.get().exceptionOrNull()?.message)
            .isEqualTo(
                """
                    $body
                """.trimIndent()
            )
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        verify { logger.error("An error occurred while building your factory on the remote server") }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
    }

    @ParameterizedTest
    @MethodSource("otherFailuresCodeAndMessage")
    fun `should properly handle failure cases for their status codes when there is throwable`(failureCodeAndMessage: Pair<Int, String>) {
        // given
        val throwable = mockk<Throwable>()
        val body = failureCodeAndMessage.second
        val response = mockk<Response>(relaxed = true)
        every { throwable.message } answers { "Service Unavailable." }
        every { logger.error(any<String>()) } answers { callOriginal() }
        every { response.code } returns failureCodeAndMessage.first
        every { response.body } returns body.toResponseBody(null)

        // when
        sseListener.onFailure(eventSource, throwable, response)

        // then
        assertThat(operationResult.get().exceptionOrNull()?.message)
            .isEqualTo(
                """
                    $body
                """.trimIndent()
            )
        verify { logger.error(ERROR_ON_SCENARIO_END) }
        verify { logger.error("An error occurred while building your factory on the remote server") }
        verify { logger.error("Error from the cloud server: Service Unavailable.") }
        assertTrue { operationResult.isDone }
        assertTrue { operationResult.get().isFailure }
        verify(exactly = 2) { operationResult.complete(withArg { it.isFailure }) }
    }

    companion object {

        const val ERROR_ON_SCENARIO_END = "An error occurred while sending the scenario(s) to the cloud server"

        @JvmStatic
        fun failureCodeAndMessage(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(401 to "Authentication failed"),
                Arguments.of(403 to "Authorization error"),
                Arguments.of(502 to "Bad Gateway"),
                Arguments.of(504 to "Gateway Timeout"),
                Arguments.of(500 to "Internal server error"),
                Arguments.of(503 to "Service Unavailable"),
            )
        }

        @JvmStatic
        fun otherFailuresCodeAndMessage(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(500 to "Internal Server error"),
                Arguments.of(503 to "Service Unavailable")
            )
        }
    }

}