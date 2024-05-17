package com.qalipsis.factory.builder

import io.micronaut.runtime.Micronaut

object FactoryBuilderApplication {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build(*args).packages("com.qalipsis.factory.builder").start()
    }
}
