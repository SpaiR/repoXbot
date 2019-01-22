package io.github.spair.repoxbot.dto

data class FileLocation(val repo: String, val org: String, val path: String)

data class UpdateCommentInfo(val id: Int, val text: String)
data class UpdateFileInfo(val location: FileLocation, val message: String, val content: String)
data class UpdateLabelInfo(val id: Int, val labels: Set<String>)
