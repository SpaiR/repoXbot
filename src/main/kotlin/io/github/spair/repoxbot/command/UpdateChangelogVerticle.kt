package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_UPDATE
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.RepoXBotConfig
import io.github.spair.repoxbot.dto.UpdateFileInfo
import io.github.spair.repoxbot.logic.generateChangelog
import io.github.spair.repoxbot.logic.mergeChangelogWithHtml
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory

class UpdateChangelogVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(UpdateChangelogVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_UPDATE) { msg ->
            generateChangelog(msg.body())?.letIfNotEmpty { changelog ->
                eventBus.send<RepoXBotConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
                    val config = readConfigRes.result().body()
                    val changelogPath = config.changelogPath
                    eventBus.send<String>(EB_GITHUB_FILE_READ, changelogPath) { readFileRes ->
                        if (readFileRes.succeeded()) {
                            val updateMessage = "Automatic changelog generation for PR #${changelog.pullRequestNumber}"
                            val newChangelogHtml = mergeChangelogWithHtml(changelog, readFileRes.result().body())
                            eventBus.send(EB_GITHUB_FILE_UPDATE, UpdateFileInfo(changelogPath, updateMessage, newChangelogHtml))
                        } else {
                            logger.error("Fail to read changelog file", readFileRes.cause())
                        }
                    }
                }
            }
        }
    }
}
