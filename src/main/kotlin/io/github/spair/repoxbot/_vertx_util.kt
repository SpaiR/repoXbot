package io.github.spair.repoxbot

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler

fun <T> reporter(future: Future<Void>): Handler<AsyncResult<T>> = Handler {
    when (it.succeeded()) {
        true -> future.complete()
        else -> future.fail(it.cause())
    }
}

fun <T> reporter(future: Future<Void>, action: () -> Unit): Handler<AsyncResult<T>> = Handler {
    if (it.succeeded()) {
        action()
        future.complete()
    } else {
        future.fail(it.cause())
    }
}
