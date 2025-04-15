package io.qalipsis.gradle.cloud.dsl

/**
 * Custom implementation of the [ConfigurableScenarioSpecification] interface.
 *
 * @author Francisca Eze
 */
internal class ConfigurableScenarioSpecificationImpl : ConfigurableScenarioSpecification {

    override var minionsCount: Int = 1

    private var zones: MutableMap<String, Int> = mutableMapOf()

    private var executionProfileConfiguration: ExternalExecutionProfileConfiguration? = null

    override fun zones(block: ZoneBuilder.() -> Unit) {
        zones = ZoneBuilder().apply(block).zones()
    }

    override fun profile(configuration: ExternalExecutionProfileConfiguration.() -> Unit) {
        val config = ExternalExecutionProfileConfigurationImpl()
        config.configuration()
        config.profile
        executionProfileConfiguration = config
    }

    fun toScenarioRequest(): ScenarioRequest {
        return if(zones.isEmpty()) {
            ScenarioRequest(
                minionsCount = minionsCount,
                zones = null,
                executionProfile = executionProfileConfiguration
            )
        } else {
            val zoneDistribution = zones.values.sum()
            require(zoneDistribution == 100) {"Distribution by zone should sum up to a 100%"}
            ScenarioRequest(
                minionsCount = minionsCount,
                zones = zones,
                executionProfile = executionProfileConfiguration
            )
        }
    }

    infix fun String.to(value: Int) {
        zones[this] = value
    }
}

/**
 * Builder class for constructing a zone distribution map. Allows definition of zone-to-value mappings using a DSL syntax.
 */
class ZoneBuilder {
    private val zoneDistribution = mutableMapOf<String, Int>()

    infix fun String.to(value: Int) {
        zoneDistribution[this] = value
    }

    fun zones(): MutableMap<String, Int> = zoneDistribution
}
