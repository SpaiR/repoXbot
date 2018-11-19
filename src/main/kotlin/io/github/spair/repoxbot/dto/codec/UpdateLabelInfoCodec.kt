package io.github.spair.repoxbot.dto.codec

import io.github.spair.repoxbot.dto.UpdateLabelInfo

class UpdateLabelInfoCodec : LocalMessageCodec<UpdateLabelInfo, UpdateLabelInfo>() {

    override fun transform(updateLabelInfo: UpdateLabelInfo): UpdateLabelInfo = updateLabelInfo

    override fun name(): String = this::class.java.name
}
