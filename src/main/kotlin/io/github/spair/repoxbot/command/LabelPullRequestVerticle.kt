package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.AGENT_NAME
import io.github.spair.repoxbot.constant.EB_COMMAND_PULLREQUEST_LABEL
import io.github.spair.repoxbot.constant.EB_GITHUB_ISSUE_LABELS_ADD
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.RepoXBotConfig
import io.github.spair.repoxbot.dto.UpdateLabelInfo
import io.github.spair.repoxbot.logic.generateChangelog
import io.github.spair.repoxbot.logic.getLabelsFromChangelog
import io.github.spair.repoxbot.logic.getLabelsFromDiffText
import io.github.spair.repoxbot.util.getSharedConfig
import io.github.spair.repoxbot.util.readConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.CompositeFuture
import io.vertx.core.http.HttpHeaders
import io.vertx.core.logging.LoggerFactory
import java.net.HttpURLConnection

class LabelPullRequestVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(LabelPullRequestVerticle::class.java)
    private val eventBus by lazy { vertx.eventBus() }
    private val httpClient by lazy { vertx.createHttpClient() }

    override fun start() {
        eventBus.localConsumer<PullRequest>(EB_COMMAND_PULLREQUEST_LABEL) { msg ->
            val pullRequest = msg.body()

            val configFuture = Future.future<RepoXBotConfig>()
            val diffTextFuture = Future.future<String>()

            eventBus.readConfig { configFuture.complete(it) }
            loadDiffText(pullRequest.diffLink).setHandler { diffTextFuture.complete(it.result()) }

            CompositeFuture.all(configFuture, diffTextFuture).setHandler {
                val labelsToAdd = mutableSetOf<String>()
                val config = configFuture.result()

                labelsToAdd.addAll(getLabelsFromChangelog(generateChangelog(pullRequest), config.changelogClasses))
                labelsToAdd.addAll(getLabelsFromDiffText(diffTextFuture.result(), config.diffPathsLabels))

                if (labelsToAdd.isNotEmpty()) {
                    eventBus.send(EB_GITHUB_ISSUE_LABELS_ADD, UpdateLabelInfo(pullRequest.number, labelsToAdd))
                }
            }
        }
    }

    private fun loadDiffText(diffLink: String): Future<String> {
        val future = Future.future<String>()
        httpClient.getAbs(diffLink).putHeader(HttpHeaders.USER_AGENT, getSharedConfig(AGENT_NAME)).setFollowRedirects(true).handler {
            if (it.statusCode() == HttpURLConnection.HTTP_OK) {
                it.bodyHandler { body -> future.complete(body.toString()) }
            } else {
                logger.warn("Unable to get diff by link: $diffLink")
                future.complete("")
            }
        }.end()
        return future
    }
}
