package io.github.spair.repoxbot.util

import io.vertx.core.AbstractVerticle

val AbstractVerticle.sharedConfig
    get() = vertx.sharedData().getLocalMap<String, String>("SHARED_CONFIGURATION_MAP")!!

fun AbstractVerticle.getSharedConfig(key: String) = sharedConfig[key]!!
