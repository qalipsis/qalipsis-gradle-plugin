package io.qalipsis.gradle.cloud.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Base class for defining campaign configuration specifications.
 *
 * @author Francisca Eze
 */
@CampaignMarker
abstract class CampaignConfigurationSpecification {

    abstract val campaignName: Property<String>
    abstract val speedFactor: Property<Double>
    abstract val startOffsetMs: Property<Long>
    abstract val campaignTimeout: Property<String>
    abstract val hardTimeout: Property<Boolean>
    private val _scenarios: MutableMap<String, ScenarioRequest> = mutableMapOf()
    val scenarios: Map<String, ScenarioRequest>
        get() = _scenarios

    fun scenario(name: String, configurationBlock: @ScenarioMarker ConfigurableScenarioSpecification.() -> Unit) {
        val configuration = ConfigurableScenarioSpecificationImpl()
        configuration.configurationBlock()
        _scenarios[name] = configuration.toScenarioRequest()
    }
}

/**
 *  Implementation of [CampaignConfigurationSpecification] used to define and configure
 *  campaign properties.
 *
 *  @author Francisca Eze
 */
class CampaignConfigurationSpecificationImpl(objects: ObjectFactory) : CampaignConfigurationSpecification() {
    override val campaignName: Property<String> = objects.property(String::class.java)
    override val speedFactor: Property<Double> = objects.property(Double::class.java)
    override val startOffsetMs: Property<Long> = objects.property(Long::class.java)
    override val campaignTimeout: Property<String> = objects.property(String::class.java)
    override val hardTimeout: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Converts the campaign dsl specification to a CampaignConfiguration type.
     */
    internal fun toCampaignConfiguration(): CampaignConfiguration {
        return CampaignConfiguration(
            name = campaignName.get(),
            speedFactor = speedFactor.get(),
            startOffsetMs = startOffsetMs.get(),
            timeout = campaignTimeout.getOrElse(null),
            hardTimeout = hardTimeout.getOrElse(null),
            scenarios = scenarios.takeIf { it.isNotEmpty() } ?: null
        )
    }
}