package io.qalipsis.gradle.bootstrap

open class QalipsisPluginsExtension {

    internal val requiredPluginsDependency = mutableSetOf<String>()

    internal val requiredPlugins = mutableSetOf<String>()

    /**
     * Enables the QALIPSIS Apache Cassandra Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun apacheCassandra(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-cassandra" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "cassandra"
    }

    /**
     * Enables the QALIPSIS Apache Kafka Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun apacheKafka(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-kafka" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "kafka"
    }

    /**
     * Enables the QALIPSIS Elasticsearch Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun elasticsearch(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-elasticsearch" + version?.let { ":$it" }
            .orEmpty()
        requiredPlugins += "elasticsearch"
    }

    /**
     * Enables the QALIPSIS Graphite Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun graphite(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-graphite" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "graphite"
    }

    /**
     * Enables the QALIPSIS Influx DB Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun influxDb(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-influxdb" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "influxdb"
    }

    /**
     * Enables the QALIPSIS Jackson Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun jackson(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jackson" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "jackson"
    }

    /**
     * Enables the QALIPSIS Java Messaging Service Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun jms(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jms" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "jms"
    }

    /**
     * Enables the QALIPSIS Jakarta Messaging Service Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun jakartaMessaging(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-jakarta-ee-messaging" + version?.let { ":$it" }
            .orEmpty()
        requiredPlugins += "jakarta-ee-messaging"
    }

    /**
     * Enables the QALIPSIS Mail Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun mail(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-mail" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "mail"
    }

    /**
     * Enables the QALIPSIS Mongo DB Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun mongoDb(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-mongodb" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "mongodb"
    }

    /**
     * Enables the QALIPSIS Netty Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun netty(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-netty" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "netty"
    }


    /**
     * Enables the QALIPSIS R2DBC Jasync Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun r2dbcJasync(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-r2dbc-jasync" + version?.let { ":$it" }
            .orEmpty()
        requiredPlugins += "r2dbc-jasync"
    }

    /**
     * Enables the QALIPSIS RabbitMQ Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun rabbitMq(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-rabbitmq" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "rabbitmq"
    }

    /**
     * Enables the QALIPSIS Redis Lettuce Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun redisLettuce(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-redis-lettuce" + version?.let { ":$it" }
            .orEmpty()
        requiredPlugins += "redis-lettuce"
    }

    /**
     * Enables the QALIPSIS Slack Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun slack(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-slack" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "slack"
    }

    /**
     * Enables the QALIPSIS SQL Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun sql(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-sql" + version?.let { ":$it" }.orEmpty()
        requiredPlugins += "sql"
    }

    /**
     * Enables the QALIPSIS Timescale DB Plugin.
     *
     * @param version version of the plugin to use, defaults to the version provided by the configured QALIPSIS version.
     */
    fun timescaleDb(version: String? = null) {
        requiredPluginsDependency += "io.qalipsis.plugin:qalipsis-plugin-timescaledb" + version?.let { ":$it" }
            .orEmpty()
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
        sql()
        timescaleDb()
    }
}