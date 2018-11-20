package io.github.spair.repoxbot.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus

// AbstractVerticle
val AbstractVerticle.sharedConfig
    get() = vertx.sharedData().getLocalMap<String, String>("SHARED_CONFIGURATION_MAP")!!

fun AbstractVerticle.getSharedConfig(key: String) = sharedConfig[key]!!

// Enum
inline fun <reified E : Enum<E>> valueOfIgnoreCase(name: String, default: E) = enumValues<E>().find {
    it.name.equals(name, ignoreCase = true)
} ?: default

// EventBus
fun EventBus.send(address: String, message: Any, codecName: String) = send(address, message, DeliveryOptions().setCodecName(codecName))!!
