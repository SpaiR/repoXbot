package io.github.spair.repoxbot.dto.codec

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

abstract class LocalMessageCodec<S, R> : MessageCodec<S, R> {

    override fun decodeFromWire(pos: Int, buffer: Buffer?): R = TODO("not implemented, only local transform support")

    override fun systemCodecID(): Byte = -1

    override fun encodeToWire(buffer: Buffer?, s: S) = TODO("not implemented, only local transform support")
}
