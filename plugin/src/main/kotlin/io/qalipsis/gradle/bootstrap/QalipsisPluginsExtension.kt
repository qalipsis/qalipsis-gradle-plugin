package io.qalipsis.gradle.bootstrap

open class QalipsisPluginsExtension {

    internal val requiredPluginsDependency = mutableSetOf<String>()

    internal val requiredPlugins = mutableSetOf<String>()

    /**
     * Enables the QALIPSIS Apache Cassandra Plugin.
     */
    fun apacheCassandra() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-cassandra"
        requiredPlugins += "cassandra"
    }

    /**
     * Enables the QALIPSIS Apache Kafka Plugin.
     */
    fun apacheKafka() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-kafka"
        requiredPlugins += "kafka"
    }

    /**
     * Enables the QALIPSIS Elasticsearch Plugin.
     */
    fun elasticsearch() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-elasticsearch"
        requiredPlugins += "elasticsearch"
    }

    /**
     * Enables the QALIPSIS Graphite Plugin.
     */
    fun graphite() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-graphite"
        requiredPlugins += "graphite"
    }

    /**
     * Enables the QALIPSIS Influx DB Plugin.
     */
    fun influxDb() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-influxdb"
        requiredPlugins += "influxdb"
    }

    /**
     * Enables the QALIPSIS Jackson Plugin.
     */
    fun jackson() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jackson"
        requiredPlugins += "jackson"
    }

    /**
     * Enables the QALIPSIS Java Messaging Service Plugin.
     */
    fun jms() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jms"
        requiredPlugins += "jms"
    }

    /**
     * Enables the QALIPSIS Jakarta Messaging Service Plugin.
     */
    fun jakartaMessaging() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jakarta-ee-messaging"
        requiredPlugins += "jakarta-ee-messaging"
    }

    /**
     * Enables the QALIPSIS Mail Plugin.
     */
    fun mail() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-mail"
        requiredPlugins += "mail"
    }

    /**
     * Enables the QALIPSIS Mongo DB Plugin.
     */
    fun mongoDb() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-mongodb"
        requiredPlugins += "mongodb"
    }

    /**
     * Enables the QALIPSIS Netty Plugin.
     */
    fun netty() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-netty"
        requiredPlugins += "netty"
    }


    /**
     * Enables the QALIPSIS R2DBC Jasync Plugin.
     */
    fun r2dbcJasync() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-r2dbc-jasync"
        requiredPlugins += "r2dbc-jasync"
    }

    /**
     * Enables the QALIPSIS RabbitMQ Plugin.
     */
    fun rabbitMq() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-rabbitmq"
        requiredPlugins += "rabbitmq"
    }

    /**
     * Enables the QALIPSIS Redis Lettuce Plugin.
     */
    fun redisLettuce() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-redis-lettuce"
        requiredPlugins += "redis-lettuce"
    }

    /**
     * Enables the QALIPSIS Redis Lettuce Plugin.
     */
    fun slack() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-slack"
        requiredPlugins += "slack"
    }

    /**
     * Enables the QALIPSIS Timescale DB Plugin.
     */
    fun timescaleDb() {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-timescaledb"
        requiredPlugins += "timescaledb"
    }

    /**
     * Adds all the plugins to the project.
     */
    fun all() {
        apacheCassandra()
        apacheKafka()
        elasticsearch()
        graphite()
        influxDb()
        jackson()
        jms()
        jakartaMessaging()
        mail()
        mongoDb()
        netty()
        r2dbcJasync()
        rabbitMq()
        redisLettuce()
        slack()
        timescaleDb()
    }
}