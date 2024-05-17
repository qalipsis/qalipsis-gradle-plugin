package com.qalipsis.factory.builder

/**
 * Exception thrown where there is an error not due to the code of the factory submitter,
 * but to internal configuration.
 *
 * Such an error requires the intervention of a QALIPSIS team member.
 */
internal class InternalBuildException(override val message: String) : RuntimeException()