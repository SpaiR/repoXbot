package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.logic.generateChangelog
import io.vertx.core.AbstractVerticle

class UpdateChangelogVerticle : AbstractVerticle() {

    override fun start() {
        vertx.eventBus().let { eventBust ->
            eventBust.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_UPDATE) { msg ->
                generateChangelog(msg.body())?.applyIfNotEmpty {
                    println(this)
                }
            }
        }
    }
}
