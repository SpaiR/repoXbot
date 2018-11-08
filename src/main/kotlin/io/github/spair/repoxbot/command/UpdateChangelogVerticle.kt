package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_READ
import io.github.spair.repoxbot.dto.GitHubConfig
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.logic.addChangelogToHtml
import io.github.spair.repoxbot.logic.generateChangelog
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory

class UpdateChangelogVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(UpdateChangelogVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_UPDATE) { msg ->
            generateChangelog(msg.body())?.letIfNotEmpty { changelog ->
                eventBus.send<GitHubConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
                    if (readConfigRes.succeeded()) {
                        val changelogPath = readConfigRes.result().body().changelogPath
                        eventBus.send<String>(EB_GITHUB_FILE_READ, changelogPath) { readFileRes ->
                            if (readFileRes.succeeded()) {
                                val newChangelogHtml = addChangelogToHtml(readFileRes.result().body(), changelog)
                            } else {
                                logger.error("Fail to read changelog file", readFileRes.cause())
                            }
                        }
                    } else {
                        logger.error("Fail read config from github", readConfigRes.cause())
                    }
                }
            }
        }
    }
}
