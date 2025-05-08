package io.qalipsis.gradle.cloud.dsl

/**
 * Represents a configurable scenario specification.
 *
 * @author Francisca Eze
 */
interface ConfigurableScenarioSpecification {

    /**
     * Defines how the start of the minion should evolve in the scenario.
     */
    var minionsCount: Int

    fun zones(block: ZoneBuilder.() -> Unit)

    /**
     * Defines how the start of the minion should evolve in the scenario.
     */
    fun profile(configuration: @ScenarioMarker ExternalExecutionProfileConfiguration.() -> Unit)
}