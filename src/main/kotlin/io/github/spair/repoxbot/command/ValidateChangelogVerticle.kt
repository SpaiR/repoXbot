package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_CHANGELOG_VALIDATE
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.RemoteConfig
import io.github.spair.repoxbot.logic.generateChangelog
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory

private const val CHANGELOG_STATUS = "###### Changelog status"

private const val OK_STATUS = ":heavy_check_mark:"
private const val NEUTRAL_STATUS = ":radio_button:"
private const val FAIL_STATUS = ":x:"

class ValidateChangelogVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(ValidateChangelogVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_CHANGELOG_VALIDATE) { msg ->
            val changelog = generateChangelog(msg.body())

            if (changelog == null) {
                sendOrUpdateStatus("$CHANGELOG_STATUS: $NEUTRAL_STATUS No changelog")
                return@localConsumer
            }

            if (changelog.isEmpty()) {
                sendOrUpdateStatus("$CHANGELOG_STATUS: $FAIL_STATUS Empty changelog (check markdown correctness)")
            } else {
                validateFromConfig(changelog)
            }
        }
    }

    private fun validateFromConfig(changelog: Changelog) {
        eventBus.send<RemoteConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
            if (readConfigRes.succeeded()) {
                val configChangelogClasses = readConfigRes.result().body().classes

                if (configChangelogClasses.isEmpty()) {
                    sendOrUpdateStatus("$CHANGELOG_STATUS: $OK_STATUS")
                    return@send
                }

                findInvalidClasses(configChangelogClasses, changelog.entries).let {
                    if (it.isEmpty()) {
                        sendOrUpdateStatus("$CHANGELOG_STATUS: $OK_STATUS")
                    } else {
                        sendOrUpdateStatus("$CHANGELOG_STATUS: $FAIL_STATUS Invalid changelog classes (${it.joinToString()})")
                    }
                }
            } else {
                logger.error("Fail to read config from github", readConfigRes.cause())
            }
        }
    }

    private fun findInvalidClasses(configChangelogClasses: Map<String, String>, changelogEntries: List<ChangelogEntry>): Set<String> {
        val invalidClasses = mutableSetOf<String>()

        changelogEntries.forEach { changelogEntry ->
            if (!configChangelogClasses.containsKey(changelogEntry.className)) {
                invalidClasses.add(changelogEntry.className)
            }
        }

        return invalidClasses
    }

    private fun sendOrUpdateStatus(statusMsg: String) {
        println(statusMsg)
    }
}
