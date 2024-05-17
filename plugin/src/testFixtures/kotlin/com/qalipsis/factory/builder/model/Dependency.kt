package com.qalipsis.factory.builder.model

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

/**
 * Model of a dependency
 *
 * @author Luis Silva
 */
@Introspected
data class Dependency(
    @NotBlank val configuration: String,
    @NotBlank val group: String,
    @NotBlank val name: String,
    val version: String? = null,
    val classifier: String? = null
)