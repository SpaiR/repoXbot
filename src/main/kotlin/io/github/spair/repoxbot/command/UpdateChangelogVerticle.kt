package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.FileLocation
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.UpdateFileInfo
import io.github.spair.repoxbot.logic.generateChangelog
import io.github.spair.repoxbot.logic.mergeChangelogWithHtml
import io.github.spair.repoxbot.util.getSharedConfig
import io.github.spair.repoxbot.util.md5
import io.github.spair.repoxbot.util.readConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory

class UpdateChangelogVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(UpdateChangelogVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_UPDATE) { msg ->
            generateChangelog(msg.body())?.runIfPresent { changelog ->
                eventBus.readConfig { config ->
                    val location = config.changelogLocation
                    eventBus.send<String>(EB_GITHUB_FILE_READ, location) { readFileRes ->
                        if (readFileRes.succeeded()) {
                            val newChangelogHtml = mergeChangelogWithHtml(changelog, readFileRes.result().body())
                            val updateMessage = "Automatic changelog generation for PR ${changelog.pullRequestLink}"
                            val updateChangelogInfo = UpdateFileInfo(location, updateMessage, newChangelogHtml)

                            eventBus.send(EB_GITHUB_FILE_UPDATE, updateChangelogInfo)

                            // Update hash file after 5 seconds delay to avoid 409 (conflict) error code.
                            if (getSharedConfig(USE_HASH).toBoolean()) {
                                vertx.setTimer(5000) {
                                    val hashLocation = FileLocation(location.org, location.repo, getSharedConfig(HASH_PATH))
                                    val updateHashInfo = UpdateFileInfo(hashLocation, "Update hash", newChangelogHtml.md5())
                                    eventBus.send(EB_GITHUB_FILE_UPDATE, updateHashInfo)
                                }
                            }
                        } else {
                            logger.error("Fail to read changelog file", readFileRes.cause())
                        }
                    }
                }
            }
        }
    }
}
