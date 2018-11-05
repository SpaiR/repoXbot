package io.github.spair.repoxbot

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import java.io.File
import java.lang.IllegalStateException

class MainVerticle : AbstractVerticle() {

    private val defaultPort = "8080"
    private val defaultEntryPoint = "/handle"

    private val verticlesToDeploy = listOf(EntryPointVerticle::class.java.name)

    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)

    init {
        File("logs").mkdir()
    }

    override fun start(startFuture: Future<Void>) {
        initializeConfig()
        deployVerticles()
        startFuture.complete()
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

        setConfigOrDefault(PORT, defaultPort)
        setConfigOrDefault(ENTRY_POINT, defaultEntryPoint)

        logger.info("Configuration initialized! " +
                "RepoXBot now works with next GitHub repository: ${sharedConfig[GITHUB_ORG]}/${sharedConfig[GITHUB_REPO]}; " +
                "Entry point: '${sharedConfig[ENTRY_POINT]}'; Port: '${sharedConfig[PORT]}'")
    }

    private fun deployVerticles() {
        verticlesToDeploy.forEach { verticleName ->
            vertx.deployVerticle(verticleName) { deployResult ->
                if (deployResult.succeeded()) {
                    logger.info("Verticle '$verticleName' deployed")
                } else {
                    throw IllegalStateException("Fail to deploy $verticleName", deployResult.cause())
                }
            }
        }
    }
}
