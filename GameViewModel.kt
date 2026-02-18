package com.battaglianavale.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battaglianavale.models.*
import com.battaglianavale.network.NetworkManager
import com.battaglianavale.network.PeerInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {

    // ── Network ──────────────────────────────────────────────────────────────
    val net = NetworkManager(app)

    // ── Game state ────────────────────────────────────────────────────────────
    private val _phase       = MutableStateFlow<GamePhase>(GamePhase.Setup)
    val phase: StateFlow<GamePhase> = _phase

    private val _isMyTurn    = MutableStateFlow(false)
    val isMyTurn: StateFlow<Boolean> = _isMyTurn

    private val _lastMessage = MutableStateFlow("")
    val lastMessage: StateFlow<String> = _lastMessage

    // Settings
    private val _settings    = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings

    // My grid
    private val _myShips     = MutableStateFlow<List<PlacedShip>>(emptyList())
    val myShips: StateFlow<List<PlacedShip>> = _myShips

    private val _enemyShotsOnMe = MutableStateFlow<Map<Position, MyCellState>>(emptyMap())
    val enemyShotsOnMe: StateFlow<Map<Position, MyCellState>> = _enemyShotsOnMe

    // Enemy grid
    private val _myShots     = MutableStateFlow<Map<Position, EnemyCellState>>(emptyMap())
    val myShots: StateFlow<Map<Position, EnemyCellState>> = _myShots

    private val _enemyFleet  = MutableStateFlow<List<FleetEntry>>(emptyList())
    val enemyFleet: StateFlow<List<FleetEntry>> = _enemyFleet

    // ── Init ─────────────────────────────────────────────────────────────────
    init {
        net.start()
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            net.incomingMessage.collect { msg ->
                msg ?: return@collect
                handle(msg)
                net.clearMessage()
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun updateSettings(newSettings: GameSettings) { _settings.value = newSettings }

    fun sendSettings() {
        net.send(GameMessage(type = MsgType.SETTINGS, settings = _settings.value))
    }

    fun connectToPeer(peer: PeerInfo) { net.connectTo(peer) }

    fun startPlacement() {
        _enemyFleet.value = _settings.value.shipConfigs.flatMap { cfg ->
            (0 until cfg.count).map { FleetEntry(name = cfg.name, size = cfg.size) }
        }
        _phase.value = GamePhase.Placement
    }

    fun confirmPlacement(ships: List<PlacedShip>) {
        _myShips.value = ships
        net.send(GameMessage(type = MsgType.PLACEMENT_READY))
        _phase.value = GamePhase.WaitingForOpponent
    }

    fun shoot(pos: Position) {
        if (!_isMyTurn.value) return
        if (_myShots.value.containsKey(pos)) return
        _isMyTurn.value = false
        net.send(GameMessage(type = MsgType.SHOT, position = pos))
    }

    fun resetGame() {
        _phase.value           = GamePhase.Setup
        _isMyTurn.value        = false
        _myShips.value         = emptyList()
        _enemyShotsOnMe.value  = emptyMap()
        _myShots.value         = emptyMap()
        _enemyFleet.value      = emptyList()
        _lastMessage.value     = ""
        _settings.value        = GameSettings()
        net.disconnect()
        net.start()
    }

    // ── Message handler ───────────────────────────────────────────────────────
    private fun handle(msg: GameMessage) {
        when (msg.type) {

            MsgType.SETTINGS -> {
                msg.settings?.let { s ->
                    _settings.value = s
                    _enemyFleet.value = s.shipConfigs.flatMap { cfg ->
                        (0 until cfg.count).map { FleetEntry(name = cfg.name, size = cfg.size) }
                    }
                    net.send(GameMessage(type = MsgType.SETTINGS_ACK))
                }
            }

            MsgType.SETTINGS_ACK -> { /* host can now proceed */ }

            MsgType.PLACEMENT_READY -> {
                if (_phase.value == GamePhase.WaitingForOpponent && net.isHost.value) {
                    val hostFirst = (0..1).random() == 0
                    _isMyTurn.value = hostFirst
                    net.send(GameMessage(type = MsgType.FIRST_TURN, hostGoesFirst = hostFirst))
                    _phase.value = GamePhase.Playing
                    _lastMessage.value = if (hostFirst) "Tocca a te! Spara!" else "Aspetta la mossa avversaria..."
                }
            }

            MsgType.FIRST_TURN -> {
                msg.hostGoesFirst?.let { hostFirst ->
                    _isMyTurn.value = !hostFirst   // guest is opposite of host
                    _phase.value = GamePhase.Playing
                    _lastMessage.value = if (!hostFirst) "Tocca a te! Spara!" else "Aspetta la mossa avversaria..."
                }
            }

            MsgType.SHOT -> {
                msg.position?.let { pos -> receiveEnemyShot(pos) }
            }

            MsgType.SHOT_RESULT -> {
                msg.position?.let { pos ->
                    receiveShotResult(pos, msg.shotOutcome, msg.sunkShipName, msg.sunkShipSize)
                }
            }
        }
    }

    // ── Enemy shoots at me ────────────────────────────────────────────────────
    private fun receiveEnemyShot(pos: Position) {
        val ships = _myShips.value.toMutableList()
        var hitIdx = -1
        for (i in ships.indices) {
            if (pos in ships[i].positions) { hitIdx = i; break }
        }

        val outcome: ShotOutcome
        var sunkName: String? = null
        var sunkSize: Int?    = null
        val newShotsOnMe = _enemyShotsOnMe.value.toMutableMap()

        if (hitIdx >= 0) {
            ships[hitIdx].receiveHit(pos)
            _myShips.value = ships
            if (ships[hitIdx].isSunk) {
                outcome  = ShotOutcome.SUNK
                sunkName = ships[hitIdx].name
                sunkSize = ships[hitIdx].size
                for (p in ships[hitIdx].positions) newShotsOnMe[p] = MyCellState.HIT_SHIP
            } else {
                outcome = ShotOutcome.HIT
                newShotsOnMe[pos] = MyCellState.HIT_SHIP
            }
        } else {
            outcome = ShotOutcome.MISS
            newShotsOnMe[pos] = MyCellState.MISSED_BY_ENEMY
        }
        _enemyShotsOnMe.value = newShotsOnMe

        net.send(GameMessage(
            type = MsgType.SHOT_RESULT, position = pos,
            shotOutcome = outcome, sunkShipName = sunkName, sunkShipSize = sunkSize
        ))

        _isMyTurn.value = true

        if (_myShips.value.all { it.isSunk }) {
            _phase.value = GamePhase.GameOver(won = false)
        }
    }

    // ── I receive result of MY shot ───────────────────────────────────────────
    private fun receiveShotResult(pos: Position, outcome: ShotOutcome?,
                                   sunkName: String?, sunkSize: Int?) {
        val shots = _myShots.value.toMutableMap()
        when (outcome) {
            ShotOutcome.MISS -> {
                shots[pos]         = EnemyCellState.MISS
                _myShots.value     = shots
                _lastMessage.value = "💧 Acqua!"
                _isMyTurn.value    = false
            }
            ShotOutcome.HIT -> {
                shots[pos]         = EnemyCellState.HIT
                _myShots.value     = shots
                _lastMessage.value = "💥 Colpito! Spara ancora!"
                _isMyTurn.value    = true   // hit = shoot again
            }
            ShotOutcome.SUNK -> {
                shots[pos]         = EnemyCellState.SUNK
                // mark all adjacent HITs as SUNK
                shots.entries.filter { it.value == EnemyCellState.HIT }.forEach { shots[it.key] = EnemyCellState.SUNK }
                _myShots.value     = shots
                _lastMessage.value = "⚓ ${sunkName ?: "Nave"} affondata!"
                _isMyTurn.value    = true

                // Update fleet panel
                sunkName?.let { name ->
                    val fleet = _enemyFleet.value.toMutableList()
                    val idx   = fleet.indexOfFirst { it.name == name && !it.isSunk }
                    if (idx >= 0) {
                        fleet[idx] = fleet[idx].copy(hitCount = fleet[idx].size)
                        _enemyFleet.value = fleet
                    }
                }
            }
            null -> {}
        }

        if (_enemyFleet.value.all { it.isSunk }) {
            _phase.value = GamePhase.GameOver(won = true)
        }
    }

    override fun onCleared() {
        net.disconnect()
        super.onCleared()
    }
}
