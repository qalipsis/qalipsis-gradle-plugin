package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.qalipsis.gradle.cloud.QalipsisCloudPlugin
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit


internal object Helper {

    val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    val jsonMapper by lazy {
        jsonMapper {
            addModule(kotlinModule())
            addModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    /**
     * Provides the configured or default root URL to use to access to the factory builder.
     */
    fun rootEndpoint(project: Project): String {
        return if (project.hasProperty("qalipsis.cloud.builder.url")) {
            project.property("qalipsis.cloud.builder.url").toString()
        } else {
            QalipsisCloudPlugin.CLOUD_ROOT_URL
        }.let(Helper::addSlashIfMissing)
    }

    /**
     * Provides the configured or default root URL to use to access to the factory builder.
     */
    fun Request.Builder.authenticate(project: Project): Request.Builder {
        val authentication = project.property("qalipsis.cloud.builder.token").toString()
        return this.addHeader("Authorization", "Bearer $authentication")
    }

    private fun addSlashIfMissing(url: String) = if (url.endsWith('/')) url else "$url/"

    fun camelCaseScenarioName(scenarioName: String): String {
        return scenarioName.split(Regex("[^a-z0-9]")).joinToString(separator = "") {
            it.capitalized()
        }
    }
}