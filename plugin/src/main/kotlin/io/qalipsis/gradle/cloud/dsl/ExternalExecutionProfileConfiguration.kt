package io.qalipsis.gradle.cloud.dsl

/**
 * Defines methods to allow for configuring of the respective execution profile types.
 *
 * @author Francisca Eze
 */
interface ExternalExecutionProfileConfiguration {

    var profile: ProfileConfiguration?

    fun stages(
        completionMode: CompletionMode = CompletionMode.GRACEFUL,
        block: @ScenarioMarker StageConfiguration.() -> Unit
    )

    fun immediate()

    fun percentage(
        completionMode: CompletionMode = CompletionMode.GRACEFUL,
        block: @ScenarioMarker PercentageStageConfiguration.() -> Unit
    )

    fun regular(periodInMs: Long, minionsCountProLaunch: Int, totalMinionsCount: Int)

    fun accelerate(startPeriodMs: Long, accelerator: Double, minPeriodMs: Long, minionsCountProLaunch: Int)

    fun more(periodMs: Long, minionsCountProLaunchAtStart: Int, multiplier: Double, maxMinionsCountProLaunch: Int)

    fun timeframe(periodInMs: Long, timeFrameInMs: Long)
}
