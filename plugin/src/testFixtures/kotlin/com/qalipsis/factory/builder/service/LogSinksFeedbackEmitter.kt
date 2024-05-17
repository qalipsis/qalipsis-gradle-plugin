package com.qalipsis.factory.builder.service

import com.qalipsis.factory.builder.model.LogMessage
import reactor.core.publisher.Sinks

internal class LogSinksFeedbackEmitter(override val buildId: String, private val sink: Sinks.Many<LogMessage>) :
    FeedbackEmitter {

    private val internalErrors = mutableListOf<Throwable>()

    override fun info(message: String?) {
        message?.let {
            sink.tryEmitNext(LogMessage("INFO", text = message))
        }
    }

    override fun error(error: Throwable) {
        internalErrors += error
        sink.tryEmitNext(LogMessage("ERROR", text = error.message!!))
    }

    override fun complete() {
        sink.tryEmitComplete()
    }

    override fun throwOrNothing() {
        if (internalErrors.isNotEmpty()) {
            throw internalErrors.first()
        }
    }

}