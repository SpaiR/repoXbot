package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.constant.ACTION
import io.github.spair.repoxbot.constant.MERGED
import io.github.spair.repoxbot.constant.PULL_REQUEST
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestType
import io.github.spair.repoxbot.util.valueOfIgnoreCase
import io.vertx.core.json.JsonObject

class JsonToPullRequestCodec : LocalMessageCodec<JsonObject, PullRequest>() {

    companion object {
        const val NAME = "localJsonToPullRequestCodec"
    }

    override fun name(): String = NAME

    override fun transform(json: JsonObject): PullRequest {
        val type = identifyType(json)
        return PullRequest(type)
    }

    private fun identifyType(json: JsonObject): PullRequestType {
        return valueOfIgnoreCase(json.getString(ACTION), PullRequestType.UNDEFINED).also {
            if (it == PullRequestType.CLOSED && json.getJsonObject(PULL_REQUEST).getBoolean(MERGED))
                PullRequestType.MERGED
        }
    }
}
