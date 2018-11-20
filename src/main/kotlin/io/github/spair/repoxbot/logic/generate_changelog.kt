package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import io.github.spair.repoxbot.dto.PullRequest

private val clText = ":cl:((?:.|\\n|\\r)*+)|\uD83C\uDD91((?:.|\\n|\\r)*+)".toRegex()
private val clRowWithClass = "[-*][ ](\\w+)(\\[link])?:[ ](.+)".toRegex()

fun generateChangelog(pullRequest: PullRequest): Changelog? {
    return findChangelogText(pullRequest.body)?.let { parseChangelog(it, pullRequest) }
}

private fun findChangelogText(pullRequestBody: String): String? {
    return clText.toPattern().matcher(pullRequestBody.sanitize()).let {
        if (it.find()) {
            (it.group(1) ?: it.group(2))!!
        } else {
            null
        } // 1 - :cl:, 2 - 🆑
    }
}

private fun parseChangelog(changelogText: String, pullRequest: PullRequest): Changelog {
    val author = getAuthor(changelogText, pullRequest.author)
    val changelogEntries = mutableListOf<ChangelogEntry>()

    changelogText.reader().forEachLine { line ->
        clRowWithClass.toPattern().matcher(line).let { matcher ->
            if (matcher.find()) {
                val className = matcher.group(1)
                val hasLink = matcher.group(2) != null
                val changes = matcher.group(3).trim().capitalize().ensureDotEnd().addLinkIfNeeded(hasLink, pullRequest.link)
                changelogEntries.add(ChangelogEntry(className, changes))
            }
        }
    }

    return Changelog(author, pullRequest.link, pullRequest.number, changelogEntries.toList())
}

private fun String.sanitize() = replace("(?s)<!--.*?-->".toRegex(), "").replace("[\\n\\r]".toRegex(), "\n")

private fun getAuthor(changelogText: String, pullRequestAuthor: String): String {
    return changelogText.substringBefore("\n").let {
        if (it.isBlank()) {
            pullRequestAuthor
        } else {
            it
        }
    }.trim()
}

private fun String.ensureDotEnd(): String {
    return this[length - 1].let { ch -> if (ch != '.' && ch != '?' && ch != '!') plus('.') else this }
}

private fun String.addLinkIfNeeded(hasLink: Boolean, link: String) = if (hasLink) plus(" [link:$link]") else this
