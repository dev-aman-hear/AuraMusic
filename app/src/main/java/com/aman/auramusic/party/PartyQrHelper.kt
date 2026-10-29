package com.aman.auramusic.party

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

sealed class ScannedParty {
    data class Internet(val code: String) : ScannedParty()
    data class Local(val host: String, val port: Int, val code: String) : ScannedParty()
}

object PartyQrHelper {

    fun createInternetPayload(code: String): String {
        return "auraparty://internet?code=${code.trim().uppercase()}"
    }

    fun createLocalPayload(host: String, port: Int, code: String): String {
        return "auraparty://local?host=${host.trim()}&port=$port&code=${code.trim().uppercase()}"
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val intArray = IntArray(width * height)
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream) ?: return null
                decodeQrFromBitmap(bitmap)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parsePartyQr(raw: String): ScannedParty? {
        val text = raw.trim()
        if (text.startsWith("auraparty://", ignoreCase = true)) {
            try {
                val uri = Uri.parse(text)
                val target = uri.host?.lowercase()
                if (target == "internet") {
                    val code = uri.getQueryParameter("code")?.uppercase()?.trim()
                    if (!code.isNullOrBlank()) return ScannedParty.Internet(code)
                } else if (target == "local") {
                    val host = uri.getQueryParameter("host")?.trim()
                    val port = uri.getQueryParameter("port")?.toIntOrNull() ?: LocalPartyServer.DEFAULT_PORT
                    val code = uri.getQueryParameter("code")?.uppercase()?.trim()
                    if (!host.isNullOrBlank() && !code.isNullOrBlank()) {
                        return ScannedParty.Local(host, port, code)
                    }
                }
            } catch (_: Exception) {}
        }

        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                val json = JSONObject(text)
                val mode = json.optString("mode", "").lowercase()
                val code = json.optString("code", "").uppercase().trim()
                if (mode == "local" || json.has("host")) {
                    val host = json.optString("host", "").trim()
                    val port = json.optInt("port", LocalPartyServer.DEFAULT_PORT)
                    if (host.isNotBlank() && code.isNotBlank()) {
                        return ScannedParty.Local(host, port, code)
                    }
                } else if (code.isNotBlank()) {
                    return ScannedParty.Internet(code)
                }
            } catch (_: Exception) {}
        }

        if (text.contains(".") && (text.contains(":") || text.contains("#"))) {
            val parts = text.split('#', ':')
            if (parts.size >= 2) {
                val host = parts[0].trim()
                val code = parts.last().uppercase().trim()
                if (host.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) && code.length in 4..12) {
                    val port = if (parts.size == 3) parts[1].toIntOrNull() ?: LocalPartyServer.DEFAULT_PORT else LocalPartyServer.DEFAULT_PORT
                    return ScannedParty.Local(host, port, code)
                }
            }
        }

        val cleanCode = text.uppercase().filter { it.isLetterOrDigit() }
        if (cleanCode.length in 4..12) {
            return ScannedParty.Internet(cleanCode)
        }

        return null
    }
}
