package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_VALIDATE
import io.github.spair.repoxbot.constant.EB_EVENT_PULLREQUEST
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestAction
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message

class DistributionVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_EVENT_PULLREQUEST, consumePullRequest())
    }

    private fun consumePullRequest() = Handler<Message<PullRequest>> { msg ->
        println("Pull request consumed: ${msg.body()}") // TODO: remove
        val pullRequest = msg.body()
        when (pullRequest.action) {
            PullRequestAction.OPENED -> eventBus.send(EB_COMMAND_CHANGELOG_VALIDATE, pullRequest)
            PullRequestAction.MERGED -> eventBus.send(EB_COMMAND_CHANGELOG_UPDATE, pullRequest)
            PullRequestAction.CLOSED, PullRequestAction.UNDEFINED -> {}
        }
    }
}
