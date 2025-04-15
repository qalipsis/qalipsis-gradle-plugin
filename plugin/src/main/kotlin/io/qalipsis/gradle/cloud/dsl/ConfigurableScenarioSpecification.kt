package io.qalipsis.gradle.cloud.dsl

/**
 * Represents a configurable scenario specification.
 *
 * @author Francisca Eze
 */
interface ConfigurableScenarioSpecification {

    /**
     * Total number of minions in the profile configuration.
     */
    var minionsCount: Int

    /**
     * Zone to percentage map to define the execution distribution of campaigns per zone.
     */
    fun zones(block: ZoneBuilder.() -> Unit)

    /**
     * Defines how the start of the minion should evolve in the scenario.
     */
    fun profile(configuration: @ScenarioMarker ExternalExecutionProfileConfiguration.() -> Unit)
}