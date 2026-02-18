package com.battaglianavale.models

import com.google.gson.Gson

// ─── Position ───────────────────────────────────────────────────────────────
data class Position(val row: Int, val col: Int)

// ─── Cell states ─────────────────────────────────────────────────────────────
enum class MyCellState   { EMPTY, SHIP, HIT_SHIP, MISSED_BY_ENEMY }
enum class EnemyCellState { UNKNOWN, HIT, MISS, SUNK }

// ─── Ship config (setup) ─────────────────────────────────────────────────────
data class ShipConfig(
    val name: String,
    val size: Int,
    var count: Int
) {
    companion object {
        val defaults = listOf(
            ShipConfig("Portaerei",          5, 1),
            ShipConfig("Corazzata",          4, 1),
            ShipConfig("Incrociatore",       3, 2),
            ShipConfig("Cacciatorpediniere", 2, 3),
            ShipConfig("Sottomarino",        1, 2),
        )
    }
}

// ─── Game settings ────────────────────────────────────────────────────────────
data class GameSettings(
    val gridSize: Int = 10,
    val shipConfigs: List<ShipConfig> = ShipConfig.defaults.map { it.copy() }
)

// ─── Placed ship ──────────────────────────────────────────────────────────────
data class PlacedShip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val size: Int,
    val positions: List<Position>,
    val hitPositions: MutableSet<Position> = mutableSetOf()
) {
    val isSunk: Boolean get() = hitPositions.size >= size

    fun receiveHit(pos: Position): Boolean {
        hitPositions.add(pos)
        return true
    }
}

// ─── Fleet entry (enemy fleet tracking) ──────────────────────────────────────
data class FleetEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val size: Int,
    var hitCount: Int = 0
) {
    val isSunk: Boolean get() = hitCount >= size
}

// ─── Ship orientation ─────────────────────────────────────────────────────────
enum class ShipOrientation {
    HORIZONTAL, VERTICAL;
    val toggled get() = if (this == HORIZONTAL) VERTICAL else HORIZONTAL
    val label   get() = if (this == HORIZONTAL) "→ Orizzontale" else "↓ Verticale"
}

// ─── Game phase ───────────────────────────────────────────────────────────────
sealed class GamePhase {
    object Setup              : GamePhase()
    object Placement          : GamePhase()
    object WaitingForOpponent : GamePhase()
    object Playing            : GamePhase()
    data class GameOver(val won: Boolean) : GamePhase()
}

// ─── Network messages ─────────────────────────────────────────────────────────
enum class MsgType { SETTINGS, SETTINGS_ACK, PLACEMENT_READY, FIRST_TURN, SHOT, SHOT_RESULT }
enum class ShotOutcome { MISS, HIT, SUNK }

data class GameMessage(
    val type: MsgType,
    val settings: GameSettings?    = null,
    val position: Position?        = null,
    val shotOutcome: ShotOutcome?  = null,
    val hostGoesFirst: Boolean?    = null,
    val sunkShipName: String?      = null,
    val sunkShipSize: Int?         = null
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): GameMessage = Gson().fromJson(json, GameMessage::class.java)
    }
}
