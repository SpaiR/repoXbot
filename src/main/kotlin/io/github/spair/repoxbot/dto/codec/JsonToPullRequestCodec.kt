package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.constant.*  // ktlint-disable
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestAction
import io.github.spair.repoxbot.util.valueOfIgnoreCase
import io.vertx.core.json.JsonObject

class JsonToPullRequestCodec : LocalMessageCodec<JsonObject, PullRequest>() {

    companion object {
        const val NAME = "localJsonToPullRequestCodec"
    }

    override fun name(): String = NAME

    override fun transform(json: JsonObject): PullRequest {
        val pullRequestObject = json.getJsonObject(PULL_REQUEST)

        val action = getAction(json)
        val author = pullRequestObject.getJsonObject(USER).getString(LOGIN)
        val number = pullRequestObject.getInteger(NUMBER)
        val title = pullRequestObject.getString(TITLE)
        val link = pullRequestObject.getString(HTML_URL)
        val diffLink = pullRequestObject.getString(DIFF_URL)
        val body = pullRequestObject.getString(BODY)
        val sender = json.getJsonObject(SENDER).getString(LOGIN)
        val touchedLabel = getTouchedLabel(json)

        return PullRequest(action, author, number, title, link, diffLink, body, sender, touchedLabel)
    }

    private fun getAction(json: JsonObject): PullRequestAction {
        return valueOfIgnoreCase(json.getString(ACTION), PullRequestAction.UNDEFINED).also {
            if (it == PullRequestAction.CLOSED && json.getJsonObject(PULL_REQUEST).getBoolean(MERGED)) {
                return PullRequestAction.MERGED
            }
        }
    }

    private fun getTouchedLabel(json: JsonObject): String = json.getJsonObject(LABEL)?.getString(NAME) ?: ""
}
