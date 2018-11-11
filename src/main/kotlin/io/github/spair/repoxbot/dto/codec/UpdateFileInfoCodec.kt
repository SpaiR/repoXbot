package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.UpdateFileInfo

class UpdateFileInfoCodec : LocalMessageCodec<UpdateFileInfo, UpdateFileInfo>() {

    override fun transform(updateFileInfo: UpdateFileInfo): UpdateFileInfo = updateFileInfo

    override fun name(): String = this::class.java.name
}
