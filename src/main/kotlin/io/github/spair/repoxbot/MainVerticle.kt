package io.github.spair.repoxbot

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import java.io.File

class MainVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)

    init {
        File("logs").mkdir()
    }

    override fun start(startFuture: Future<Void>) {
        vertx.createHttpServer().requestHandler { req ->
            req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!")
        }.listen(3000) { http ->
            if (http.succeeded()) {
                startFuture.complete()
                logger.info("HTTP server started on port ${http.result().actualPort()}")
            } else {
                startFuture.fail(http.cause())
            }
        }
    }
}
