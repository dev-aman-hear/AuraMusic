package com.aman.auramusic.online.player

/**
 * Filters duplicate and clearly premature online track-end callbacks before queue advancement
 * or sleep-timer handling.
 */
internal object NaturalTrackEndGuard {
    private const val END_EVENT_DEBOUNCE_MS = 500L
    private const val MIN_TRACK_DURATION_FOR_PREMATURE_CHECK_MS = 20_000L
    private const val VERY_EARLY_POSITION_MS = 15_000L
    private const val MIN_COMPLETION_FRACTION = 0.85

    fun shouldHandle(
        nowMs: Long,
        lastHandledTimeMs: Long,
        durationMs: Long,
        positionMs: Long
    ): Boolean {
        if (nowMs - lastHandledTimeMs < END_EVENT_DEBOUNCE_MS) return false

        if (
            durationMs > MIN_TRACK_DURATION_FOR_PREMATURE_CHECK_MS &&
            positionMs < VERY_EARLY_POSITION_MS &&
            positionMs < (durationMs * MIN_COMPLETION_FRACTION).toLong()
        ) {
            return false
        }

        return true
    }
}
