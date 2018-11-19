package io.github.spair.repoxbot.command

import io.github.spair.repoxbot.constant.EB_COMMAND_ISSUE_LABEL
import io.github.spair.repoxbot.constant.EB_GITHUB_ISSUE_LABELS_ADD
import io.github.spair.repoxbot.dto.Issue
import io.github.spair.repoxbot.dto.UpdateLabelInfo
import io.vertx.core.AbstractVerticle

private const val PROPOSAL_TAG = "[proposal]"
private const val PROPOSAL_LABEL = "Proposal"
private const val BUG_LABEL = "Bug"

class LabelIssueVerticle : AbstractVerticle() {

    private val eventBus by lazy { vertx.eventBus() }

    override fun start() {
        eventBus.localConsumer<Issue>(EB_COMMAND_ISSUE_LABEL) { msg ->
            val issue = msg.body()
            val isProposal = issue.title.contains(PROPOSAL_TAG, ignoreCase = true)
            val labelToSend: String = if (isProposal) PROPOSAL_LABEL else BUG_LABEL
            eventBus.send(EB_GITHUB_ISSUE_LABELS_ADD, UpdateLabelInfo(issue.number, setOf(labelToSend)))
        }
    }
}
