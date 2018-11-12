package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.RemoteConfig
import io.github.spair.repoxbot.dto.UpdateFileInfo
import io.github.spair.repoxbot.dto.codec.StringJsonToRemoteConfigCodec
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.net.HttpURLConnection
import java.util.Base64

class GithubVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(GithubVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }
    private val httpClient by lazy { vertx.createHttpClient() }

    override fun start() {
        eventBus.localConsumer<JsonObject>(EB_GITHUB_CONFIG_READ, readRemoteConfig())
        eventBus.localConsumer<String>(EB_GITHUB_FILE_READ, readGithubFile())
        eventBus.localConsumer<UpdateFileInfo>(EB_GITHUB_FILE_UPDATE, updateGithubFile())
    }

    private fun readRemoteConfig() = Handler<Message<JsonObject>> { msg ->
        httpClient.getAbs(contents(getSharedConfig(CONFIG_PATH))).authHeader().handler {
            if (it.statusCode() == HttpURLConnection.HTTP_OK) {
                it.bodyHandler { body ->
                    msg.reply(body.toJsonObject().readContents(), DeliveryOptions().setCodecName(StringJsonToRemoteConfigCodec.NAME))
                }
            } else {
                msg.reply(RemoteConfig())
            }
        }.end()
    }

    private fun readGithubFile() = Handler<Message<String>> { msg ->
        httpClient.getAbs(contents(msg.body())).authHeader().handler {
            if (it.statusCode() == HttpURLConnection.HTTP_OK) {
                it.bodyHandler { body ->
                    msg.reply(body.toJsonObject().readContents())
                }
            } else {
                logger.error("Unable to read Github file: ${msg.body()}")
                msg.fail(ReplyFailure.RECIPIENT_FAILURE.toInt(), "no file exist")
            }
        }.end()
    }

    private fun updateGithubFile() = Handler<Message<UpdateFileInfo>> { msg ->
        val updateFileInfo = msg.body()
        getFileSha(updateFileInfo.path).setHandler { ar ->
            if (ar.succeeded()) {
                httpClient.putAbs(contents(updateFileInfo.path)).authHeader().jsonHeader().handler {
                    if (it.statusCode() != HttpURLConnection.HTTP_OK) {
                        logger.error("Unable to update Github file: ${updateFileInfo.path}")
                    }
                }.end(JsonObject().apply {
                    put(MESSAGE, updateFileInfo.message)
                    put(CONTENT, updateFileInfo.content.encodeBase64())
                    put(SHA, ar.result())
                }.toBuffer())
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
                dirPath = ""
                fileName = path
            }
        }

        return Future.future<String>().apply {
            httpClient.getAbs(contents(dirPath)).authHeader().handler {
                it.bodyHandler { body ->
                    for (node in body.toJsonArray()) {
                        if (node is JsonObject && node.getString(NAME) == fileName) {
                            complete(node.getString(SHA))
                            return@bodyHandler
                        }
                    }
                    fail(IllegalArgumentException("Can't get file SHA from path: $path"))
                }
            }.end()
        }
    }

    private fun HttpClientRequest.authHeader(): HttpClientRequest = apply {
        putHeader(HttpHeaders.AUTHORIZATION, "token ${getSharedConfig(GITHUB_TOKEN)}")
        putHeader(HttpHeaders.USER_AGENT, getSharedConfig(AGENT_NAME))
    }

    private fun HttpClientRequest.jsonHeader(): HttpClientRequest = apply {
        putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
    }
}

private fun JsonObject.readContents(): String = getString(CONTENT).decodeBase64()

private fun String.decodeBase64(): String = Base64.getMimeDecoder().decode(this).toString(Charsets.UTF_8)
private fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())

@Suppress("NOTHING_TO_INLINE")
inline fun GithubVerticle.contents(relPath: String): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/contents/$relPath"
}
