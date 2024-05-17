package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.qalipsis.gradle.cloud.QalipsisCloudPlugin
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.util.concurrent.TimeUnit

/**
 * Contains utility methods to help with sending and converting of HTTP requests and responses.
 */
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
    fun buildUrl(project: Project, uri: String = ""): String {
        val urlBuilder = if (project.hasProperty("qalipsis.cloud.registry.url")) {
            project.property("qalipsis.cloud.registry.url").toString().toHttpUrl()
        } else {
            QalipsisCloudPlugin.DEFAULT_SCENARIOS_API_ROOT_URL
        }.newBuilder()

        if (uri.isNotEmpty()) {
            urlBuilder.addPathSegment(uri)
        }

        if (project.hasProperty("qalipsis.cloud.registry.parameters")) {
            project.property("qalipsis.cloud.registry.parameters").toString().split('&')
                .forEach {
                    val (key, value) = it.split('=')
                    urlBuilder.addQueryParameter(key, value)
                }
        }

        return urlBuilder.build().toString()
    }

    /**
     * Provides the configured or default root URL to use to access to the factory builder.
     */
    fun Request.Builder.authenticate(project: Project): Request.Builder {
        require(project.hasProperty("qalipsis.cloud.registry.token")) {
            """|The property 'qalipsis.cloud.registry.token' is not set. 
               |  1. Create a token on your QALIPSIS GUI or with the API, with the permissions `read:scenario` and `write:scenario`.
               |  2. Add its value to your Gradle build as the property `qalipsis.cloud.registry.token`.""".trimMargin()
        }
        val authentication = project.property("qalipsis.cloud.registry.token").toString()
        return this.addHeader("Authorization", "Bearer $authentication")
    }

    fun camelCaseScenarioName(scenarioName: String): String {
        return scenarioName.split(Regex("[^a-z0-9]")).joinToString(separator = "") {
            it.capitalized()
        }
    }
}