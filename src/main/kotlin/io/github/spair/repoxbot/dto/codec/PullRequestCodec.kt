package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.PullRequest

class PullRequestCodec : LocalMessageCodec<PullRequest, PullRequest>() {

    companion object {
        const val NAME = "localPullRequestCoded"
    }

    override fun transform(pullRequest: PullRequest): PullRequest = pullRequest

    override fun name(): String = NAME
}
