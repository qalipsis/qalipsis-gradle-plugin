package io.qalipsis.gradle

import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.CacheControl
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondTextWriter
import io.ktor.server.response.respondText
import io.ktor.server.response.cacheControl
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.coroutines.delay
import java.net.ServerSocket
import java.time.Instant

/**
 * Mini server to send server side events over HTTP to facilitate testing.
 *
 * @author Francisca Eze
 */
object SSETestServer {

    /**
     * Starts and returns an instance of a test server capable of managing server side streams.
     */
    fun start() = server.start(wait = false)

    /**
     * Stops the SSE test server gracefully.
     */
    fun stop() = server.stop(1000, 1000)

    val port: Int = getRandomPort()

    /**
     * Gets a random available port to be used by the server.
     */
    private fun getRandomPort() = ServerSocket(0).use { socket -> socket.localPort }

    /**
     * Embedded Netty server instance.
     */
    private val server = embeddedServer(Netty, port = port) {
        install(SSE)
        routing {
            get("/scenarios") {
                call.respondText(
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
            }

            post("/scenarios") {
                val query = call.request.queryParameters["type"]
                when (query) {
                    "server-shutdown" -> call.sendSuccessAndShutdown()
                    "401-unauthorized" -> call.unauthorized()
                    "403-forbidden" -> call.forbidden()
                    "502-badGateway" -> call.badGateway()
                    "504-serviceUnavailable" -> call.gatewayTimeout()
                    "500-internalServerError" -> call.internalServerError()
                    else -> call.processSuccessResponse()
                }
            }
        }
    }

    /**
     * Returns a 200 success response with the response body as server side event streams.
     */
    private suspend fun RoutingCall.processSuccessResponse() {
        response.cacheControl(CacheControl.NoCache(null))
        respondTextWriter(contentType = ContentType.Text.EventStream) {
            repeat(6) {
                write(
                    "data: { \"text\": \"Successfully published Scenario.. $it\"," +
                            "\"timestamp\": \"${Instant.parse("2025-03-23T08:07:0${it}Z")}\"," +
                            "\"level\": \"INFO\"}\n\n"
                )
                flush()
                delay(200)
            }
        }
    }

    /**
     * Returns a success response as server side event streams for a while and then shuts down the server.
     * This simulates a scenario of the server suddenly shutting down while processing/returning a response.
     */
    private suspend fun RoutingCall.sendSuccessAndShutdown() {
        response.cacheControl(CacheControl.NoCache(null))
        respondTextWriter(contentType = ContentType.Text.EventStream) {
            repeat(3) {
                if (it == 2) {
                    server.stop(100, 100)
                }
                write(
                    "data: { \"text\": \"Successfully published Scenario.. $it\"," +
                            "\"timestamp\": \"${Instant.parse("2025-03-23T08:07:0${it}Z")}\"," +
                            "\"level\": \"INFO\"}\n\n"
                )
                flush()
                delay(200)
            }
        }
    }

    /**
     * Returns a plain text message indicating an invalid authentication as well as an
     * HTTP 401 unauthorized status.
     */
    private suspend fun RoutingCall.unauthorized() {
        response.status(HttpStatusCode.Unauthorized)
        respondText("Unauthorized: Invalid authentication", ContentType.Text.Plain)
    }

    /**
     * Returns a plain text message indicating that access is denied as well as an
     * HTTP 403 forbidden status.
     */
    private suspend fun RoutingCall.forbidden() {
        response.status(HttpStatusCode.Forbidden)
        respondText("Forbidden: Access denied", ContentType.Text.Plain)
    }

    /**
     * Returns a plain text message indicating a bad gateway as well as an
     * HTTP 502 bad gateway status.
     */
    private suspend fun RoutingCall.badGateway() {
        response.status(HttpStatusCode.BadGateway)
        respondText("Bad Gateway: Upstream server error", ContentType.Text.Plain)
    }

    /**
     * Returns a plain text message indicating that the service is unavailable as
     * well as an HTTP 504 gateway timeout status.
     */
    private suspend fun RoutingCall.gatewayTimeout() {
        response.status(HttpStatusCode.GatewayTimeout)
        respondText("Service Unavailable: Try again later", ContentType.Text.Plain)
    }

    /**
     * Returns a plain text message indicating a server error as well as an
     * HTTP 500 internal server status.
     */
    private suspend fun RoutingCall.internalServerError() {
        response.status(HttpStatusCode.InternalServerError)
        respondText("Internal Server Error: Something went wrong", ContentType.Text.Plain)
    }
}