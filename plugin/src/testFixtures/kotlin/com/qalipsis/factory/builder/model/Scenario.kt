package com.qalipsis.factory.builder.model

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

/**
 *  Model of a scenario
 *
 * @author Luis Silva
 */
@Introspected
data class Scenario(
    @NotBlank val name: String,
    val description: String? = null,
    @NotBlank val version: String
)
