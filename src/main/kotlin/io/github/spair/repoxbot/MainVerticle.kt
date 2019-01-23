package io.github.spair.repoxbot

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.*       // ktlint-disable
import io.github.spair.repoxbot.dto.codec.* // ktlint-disable
import io.github.spair.repoxbot.command.UpdateChangelogVerticle
import io.github.spair.repoxbot.command.ValidateChangelogVerticle
import io.github.spair.repoxbot.command.LabelPullRequestVerticle
import io.github.spair.repoxbot.command.LabelIssueVerticle
import io.github.spair.repoxbot.util.reporter
import io.github.spair.repoxbot.util.sharedConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Verticle
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

        Future.future<Void>().apply {
            deployVerticles(this)
        }.setHandler {
            if (it.succeeded()) {
                logger.info("RepoXBot configured and initialized! " +
                    "GitHub path: '${sharedConfig[GITHUB_ORG]}/${sharedConfig[GITHUB_REPO]}' " +
                    "Remote config path: '${sharedConfig[CONFIG_PATH]}'"
                )
                startFuture.complete()
            } else {
                logger.error("Something went wrong and RepoXBot can't be configured and initialized")
                startFuture.fail(it.cause())
            }
        }
    }

    private fun initializeConfig() {
        val setConfigOrThrow = { propName: String ->
            sharedConfig[propName] = System.getenv(propName)
                ?: throw IllegalStateException("'$propName' value should be specified as environment variable")
        }
        val setConfigOrDefault = { propName: String, default: String ->
            sharedConfig[propName] = System.getenv(propName) ?: default
        }

        setConfigOrThrow(GITHUB_ORG)
        setConfigOrThrow(GITHUB_REPO)
        setConfigOrThrow(GITHUB_TOKEN)
        setConfigOrThrow(GITHUB_SECRET)

        setConfigOrDefault(CHECK_SIGN, DEFAULT_CHECK_SIGN)
        setConfigOrDefault(CONFIG_PATH, DEFAULT_CONFIG_PATH)
        setConfigOrDefault(AGENT_NAME, DEFAULT_AGENT_NAME)
        setConfigOrDefault(USE_HASH, DEFAULT_USE_HASH)
        setConfigOrDefault(HASH_PATH, DEFAULT_HASH_PATH)

        logger.info("Configuration initialized")
    }

    private fun registerEventBusCodecs() {
        with(vertx.eventBus()) {
            registerDefaultCodec(UpdateFileInfo::class.java, UpdateFileInfoCodec())
            registerDefaultCodec(PullRequest::class.java, PullRequestCodec())
            registerDefaultCodec(Issue::class.java, IssueCodec())
            registerDefaultCodec(RepoXBotConfig::class.java, RepoXBotConfigCodec())
            registerDefaultCodec(UpdateCommentInfo::class.java, UpdateCommentInfoCodec())
            registerDefaultCodec(UpdateLabelInfo::class.java, UpdateLabelInfoCodec())
            registerDefaultCodec(FileLocation::class.java, FileLocationCodec())

            registerCodec(JsonToPullRequestCodec())
            registerCodec(StringJsonToRepoXBotConfigCodec())
            registerCodec(IssueCommentListCodec())
            registerCodec(JsonToIssueCodec())

            logger.info("Event bus codecs registered")
        }
    }

    private fun deployVerticles(future: Future<Void>) {
        fun initVerticle(verticle: Verticle): Future<Void> = Future.future<Void>().apply {
            vertx.deployVerticle(verticle, reporter(this) { logger.info("Verticle '${verticle.javaClass.name}' deployed") })
        }

        val verticlesList = listOf(
            initVerticle(EntryPointVerticle()),

            initVerticle(GithubVerticle()),
            initVerticle(DistributionVerticle()),

            initVerticle(UpdateChangelogVerticle()),
            initVerticle(ValidateChangelogVerticle()),
            initVerticle(LabelPullRequestVerticle()),
            initVerticle(LabelIssueVerticle())
        )

        CompositeFuture.all(verticlesList).setHandler(reporter(future) {
            logger.info("All verticles deployed (${verticlesList.size})")
        })
    }
}
