package io.qalipsis.gradle.cloud.dsl

/**
 * Model to create scenario configuration to start a new campaign into QALIPSIS.
 *
 * @author Francisca Eze
 */
data class ScenarioRequest(
    val minionsCount: Int,
    val zones: Map<String, Int>? = null,
    val executionProfile: ExternalExecutionProfileConfiguration? = null
)