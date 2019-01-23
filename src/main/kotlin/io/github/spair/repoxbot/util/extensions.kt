package io.github.spair.repoxbot.util

import io.github.spair.repoxbot.constant.EB_GITHUB_CONFIG_READ
import io.github.spair.repoxbot.constant.GITHUB_ORG
import io.github.spair.repoxbot.constant.GITHUB_REPO
import io.github.spair.repoxbot.dto.FileLocation
import io.github.spair.repoxbot.dto.RepoXBotConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import java.math.BigInteger
import java.security.MessageDigest

// AbstractVerticle
val AbstractVerticle.sharedConfig
    get() = vertx.sharedData().getLocalMap<String, String>("SC")!!

fun AbstractVerticle.getSharedConfig(key: String) = sharedConfig[key]!!

fun AbstractVerticle.relLocation(path: String) = FileLocation(getSharedConfig(GITHUB_ORG), getSharedConfig(GITHUB_REPO), path)

// Enum
inline fun <reified E : Enum<E>> valueOfIgnoreCase(name: String, default: E) = enumValues<E>().find {
    it.name.equals(name, ignoreCase = true)
} ?: default

// EventBus
fun EventBus.send(address: String, message: Any, codecName: String) = send(address, message, DeliveryOptions().setCodecName(codecName))!!

fun EventBus.readConfig(handler: (RepoXBotConfig) -> Unit): EventBus {
    return this.send<RepoXBotConfig>(EB_GITHUB_CONFIG_READ, null) { handler(it.result().body()) }!!
}

// Message
fun <T> Message<T>.reply(msg: Any, codecName: String) = reply(msg, DeliveryOptions().setCodecName(codecName))

// String
fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}
