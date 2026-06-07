package com.aman.auramusic.util

import android.media.MediaMetadataRetriever
import com.aman.auramusic.data.model.Song

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

fun audioBadge(filePath: String, mimeType: String): String {
    return audioQuality(filePath, mimeType).badge
}

fun audioQuality(song: Song): AudioQuality {
    return audioQuality(
        filePath = song.filePath,
        mimeType = song.mimeType,
        metadata = readAudioQualityMetadata(song.filePath)
    )
}

fun audioQuality(filePath: String, mimeType: String): AudioQuality {
    return audioQuality(filePath, mimeType, readAudioQualityMetadata(filePath))
}

private fun audioQuality(
    filePath: String,
    mimeType: String,
    metadata: AudioQualityMetadata
): AudioQuality {
    val bitDepthValue = metadata.bitDepth ?: inferBitDepth(filePath)
    val sampleRateValue = metadata.sampleRateHz ?: inferSampleRate(filePath)
    val isLossless = filePath.endsWith(".flac", true) ||
        filePath.endsWith(".wav", true) ||
        filePath.endsWith(".alac", true)
    val isAppleLosslessCandidate = filePath.endsWith(".m4a", true)
    val isHiRes = isLossless &&
        ((bitDepthValue ?: 0) >= 24 || (sampleRateValue ?: 0) >= 88_200)

    val badge = when {
        isHiRes -> "Hi-Res Lossless"
        isLossless || isAppleLosslessCandidate -> "Lossless"
        mimeType.contains("mpeg", true) -> "High Quality"
        else -> mimeType.substringAfter("/").uppercase()
    }

    val bitDepth = bitDepthValue?.let { "$it-bit" } ?: if (isLossless) "24-bit" else "16-bit"
    val sampleRate = sampleRateValue?.let(::formatSampleRate) ?: defaultSampleRate(filePath)

    return AudioQuality(
        badge = badge,
        detail = "$bitDepth / $sampleRate",
        compact = bitDepth,
        bitDepth = bitDepth,
        sampleRate = sampleRate,
        bitrate = metadata.bitrate?.let { "${it / 1000} kbps" } ?: "Unknown",
        format = formatName(filePath, mimeType)
    )
}

private fun readAudioQualityMetadata(filePath: String): AudioQualityMetadata {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val metadata = AudioQualityMetadata(
            bitDepth = retriever.extractOptionalMetadata("METADATA_KEY_BITS_PER_SAMPLE"),
            sampleRateHz = retriever.extractOptionalMetadata("METADATA_KEY_SAMPLERATE"),
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()
        )

        retriever.release()
        metadata
    } catch (e: Exception) {
        AudioQualityMetadata()
    }
}

private fun MediaMetadataRetriever.extractOptionalMetadata(fieldName: String): Int? {
    return try {
        val key = MediaMetadataRetriever::class.java
            .getField(fieldName)
            .getInt(null)
        extractMetadata(key)?.toIntOrNull()
    } catch (e: Exception) {
        null
    }
}

private fun inferBitDepth(filePath: String): Int? {
    val normalized = filePath.lowercase()
    return when {
        normalized.contains("24bit") || normalized.contains("24 bit") -> 24
        normalized.contains("16bit") || normalized.contains("16 bit") -> 16
        normalized.endsWith(".flac") || normalized.endsWith(".wav") -> 24
        normalized.endsWith(".m4a") -> 16
        else -> null
    }
}

private fun inferSampleRate(filePath: String): Int? {
    val normalized = filePath.lowercase()
    return when {
        normalized.contains("192khz") || normalized.contains("192 khz") -> 192_000
        normalized.contains("98khz") || normalized.contains("98 khz") -> 98_000
        normalized.contains("96khz") || normalized.contains("96 khz") -> 96_000
        normalized.contains("48khz") || normalized.contains("48 khz") -> 48_000
        normalized.contains("44.1khz") || normalized.contains("44.1 khz") -> 44_100
        else -> null
    }
}

private fun defaultSampleRate(filePath: String): String {
    return if (filePath.endsWith(".flac", true) || filePath.endsWith(".wav", true)) {
        "96 kHz"
    } else {
        "44.1 kHz"
    }
}

private fun formatSampleRate(sampleRate: Int): String {
    return if (sampleRate % 1000 == 0) {
        "${sampleRate / 1000} kHz"
    } else {
        "%.1f kHz".format(sampleRate / 1000f)
    }
}

private fun formatName(filePath: String, mimeType: String): String {
    val extension = filePath.substringAfterLast(".", "").uppercase()
    return extension.ifBlank { mimeType.substringAfter("/").uppercase() }
}

data class AudioQuality(
    val badge: String,
    val detail: String,
    val compact: String,
    val bitDepth: String,
    val sampleRate: String,
    val bitrate: String,
    val format: String
)

private data class AudioQualityMetadata(
    val bitDepth: Int? = null,
    val sampleRateHz: Int? = null,
    val bitrate: Int? = null
)
