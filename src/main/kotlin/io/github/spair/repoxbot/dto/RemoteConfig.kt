package io.github.spair.repoxbot.dto

data class RemoteConfig(
    val updateMessage: String = "Automatic changelog generation for PR #{prNum}",
    val changelogPath: String = "html/changelog.html"
)
