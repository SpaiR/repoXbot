package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_UPDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.constant.EB_GITHUB_FILE_READ
import io.github.spair.repoxbot.dto.GithubConfig
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.logic.mergeChangelogWithHtml
import io.github.spair.repoxbot.logic.generateChangelog
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory

class UpdateChangelogVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(UpdateChangelogVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_UPDATE) { msg ->
            generateChangelog(msg.body())?.letIfNotEmpty { changelog ->
                eventBus.send<GithubConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
                    if (readConfigRes.succeeded()) {
                        val githubConfig = readConfigRes.result().body()
                        eventBus.send<String>(EB_GITHUB_FILE_READ, githubConfig.changelogPath) { readFileRes ->
                            if (readFileRes.succeeded()) {
                                val newChangelogHtml = mergeChangelogWithHtml(changelog, readFileRes.result().body())
                                println(newChangelogHtml)
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
}
