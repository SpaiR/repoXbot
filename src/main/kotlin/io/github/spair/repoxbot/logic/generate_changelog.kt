package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import io.github.spair.repoxbot.dto.PullRequest

private val clText = ":cl:((?:.|\\n|\\r)*+)|\uD83C\uDD91((?:.|\\n|\\r)*+)".toRegex()
private val authorBeforeChanges = ".*".toRegex()
private val clRowWithClass = "-[ ](\\w+)(\\[link])?:[ ](.+)".toRegex()

fun generateChangelog(pullRequest: PullRequest): Changelog? {
    return findChangelogText(pullRequest.body)?.let { parseChangelog(it, pullRequest) }
}

private fun findChangelogText(pullRequestBody: String): String? {
    val strippedBody = pullRequestBody.replace("(?s)<!--.*?-->".toRegex(), "")
    return clText.toPattern().matcher(strippedBody).let {
        if (it.find()) {
            (it.group(1) ?: it.group(2))!!
        } else {
            null
        } // 1 - :cl:, 2 - 🆑
    }
}

private fun parseChangelog(changelogText: String, pullRequest: PullRequest): Changelog {
    val author = authorBeforeChanges.toPattern().matcher(changelogText).let { if (it.find()) it.group().trim() else null }
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

    return Changelog(author ?: pullRequest.author, pullRequest.link, pullRequest.number, changelogEntries.toList())
}

private fun String.ensureDotEnd(): String {
    return this[length - 1].let { ch -> if (ch != '.' && ch != '?' && ch != '!') plus('.') else this }
}

private fun String.addLinkIfNeeded(hasLink:Boolean, link: String) = if (hasLink) plus(" [link:$link]") else this
