package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.util.contents
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import java.util.Base64

class GitHubVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }
    private val httpClient by lazy { vertx.createHttpClient() }

    override fun start() {
        eventBus.localConsumer<Any>(EB_GITHUB_CONFIG_READ, readGitHubConfig())
    }

    private fun readGitHubConfig() = Handler { msg: Message<Any> ->
        httpClient.getAbs(contents(getSharedConfig(CONFIG_PATH))).authHeaders().handler { resp ->
            resp.bodyHandler { body ->
                msg.reply(JsonObject(Base64.getMimeDecoder().decode(body.toJsonObject().getString(CONTENT)).toString(Charsets.UTF_8)))
            }.exceptionHandler {
                msg.fail(ReplyFailure.RECIPIENT_FAILURE.toInt(), "Unable to get config from GitHub")
            }
        }.end()
    }

    private fun HttpClientRequest.authHeaders(): HttpClientRequest {
        return apply {
            putHeader(HttpHeaders.USER_AGENT, getSharedConfig(AGENT_NAME))
            putHeader(HttpHeaders.AUTHORIZATION, "token ${getSharedConfig(GITHUB_TOKEN)}")
        }
    }
}
