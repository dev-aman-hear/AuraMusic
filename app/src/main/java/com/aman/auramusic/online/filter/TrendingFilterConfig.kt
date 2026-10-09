package com.aman.auramusic.online.filter

/**
 * Configuration for the smart trending song filter.
 *
 * Provides configurable thresholds, provider-specific penalty tuning,
 * deduplication behavior, and diagnostic dry-run modes.
 */
data class TrendingFilterConfig(
    /** Maximum duration in seconds with zero penalty (default: 15 minutes = 900s). */
    val maxDurationNoPenaltySec: Long = 15 * 60L,

    /** Soft duration threshold in seconds where ranking penalty starts (default: 25 minutes = 1500s). */
    val softDurationLimitSec: Long = 25 * 60L,

    /** Hard duration ceiling in seconds where a track is treated as an indisputable compilation (default: 45 minutes = 2700s). */
    val hardDurationExclusionSec: Long = 45 * 60L,

    /** Apply stricter duration penalties to YouTube Music than studio catalogs like JioSaavn. */
    val strictYouTubeDurationPenalty: Boolean = true,

    /** When true, records exclusions and reasons in decisions without stripping tracks from the output. */
    val dryRun: Boolean = false,

    /** When true, removes duplicate tracks across different providers based on normalized title and artist. */
    val deduplicateAcrossProviders: Boolean = false,

    /** Minimum number of songs desired after filtering; triggers secondary fetch if available. */
    val minResultsAfterFilter: Int = 10,

    /** Maximum pagination / fallback fetch attempts to prevent infinite loading loops. */
    val maxPaginationRetries: Int = 2
)
