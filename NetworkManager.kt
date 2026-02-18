package com.battaglianavale.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.battaglianavale.models.GameMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket

private const val SERVICE_TYPE = "_battaglianav._tcp."
private const val SERVICE_NAME = "BattagliaNavale"
private const val PORT         = 47832

data class PeerInfo(val name: String, val host: String, val port: Int)

class NetworkManager(private val context: Context) {

    // ── State flows ──────────────────────────────────────────────────────────
    private val _discoveredPeers = MutableStateFlow<List<PeerInfo>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerInfo>> = _discoveredPeers

    private val _connectionStatus = MutableStateFlow("Cercando giocatori vicini...")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    private val _incomingMessage = MutableStateFlow<GameMessage?>(null)
    val incomingMessage: StateFlow<GameMessage?> = _incomingMessage

    private val _connectedPeerName = MutableStateFlow("")
    val connectedPeerName: StateFlow<String> = _connectedPeerName

    // ── Internal ─────────────────────────────────────────────────────────────
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket?       = null
    private var writer: PrintWriter?        = null
    private var reader: BufferedReader?     = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener?       = null

    // ── Start ────────────────────────────────────────────────────────────────

    fun start() {
        startServer()
        registerService()
        discoverServices()
    }

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                val client = serverSocket!!.accept()  // blocks until a peer connects
                setupConnection(client, asHost = true)
            } catch (e: Exception) {
                // socket closed when we connect as client instead
            }
        }
    }

    private fun registerService() {
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port        = PORT
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, err: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, err: Int) {}
        }
        try { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener) }
        catch (e: Exception) { /* already registered */ }
    }

    private fun discoverServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != SERVICE_TYPE) return
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, err: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val peer = PeerInfo(
                            name = info.serviceName,
                            host = info.host.hostAddress ?: return,
                            port = info.port
                        )
                        val current = _discoveredPeers.value.toMutableList()
                        if (current.none { it.host == peer.host }) {
                            current.add(peer)
                            _discoveredPeers.value = current
                        }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredPeers.value = _discoveredPeers.value
                    .filter { it.name != service.serviceName }
            }
        }
        try { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener) }
        catch (e: Exception) {}
    }

    // ── Connect as guest ─────────────────────────────────────────────────────

    fun connectTo(peer: PeerInfo) {
        scope.launch {
            try {
                serverSocket?.close() // stop being a server
                _connectionStatus.value = "Connessione a ${peer.name}..."
                val socket = Socket(peer.host, peer.port)
                setupConnection(socket, asHost = false)
            } catch (e: Exception) {
                _connectionStatus.value = "Errore connessione: ${e.message}"
            }
        }
    }

    private fun setupConnection(socket: Socket, asHost: Boolean) {
        clientSocket = socket
        writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        _isHost.value = asHost
        _isConnected.value = true
        _connectionStatus.value = "Connesso!"
        startReading()
    }

    // ── Send / Receive ────────────────────────────────────────────────────────

    fun send(msg: GameMessage) {
        scope.launch {
            try { writer?.println(msg.toJson()) }
            catch (e: Exception) { /* connection lost */ }
        }
    }

    private fun startReading() {
        scope.launch {
            try {
                while (true) {
                    val line = reader?.readLine() ?: break
                    val msg  = GameMessage.fromJson(line)
                    _incomingMessage.value = msg
                }
            } catch (e: Exception) {
                _isConnected.value   = false
                _connectionStatus.value = "Disconnesso"
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun disconnect() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            discoveryListener?.let    { nsdManager.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        clientSocket?.close()
        serverSocket?.close()
        _isConnected.value   = false
        _discoveredPeers.value = emptyList()
    }

    fun clearMessage() { _incomingMessage.value = null }
}
