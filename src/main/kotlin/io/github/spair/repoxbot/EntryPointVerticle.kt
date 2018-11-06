package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.util.Signature
import io.github.spair.repoxbot.util.getSharedConfig
import io.github.spair.repoxbot.util.reporter
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.net.HttpURLConnection
import com.fasterxml.jackson.core.JsonParseException

class EntryPointVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(EntryPointVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        vertx.createHttpServer().requestHandler { request ->
            if (isValidRequest(request)) {
                handle(request)
            } else {
                request.response().setStatusCode(HttpURLConnection.HTTP_NOT_FOUND).end()
            }
        }.exceptionHandler {
            logger.error("Server exception", it)
        }.listen(getSharedConfig(PORT).toInt(), reporter(startFuture))
    }

    private fun isValidRequest(request: HttpServerRequest): Boolean {
        return with(request) {
            method() == HttpMethod.POST && path() == getSharedConfig(ENTRY_POINT) && with(headers()) {
                contains(HttpHeaders.CONTENT_TYPE) && this[HttpHeaders.CONTENT_TYPE] == APPLICATION_JSON &&
                    contains(SIGNATURE) && contains(EVENT_HEADER)
            }
        }
    }

    private fun handle(request: HttpServerRequest) {
        request.bodyHandler { body ->
            try {
                val signature = request.headers()[SIGNATURE]
                val secretKey = getSharedConfig(GITHUB_SECRET)
                val payload = body.toJsonObject()

                if (Signature.isEqualSignature(signature, secretKey, payload.toString())) {
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
            PING_EVENT -> {
                val zen = payload.getString("zen")
                logger.info("Ping event caught. Zen: $zen")
                zen
            }
            else -> "Unknown event caught. Event: $event"
        } ?: "Empty response"
    }
}
