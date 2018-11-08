@file:Suppress("NOTHING_TO_INLINE")

package io.github.spair.repoxbot.util

import io.github.spair.repoxbot.GithubVerticle
import io.github.spair.repoxbot.constant.GITHUB_API_PATH
import io.github.spair.repoxbot.constant.GITHUB_ORG
import io.github.spair.repoxbot.constant.GITHUB_REPO

inline fun GithubVerticle.contents(relPath: String): String {
    return "$GITHUB_API_PATH/repos/${getSharedConfig(GITHUB_ORG)}/${getSharedConfig(GITHUB_REPO)}/contents$relPath"
}
