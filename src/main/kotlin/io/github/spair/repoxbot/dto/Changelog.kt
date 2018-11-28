package io.github.spair.repoxbot.dto

data class Changelog(
    val author: String,
    val pullRequestLink: String,
    val pullRequestNumber: Int,
    val entries: List<ChangelogEntry>
) {
    fun isEmpty(): Boolean = entries.isEmpty()

    inline fun <R> runIfPresent(block: (Changelog) -> R) {
        if (!isEmpty()) block(this)
    }
}

data class ChangelogEntry(val className: String, val changeText: String)
