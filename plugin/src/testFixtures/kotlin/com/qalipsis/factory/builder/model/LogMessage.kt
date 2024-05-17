package com.qalipsis.factory.builder.model

import io.micronaut.core.annotation.Introspected
import java.time.Instant

/**
 * Structured representation of a log event.
 */
@Introspected
data class LogMessage(val level: String, val timestamp: Instant = Instant.now(), val text: String)