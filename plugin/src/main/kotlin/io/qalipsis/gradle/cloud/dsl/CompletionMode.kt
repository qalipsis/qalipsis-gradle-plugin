package io.qalipsis.gradle.cloud.dsl

/**
 * A strategy configuration indicator determining how to end the scenario.
 *
 * @author Francisca Eze
 */
enum class CompletionMode {

    /**
     * No minion can be restarted if the remaining time is less
     * than the elapsed time to execute the scenario.
     */
    HARD,

    /**
     * Restart the minions unless the end of the latest stage is reached.
     */
    GRACEFUL
}