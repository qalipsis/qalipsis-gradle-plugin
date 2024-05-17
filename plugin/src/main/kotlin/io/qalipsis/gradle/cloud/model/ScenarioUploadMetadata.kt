package io.qalipsis.gradle.cloud.model


/**
 * Metadata to describe an uploaded set of scenarios.
 *
 */
internal data class ScenarioUploadMetadata(
    val dependencies: Collection<Dependency>,
    val plugins: Collection<String>,
    val platformVersion: String,
    val jarName: String,
)

internal data class Dependency(
    val configuration: String,
    val group: String,
    val name: String,
    val version: String? = null,
    val classifier: String? = null,
    val category: String? = null
)