package com.qalipsis.factory.builder.service

internal interface FeedbackEmitter {

    val buildId: String

    fun info(message: String?)

    fun error(error: Throwable)

    fun complete()

    fun throwOrNothing()
}