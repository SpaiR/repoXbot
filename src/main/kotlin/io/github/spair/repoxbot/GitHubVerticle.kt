package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.codec.JsonToGithubConfigCodec
import io.github.spair.repoxbot.util.contents
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import java.util.Base64

class GithubVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }
    private val httpClient by lazy { vertx.createHttpClient() }

    override fun start() {
        eventBus.localConsumer<JsonObject>(EB_GITHUB_CONFIG_READ, readGitHubConfig())
        eventBus.localConsumer<String>(EB_GITHUB_FILE_READ, readGitHubFile())
    }

    private fun readGitHubConfig() = Handler { msg: Message<JsonObject> ->
        val json = JsonObject().put("changelogPath", "html/changelog.html")
        msg.reply(json, DeliveryOptions().setCodecName(JsonToGithubConfigCodec.NAME))

        // TODO: remove
/*        httpClient.getAbs(contents(getSharedConfig(CONFIG_PATH))).authHeaders().handler { resp ->
            resp.bodyHandler { body ->
                println(body.toString())    // TODO: remove
                val json = JsonObject().put("changelogPath", "html/changelog.html")
                msg.reply(json, DeliveryOptions().setCodecName(JsonToGitHubConfigCodec.NAME))
            }.exceptionHandler {
                msg.fail(ReplyFailure.RECIPIENT_FAILURE.toInt(), "Unable to get config from GitHub")
            }
        }.end()*/
    }

    private fun readGitHubFile() = Handler { msg: Message<String> ->
        httpClient.getAbs(contents(msg.body())).authHeaders().handler { resp ->
            resp.bodyHandler { body ->
                msg.reply(decodeAndReadFileContents(body.toJsonObject()))
            }.exceptionHandler {
                msg.fail(ReplyFailure.RECIPIENT_FAILURE.toInt(), "Unable to read file contents")
            }
        }.end()
    }

    private fun HttpClientRequest.authHeaders(): HttpClientRequest {
        return apply {
            putHeader(HttpHeaders.USER_AGENT, getSharedConfig(AGENT_NAME))
            putHeader(HttpHeaders.AUTHORIZATION, "token ${getSharedConfig(GITHUB_TOKEN)}")
        }
    }

    private fun decodeAndReadFileContents(jsonObject: JsonObject): String {
        return Base64.getMimeDecoder().decode(jsonObject.getString(CONTENT)).toString(Charsets.UTF_8)
    }
}
