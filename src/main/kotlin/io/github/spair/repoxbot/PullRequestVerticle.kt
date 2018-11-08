package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_EVENT_PULLREQUEST
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestAction
import io.vertx.core.AbstractVerticle

class PullRequestVerticle : AbstractVerticle() {

    override fun start() {
        vertx.eventBus().let { eventBus ->
            eventBus.localConsumer<PullRequest>(EB_EVENT_PULLREQUEST) { msg ->
                println("Pull request consumed: ${msg.body()}") // TODO: remove
                when (msg.body().action) {
                    PullRequestAction.MERGED -> eventBus.send(EB_COMMAND_CHANGELOG_UPDATE, msg.body())
                    PullRequestAction.CLOSED -> TODO()
                    PullRequestAction.UNDEFINED -> TODO()
                }
            }
        }
    }
}
