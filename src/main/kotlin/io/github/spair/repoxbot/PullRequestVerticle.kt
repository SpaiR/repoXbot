package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.EB_EVENT_PULL_REQUEST
import io.github.spair.repoxbot.dto.PullRequest
import io.vertx.core.AbstractVerticle

class PullRequestVerticle : AbstractVerticle() {

    override fun start() {
        vertx.eventBus().localConsumer<PullRequest>(EB_EVENT_PULL_REQUEST) { msg ->
            println("Pull request consumed: ${msg.body()}")
        }
    }
}
