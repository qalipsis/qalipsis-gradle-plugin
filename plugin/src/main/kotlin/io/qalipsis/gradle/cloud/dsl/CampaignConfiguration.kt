package io.qalipsis.gradle.cloud.dsl

/**
 * Represents the configuration settings for a campaign.
 *
 * @author Francisca Eze
 */
data class CampaignConfiguration(
    val name: String,
    val speedFactor: Double = 1.0,
    val startOffsetMs: Long = 1000,
    val timeout: String? = null,
    val hardTimeout: Boolean? = null,
    val scenarios: Map<String, ScenarioRequest>?
)