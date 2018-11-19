package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.AGENT_NAME
import io.github.spair.repoxbot.constant.EB_COMMAND_PULLREQUEST_LABEL
import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.RemoteConfig
import io.github.spair.repoxbot.logic.generateChangelog
import io.github.spair.repoxbot.logic.getLabelsFromChangelog
import io.github.spair.repoxbot.logic.getLabelsFromDiffText
import io.github.spair.repoxbot.util.getSharedConfig
import io.vertx.core.*
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

            val remoteConfigFuture = Future.future<RemoteConfig>()
            val diffTextFuture = Future.future<String>()

            eventBus.send<RemoteConfig>(EB_GITHUB_CONFIG_READ, null) { remoteConfigFuture.complete(it.result().body()) }
            loadDiffText(pullRequest.diffLink).setHandler { diffTextFuture.complete(it.result()) }

            val labelsToAdd = mutableSetOf<String>()

            CompositeFuture.all(remoteConfigFuture, diffTextFuture).setHandler {
                val remoteConfig = remoteConfigFuture.result()

                generateChangelog(pullRequest)?.letIfNotEmpty { changelog ->
                    labelsToAdd.addAll(getLabelsFromChangelog(changelog, remoteConfig.classes))
                }
                labelsToAdd.addAll(getLabelsFromDiffText(diffTextFuture.result(), remoteConfig.pathsLabels))

                println(labelsToAdd)
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
