package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_VALIDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_ISSUE_LIST
import io.github.spair.repoxbot.dto.IssueComment
import io.github.spair.repoxbot.dto.PullRequest
import io.vertx.core.AbstractVerticle

class ValidateChangelogVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_VALIDATE) { msg ->
            val pullRequest = msg.body()
            eventBus.send<List<IssueComment>>(EB_GITHUB_ISSUE_LIST, pullRequest.number) { issueComments ->
                println(issueComments.result().body())
            }
        }
    }
}
