package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog

fun getLabelsFromChangelog(changelog: Changelog?, classes: Map<String, String>): Set<String> {
    val labelsToAdd = mutableSetOf<String>()

    if (changelog == null || changelog.isEmpty()) {
        return labelsToAdd
    }

    changelog.entries.forEach { entry ->
        classes[entry.className]?.let { label ->
            if (label.isNotBlank()) {
                labelsToAdd.add(label)
            }
        }
    }

    return labelsToAdd
}

fun getLabelsFromDiffText(diffText: String, pathsLabels: Map<String, String>): Set<String> {
    val labelsToAdd = mutableSetOf<String>()

    pathsLabels.forEach { regexPath, labelName ->
        diffText.reader().forEachLine { diffLine ->
            if (regexPath.toPattern().matcher(diffLine).find()) {
                labelsToAdd.add(labelName)
            }
        }
    }

    return labelsToAdd
}
