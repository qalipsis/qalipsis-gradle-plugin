package io.qalipsis.gradle.cloud.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Base class for defining campaign configuration specifications.
 *
 * @author Francisca Eze
 */
@CampaignMarker
interface CampaignConfigurationSpecification {

    val campaignName: String
    val speedFactor: Property<Double>
    val startOffsetMs: Property<Long>
    val campaignTimeout: Property<String>
    val hardTimeout: Property<Boolean>
    fun scenario(name: String, configurationBlock: @ScenarioMarker ConfigurableScenarioSpecification.() -> Unit)
}

/**
 *  Implementation of [CampaignConfigurationSpecification] used to define and configure
 *  campaign properties.
 *
 *  @author Francisca Eze
 */
internal class CampaignConfigurationSpecificationImpl(name: String, objectFactory: ObjectFactory) : CampaignConfigurationSpecification {

    private val _scenarios: MutableMap<String, ScenarioRequest> = mutableMapOf()

    override val campaignName: String = name
    override val speedFactor: Property<Double> = objectFactory.property(Double::class.java)
    override val startOffsetMs: Property<Long> = objectFactory.property(Long::class.java)
    override val campaignTimeout: Property<String> = objectFactory.property(String::class.java)
    override val hardTimeout: Property<Boolean> = objectFactory.property(Boolean::class.java)
    val scenarios: Map<String, ScenarioRequest> = _scenarios
    override fun scenario(name: String, configurationBlock: ConfigurableScenarioSpecification.() -> Unit) {
        val configuration = ConfigurableScenarioSpecificationImpl()
        configuration.configurationBlock()
        _scenarios[name] = configuration.toScenarioRequest()
    }

    /**
     * Converts the campaign dsl specification to a CampaignConfiguration type.
     */
    internal fun toCampaignConfiguration(): CampaignConfiguration {
        return CampaignConfiguration(
            name = campaignName,
            speedFactor = speedFactor.get(),
            startOffsetMs = startOffsetMs.get(),
            timeout = campaignTimeout.getOrElse(null),
            hardTimeout = hardTimeout.getOrElse(null),
            scenarios = scenarios.takeIf { it.isNotEmpty() } ?: null
        )
    }
}