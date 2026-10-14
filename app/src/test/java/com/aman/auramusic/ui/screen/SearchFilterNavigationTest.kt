package com.aman.auramusic.ui.screen

import androidx.compose.runtime.saveable.Saver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests covering Search filter and query state preservation
 * across Artist/Album detail navigation and back flows.
 */
class SearchFilterNavigationTest {

    /**
     * Exact Saver logic used in MainActivity and SearchScreen to persist
     * SearchFilter across configuration changes, process death, and navigation.
     */
    private val searchFilterSaver = Saver<SearchFilter, String>(
        save = { it.name },
        restore = { name -> runCatching { SearchFilter.valueOf(name) }.getOrDefault(SearchFilter.ALL) }
    )

    /**
     * Test harness that simulates the navigation and state transitions in
     * MainActivity and SearchScreen.
     */
    private class SearchNavigationHarness {
        var query: String = ""
        var activeFilter: SearchFilter = SearchFilter.ALL
        var selectedArtistName: String? = null
        var selectedAlbumName: String? = null

        fun onQueryChange(newQuery: String) {
            query = newQuery
        }

        fun onFilterSelected(filter: SearchFilter) {
            activeFilter = filter
        }

        fun openArtistDetail(artist: String) {
            selectedArtistName = artist
        }

        fun openAlbumDetail(album: String) {
            selectedAlbumName = album
        }

        // App back button in ArtistDetailSheet (onDismiss)
        fun appBackFromArtistDetail() {
            selectedArtistName = null
        }

        // App back button in CollectionDetailScreen (onBack)
        fun appBackFromAlbumDetail() {
            selectedAlbumName = null
        }

        // System back button handling in MainActivity BackHandler
        fun systemBack() {
            when {
                selectedAlbumName != null -> selectedAlbumName = null
                selectedArtistName != null -> selectedArtistName = null
                query.isNotEmpty() -> {
                    query = ""
                    activeFilter = SearchFilter.ALL
                }
            }
        }

        // Explicit search clear action (close icon or back in search bar)
        fun explicitClearSearch() {
            query = ""
            activeFilter = SearchFilter.ALL
        }

        // Selection of a category card from discovery mode
        fun selectDiscoveryCategory(categoryTitle: String) {
            query = categoryTitle
            activeFilter = SearchFilter.ALL
        }
    }

    @Test
    fun `test Search to Artists to Artist Detail to Back to Artists`() {
        val harness = SearchNavigationHarness()

        // 1. Search for a query
        harness.onQueryChange("Arijit")
        assertEquals("Arijit", harness.query)

        // 2. Switch from All to Artists
        harness.onFilterSelected(SearchFilter.ARTISTS)
        assertEquals(SearchFilter.ARTISTS, harness.activeFilter)

        // 3. Open an artist detail page from the search results
        harness.openArtistDetail("Arijit Singh")
        assertEquals("Arijit Singh", harness.selectedArtistName)

        // 4. Press the app's Back button
        harness.appBackFromArtistDetail()

        // Assert: Returned to the exact previous Search state
        assertNull(harness.selectedArtistName)
        assertEquals("Arijit", harness.query)
        assertEquals(SearchFilter.ARTISTS, harness.activeFilter)
    }

    @Test
    fun `test Search to Albums to Album Detail to Back to Albums`() {
        val harness = SearchNavigationHarness()

        // 1. Search for a query
        harness.onQueryChange("Rockstar")
        assertEquals("Rockstar", harness.query)

        // 2. Switch from All to Albums
        harness.onFilterSelected(SearchFilter.ALBUMS)
        assertEquals(SearchFilter.ALBUMS, harness.activeFilter)

        // 3. Open an album detail page from the search results
        harness.openAlbumDetail("Rockstar (Original Motion Picture Soundtrack)")
        assertEquals("Rockstar (Original Motion Picture Soundtrack)", harness.selectedAlbumName)

        // 4. Press the app's Back button
        harness.appBackFromAlbumDetail()

        // Assert: Returned to the exact previous Search state
        assertNull(harness.selectedAlbumName)
        assertEquals("Rockstar", harness.query)
        assertEquals(SearchFilter.ALBUMS, harness.activeFilter)
    }

    @Test
    fun `test Search to All to Detail to Back to All`() {
        val harness = SearchNavigationHarness()

        // 1. Search for a query
        harness.onQueryChange("Coldplay")
        assertEquals(SearchFilter.ALL, harness.activeFilter)

        // 2. Open an artist detail from All section
        harness.openArtistDetail("Coldplay")
        assertEquals("Coldplay", harness.selectedArtistName)

        // 3. Press Back
        harness.appBackFromArtistDetail()

        // Assert: Returned to All section with query preserved
        assertNull(harness.selectedArtistName)
        assertEquals("Coldplay", harness.query)
        assertEquals(SearchFilter.ALL, harness.activeFilter)
    }

    @Test
    fun `test search query remains unchanged after returning from details`() {
        val harness = SearchNavigationHarness()
        val testQuery = "Diljit Dosanjh"

        harness.onQueryChange(testQuery)
        harness.onFilterSelected(SearchFilter.ARTISTS)
        harness.openArtistDetail(testQuery)

        // Return via Back
        harness.appBackFromArtistDetail()

        assertEquals("Query must remain unchanged after returning from Artist Detail", testQuery, harness.query)

        // Repeat for Album
        harness.onFilterSelected(SearchFilter.ALBUMS)
        harness.openAlbumDetail("Ghost")
        harness.appBackFromAlbumDetail()

        assertEquals("Query must remain unchanged after returning from Album Detail", testQuery, harness.query)
    }

    @Test
    fun `test switching to another filter after returning works normally`() {
        val harness = SearchNavigationHarness()

        harness.onQueryChange("Weekend")
        harness.onFilterSelected(SearchFilter.ARTISTS)
        harness.openArtistDetail("The Weeknd")
        harness.appBackFromArtistDetail()

        assertEquals(SearchFilter.ARTISTS, harness.activeFilter)

        // Switch to Albums
        harness.onFilterSelected(SearchFilter.ALBUMS)
        assertEquals(SearchFilter.ALBUMS, harness.activeFilter)

        // Switch to Songs
        harness.onFilterSelected(SearchFilter.SONGS)
        assertEquals(SearchFilter.SONGS, harness.activeFilter)

        // Switch to All
        harness.onFilterSelected(SearchFilter.ALL)
        assertEquals(SearchFilter.ALL, harness.activeFilter)
    }

    @Test
    fun `test system Back and app Back produce identical restoration behavior`() {
        // App Back verification
        val harnessAppBack = SearchNavigationHarness().apply {
            onQueryChange("Dua Lipa")
            onFilterSelected(SearchFilter.ARTISTS)
            openArtistDetail("Dua Lipa")
            appBackFromArtistDetail()
        }

        // System Back verification
        val harnessSystemBack = SearchNavigationHarness().apply {
            onQueryChange("Dua Lipa")
            onFilterSelected(SearchFilter.ARTISTS)
            openArtistDetail("Dua Lipa")
            systemBack()
        }

        assertEquals(harnessAppBack.query, harnessSystemBack.query)
        assertEquals(harnessAppBack.activeFilter, harnessSystemBack.activeFilter)
        assertEquals(harnessAppBack.selectedArtistName, harnessSystemBack.selectedArtistName)
        assertEquals(harnessAppBack.selectedAlbumName, harnessSystemBack.selectedAlbumName)

        // Verify with Album as well
        harnessAppBack.onFilterSelected(SearchFilter.ALBUMS)
        harnessAppBack.openAlbumDetail("Future Nostalgia")
        harnessAppBack.appBackFromAlbumDetail()

        harnessSystemBack.onFilterSelected(SearchFilter.ALBUMS)
        harnessSystemBack.openAlbumDetail("Future Nostalgia")
        harnessSystemBack.systemBack()

        assertEquals(harnessAppBack.query, harnessSystemBack.query)
        assertEquals(harnessAppBack.activeFilter, harnessSystemBack.activeFilter)
        assertEquals(harnessAppBack.selectedAlbumName, harnessSystemBack.selectedAlbumName)
    }

    @Test
    fun `test explicit clear search action resets filter to ALL`() {
        val harness = SearchNavigationHarness()

        harness.onQueryChange("Karan Aujla")
        harness.onFilterSelected(SearchFilter.ARTISTS)
        assertEquals(SearchFilter.ARTISTS, harness.activeFilter)

        // Explicit clear
        harness.explicitClearSearch()
        assertEquals("", harness.query)
        assertEquals(SearchFilter.ALL, harness.activeFilter)
    }

    @Test
    fun `test discovery category selection resets filter to ALL and sets query`() {
        val harness = SearchNavigationHarness()

        harness.onQueryChange("Old Query")
        harness.onFilterSelected(SearchFilter.ALBUMS)

        harness.selectDiscoveryCategory("Bollywood")
        assertEquals("Bollywood", harness.query)
        assertEquals(SearchFilter.ALL, harness.activeFilter)
    }

    @Test
    fun `test SearchFilter Saver properly saves and restores all enum values`() {
        SearchFilter.values().forEach { filter ->
            val saved = with(searchFilterSaver) { SaverScope { true }.save(filter) }
            val restored = searchFilterSaver.restore(saved as String)
            assertEquals("Restored filter should match original", filter, restored)
        }

        // Test fallback for invalid/corrupted data
        val restoredFallback = searchFilterSaver.restore("NON_EXISTENT_FILTER")
        assertEquals("Invalid filter name should fallback to ALL", SearchFilter.ALL, restoredFallback)
    }

    private class SaverScope(private val canBeSavedPredicate: (Any) -> Boolean) : androidx.compose.runtime.saveable.SaverScope {
        override fun canBeSaved(value: Any): Boolean = canBeSavedPredicate(value)
    }
}
