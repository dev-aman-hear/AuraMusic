package com.aman.auramusic.party

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.Inet4Address

class LocalNetworkAddressTest {

    @Test
    fun testInterfaceScoringPrioritizesWifiAndHotspot() {
        val wifiScore = LocalPartyServer.scoreInterface("wlan0")
        val hotspotScore = LocalPartyServer.scoreInterface("ap0")
        val softApScore = LocalPartyServer.scoreInterface("softap0")
        val p2pScore = LocalPartyServer.scoreInterface("p2p-wlan0-0")
        val cellularScore = LocalPartyServer.scoreInterface("rmnet_data0")
        val vpnScore = LocalPartyServer.scoreInterface("tun0")

        assertTrue("Wi-Fi should score higher than hotspot", wifiScore >= 100)
        assertTrue("Hotspot AP should have high score", hotspotScore >= 90)
        assertTrue("SoftAP should have high score", softApScore >= 90)
        assertTrue("P2P should have moderate score", p2pScore >= 70)
        assertTrue("Cellular should be negative", cellularScore < 0)
        assertTrue("VPN should be strongly negative", vpnScore <= -100)
    }

    @Test
    fun testAddressScoringRejectsLoopbackAndLinkLocal() {
        val loopback = InetAddress.getByName("127.0.0.1") as Inet4Address
        val linkLocal = InetAddress.getByName("169.254.12.34") as Inet4Address
        val validLan = InetAddress.getByName("192.168.1.150") as Inet4Address
        val hotspotGw = InetAddress.getByName("192.168.43.1") as Inet4Address

        assertEquals(-10000, LocalPartyServer.scoreAddress(loopback, "wlan0"))
        assertEquals(-10000, LocalPartyServer.scoreAddress(linkLocal, "wlan0"))

        val validLanScore = LocalPartyServer.scoreAddress(validLan, "wlan0")
        val hotspotScore = LocalPartyServer.scoreAddress(hotspotGw, "ap0")

        assertTrue("Valid LAN on wlan0 should have positive high score", validLanScore > 900)
        assertTrue("Hotspot AP gateway should have high score", hotspotScore > 900)
    }

    @Test
    fun testCellularAndVpnAddressesAreDeprioritized() {
        val cellularIp = InetAddress.getByName("10.50.2.14") as Inet4Address
        val vpnIp = InetAddress.getByName("10.8.0.5") as Inet4Address
        val wifiIp = InetAddress.getByName("192.168.1.10") as Inet4Address

        val cellScore = LocalPartyServer.scoreAddress(cellularIp, "rmnet0")
        val vpnScore = LocalPartyServer.scoreAddress(vpnIp, "tun0")
        val wifiScore = LocalPartyServer.scoreAddress(wifiIp, "wlan0")

        assertTrue("Wi-Fi score should dominate cellular and VPN", wifiScore > cellScore)
        assertTrue("Wi-Fi score should dominate VPN", wifiScore > vpnScore)
        assertTrue("Cellular score should be negative", cellScore < 0)
        assertTrue("VPN score should be negative", vpnScore < 0)
    }

    @Test
    fun testPairingCodeAndHostTokenGeneration() {
        val code1 = LocalPartyServer.newPairingCode()
        val code2 = LocalPartyServer.newPairingCode()
        val token = LocalPartyServer.newHostToken()

        assertEquals(6, code1.length)
        assertEquals(6, code2.length)
        assertTrue("Pairing code must only contain allowed alphanumeric characters", code1.all { it.isLetterOrDigit() })
        assertNotNull(token)
        assertTrue("Token must be a valid UUID string", token.length >= 32)
    }
}
