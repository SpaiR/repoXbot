package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.UpdateCommentInfo

class UpdateCommentInfoCodec : LocalMessageCodec<UpdateCommentInfo, UpdateCommentInfo>() {

    override fun transform(updateCommentInfo: UpdateCommentInfo): UpdateCommentInfo = updateCommentInfo

    override fun name(): String = this::class.java.name
}
