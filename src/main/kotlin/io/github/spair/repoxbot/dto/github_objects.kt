package io.github.spair.repoxbot.dto

data class PullRequest(
    val action: PullRequestAction,
    val author: String,
    val number: Int,
    val title: String,
    val link: String,
    val diffLink: String,
    val body: String
)
enum class PullRequestAction { OPENED, EDITED, CLOSED, MERGED, UNDEFINED }

data class Issue(val action: IssueAction, val number: Int, val title: String)
enum class IssueAction { OPENED, UNDEFINED }

data class IssueComment(val id: Int, val user: String, val body: String)
