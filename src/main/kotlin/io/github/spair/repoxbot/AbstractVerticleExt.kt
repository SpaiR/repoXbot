package io.github.spair.repoxbot

import io.vertx.core.AbstractVerticle

val AbstractVerticle.sharedConfig
    get() = vertx.sharedData().getLocalMap<String, String>("SHARED_CONFIGURATION_MAP")!!

fun AbstractVerticle.getConfig(key: String) = sharedConfig[key]!!
