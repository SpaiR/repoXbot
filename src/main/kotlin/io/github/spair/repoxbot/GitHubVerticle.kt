@file:Suppress("NOTHING_TO_INLINE")

package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.IssueComment
import io.github.spair.repoxbot.dto.RemoteConfig
import io.github.spair.repoxbot.dto.UpdateFileInfo
import io.github.spair.repoxbot.dto.codec.IssueCommentListCodec
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

        eventBus.localConsumer<Int>(EB_GITHUB_ISSUE_LIST, listIssueComments())
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

    private fun listIssueComments() = Handler<Message<Int>> { msg ->
        val issueComments = mutableListOf<IssueComment>()
        recursiveLinkProcess(issueComments(msg.body())) {
            issueComments.add(IssueComment(it.getInteger(ID), it.getJsonObject(USER).getString(LOGIN), it.getString(BODY)))
        }.setHandler {
            msg.reply(issueComments.toList(), DeliveryOptions().setCodecName(IssueCommentListCodec.NAME))
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

    private fun recursiveLinkProcess(link: String, action: (JsonObject) -> Unit): Future<Unit> {
        val nextLinkReg = "<([\\w\\d/?=:.]*)>;[ ]rel=\"next\"".toRegex()
        val future = Future.future<Unit>()

        fun process(linkToProcess: String) {
            httpClient.getAbs(linkToProcess).authHeader().handler { resp ->
                resp.bodyHandler { body ->
                    body.toJsonArray().forEach { node -> if (node is JsonObject) action(node) }
                    val headers = resp.headers()
                    if (headers.contains("link")) {
                        val nextLink = nextLinkReg.toPattern().matcher(headers["link"])
                        if (nextLink.find()) {
                            process(nextLink.group(1))
                        } else {
                            future.complete()
                        }
                    } else {
                        future.complete()
                    }
                }
            }.end()
        }
        process(link)
        return future
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

private inline fun GithubVerticle.contents(relPath: String): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/contents/$relPath"
}

private inline fun GithubVerticle.issueComments(issueNum: Int): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/issues/$issueNum/comments"
}
