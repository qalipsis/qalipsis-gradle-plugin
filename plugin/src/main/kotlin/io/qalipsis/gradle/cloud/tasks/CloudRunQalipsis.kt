package io.qalipsis.gradle.cloud.tasks

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.qalipsis.gradle.cloud.dsl.CampaignConfigurationSpecification
import io.qalipsis.gradle.cloud.dsl.CampaignConfigurationSpecificationImpl
import io.qalipsis.gradle.cloud.model.Campaign
import io.qalipsis.gradle.cloud.model.ErrorMessage
import io.qalipsis.gradle.cloud.model.ErrorResponse
import io.qalipsis.gradle.cloud.tasks.Helper.authenticate
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import javax.inject.Inject


/**
 * Task in charge of creating new campaigns in the cloud.
 *
 * @author Francisca Eze
 */
abstract class CloudRunQalipsis @Inject constructor() : DefaultTask() {

    private val mapper = jacksonObjectMapper()

    private lateinit var campaignSpecification: CampaignConfigurationSpecificationImpl

    /**
     * Uses a DSL-style configuration block to set up a [CampaignConfigurationSpecification].
     */
    fun campaign(name: String, block: CampaignConfigurationSpecification.() -> Unit) {
        val specification = CampaignConfigurationSpecificationImpl(name, project.objects)
        specification.block()
        this.campaignSpecification = specification
    }

    @TaskAction
    fun execute() {
        val json = transformJson(mapper.writeValueAsString(campaignSpecification.toCampaignConfiguration()))
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val url = if (project.hasProperty("qalipsis.cloud.api.url")) {
            project.property("qalipsis.cloud.api.url").toString().toHttpUrl()
        } else {
            DEFAULT_API_ROOT_URL
        }.newBuilder()
        val request = Request.Builder()
            .url(url.addPathSegment(PATH).build())
            .authenticate(project)
            .post(requestBody)
            .build()
        project.logger.lifecycle("Starting a new campaign with name ${campaignSpecification.campaignName}")

        val campaign = Helper.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    in setOf(401, 403) -> {
                        val errorMessage = """
                                    | There was an authentication issue, please check the API token you configured as Gradle property `qalipsis.cloud.registry.token`. Is it still valid and has the permission to write campaigns?
                                """.trimMargin()
                        throw IOException(errorMessage)
                    }

                    in setOf(502, 504) -> {
                        val errorMessage = """
                                    | The QALIPSIS server is currently not available, and the campaign could not be started.
                                    | Please try again in a moment.
                                    | If the problem persists, please contact the support.
                                """.trimMargin()
                        throw IOException(errorMessage)
                    }

                    else -> {
                        val bodyString = response.body
                        val errorMessage = if (response.code == 400) {
                            kotlin.runCatching {
                               Helper.jsonMapper.readValue<ErrorResponse>(bodyString!!.bytes()).errors.joinToString(separator = ",\n") {
                                    """Property ${it.property} ${it.message}""".trimIndent()
                                }
                            }.getOrDefault(bodyString.toString())
                        } else {
                            kotlin.runCatching { Helper.jsonMapper.readValue<ErrorMessage>(bodyString!!.bytes()).message }
                                .getOrDefault(bodyString.toString())
                        }
                        throw RuntimeException(errorMessage)
                    }
                }
            } else {
                Helper.jsonMapper.readValue<Campaign>(requireNotNull(response.body?.bytes()) { "The response does not contain any data" })
            }
        }
        project.logger.debug("Campaign with key ${campaign.key} was started successfully")
        project.logger.lifecycle("You can retrieve the details of the campaign from the link $url/${campaign.key}")
    }


    /**
     * Transforms the campaign string and strips out the profile wrapper from each of the scenario.
     */
    private fun transformJson(campaign: String): String {
        val mapper = jacksonObjectMapper()
        val rootNode = mapper.readTree(campaign)

        // Process each scenario.
        rootNode.path("scenarios").fields().forEach { (_, scenarioNode) ->
            val executionProfile = scenarioNode.path("executionProfile")
            if (!executionProfile.isMissingNode) {
                val pluginNode = executionProfile.path("profile")
                if (!pluginNode.isMissingNode) {
                    (scenarioNode as ObjectNode).replace(
                        "executionProfile",
                        pluginNode
                    )
                }
            }
        }

        return mapper.writeValueAsString(rootNode)
    }

    companion object {

        val DEFAULT_API_ROOT_URL = "https://app.qalipsis.io/api".toHttpUrl()

        const val PATH = "campaigns"
    }

}