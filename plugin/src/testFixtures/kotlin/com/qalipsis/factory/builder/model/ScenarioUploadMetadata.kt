package com.qalipsis.factory.builder.model

import io.micronaut.core.annotation.Introspected

/**
 * Metadata to describe an uploaded set of scenarios.
 *
 * @author Luis Silva
 */
@Introspected
data class ScenarioUploadMetadata(
    val dependencies: Collection<Dependency>,
    val plugins: Collection<String>,
    val platformVersion: String,
    val jarName: String,
)
