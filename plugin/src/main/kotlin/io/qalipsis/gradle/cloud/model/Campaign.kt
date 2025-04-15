package io.qalipsis.gradle.cloud.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

/**
 * External representation of a campaign.
 *
 * @author Francisca Eze
 */
internal data class Campaign(

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    val version: Instant,

    val key: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    val creation: Instant,

    val name: String,

    val speedFactor: Double,

    val scheduledMinions: Int?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    val softTimeout: Instant? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
    val hardTimeout: Instant? = null,

    val start: Instant?,

    val end: Instant?,

    val status: ExecutionStatus,

    val failureReason: String? = null,

    val configurerName: String?,

    val aborterName: String? = null,

    val scenarios: Collection<CampaignScenario>,

    val zones: Set<String> = emptySet()
) {
    companion object {
        const val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
    }
}


/**
 * Execution status of a [Campaign].
 *
 * @property SUCCESSFUL all the steps, were successful
 * @property WARNING a deeper look at the reports is required, but the campaign does not fail
 * @property FAILED the campaign went until the end, but got errors
 * @property ABORTED the campaign was aborted, either by a user or a critical failure
 * @property SCHEDULED the campaign is scheduled for a later point in time
 * @property QUEUED the campaign is being prepared and will start very soon
 * @property IN_PROGRESS the campaign is currently running
 *
 * @author Francisca Eze
 */
enum class ExecutionStatus {
    SUCCESSFUL,
    WARNING,
    FAILED,
    ABORTED,
    SCHEDULED,
    QUEUED,
    IN_PROGRESS
}

/**
 * Details of a scenario to be executed in a campaign.
 */
internal data class CampaignScenario(

    val name: String,

    val minionsCount: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val version: Instant
)
