package io.qalipsis.gradle.campaign

import io.qalipsis.gradle.cloud.dsl.CampaignConfiguration
import io.qalipsis.gradle.cloud.dsl.CampaignConfigurationSpecification
import io.qalipsis.gradle.cloud.dsl.CampaignConfigurationSpecificationImpl
import org.gradle.api.model.ObjectFactory

/**
 * Class to use to create a new campaign specification for testing purpose.
 */
object TestCampaignFactory {

    fun campaign(
        projectObjects: ObjectFactory,
        block: CampaignConfigurationSpecification.() -> Unit
    ): CampaignConfiguration {
        val spec = CampaignConfigurationSpecificationImpl(projectObjects)
        spec.block()
        return spec.toCampaignConfiguration()
    }
}