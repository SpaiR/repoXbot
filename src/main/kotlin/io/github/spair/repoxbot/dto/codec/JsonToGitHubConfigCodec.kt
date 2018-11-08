package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.GithubConfig
import io.vertx.core.json.JsonObject

class JsonToGithubConfigCodec : LocalMessageCodec<JsonObject, GithubConfig>() {

    companion object {
        const val NAME = "localJsonToGitHubConfigCodec"
    }

    override fun transform(json: JsonObject): GithubConfig {
        return GithubConfig(json.getString(GithubConfig::changelogPath.name))
    }

    override fun name(): String = NAME
}
