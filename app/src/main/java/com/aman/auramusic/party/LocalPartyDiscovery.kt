package com.aman.auramusic.party

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/**
 * Metadata for a local Party Mode host discovered on the LAN via Android NSD / mDNS.
 */
data class DiscoveredParty(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
    val pairingCode: String = "",
    val displayName: String = ""
)

/**
 * Manages automatic local party advertisement and discovery using Android Network Service Discovery (NSD).
 * Retains manual IP connection as a fallback.
 */
class LocalPartyDiscovery(private val context: Context) {
    companion object {
        const val SERVICE_TYPE = "_auramusic-party._tcp."
        private const val TAG = "LocalPartyDiscovery"
        private const val PREFIX = "AuraParty-"
    }

    private val nsdManager: NsdManager? by lazy {
        runCatching { context.getSystemService(Context.NSD_SERVICE) as? NsdManager }.getOrNull()
    }

    private val _discoveredParties = MutableStateFlow<List<DiscoveredParty>>(emptyList())
    val discoveredParties: StateFlow<List<DiscoveredParty>> = _discoveredParties.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    @Volatile private var isDiscovering = false
    @Volatile private var isResolving = false
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()

    /**
     * Advertises this device as a Party Mode host on the local Wi-Fi / hotspot.
     */
    fun registerParty(pairingCode: String, displayName: String, port: Int = LocalPartyServer.DEFAULT_PORT) {
        val nsd = nsdManager ?: return
        unregisterParty()

        val serviceInfo = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = "$PREFIX$pairingCode"
            setPort(port)
            setAttribute("code", pairingCode)
            if (displayName.isNotBlank()) {
                setAttribute("name", displayName.take(32))
            }
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(service: NsdServiceInfo) {
                Log.d(TAG, "Local party registered with NSD: ${service.serviceName}")
            }

            override fun onRegistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD party registration failed: code $errorCode")
            }

            override fun onServiceUnregistered(service: NsdServiceInfo) {
                Log.d(TAG, "Local party unregistered: ${service.serviceName}")
            }

            override fun onUnregistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD party unregistration failed: code $errorCode")
            }
        }

        registrationListener = listener
        runCatching {
            nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            Log.w(TAG, "Failed to start NSD registration", it)
            registrationListener = null
        }
    }

    /**
     * Stops advertising this device as a Party Mode host.
     */
    fun unregisterParty() {
        val listener = registrationListener ?: return
        registrationListener = null
        runCatching { nsdManager?.unregisterService(listener) }
    }

    /**
     * Starts listening for other Party Mode hosts on the local network.
     */
    fun startDiscovery() {
        val nsd = nsdManager ?: return
        if (isDiscovering) return
        _discoveredParties.value = emptyList()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                isDiscovering = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("auramusic-party") || service.serviceName.startsWith(PREFIX)) {
                    enqueueResolve(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val name = service.serviceName
                _discoveredParties.value = _discoveredParties.value.filterNot { it.serviceName == name }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                runCatching { nsd.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
            }
        }

        discoveryListener = listener
        runCatching {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            isDiscovering = false
            discoveryListener = null
        }
    }

    /**
     * Stops discovering local parties.
     */
    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        isDiscovering = false
        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }
        runCatching { nsdManager?.stopServiceDiscovery(listener) }
    }

    private fun enqueueResolve(service: NsdServiceInfo) {
        synchronized(resolveQueue) {
            resolveQueue.add(service)
            if (!isResolving) {
                resolveNext()
            }
        }
    }

    private fun resolveNext() {
        val next = synchronized(resolveQueue) {
            if (resolveQueue.isEmpty()) {
                isResolving = false
                null
            } else {
                isResolving = true
                resolveQueue.poll()
            }
        } ?: return

        val nsd = nsdManager ?: run {
            synchronized(resolveQueue) { isResolving = false }
            return
        }

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                synchronized(resolveQueue) { resolveNext() }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress
                val port = serviceInfo.port
                if (!host.isNullOrBlank() && port > 0) {
                    val codeAttr = serviceInfo.attributes?.get("code")?.let { String(it, Charsets.UTF_8) }
                    val nameAttr = serviceInfo.attributes?.get("name")?.let { String(it, Charsets.UTF_8) }
                    val fallbackCode = serviceInfo.serviceName.removePrefix(PREFIX).take(12)
                    val party = DiscoveredParty(
                        serviceName = serviceInfo.serviceName,
                        hostAddress = host,
                        port = port,
                        pairingCode = codeAttr ?: fallbackCode,
                        displayName = nameAttr.orEmpty()
                    )
                    _discoveredParties.value = (_discoveredParties.value.filterNot { it.serviceName == party.serviceName } + party)
                }
                synchronized(resolveQueue) { resolveNext() }
            }
        }

        runCatching {
            nsd.resolveService(next, resolveListener)
        }.onFailure {
            synchronized(resolveQueue) { resolveNext() }
        }
    }
}
