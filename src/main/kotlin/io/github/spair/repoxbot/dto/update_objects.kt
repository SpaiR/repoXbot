package io.github.spair.repoxbot.dto

data class UpdateCommentInfo(val id: Int, val text: String)
data class UpdateFileInfo(val path: String, val message: String, val content: String)
data class UpdateLabelInfo(val id: Int, val labels: Set<String>)
