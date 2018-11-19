package io.github.spair.repoxbot.dto

data class RepoXBotConfig(
    val path: String = "html/changelog.html",
    val classes: Map<String, String> = emptyMap(),
    val pathsLabels: Map<String, String> = emptyMap()
)
