package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.PullRequest

class PullRequestCodec : LocalMessageCodec<PullRequest, PullRequest>() {

    override fun transform(pullRequest: PullRequest): PullRequest = pullRequest

    override fun name(): String = this::class.java.name
}
