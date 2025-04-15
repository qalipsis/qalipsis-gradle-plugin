package io.qalipsis.gradle.cloud.model

/**
 * Class to encapsulate the properties of an error message.
 */
internal data class ErrorMessage(val message: String, val property: String? = null)


/**
 *
 */
internal data class ErrorResponse(val errors: List<ErrorMessage>)
