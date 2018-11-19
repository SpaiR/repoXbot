@file:Suppress("NOTHING_TO_INLINE")

package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.*       // ktlint-disable
import io.github.spair.repoxbot.dto.codec.IssueCommentListCodec
import io.github.spair.repoxbot.dto.codec.StringJsonToRepoXBotConfigCodec
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.net.HttpURLConnection
import java.util.Base64

class GithubVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(GithubVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }
    private val httpClient by lazy { vertx.createHttpClient() }

    override fun start() {
        eventBus.localConsumer<JsonObject>(EB_GITHUB_CONFIG_READ, readRepoXBotConfig())

        eventBus.localConsumer<String>(EB_GITHUB_FILE_READ, readGithubFile())
        eventBus.localConsumer<UpdateFileInfo>(EB_GITHUB_FILE_UPDATE, updateGithubFile())

        eventBus.localConsumer<Int>(EB_GITHUB_ISSUE_COMMENT_LIST, listIssueComments())
        eventBus.localConsumer<UpdateCommentInfo>(EB_GITHUB_ISSUE_COMMENT_CREATE, createIssueComment())
        eventBus.localConsumer<UpdateCommentInfo>(EB_GITHUB_ISSUE_COMMENT_UPDATE, updateIssueComment())

        eventBus.localConsumer<UpdateLabelInfo>(EB_GITHUB_ISSUE_LABELS_ADD, addIssueLabels())
    }

    private fun readRepoXBotConfig() = Handler<Message<JsonObject>> { msg ->
        httpClient.getAbs(contents(getSharedConfig(CONFIG_PATH))).authHeader().handler {
            if (it.statusCode() == HttpURLConnection.HTTP_OK) {
                it.bodyHandler { body ->
                    msg.reply(body.toJsonObject().readContents(), DeliveryOptions().setCodecName(StringJsonToRepoXBotConfigCodec.NAME))
                }
            } else {
                msg.reply(RepoXBotConfig())
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
                val fileSha = ar.result()
                httpClient.putAbs(contents(updateFileInfo.path)).authHeader().jsonHeader().handler {
                    if (it.statusCode() != HttpURLConnection.HTTP_OK) {
                        logger.error("Unable to update Github file: ${updateFileInfo.path} Code: ${it.statusCode()}")
                    }
                }.end(JsonObject().apply {
                    put(MESSAGE, updateFileInfo.message)
                    put(CONTENT, updateFileInfo.content.encodeBase64())
                    put(SHA, fileSha)
                }.toBuffer())
            } else {
                logger.error("Error while updating Github file", ar.cause())
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

    private fun createIssueComment() = Handler<Message<UpdateCommentInfo>> { msg ->
        val updateCommentInfo = msg.body()
        httpClient.postAbs(issueComments(updateCommentInfo.id)).authHeader().jsonHeader().handler {
            if (it.statusCode() != HttpURLConnection.HTTP_CREATED) {
                logger.error("Unable to create comment for issue: ${updateCommentInfo.id} Code: ${it.statusCode()}")
            }
        }.end(JsonObject().apply {
            put(BODY, updateCommentInfo.text)
        }.toBuffer())
    }

    private fun updateIssueComment() = Handler<Message<UpdateCommentInfo>> { msg ->
        val updateCommentInfo = msg.body()
        httpClient.requestAbs(HttpMethod.PATCH, issueComment(updateCommentInfo.id)).authHeader().jsonHeader().handler {
            if (it.statusCode() != HttpURLConnection.HTTP_OK) {
                logger.error("Unable to update comment with id: ${updateCommentInfo.id} Code: ${it.statusCode()}")
            }
        }.end(JsonObject().apply {
            put(BODY, updateCommentInfo.text)
        }.toBuffer())
    }

    private fun addIssueLabels() = Handler<Message<UpdateLabelInfo>> { msg ->
        val updateLabelInfo = msg.body()
        httpClient.postAbs(issueLabels(updateLabelInfo.id)).authHeader().jsonHeader().handler {
            if (it.statusCode() != HttpURLConnection.HTTP_OK) {
                logger.error("Unable to add labels to issue with id: ${updateLabelInfo.id} Code: ${it.statusCode()}")
            }
        }.end(JsonArray(updateLabelInfo.labels.toList()).toBuffer())
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
        return Future.future<Unit>().apply {
            fun process(linkToProcess: String) {
                httpClient.getAbs(linkToProcess).authHeader().handler { resp ->
                    resp.bodyHandler { body ->
                        body.toJsonArray().forEach { node ->
                            if (node is JsonObject) {
                                action(node)
                            }
                        }
                        val headers = resp.headers()
                        if (headers.contains("link")) {
                            val nextLink = nextLinkReg.toPattern().matcher(headers["link"])
                            if (nextLink.find()) {
                                process(nextLink.group(1))
                            } else {
                                complete()
                            }
                        } else {
                            complete()
                        }
                    }
                }.end()
            }
            process(link)
        }
    }

    private fun HttpClientRequest.authHeader(): HttpClientRequest = apply {
        putHeader(HttpHeaders.AUTHORIZATION, "token ${getSharedConfig(GITHUB_TOKEN)}")
        putHeader(HttpHeaders.USER_AGENT, getSharedConfig(AGENT_NAME))
    }

    private fun HttpClientRequest.jsonHeader(): HttpClientRequest = apply {
        putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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

private inline fun GithubVerticle.issueComment(commentId: Int): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/issues/comments/$commentId"
}

private inline fun GithubVerticle.issueLabels(issueNum: Int): String {
    return "$GITHUB_API_URL/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/issues/$issueNum/labels"
}
