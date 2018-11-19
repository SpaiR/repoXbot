package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.Issue
import io.github.spair.repoxbot.dto.IssueAction
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestAction
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message

class DistributionVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_EVENT_PULLREQUEST, consumePullRequest())
        eventBus.localConsumer<Issue>(EB_EVENT_ISSUES, consumeIssue())
    }

    private fun consumePullRequest() = Handler<Message<PullRequest>> { msg ->
        println("Pull request consumed: ${msg.body()}") // TODO: remove
        val pullRequest = msg.body()
        when (pullRequest.action) {
            PullRequestAction.OPENED -> {
                eventBus.send(EB_COMMAND_PULLREQUEST_LABEL, pullRequest)
                eventBus.send(EB_COMMAND_CHANGELOG_VALIDATE, pullRequest)
            }
            PullRequestAction.EDITED -> eventBus.send(EB_COMMAND_CHANGELOG_VALIDATE, pullRequest)
            PullRequestAction.MERGED -> eventBus.send(EB_COMMAND_CHANGELOG_UPDATE, pullRequest)
            PullRequestAction.CLOSED, PullRequestAction.UNDEFINED -> {}
        }
    }

    private fun consumeIssue() = Handler<Message<Issue>> { msg ->
        println("Issue consumed: ${msg.body()}")    // TODO: remove
        val issue = msg.body()
        when (issue.action) {
            IssueAction.OPENED -> eventBus.send(EB_COMMAND_ISSUE_LABEL, issue)
            IssueAction.UNDEFINED -> {}
        }
    }
}
