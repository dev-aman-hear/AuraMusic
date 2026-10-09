package com.aman.auramusic.online.network.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * High-performance DES decryptor for JioSaavn media URLs.
 * As identified across repositories like BlackHole, Sangit, and JioSaavn-API,
 * JioSaavn encrypts CDN URLs using standard DES in ECB mode with PKCS5 padding
 * and static symmetric key: "38346591".
 */
object DesDecryptor {
    private const val DES_KEY = "38346591"
    private const val ALGORITHM = "DES"
    private const val TRANSFORMATION = "DES/ECB/PKCS5Padding"

    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isBlank()) return ""
        return try {
            val keyBytes = DES_KEY.toByteArray(StandardCharsets.US_ASCII)
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = decodeBase64Safe(encryptedBase64.trim())
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Replaces default bitrate suffix (_96.mp4 / _160.mp4) with desired quality (_320.mp4).
     */
    fun upgradeBitrate(streamUrl: String, desiredBitrate: String = "320"): String {
        if (streamUrl.isBlank()) return streamUrl
        val targetSuffix = "_$desiredBitrate.mp4"
        return streamUrl
            .replace(Regex("_(96|160|320)\\.mp4"), targetSuffix)
            .replace(Regex("_(96|160|320)\\.m4a"), "_$desiredBitrate.m4a")
    }

    /**
     * Cross-platform Base64 decoder supporting both Android runtime and standard JVM environments.
     */
    private fun decodeBase64Safe(input: String): ByteArray {
        return try {
            android.util.Base64.decode(input, android.util.Base64.DEFAULT)
        } catch (t: Throwable) {
            java.util.Base64.getDecoder().decode(input)
        }
    }
}
