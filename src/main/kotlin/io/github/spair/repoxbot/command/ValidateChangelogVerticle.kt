package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.*  //ktlint-disable
import io.github.spair.repoxbot.dto.*   // ktlint-disable
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
            val pullRequest = msg.body()
            val changelog = generateChangelog(pullRequest)

            if (changelog == null) {
                sendOrUpdateStatus("$CHANGELOG_STATUS: $NEUTRAL_STATUS No changelog", pullRequest.number)
                return@localConsumer
            }

            if (changelog.isEmpty()) {
                val statusMsg = "$CHANGELOG_STATUS: $FAIL_STATUS Empty changelog (check markdown correctness)"
                sendOrUpdateStatus(statusMsg, pullRequest.number)
            } else {
                validateFromConfig(changelog)
            }
        }
    }

    private fun validateFromConfig(changelog: Changelog) {
        eventBus.send<RemoteConfig>(EB_GITHUB_CONFIG_READ, null) { readConfigRes ->
            val configChangelogClasses = readConfigRes.result().body().classes

            if (configChangelogClasses.isEmpty()) {
                sendOrUpdateStatus("$CHANGELOG_STATUS: $OK_STATUS", changelog.pullRequestNumber)
                return@send
            }

            findInvalidClasses(configChangelogClasses, changelog.entries).let {
                if (it.isEmpty()) {
                    sendOrUpdateStatus("$CHANGELOG_STATUS: $OK_STATUS", changelog.pullRequestNumber)
                } else {
                    val statusMsg = "$CHANGELOG_STATUS: $FAIL_STATUS Invalid changelog classes (${it.joinToString()})"
                    sendOrUpdateStatus(statusMsg, changelog.pullRequestNumber)
                }
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

    private fun sendOrUpdateStatus(statusMsg: String, prNum: Int) {
        eventBus.send<List<IssueComment>>(EB_GITHUB_ISSUE_COMMENT_LIST, prNum) { resp ->
            if (resp.succeeded()) {
                val issueComments = resp.result().body()
                var commentId: Int = -1

                for (comment in issueComments) {
                    if (comment.body.contains(CHANGELOG_STATUS)) {
                        commentId = comment.id
                        break
                    }
                }

                if (commentId == -1) {
                    eventBus.send(EB_GITHUB_ISSUE_COMMENT_CREATE, UpdateCommentInfo(prNum, statusMsg))
                } else {
                    eventBus.send(EB_GITHUB_ISSUE_COMMENT_UPDATE, UpdateCommentInfo(commentId, statusMsg))
                }
            } else {
                logger.error("Unable to get list of comments for PR#$prNum")
            }
        }
    }
}
