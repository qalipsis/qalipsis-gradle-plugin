package io.qalipsis.gradle.cloud.dsl

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Add implementations for methods to instantiate the profile configuration object.
 *
 * @author Francisca Eze
 */
class ExternalExecutionProfileConfigurationImpl : ExternalExecutionProfileConfiguration {

    override var profile: ProfileConfiguration? = null

    override fun stages(completionMode: CompletionMode, block: StageConfiguration.() -> Unit) {
        profile = StageProfileConfigurationImpl().also {
            it.completion = completionMode
            it.block()
        }
    }

    override fun immediate() {
        require(profile == null) { "Expected profile to be null" }
        profile = ImmediateProfileConfigurationImpl()
    }

    override fun percentage(completionMode: CompletionMode, block: PercentageStageConfiguration.() -> Unit) {
        profile = PercentageStageProfileConfigurationImpl().also {
            it.completion = completionMode
            it.block()
        }
    }

    override fun regular(periodInMs: Long, minionsCountProLaunch: Int, totalMinionsCount: Int) {
        profile = RegularProfileConfigurationImpl(periodInMs, minionsCountProLaunch, totalMinionsCount)
    }

    override fun accelerate(startPeriodMs: Long, accelerator: Double, minPeriodMs: Long, minionsCountProLaunch: Int) {
        profile = AcceleratingProfileConfigurationImpl(startPeriodMs, accelerator, minPeriodMs, minionsCountProLaunch)
    }

    override fun more(
        periodMs: Long,
        minionsCountProLaunchAtStart: Int,
        multiplier: Double,
        maxMinionsCountProLaunch: Int
    ) {
        profile = ProgressiveVolumeProfileConfigurationImpl(
            periodMs,
            minionsCountProLaunchAtStart,
            multiplier,
            maxMinionsCountProLaunch
        )
    }

    override fun timeframe(periodInMs: Long, timeFrameInMs: Long) {
        profile = TimeframeProfileConfigurationImpl(periodInMs, timeFrameInMs)
    }

}

/**
 * Interface of execution profile configuration strategies.
 *
 * @author Francisca Eze
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "profile"
)

@JsonSubTypes(
    JsonSubTypes.Type(value = RegularProfileConfigurationImpl::class, name = "REGULAR"),
    JsonSubTypes.Type(value = ImmediateProfileConfigurationImpl::class, name = "IMMEDIATELY"),
    JsonSubTypes.Type(value = AcceleratingProfileConfigurationImpl::class, name = "ACCELERATING"),
    JsonSubTypes.Type(
        value = ProgressiveVolumeProfileConfigurationImpl::class,
        name = "PROGRESSING_VOLUME"
    ),
    JsonSubTypes.Type(value = PercentageStageProfileConfigurationImpl::class, name = "PERCENTAGE_STAGE"),
    JsonSubTypes.Type(value = StageProfileConfigurationImpl::class, name = "STAGE"),
    JsonSubTypes.Type(value = TimeframeProfileConfigurationImpl::class, name = "TIME_FRAME")
)
interface ProfileConfiguration

/**
 * Details of the ImmediateProfileConfigurationImpl to create the execution profile.
 */
class ImmediateProfileConfigurationImpl : ProfileConfiguration

/**
 * Details of the ProgressiveVolumeProfileConfigurationImpl to create the execution profile.
 */
class ProgressiveVolumeProfileConfigurationImpl(
    val periodMs: Long,
    val minionsCountProLaunchAtStart: Int,
    val multiplier: Double,
    val maxMinionsCountProLaunch: Int
) : ProfileConfiguration

/**
 * Details of the RegularProfileConfigurationImpl to create the execution profile.
 */
class RegularProfileConfigurationImpl(
    val periodInMs: Long,
    val minionsCountProLaunch: Int,
    val totalMinionsCount: Int
) : ProfileConfiguration

/**
 * Details of the TimeframeProfileConfigurationImpl to create the execution profile.
 */
class TimeframeProfileConfigurationImpl(
    val periodInMs: Long,
    val timeFrameInMs: Long
) : ProfileConfiguration

class AcceleratingProfileConfigurationImpl(
    val startPeriodMs: Long,
    val accelerator: Double,
    val minPeriodMs: Long,
    val minionsCountProLaunch: Int
) : ProfileConfiguration

/**
 * Details of the StageProfileConfigurationImpl to create the execution profile.
 */
class StageProfileConfigurationImpl : StageConfiguration {

    val stages: MutableList<Stage> = mutableListOf()

    override var completion: CompletionMode = CompletionMode.GRACEFUL

    override fun stage(
        minionsCount: Int,
        rampUpDurationMs: Long,
        totalDurationMs: Long,
        resolutionMs: Long
    ): StageConfiguration {
        stages += Stage(minionsCount, rampUpDurationMs, totalDurationMs, resolutionMs)
        return this
    }

}

/**
 * Details of the PercentageStageProfileConfigurationImpl to create the execution profile.
 */
class PercentageStageProfileConfigurationImpl : PercentageStageConfiguration {

    override var completion: CompletionMode = CompletionMode.GRACEFUL

    val stages: MutableList<PercentageStage> = mutableListOf()

    override fun stage(
        minionsPercentage: Double, rampUpDurationMs: Long, totalDurationMs: Long, resolutionMs: Long
    ): PercentageStageConfiguration {
        stages += PercentageStage(minionsPercentage, rampUpDurationMs, totalDurationMs, resolutionMs)
        return this
    }
}

interface StageConfiguration : ProfileConfiguration {

    val completion: CompletionMode

    fun stage(
        minionsCount: Int,
        rampUpDurationMs: Long,
        totalDurationMs: Long,
        resolutionMs: Long
    ): StageConfiguration
}

interface PercentageStageConfiguration : ProfileConfiguration {

    val completion: CompletionMode

    fun stage(
        minionsPercentage: Double,
        rampUpDurationMs: Long,
        totalDurationMs: Long,
        resolutionMs: Long,
    ): PercentageStageConfiguration
}

data class Stage(

    /**
     * Total number of minions to start in the stage.
     */
    val minionsCount: Int,

    /**
     * Minions ramp up duration, in milliseconds.
     */
    val rampUpDurationMs: Long,

    /**
     * Stage duration, in milliseconds.
     */
    val totalDurationMs: Long,

    /**
     * Minimal duration between two triggering of minions start, default to 500 ms.
     */
    val resolutionMs: Long = 500
)

data class PercentageStage(

    /**
     * Percentage of minions to start in that stage.
     */
    val minionsPercentage: Double,

    /**
     * Minions ramp up duration, in milliseconds.
     */
    val rampUpDurationMs: Long,

    /**
     * Stage duration, in milliseconds.
     */
    val totalDurationMs: Long,

    /**
     * Minimal duration between two triggering of minions start, default to 500 ms.
     */
    val resolutionMs: Long
)
