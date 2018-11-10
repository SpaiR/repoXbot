package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.UpdateFileInfo
import io.github.spair.repoxbot.dto.codec.StringJsonToRemoteConfigCodec
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import java.net.HttpURLConnection
import java.util.Base64

class GithubVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(GithubVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }
    private val webClient by lazy {
        WebClient.create(vertx, WebClientOptions().apply {
            userAgent = getSharedConfig(AGENT_NAME)
        })
    }

    override fun start() {
        eventBus.localConsumer<JsonObject>(EB_GITHUB_CONFIG_READ, readRemoteConfig())
        eventBus.localConsumer<String>(EB_GITHUB_FILE_READ, readGithubFile())
        eventBus.localConsumer<UpdateFileInfo>(EB_GITHUB_FILE_UPDATE, updateGithubFile())
    }

    private fun readRemoteConfig() = Handler<Message<JsonObject>> { msg ->
        webClient.getAbs(contents(getSharedConfig(CONFIG_PATH))).authHeader().send { resp ->
            with(resp.result().bodyAsJsonObject()) {
                msg.reply(readContents(), DeliveryOptions().setCodecName(StringJsonToRemoteConfigCodec.NAME))
            }
        }
    }

    private fun readGithubFile() = Handler<Message<String>> { msg ->
        webClient.getAbs(contents(msg.body())).authHeader().send { resp -> msg.reply(resp.result().bodyAsJsonObject().readContents()) }
    }

    private fun updateGithubFile() = Handler<Message<UpdateFileInfo>> { msg ->
        val updateFileInfo = msg.body()
        getFileSha(updateFileInfo.path).setHandler { ar ->
            if (ar.succeeded()) {
                val result = ar.result()
                webClient.putAbs(contents(updateFileInfo.path)).authHeader().sendJsonObject(JsonObject().apply {
                    put(MESSAGE, updateFileInfo.message)
                    put(CONTENT, updateFileInfo.content.encodeBase64())
                    put(SHA, result)
                }) {
                    if ((it.succeeded() && it.result().statusCode() != HttpURLConnection.HTTP_OK) || it.failed()) {
                        logger.error("Unable to update Github file: ${updateFileInfo.path}", it.cause())
                    }
                }
            } else {
                logger.error("Error while updating github file", ar.cause())
            }
        }
    }

    private fun getFileSha(path: String): Future<String> {
        var dirPath: String
        var fileName: String

        with("(.*)/(.*)".toPattern().matcher(path)) {
            if (find()) {
                dirPath = group(1)
                fileName = group(2)
            } else {
                dirPath = "/"
                fileName = path
            }
        }

        return Future.future<String>().apply {
            webClient.getAbs(contents(dirPath)).authHeader().send { resp ->
                for (node in resp.result().bodyAsJsonArray()) {
                    if (node is JsonObject && node.getString(NAME) == fileName) {
                        complete(node.getString(SHA))
                        return@send
                    }
                }
                fail(IllegalArgumentException("Can't get file SHA from path: $path"))
            }
        }
    }

    private fun <T> HttpRequest<T>.authHeader(): HttpRequest<T> = apply {
        headers().add(HttpHeaders.AUTHORIZATION, "token ${getSharedConfig(GITHUB_TOKEN)}")
    }
}

private fun JsonObject.readContents(): String = getString(CONTENT).decodeBase64()

private fun String.decodeBase64(): String = Base64.getMimeDecoder().decode(this).toString(Charsets.UTF_8)
private fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())

@Suppress("NOTHING_TO_INLINE")
inline fun GithubVerticle.contents(relPath: String): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/contents/$relPath"
}
