package io.qalipsis.gradle.cloud.dsl

import assertk.all
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.index
import assertk.assertions.hasSize
import io.qalipsis.gradle.campaign.TestCampaignFactory.campaign
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CampaignDslTest {

    private val objectFactory = ProjectBuilder.builder().build().objects

    @Test
    internal fun `should define a CampaignConfiguration instance from the dsl`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
            prop(CampaignConfiguration::scenarios).isNull()
        }
    }

    @Test
    internal fun `should throw an error for zone values greater than 100 percent`() {
        // given + when
        val exception = assertThrows<IllegalArgumentException> {
            campaign("Dummy campaign configuration", objectFactory) {
                speedFactor.set(2.0)
                startOffsetMs.set(2000)
                campaignTimeout.set("PT1H30M")
                hardTimeout.set(false)
                scenario("My scenario 1") {
                    minionsCount = 7
                    zones {
                        "CH" to 45
                        "AS" to 55
                        "US" to 15
                    }
                }
            }
        }

        //then
        assertThat(exception.message).isEqualTo("Distribution by zone should sum up to a 100%")
    }

    @Test
    internal fun `should allow valid zone values`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
            }
        }

        //then
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
            prop(ScenarioRequest::executionProfile).isNull()
        }
    }

    @Test
    internal fun `should allow null zone values`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                null
            }
        }

        //then
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isNull()
            prop(ScenarioRequest::executionProfile).isNull()
        }
    }

    @Test
    internal fun `should allow null values for execution profiles`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
            prop(ScenarioRequest::executionProfile).isNull()
        }
    }

    @Test
    internal fun `should accept multiple scenario entries`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
            }
            scenario("My scenario 2") {
                minionsCount = 1000
                zones {
                    "CH" to 25
                    "AS" to 31
                    "US" to 44
                }
                profile {
                    more(3000, 1000, 2.0, 2000)
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
            prop(CampaignConfiguration::scenarios).isNotNull().all {
                hasSize(2)
            }
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
            prop(ScenarioRequest::executionProfile).isNull()
        }
        val scenario2 = campaign.scenarios?.get("My scenario 2")
        assertThat(scenario2).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(1000)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 25, "AS" to 31, "US" to 44))
            prop(ScenarioRequest::executionProfile).isNotNull()
                .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                    prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                        .isInstanceOf(ProgressiveVolumeProfileConfigurationImpl::class).all {
                            prop(ProgressiveVolumeProfileConfigurationImpl::periodMs).isEqualTo(3000)
                            prop(ProgressiveVolumeProfileConfigurationImpl::minionsCountProLaunchAtStart).isEqualTo(1000)
                            prop(ProgressiveVolumeProfileConfigurationImpl::multiplier).isEqualTo(2.0)
                            prop(ProgressiveVolumeProfileConfigurationImpl::maxMinionsCountProLaunch).isEqualTo(2000)
                        }
                }
        }
    }

    @Test
    internal fun `should allow profiles of ImmediateConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    immediate()
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
            prop(ScenarioRequest::executionProfile).isNotNull()
        }

        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(ImmediateProfileConfigurationImpl::class)
            }
    }

    @Test
    internal fun `should allow profiles of ProgressiveVolumeConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    more(
                        periodMs = 11,
                        minionsCountProLaunchAtStart = 23,
                        multiplier = 2.0,
                        maxMinionsCountProLaunch = 7
                    )
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(ProgressiveVolumeProfileConfigurationImpl::class).all {
                        prop(ProgressiveVolumeProfileConfigurationImpl::periodMs).isEqualTo(11)
                        prop(ProgressiveVolumeProfileConfigurationImpl::minionsCountProLaunchAtStart).isEqualTo(23)
                        prop(ProgressiveVolumeProfileConfigurationImpl::multiplier).isEqualTo(2.0)
                        prop(ProgressiveVolumeProfileConfigurationImpl::maxMinionsCountProLaunch).isEqualTo(7)
                    }
            }
    }

    @Test
    internal fun `should allow profiles of RegularConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    regular(
                        periodInMs = 1000,
                        minionsCountProLaunch = 100,
                        totalMinionsCount = 2000,
                    )
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(RegularProfileConfigurationImpl::class).all {
                        prop(RegularProfileConfigurationImpl::periodInMs).isEqualTo(1000)
                        prop(RegularProfileConfigurationImpl::minionsCountProLaunch).isEqualTo(100)
                        prop(RegularProfileConfigurationImpl::totalMinionsCount).isEqualTo(2000)
                    }
            }
    }

    @Test
    internal fun `should allow profiles of AcceleratingConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    accelerate(
                        startPeriodMs = 1000,
                        accelerator = 2.0,
                        minPeriodMs = 1000,
                        minionsCountProLaunch = 2000
                    )
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(AcceleratingProfileConfigurationImpl::class).all {
                        prop(AcceleratingProfileConfigurationImpl::startPeriodMs).isEqualTo(1000)
                        prop(AcceleratingProfileConfigurationImpl::accelerator).isEqualTo(2.0)
                        prop(AcceleratingProfileConfigurationImpl::minPeriodMs).isEqualTo(1000)
                        prop(AcceleratingProfileConfigurationImpl::minionsCountProLaunch).isEqualTo(2000)
                    }
            }
    }

    @Test
    internal fun `should allow profiles of TimeframeConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    timeframe(
                        periodInMs = 10000,
                        timeFrameInMs = 1000
                    )
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(TimeframeProfileConfigurationImpl::class).all {
                        prop(TimeframeProfileConfigurationImpl::periodInMs).isEqualTo(10000)
                        prop(TimeframeProfileConfigurationImpl::timeFrameInMs).isEqualTo(1000)
                    }
            }
    }

    @Test
    internal fun `should allow profiles of StagesConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    stages(completionMode = CompletionMode.HARD) {
                        stage(
                            minionsCount = 100,
                            rampUpDurationMs = 10,
                            totalDurationMs = 100000,
                            resolutionMs = 10000
                        )
                        stage(
                            minionsCount = 2000,
                            rampUpDurationMs = 100,
                            totalDurationMs = 10000,
                            resolutionMs = 1000
                        )
                        stage(
                            minionsCount = 5000,
                            rampUpDurationMs = 2000,
                            totalDurationMs = 1000,
                            resolutionMs = 4000
                        )
                    }
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(StageProfileConfigurationImpl::class).all {
                        prop(StageProfileConfigurationImpl::completion).isEqualTo(CompletionMode.HARD)
                        prop(StageProfileConfigurationImpl::stages).all {
                            hasSize(3)
                            index(0).all {
                                prop(Stage::minionsCount).isEqualTo(100)
                                prop(Stage::rampUpDurationMs).isEqualTo(10)
                                prop(Stage::totalDurationMs).isEqualTo(100000)
                                prop(Stage::resolutionMs).isEqualTo(10000)
                            }
                            index(1).all {
                                prop(Stage::minionsCount).isEqualTo(2000)
                                prop(Stage::rampUpDurationMs).isEqualTo(100)
                                prop(Stage::totalDurationMs).isEqualTo(10000)
                                prop(Stage::resolutionMs).isEqualTo(1000)
                            }
                            index(2).all {
                                prop(Stage::minionsCount).isEqualTo(5000)
                                prop(Stage::rampUpDurationMs).isEqualTo(2000)
                                prop(Stage::totalDurationMs).isEqualTo(1000)
                                prop(Stage::resolutionMs).isEqualTo(4000)
                            }
                        }
                    }
            }
    }


    @Test
    internal fun `should allow profiles of PercentageStageConfigurationImpl types`() {
        // given + when
        val campaign = campaign("Dummy campaign configuration", objectFactory) {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            scenario("My scenario 1") {
                minionsCount = 7
                zones {
                    "CH" to 45
                    "AS" to 55
                }
                profile {
                    percentage(completionMode = CompletionMode.GRACEFUL) {
                        stage(
                            minionsPercentage = 10.0,
                            rampUpDurationMs = 10,
                            totalDurationMs = 100000,
                            resolutionMs = 10000
                        )
                        stage(
                            minionsPercentage = 2.0,
                            rampUpDurationMs = 100,
                            totalDurationMs = 10000,
                            resolutionMs = 1000
                        )
                        stage(
                            minionsPercentage = 5.0,
                            rampUpDurationMs = 2000,
                            totalDurationMs = 1000,
                            resolutionMs = 4000
                        )
                    }
                }
            }
        }

        //then
        assertThat(campaign).all {
            prop(CampaignConfiguration::name).isEqualTo("Dummy campaign configuration")
            prop(CampaignConfiguration::speedFactor).isEqualTo(2.0)
            prop(CampaignConfiguration::startOffsetMs).isEqualTo(2000)
            prop(CampaignConfiguration::timeout).isEqualTo("PT1H30M")
            prop(CampaignConfiguration::hardTimeout).isEqualTo(false)
        }
        val scenario = campaign.scenarios?.get("My scenario 1")
        assertThat(scenario).isNotNull().all {
            prop(ScenarioRequest::minionsCount).isEqualTo(7)
            prop(ScenarioRequest::zones).isEqualTo(mapOf("CH" to 45, "AS" to 55))
        }
        assertThat(scenario?.executionProfile).isNotNull()
            .isInstanceOf(ExternalExecutionProfileConfigurationImpl::class).all {
                prop(ExternalExecutionProfileConfigurationImpl::profile).isNotNull()
                    .isInstanceOf(PercentageStageProfileConfigurationImpl::class).all {
                        prop(PercentageStageProfileConfigurationImpl::completion).isEqualTo(CompletionMode.GRACEFUL)
                        prop(PercentageStageProfileConfigurationImpl::stages).all {
                        hasSize(3)
                        index(0).all {
                            prop(PercentageStage::minionsPercentage).isEqualTo(10.0)
                            prop(PercentageStage::rampUpDurationMs).isEqualTo(10)
                            prop(PercentageStage::totalDurationMs).isEqualTo(100000)
                            prop(PercentageStage::resolutionMs).isEqualTo(10000)
                        }
                        index(1).all {
                            prop(PercentageStage::minionsPercentage).isEqualTo(2.0)
                            prop(PercentageStage::rampUpDurationMs).isEqualTo(100)
                            prop(PercentageStage::totalDurationMs).isEqualTo(10000)
                            prop(PercentageStage::resolutionMs).isEqualTo(1000)
                        }
                        index(2).all {
                            prop(PercentageStage::minionsPercentage).isEqualTo(5.0)
                            prop(PercentageStage::rampUpDurationMs).isEqualTo(2000)
                            prop(PercentageStage::totalDurationMs).isEqualTo(1000)
                            prop(PercentageStage::resolutionMs).isEqualTo(4000)
                        }
                    }
                    }
            }
    }

}