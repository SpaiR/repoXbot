package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.*   // ktlint-disable

class UpdateFileInfoCodec : LocalMessageCodec<UpdateFileInfo, UpdateFileInfo>() {
    override fun transform(updateFileInfo: UpdateFileInfo): UpdateFileInfo = updateFileInfo
    override fun name(): String = this::class.java.name
}

class UpdateLabelInfoCodec : LocalMessageCodec<UpdateLabelInfo, UpdateLabelInfo>() {
    override fun transform(updateLabelInfo: UpdateLabelInfo): UpdateLabelInfo = updateLabelInfo
    override fun name(): String = this::class.java.name
}

class UpdateCommentInfoCodec : LocalMessageCodec<UpdateCommentInfo, UpdateCommentInfo>() {
    override fun transform(updateCommentInfo: UpdateCommentInfo): UpdateCommentInfo = updateCommentInfo
    override fun name(): String = this::class.java.name
}

class RepoXBotConfigCodec : LocalMessageCodec<RepoXBotConfig, RepoXBotConfig>() {
    override fun transform(repoXBotConfig: RepoXBotConfig): RepoXBotConfig = repoXBotConfig
    override fun name(): String = this::class.java.name
}

class PullRequestCodec : LocalMessageCodec<PullRequest, PullRequest>() {
    override fun transform(pullRequest: PullRequest): PullRequest = pullRequest
    override fun name(): String = this::class.java.name
}

class IssueCodec : LocalMessageCodec<Issue, Issue>() {
    override fun transform(issue: Issue): Issue = issue
    override fun name(): String = this::class.java.name
}
