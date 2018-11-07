package io.github.spair.repoxbot.util

import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus

fun EventBus.send(address: String, message: Any, codecName: String) = send(address, message, DeliveryOptions().setCodecName(codecName))!!
