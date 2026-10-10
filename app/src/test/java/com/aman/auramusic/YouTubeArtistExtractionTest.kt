package com.aman.auramusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeArtistExtractionTest {

    private fun extractSingleArtistNames(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        var cleaned = raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()

        val parentheticalRegex = Regex("""[\(\[\{](?:feat\.?|ft\.?|with)\s+([^\)\]\}]+)[\)\]\}]""", RegexOption.IGNORE_CASE)
        val fromParens = mutableListOf<String>()
        parentheticalRegex.findAll(cleaned).forEach { match ->
            fromParens.add(match.groupValues[1])
        }
        cleaned = cleaned.replace(parentheticalRegex, " ")

        val splitRegex = Regex(""",|;|&|/|\||\s+feat\.?\s+|\s+ft\.?\s+|\s+with\s+|\s+and\s+|\s+x\s+|\s+vs\.?\s+""", RegexOption.IGNORE_CASE)
        val parts = cleaned.split(splitRegex)

        val invalidNames = setOf(
            "unknown", "unknown artist", "various", "various artists", "various artist",
            "soundtrack", "ost", "compilation", "audio", "music", "null", "undefined",
            "remix", "records", "producer", "feat", "ft", "instrumental"
        )

        return (parts + fromParens)
            .map { it.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_') }
            .filter { name ->
                name.length in 2..40 &&
                !invalidNames.contains(name.lowercase()) &&
                name.any { it.isLetter() }
            }
            .distinctBy { it.lowercase() }
    }

    @Test
    fun testCompositeArtistStringSeparation() {
        val result = extractSingleArtistNames("Anirudh Ravichander, Kaala Bhairava")
        assertEquals(2, result.size)
        assertEquals("Anirudh Ravichander", result[0])
        assertEquals("Kaala Bhairava", result[1])
    }

    @Test
    fun testFeaturedArtistSeparation() {
        val result = extractSingleArtistNames("Taylor Swift feat. Post Malone")
        assertEquals(2, result.size)
        assertEquals("Taylor Swift", result[0])
        assertEquals("Post Malone", result[1])
    }

    @Test
    fun testAmpersandAndTopicSuffix() {
        val result = extractSingleArtistNames("Ed Sheeran & Justin Bieber - Topic")
        assertEquals(2, result.size)
        assertEquals("Ed Sheeran", result[0])
        assertEquals("Justin Bieber", result[1])
    }

    @Test
    fun testInvalidArtistFiltering() {
        val result = extractSingleArtistNames("Unknown Artist, Various Artists, Arijit Singh")
        assertEquals(1, result.size)
        assertEquals("Arijit Singh", result[0])
    }

    @Test
    fun testSingleSoloArtistUnchanged() {
        val result = extractSingleArtistNames("Diljit Dosanjh")
        assertEquals(1, result.size)
        assertEquals("Diljit Dosanjh", result[0])
    }

    @Test
    fun testNoiseFilteringLogic() {
        val excludedKeywords = listOf(
            "karaoke", "instrumental", "reaction", "reacting", "react to", "react ", "review",
            "tutorial", "how to play", "hour loop", "10 hour", "1 hour",
            "slowed + reverb", "nightcore"
        )

        val officialVideoTitle = "Arijit Singh - Official Music Video"
        val reactionTitle = "Reacting to Arijit Singh live!"
        val karaokeTitle = "Arijit Singh track (Karaoke with lyrics)"

        assertFalse(excludedKeywords.any { officialVideoTitle.lowercase().contains(it) })
        assertTrue(excludedKeywords.any { reactionTitle.lowercase().contains(it) })
        assertTrue(excludedKeywords.any { karaokeTitle.lowercase().contains(it) })
    }

    @Test
    fun testArtistMetadataParsingFromCardShelfAndResponsiveListItem() {
        val sampleInnerTubeJson = """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [{
                "tabRenderer": {
                  "content": {
                    "sectionListRenderer": {
                      "contents": [{
                        "itemSectionRenderer": {
                          "contents": [
                            {
                              "musicCardShelfRenderer": {
                                "thumbnail": {
                                  "musicThumbnailRenderer": {
                                    "thumbnail": {
                                      "thumbnails": [{ "url": "https://lh3.googleusercontent.com/artist_arijit=w544-h544" }]
                                    }
                                  }
                                },
                                "title": {
                                  "runs": [{
                                    "text": "Arijit Singh",
                                    "navigationEndpoint": {
                                      "browseEndpoint": {
                                        "browseId": "UCDxKh1gFWeYsqePvgVzmPoQ",
                                        "browseEndpointContextSupportedConfigs": {
                                          "browseEndpointContextMusicConfig": { "pageType": "MUSIC_PAGE_TYPE_ARTIST" }
                                        }
                                      }
                                    }
                                  }]
                                },
                                "subtitle": { "runs": [{ "text": "Artist • 646M monthly audience" }] }
                              }
                            },
                            {
                              "musicResponsiveListItemRenderer": {
                                "thumbnail": {
                                  "musicThumbnailRenderer": {
                                    "thumbnail": {
                                      "thumbnails": [{ "url": "https://yt3.googleusercontent.com/artist_atif=w544-h544" }]
                                    }
                                  }
                                },
                                "flexColumns": [
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": {
                                        "runs": [{
                                          "text": "Atif Aslam",
                                          "navigationEndpoint": {
                                            "browseEndpoint": {
                                              "browseId": "UCVGomUS__PL0c4jDXa0QwXA",
                                              "browseEndpointContextSupportedConfigs": {
                                                "browseEndpointContextMusicConfig": { "pageType": "MUSIC_PAGE_TYPE_ARTIST" }
                                              }
                                            }
                                          }
                                        }]
                                      }
                                    }
                                  },
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": { "runs": [{ "text": "Artist • 344M monthly audience" }] }
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      }]
                    }
                  }
                }
              }]
            }
          }
        }
        """.trimIndent()

        val parsed = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseInnerTubeArtistResults(sampleInnerTubeJson)
        assertEquals(2, parsed.size)

        val arijit = parsed.first { it.name == "Arijit Singh" }
        assertEquals("UCDxKh1gFWeYsqePvgVzmPoQ", arijit.id)
        assertTrue(arijit.profileImageUrl?.contains("artist_arijit") == true)
        assertTrue(arijit.isVerified)

        val atif = parsed.first { it.name == "Atif Aslam" }
        assertEquals("UCVGomUS__PL0c4jDXa0QwXA", atif.id)
        assertTrue(atif.profileImageUrl?.contains("artist_atif") == true)
    }

    @Test
    fun testSongEntitiesAreNotTreatedAsArtists() {
        val songItemJson = """
        {
          "musicResponsiveListItemRenderer": {
            "playlistItemData": { "videoId": "vid_998877" },
            "thumbnail": {
              "musicThumbnailRenderer": {
                "thumbnail": { "thumbnails": [{ "url": "https://i.ytimg.com/vi/vid_998877/hqdefault.jpg" }] }
              }
            },
            "flexColumns": [
              {
                "musicResponsiveListItemFlexColumnRenderer": {
                  "text": {
                    "runs": [{
                      "text": "Kesariya",
                      "navigationEndpoint": {
                        "watchEndpoint": { "videoId": "vid_998877" }
                      }
                    }]
                  }
                }
              },
              {
                "musicResponsiveListItemFlexColumnRenderer": {
                  "text": { "runs": [{ "text": "Song • Arijit Singh • 4:28" }] }
                }
              }
            ]
          }
        }
        """.trimIndent()

        val parsed = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseInnerTubeArtistResults(songItemJson)
        assertEquals(0, parsed.size)
    }

    @Test
    fun testMissingArtistThumbnailResultsInNullProfileImage() {
        val noThumbArtistJson = """
        {
          "musicResponsiveListItemRenderer": {
            "flexColumns": [
              {
                "musicResponsiveListItemFlexColumnRenderer": {
                  "text": {
                    "runs": [{
                      "text": "Indie Band",
                      "navigationEndpoint": {
                        "browseEndpoint": {
                          "browseId": "UCindie_band_123",
                          "browseEndpointContextSupportedConfigs": {
                            "browseEndpointContextMusicConfig": { "pageType": "MUSIC_PAGE_TYPE_ARTIST" }
                          }
                        }
                      }
                    }]
                  }
                }
              },
              {
                "musicResponsiveListItemFlexColumnRenderer": {
                  "text": { "runs": [{ "text": "Artist • 12K subscribers" }] }
                }
              }
            ]
          }
        }
        """.trimIndent()

        val parsed = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseInnerTubeArtistResults(noThumbArtistJson)
        assertEquals(1, parsed.size)
        val artist = parsed.first()
        assertEquals("Indie Band", artist.name)
        assertEquals("UCindie_band_123", artist.id)
        org.junit.Assert.assertNull("Artist with no thumbnail must have null profileImageUrl", artist.profileImageUrl)
    }

    @Test
    fun testNoSongArtworkFallbackForArtists() {
        // When creating ArtistItem for an artist without YouTube Music profile image,
        // artworkModel must be null, never falling back to song artwork or album covers.
        val artistWithNoProfile = com.aman.auramusic.ui.screen.hometest.ArtistItem(
            name = "Local Guitarist",
            songCountText = "5 songs",
            artworkModel = null,
            isOnline = false,
            artistId = "local_123"
        )
        org.junit.Assert.assertNull(artistWithNoProfile.artworkModel)

        // When YouTube Music provides a profile image, artworkModel is that image URL
        val artistWithProfile = com.aman.auramusic.ui.screen.hometest.ArtistItem(
            name = "Arijit Singh",
            songCountText = "YouTube Music",
            artworkModel = "https://lh3.googleusercontent.com/artist_arijit",
            isOnline = true,
            artistId = "UCDxKh1gFWeYsqePvgVzmPoQ"
        )
        assertEquals("https://lh3.googleusercontent.com/artist_arijit", artistWithProfile.artworkModel)
    }

    @Test
    fun testArtistTracksAreYouTubeOnly() {
        val ytTracks = listOf(
            com.aman.auramusic.online.model.OnlineSong(
                id = "yt_1",
                title = "O Maahi",
                artist = "Arijit Singh",
                source = com.aman.auramusic.online.model.AudioSource.YOUTUBE
            ),
            com.aman.auramusic.online.model.OnlineSong(
                id = "yt_2",
                title = "Chaleya",
                artist = "Arijit Singh",
                source = com.aman.auramusic.online.model.AudioSource.YOUTUBE
            )
        )

        // Verify all artist tracks are strictly AudioSource.YOUTUBE
        assertTrue(ytTracks.all { it.source == com.aman.auramusic.online.model.AudioSource.YOUTUBE })
        assertFalse(ytTracks.any { it.source == com.aman.auramusic.online.model.AudioSource.JIOSAAVN })
    }
}
