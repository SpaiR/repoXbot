package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.RepoXBotConfig
import io.vertx.core.json.Json

class StringJsonToRepoXBotConfigCodec : LocalMessageCodec<String, RepoXBotConfig>() {

    companion object {
        const val NAME = "localStringJsonToRepoXBotConfigCodec"
    }

    override fun transform(stringJson: String): RepoXBotConfig = Json.decodeValue(stringJson, RepoXBotConfig::class.java)

    override fun name(): String = NAME
}
