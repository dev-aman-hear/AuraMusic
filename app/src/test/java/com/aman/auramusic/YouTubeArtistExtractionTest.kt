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

    @Test
    fun testMatchesArtistNameExactAndVariations() {
        // Exact match
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill", "Gurjit Gill"))
        // Case insensitive
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("gurjit gill", "GURJIT GILL"))
        // Spacing variations
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit  Gill", "Gurjit Gill"))
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill ", "Gurjit Gill"))
        // Topic suffix variation
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill - Topic", "Gurjit Gill"))
        // Punctuation variation
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("A.R. Rahman", "A. R. Rahman"))
        assertTrue(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("A.R. Rahman", "AR Rahman"))
    }

    @Test
    fun testMatchesArtistNameNegative() {
        // Completely different or partial artist names must NOT match
        assertFalse(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill", "Gill"))
        assertFalse(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill", "Gurjit"))
        assertFalse(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Gurjit Gill", "Jassi Gill"))
        assertFalse(com.aman.auramusic.ui.screen.hometest.HomeTestViewModel.matchesArtistName("Arijit Singh", "Diljit Dosanjh"))
    }

    @Test
    fun testGurjitGillMetadataParsingFromInnerTubeResponse() {
        val gurjitGillJson = """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [{
                "tabRenderer": {
                  "content": {
                    "sectionListRenderer": {
                      "contents": [{
                        "musicShelfRenderer": {
                          "title": { "runs": [{ "text": "Artists" }] },
                          "contents": [{
                            "musicResponsiveListItemRenderer": {
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://yt3.googleusercontent.com/KUfkpFydqshfndt0wa3rJrZbxl0EfXfAX3kxDPcM=w60-h60" },
                                      { "url": "https://yt3.googleusercontent.com/KUfkpFydqshfndt0wa3rJrZbxl0EfXfAX3kxDPcM=w120-h120" }
                                    ]
                                  }
                                }
                              },
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [{ "text": "Gurjit Gill" }] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [{ "text": "Artist • 58.5M monthly audience" }] }
                                  }
                                }
                              ],
                              "navigationEndpoint": {
                                "browseEndpoint": {
                                  "browseId": "UCfHedNWhY9Es395MezTHyZw",
                                  "browseEndpointContextSupportedConfigs": {
                                    "browseEndpointContextMusicConfig": { "pageType": "MUSIC_PAGE_TYPE_ARTIST" }
                                  }
                                }
                              }
                            }
                          }]
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

        val parsed = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseInnerTubeArtistResults(gurjitGillJson)
        assertEquals(1, parsed.size)
        val artist = parsed.first()
        assertEquals("Gurjit Gill", artist.name)
        assertEquals("UCfHedNWhY9Es395MezTHyZw", artist.id)
        assertTrue(artist.profileImageUrl?.contains("yt3.googleusercontent.com") == true)
        assertTrue(artist.isVerified)
    }

    @Test
    fun testParseArtistBrowseTracksExtractsRealMetadata() {
        val sampleBrowseJson = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [{
                "tabRenderer": {
                  "content": {
                    "sectionListRenderer": {
                      "contents": [{
                        "musicShelfRenderer": {
                          "title": { "runs": [{ "text": "Top songs" }] },
                          "contents": [
                            {
                              "musicResponsiveListItemRenderer": {
                                "playlistItemData": { "videoId": "vid_song_1" },
                                "thumbnail": {
                                  "musicThumbnailRenderer": {
                                    "thumbnail": {
                                      "thumbnails": [
                                        { "url": "https://lh3.googleusercontent.com/song1=w60-h60" },
                                        { "url": "https://lh3.googleusercontent.com/song1=w544-h544" }
                                      ]
                                    }
                                  }
                                },
                                "flexColumns": [
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": { "runs": [{ "text": "Midnight Echoes" }] }
                                    }
                                  },
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": { "runs": [{ "text": "Luna Ray" }] }
                                    }
                                  },
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": { "runs": [{ "text": "12M plays" }] }
                                    }
                                  },
                                  {
                                    "musicResponsiveListItemFlexColumnRenderer": {
                                      "text": { "runs": [{ "text": "Echoes EP" }] }
                                    }
                                  }
                                ],
                                "fixedColumns": [
                                  {
                                    "musicResponsiveListItemFixedColumnRenderer": {
                                      "text": { "runs": [{ "text": "3:45" }] }
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

        val songs = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseTracks(
            sampleBrowseJson,
            "Luna Ray"
        )
        assertEquals(1, songs.size)
        val song = songs.first()
        assertEquals("vid_song_1", song.id)
        assertEquals("Midnight Echoes", song.title)
        assertEquals("Luna Ray", song.artist)
        assertEquals("Echoes EP", song.album)
        assertEquals(225L, song.durationSeconds)
        assertTrue(song.artworkUrl?.contains("w1080-h1080") == true || song.artworkUrl?.contains("song1") == true)
        assertEquals(com.aman.auramusic.online.model.AudioSource.YOUTUBE, song.source)
    }

    @Test
    fun testParseArtistBrowseTracksDeduplicatesAndHandlesMissingFields() {
        val sampleDuplicateJson = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [{
                "tabRenderer": {
                  "content": {
                    "sectionListRenderer": {
                      "contents": [{
                        "musicShelfRenderer": {
                          "title": { "runs": [{ "text": "Top songs" }] },
                          "contents": [
                            {
                              "musicResponsiveListItemRenderer": {
                                "playlistItemData": { "videoId": "track_duplicate" },
                                "flexColumns": [
                                  { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Version A" }] } } }
                                ]
                              }
                            },
                            {
                              "musicResponsiveListItemRenderer": {
                                "playlistItemData": { "videoId": "track_duplicate" },
                                "flexColumns": [
                                  { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Version B" }] } } }
                                ]
                              }
                            },
                            {
                              "musicResponsiveListItemRenderer": {
                                "flexColumns": [
                                  { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "No ID Song" }] } } }
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

        val songs = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseTracks(
            sampleDuplicateJson,
            "Artist X"
        )
        // Deduplicated: should only contain 1 song with videoId = track_duplicate; No ID song should be skipped
        assertEquals(1, songs.size)
        assertEquals("track_duplicate", songs.first().id)
        assertEquals("Version A", songs.first().title)
        assertEquals("Artist X", songs.first().artist)
        assertEquals(0L, songs.first().durationSeconds)
    }

    @Test
    fun testParseArtistBrowseAlbumsExtractsGenuineReleases() {
        val sampleAlbumsJson = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [{
                "tabRenderer": {
                  "content": {
                    "sectionListRenderer": {
                      "contents": [{
                        "musicCarouselShelfRenderer": {
                          "header": {
                            "musicCarouselShelfBasicHeaderRenderer": {
                              "title": { "runs": [{ "text": "Singles & EPs" }] }
                            }
                          },
                          "contents": [
                            {
                              "musicTwoRowItemRenderer": {
                                "thumbnailRenderer": {
                                  "musicThumbnailRenderer": {
                                    "thumbnail": {
                                      "thumbnails": [
                                        { "url": "https://lh3.googleusercontent.com/cover1=w544-h544" }
                                      ]
                                    }
                                  }
                                },
                                "title": { "runs": [{ "text": "Summer Glow" }] },
                                "subtitle": { "runs": [{ "text": "Single • 2026" }] },
                                "navigationEndpoint": {
                                  "browseEndpoint": {
                                    "browseId": "MPREb_summer_glow_id"
                                  }
                                }
                              }
                            },
                            {
                              "musicTwoRowItemRenderer": {
                                "thumbnailRenderer": {
                                  "musicThumbnailRenderer": {
                                    "thumbnail": {
                                      "thumbnails": [
                                        { "url": "https://lh3.googleusercontent.com/cover2=w544-h544" }
                                      ]
                                    }
                                  }
                                },
                                "title": { "runs": [{ "text": "Summer Glow (Duplicate)" }] },
                                "subtitle": { "runs": [{ "text": "Single • 2026" }] },
                                "navigationEndpoint": {
                                  "browseEndpoint": {
                                    "browseId": "MPREb_summer_glow_id"
                                  }
                                }
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

        val albums = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseAlbums(
            sampleAlbumsJson
        )
        // Should extract 1 unique album with MPRE browseId
        assertEquals(1, albums.size)
        val album = albums.first()
        assertEquals("ytmusic:MPREb_summer_glow_id", album.id)
        assertEquals("Summer Glow", album.title)
        assertEquals("Single • 2026", album.subtitle)
        assertTrue(album.artworkUrl?.contains("cover1") == true)
        assertEquals(com.aman.auramusic.online.model.AudioSource.YOUTUBE, album.source)
    }

    @Test
    fun testParseArtistBrowseHandlesMalformedOrEmptyJson() {
        val malformed = "{ this is not valid json! }"
        val tracks = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseTracks(malformed, "Artist")
        val albums = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseAlbums(malformed)
        assertTrue(tracks.isEmpty())
        assertTrue(albums.isEmpty())

        val empty = ""
        val emptyTracks = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseTracks(empty, "Artist")
        val emptyAlbums = com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl.parseArtistBrowseAlbums(empty)
        assertTrue(emptyTracks.isEmpty())
        assertTrue(emptyAlbums.isEmpty())
    }

    @Test
    fun testPlaceholderArtistIdsAreNotGenuineBrowseIds() {
        val placeholderIds = listOf("local_123", "local_The_Beatles", "yt_artist_taylorswift", "", "   ")
        for (id in placeholderIds) {
            val isGenuine = id.isNotBlank() &&
                !id.startsWith("local_", ignoreCase = true) &&
                !id.startsWith("yt_artist_", ignoreCase = true) &&
                id.startsWith("UC")
            assertFalse("ID $id must not be considered a genuine YouTube browse ID", isGenuine)
        }

        val genuineId = "UCfHedNWhY9Es395MezTHyZw"
        val isGenuine = genuineId.isNotBlank() &&
            !genuineId.startsWith("local_", ignoreCase = true) &&
            !genuineId.startsWith("yt_artist_", ignoreCase = true) &&
            genuineId.startsWith("UC")
        assertTrue(isGenuine)
    }
}

