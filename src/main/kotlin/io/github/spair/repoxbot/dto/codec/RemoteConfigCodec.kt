package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.RemoteConfig

class RemoteConfigCodec : LocalMessageCodec<RemoteConfig, RemoteConfig>() {

    override fun transform(remoteConfig: RemoteConfig): RemoteConfig = remoteConfig

    override fun name(): String = this::class.java.name
}
