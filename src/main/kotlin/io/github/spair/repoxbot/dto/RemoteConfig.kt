package io.github.spair.repoxbot.dto

data class RemoteConfig(
    val path: String = "html/changelog.html",
    val classes: Map<String, String> = emptyMap()
)
