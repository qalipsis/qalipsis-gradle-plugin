package io.qalipsis.gradle.cloud.model


/**
 *  Model of a scenario
 */
internal data class Scenario(
    val name: String,
    val description: String? = null,
    val version: String
)
