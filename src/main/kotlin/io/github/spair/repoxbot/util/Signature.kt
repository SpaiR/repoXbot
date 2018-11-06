package io.github.spair.repoxbot.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signature {

    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"
    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun isEqualSignature(signature: String, secretKey: String, xData: String): Boolean {
        val signingKey = SecretKeySpec(secretKey.toByteArray(), HMAC_SHA1_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM).apply { init(signingKey) }
        return signature.equals(toHextString(mac.doFinal(xData.toByteArray())), ignoreCase = true)
    }

    private fun toHextString(bytes: ByteArray): String {
        return buildString {
            bytes.forEach {
                val octet = it.toInt()
                val firstIndex = (octet and 0xF0) ushr (4)
                val secondIndex = octet and 0x0F
                append(hexArray[firstIndex])
                append(hexArray[secondIndex])
            }
        }
    }
}
