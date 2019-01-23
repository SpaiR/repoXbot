package io.github.spair.repoxbot.dto

import io.github.spair.repoxbot.constant.GITHUB_ORG
import io.github.spair.repoxbot.constant.GITHUB_REPO

data class RepoXBotConfig(
    val changelogLocation: FileLocation = FileLocation(System.getenv(GITHUB_ORG), System.getenv(GITHUB_REPO), "index.html"),
    val changelogClasses: Map<String, String> = emptyMap(),
    val diffPathsLabels: Map<String, String> = emptyMap()
)
