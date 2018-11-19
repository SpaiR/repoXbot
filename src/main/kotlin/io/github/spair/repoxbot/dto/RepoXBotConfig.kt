package io.github.spair.repoxbot.dto

data class RepoXBotConfig(
    val changelogPath: String = "html/changelog.html",
    val changelogClasses: Map<String, String> = emptyMap(),
    val diffPathsLabels: Map<String, String> = emptyMap()
)
