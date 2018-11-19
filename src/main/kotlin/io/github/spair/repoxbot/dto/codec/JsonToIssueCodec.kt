package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.constant.ACTION
import io.github.spair.repoxbot.constant.ISSUE
import io.github.spair.repoxbot.constant.NUMBER
import io.github.spair.repoxbot.constant.TITLE
import io.github.spair.repoxbot.dto.Issue
import io.github.spair.repoxbot.dto.IssueAction
import io.github.spair.repoxbot.util.valueOfIgnoreCase
import io.vertx.core.json.JsonObject

class JsonToIssueCodec : LocalMessageCodec<JsonObject, Issue>() {

    companion object {
        const val NAME = "localJsonToIssueCodec"
    }

    override fun transform(json: JsonObject): Issue {
        val issueObject = json.getJsonObject(ISSUE)

        val action = valueOfIgnoreCase(json.getString(ACTION), IssueAction.UNDEFINED)
        val number = issueObject.getInteger(NUMBER)
        val title = issueObject.getString(TITLE)

        return Issue(action, number, title)
    }

    override fun name(): String = NAME
}
