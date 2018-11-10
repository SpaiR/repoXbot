package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.RemoteConfig
import io.vertx.core.json.Json

class StringJsonToRemoteConfigCodec : LocalMessageCodec<String, RemoteConfig>() {

    companion object {
        const val NAME = "localStringJsonToRemoteConfigCodec"
    }

    override fun transform(stringJson: String): RemoteConfig = Json.decodeValue(stringJson, RemoteConfig::class.java)

    override fun name(): String = NAME
}
