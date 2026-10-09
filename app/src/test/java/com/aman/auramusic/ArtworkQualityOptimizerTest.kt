package com.aman.auramusic

import com.aman.auramusic.online.util.ArtworkQualityOptimizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkQualityOptimizerTest {

    @Test
    fun testJioSaavnDimensionUpgrade() {
        val saavn150 = "https://c.saavncdn.com/611/Kesariya-Lofi-Flip-Hindi-2022-20221004184611-150x150.jpg"
        val expected = "https://c.saavncdn.com/611/Kesariya-Lofi-Flip-Hindi-2022-20221004184611-500x500.jpg"
        assertEquals(expected, ArtworkQualityOptimizer.optimizeUrl(saavn150))

        val saavn50 = "http://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_50x50.jpg"
        val expectedHttps500 = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg"
        assertEquals(expectedHttps500, ArtworkQualityOptimizer.optimizeUrl(saavn50))

        val saavn250 = "//c.saavncdn.com/250/Track_250x250.jpg"
        val expectedHttps250 = "https://c.saavncdn.com/250/Track_500x500.jpg"
        assertEquals(expectedHttps250, ArtworkQualityOptimizer.optimizeUrl(saavn250))
    }

    @Test
    fun testYouTubeMusicGoogleusercontentUpgrade() {
        val yt60 = "https://yt3.googleusercontent.com/dcxXIIlest09vnvKznWM9VWQXu1EL7lKxBzXGzwgmVjmMNBm1dEWT_0qn1xrEZYyKF_qRE1TLq8P_JY_mQ=w60-h60-l90-rj"
        val expected60 = "https://yt3.googleusercontent.com/dcxXIIlest09vnvKznWM9VWQXu1EL7lKxBzXGzwgmVjmMNBm1dEWT_0qn1xrEZYyKF_qRE1TLq8P_JY_mQ=w1080-h1080-l90-rj"
        assertEquals(expected60, ArtworkQualityOptimizer.optimizeUrl(yt60))

        val yt120 = "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w120-h120-p-l90-rj"
        val expected120 = "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w1080-h1080-l90-rj"
        assertEquals(expected120, ArtworkQualityOptimizer.optimizeUrl(yt120))

        val avatar = "https://yt3.ggpht.com/ytc/AIdro_k123=s88-c-k-c0x00ffffff-no-rj"
        val expectedAvatar = "https://yt3.ggpht.com/ytc/AIdro_k123=s800-c-k-c0x00ffffff-no-rj"
        assertEquals(expectedAvatar, ArtworkQualityOptimizer.optimizeUrl(avatar))
    }

    @Test
    fun testYouTubeVideoThumbnailsUpgrade() {
        val thumbWithParams = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg?sqp=-oaymwEWCJADEOEBIAQqCggAEOADGC0guwJIWg&rs=AMzJL3nIC7d1SOq9BBgMkRglnHI-OXWXLw"
        val expectedThumb = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg"
        assertEquals(expectedThumb, ArtworkQualityOptimizer.optimizeUrl(thumbWithParams))

        val mqThumb = "https://i.ytimg.com/vi/34Na4j8AVgA/mqdefault.jpg"
        assertEquals(expectedThumb, ArtworkQualityOptimizer.optimizeUrl(mqThumb))

        val defaultThumb = "https://i.ytimg.com/vi/34Na4j8AVgA/default.jpg"
        assertEquals(expectedThumb, ArtworkQualityOptimizer.optimizeUrl(defaultThumb))
    }

    @Test
    fun testUnsplashPlaylistsUpgrade() {
        val unsplash = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80"
        val expected = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1080&q=85"
        assertEquals(expected, ArtworkQualityOptimizer.optimizeUrl(unsplash))
    }
}
