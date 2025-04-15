package io.qalipsis.gradle.cloud.dsl

/**
 *  Marks the scope of the scenario DSL to prevent mixing of receivers from other DSL contexts.
 */
@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class ScenarioMarker