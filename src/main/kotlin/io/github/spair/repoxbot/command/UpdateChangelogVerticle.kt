package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_UPDATE
import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.RemoteConfig
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
                eventBus.send<RemoteConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
                    if (readConfigRes.succeeded()) {
                        val remoteConfig = readConfigRes.result().body()
                        val changelogPath = remoteConfig.changelogPath
                        eventBus.send<String>(EB_GITHUB_FILE_READ, changelogPath) { readFileRes ->
                            if (readFileRes.succeeded()) {
                                val updateMessage = getUpdateMessage(remoteConfig, changelog)
                                val newChangelogHtml = mergeChangelogWithHtml(changelog, readFileRes.result().body())
                                eventBus.send(EB_GITHUB_FILE_UPDATE, UpdateFileInfo(changelogPath, updateMessage, newChangelogHtml))
                            } else {
                                logger.error("Fail to read changelog file", readFileRes.cause())
                            }
                        }
                    } else {
                        logger.error("Fail to read config from github", readConfigRes.cause())
                    }
                }
            }
        }
    }

    private fun getUpdateMessage(remoteConfig: RemoteConfig, changelog: Changelog): String {
        return remoteConfig.updateMessage.replace("{prNum}", changelog.pullRequestNumber.toString())
    }
}
