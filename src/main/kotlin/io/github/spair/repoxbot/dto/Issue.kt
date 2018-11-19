package io.github.spair.repoxbot.dto

data class Issue(val action: IssueAction, val number: Int, val title: String)

enum class IssueAction {
    OPENED, UNDEFINED
}
