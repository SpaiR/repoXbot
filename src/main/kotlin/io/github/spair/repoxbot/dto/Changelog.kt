package io.github.spair.repoxbot.dto

data class Changelog(
    val author: String,
    val pullRequestLink: String,
    val pullRequestNumber: Int,
    val entries: List<ChangelogEntry>
) {

    fun isEmpty(): Boolean = entries.isEmpty()

    inline fun applyIfNotEmpty(block: Changelog.() -> Unit): Changelog {
        if (!isEmpty()) block(this)
        return this
    }
}

data class ChangelogEntry(val className: String, val changeText: String)
