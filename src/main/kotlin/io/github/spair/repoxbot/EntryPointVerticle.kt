package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import com.fasterxml.jackson.core.JsonParseException
import io.github.spair.repoxbot.dto.codec.JsonToIssueCodec
import io.github.spair.repoxbot.dto.codec.JsonToPullRequestCodec
import io.github.spair.repoxbot.logic.isCorrectSignature
import io.github.spair.repoxbot.util.getSharedConfig
import io.github.spair.repoxbot.util.reporter
import io.github.spair.repoxbot.util.send
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.net.HttpURLConnection

class EntryPointVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(EntryPointVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        vertx.createHttpServer().requestHandler { request ->
            if (isValidRequest(request)) {
                handle(request)
            } else {
                request.response().setStatusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE).end()
            }
        }.exceptionHandler {
            logger.error("Server exception", it)
        }.listen(8080, reporter(startFuture))
    }

    private fun isValidRequest(request: HttpServerRequest): Boolean {
        val methodIsPost = request.method() == HttpMethod.POST
        val contentTypeValid = request.headers().contains(HttpHeaders.CONTENT_TYPE) &&
            request.getHeader(HttpHeaders.CONTENT_TYPE) == "application/json"

        val shouldCheckSign = getSharedConfig(CHECK_SIGN).toBoolean()
        val signHeaderChecked = !shouldCheckSign || (shouldCheckSign && request.headers().contains(SIGNATURE))

        val hasEventHeader = request.headers().contains(EVENT_HEADER)

        return methodIsPost && contentTypeValid && signHeaderChecked && hasEventHeader
    }

    private fun handle(request: HttpServerRequest) {
        request.bodyHandler { body ->
            try {
                val signature = request.headers()[SIGNATURE]?.substringAfter("sha1=")
                val secretKey = getSharedConfig(GITHUB_SECRET)
                val payload = body.toJsonObject()

                val shouldCheckSign = getSharedConfig(CHECK_SIGN).toBoolean()

                if (!shouldCheckSign || (shouldCheckSign && isCorrectSignature(signature!!, secretKey, payload.toString()))) {
                    val response = processPayload(request.headers()[EVENT_HEADER], payload)
                    request.response().setStatusCode(HttpURLConnection.HTTP_OK).end(response)
                } else {
                    request.response().setStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED).end()
                }
            } catch (e: JsonParseException) {
                request.response().setStatusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE).end()
            } catch (e: Exception) {
                logger.error("Exception on body handling", e)
                request.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR).end()
            }
        }
    }

    private fun processPayload(event: String, payload: JsonObject): String {
        return when (event) {
            EVENT_PING -> {
                val zen = payload.getString("zen")
                logger.info("Ping event caught. Zen: $zen")
                "Pong! Zen: '$zen'"
            }
            EVENT_PULL_REQUEST -> {
                vertx.eventBus().send(EB_EVENT_PULLREQUEST, payload, JsonToPullRequestCodec.NAME)
                "Pull request event caught. Async handling in process."
            }
            EVENT_ISSUES -> {
                vertx.eventBus().send(EB_EVENT_ISSUES, payload, JsonToIssueCodec.NAME)
                "Issue event caught. Async handling in process."
            }
            else -> "Unexpected event ($event) caught."
        }
    }
}
