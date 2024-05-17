package io.qalipsis.gradle.cloud.model

import java.time.Instant

/**
 * Structured representation of a log event.
 */
internal data class LogMessage(val level: String, val timestamp: Instant, val text: String)