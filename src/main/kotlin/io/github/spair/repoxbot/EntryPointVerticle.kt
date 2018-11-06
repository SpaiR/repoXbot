package io.github.spair.repoxbot

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServerRequest
import java.net.HttpURLConnection

class EntryPointVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        vertx.createHttpServer().requestHandler { request ->
            when (request.path()) {
                getConfig(ENTRY_POINT) -> handle(request)
                else -> request.response().setStatusCode(HttpURLConnection.HTTP_NOT_FOUND).end()
            }
        }.listen(getConfig(PORT).toInt(), reporter(startFuture))
    }

    private fun handle(request: HttpServerRequest) {
        request.response().end("Hello, World!")
    }
}
