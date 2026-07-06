package io.qalipsis.gradle

import org.junit.jupiter.api.extension.ExtendWith
import org.mockserver.integration.ClientAndServer
import org.mockserver.junit.jupiter.MockServerExtension

/**
 * Returns a containerized instance of MockServer for testing HTTP requests and responses.
 */
@ExtendWith(MockServerExtension::class)
abstract class MockserverBasedTest(
    protected val mockServer: ClientAndServer,
)