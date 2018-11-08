package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.GitHubConfig
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

class JsonToGitHubConfigCodec : LocalMessageCodec<JsonObject, GitHubConfig>() {

    companion object {
        const val NAME = "localJsonToGitHubConfigCodec"
    }

    override fun transform(json: JsonObject?): GitHubConfig = Json.decodeValue(json.toString(), GitHubConfig::class.java)

    override fun name(): String = NAME
}
