package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.IssueComment

class IssueCommentListCodec : LocalMessageCodec<List<IssueComment>, List<IssueComment>>() {

    companion object {
        const val NAME = "localIssueCommentListCodec"
    }

    override fun transform(issueCommentList: List<IssueComment>): List<IssueComment> = issueCommentList

    override fun name(): String = NAME
}
