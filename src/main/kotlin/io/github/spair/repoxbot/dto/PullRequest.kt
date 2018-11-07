package io.github.spair.repoxbot.dto

data class PullRequest(val type: PullRequestType)

enum class PullRequestType {
    CLOSED, MERGED, UNDEFINED
}
