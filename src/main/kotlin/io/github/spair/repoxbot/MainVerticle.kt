package io.github.spair.repoxbot

import io.github.spair.repoxbot.command.UpdateChangelogVerticle
import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.codec.JsonToGitHubConfigCodec
import io.github.spair.repoxbot.dto.codec.JsonToPullRequestCodec
import io.github.spair.repoxbot.dto.codec.PullRequestCodec
import io.github.spair.repoxbot.util.reporter
import io.github.spair.repoxbot.util.sharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import java.io.File

class MainVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)

    init {
        File("logs").mkdir()
    }

    override fun start(startFuture: Future<Void>) {
        initializeConfig()
        registerEventBusCodecs()
        deployVerticles(startFuture)
    }

    private fun initializeConfig() {
        val setConfigOrThrow = { propName: String ->
            sharedConfig[propName] = System.getProperty(propName)
                ?: throw IllegalStateException("'$propName' value should be specified as system variable")
        }
        val setConfigOrDefault = { propName: String, default: String ->
            sharedConfig[propName] = System.getProperty(propName) ?: default
        }

        setConfigOrThrow(GITHUB_ORG)
        setConfigOrThrow(GITHUB_REPO)
        setConfigOrThrow(GITHUB_TOKEN)
        setConfigOrThrow(GITHUB_SECRET)

        setConfigOrDefault(PORT, DEFAULT_PORT)
        setConfigOrDefault(ENTRY_POINT, DEFAULT_ENTRY_POINT)
        setConfigOrDefault(CHECK_SIGN, DEFAULT_CHECK_SIGN)
        setConfigOrDefault(CONFIG_PATH, DEFAULT_CONFIG_PATH)
        setConfigOrDefault(AGENT_NAME, DEFAULT_AGENT_NAME)

        logger.info("Configuration initialized! " +
            "RepoXBot now works with next GitHub repository: ${sharedConfig[GITHUB_ORG]}/${sharedConfig[GITHUB_REPO]}; " +
            "Entry point: '${sharedConfig[ENTRY_POINT]}'; Port: '${sharedConfig[PORT]}'"
        )
    }

    private fun registerEventBusCodecs() {
        with(vertx.eventBus()) {
            registerDefaultCodec(PullRequest::class.java, PullRequestCodec())
            registerCodec(JsonToPullRequestCodec())
            registerCodec(JsonToGitHubConfigCodec())
            logger.info("Event bus codecs registered")
        }
    }

    private fun deployVerticles(future: Future<Void>) {
        CompositeFuture.all(listOf(
            initVerticle(EntryPointVerticle::class.java.name),
            initVerticle(GitHubVerticle::class.java.name),
            initVerticle(PullRequestVerticle::class.java.name),
            initVerticle(UpdateChangelogVerticle::class.java.name)
        )).setHandler(reporter(future) {
            logger.info("All verticles deployed")
        })
    }

    private fun initVerticle(verticleName: String): Future<Void> {
        return Future.future<Void>().also {
            vertx.deployVerticle(verticleName, reporter(it) { logger.info("Verticle '$verticleName' deployed") })
        }
    }
}
