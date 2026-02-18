package com.battaglianavale.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battaglianavale.game.GameViewModel
import com.battaglianavale.models.*
import com.battaglianavale.ui.components.HandDrawnGrid
import com.battaglianavale.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameScreen(vm: GameViewModel) {
    val isMyTurn    by vm.isMyTurn.collectAsState()
    val myShips     by vm.myShips.collectAsState()
    val enemyShots  by vm.enemyShotsOnMe.collectAsState()
    val myShots     by vm.myShots.collectAsState()
    val enemyFleet  by vm.enemyFleet.collectAsState()
    val lastMsg     by vm.lastMessage.collectAsState()
    val settings    by vm.settings.collectAsState()
    val gridSize     = settings.gridSize

    val config       = LocalConfiguration.current
    val isLandscape  = config.screenWidthDp > config.screenHeightDp
    val screenW      = config.screenWidthDp.dp

    // Toast
    var showToast   by remember { mutableStateOf(false) }
    var toastText   by remember { mutableStateOf("") }
    LaunchedEffect(lastMsg) {
        if (lastMsg.isNotEmpty()) {
            toastText = lastMsg; showToast = true
            delay(2000); showToast = false
        }
    }

    // Cell lists
    val myShipCells   = myShips.flatMap { it.positions }
    val hitOnMeCells  = enemyShots.filter { it.value == MyCellState.HIT_SHIP }.map { it.key }
    val missOnMeCells = enemyShots.filter { it.value == MyCellState.MISSED_BY_ENEMY }.map { it.key }
    val sunkOnMeCells = myShips.filter { it.isSunk }.flatMap { it.positions }
    val enemyHitCells = myShots.filter { it.value == EnemyCellState.HIT }.map { it.key }
    val enemyMissCells= myShots.filter { it.value == EnemyCellState.MISS }.map { it.key }
    val enemySunkCells= myShots.filter { it.value == EnemyCellState.SUNK }.map { it.key }

    val gridCellW    = if (isLandscape) (screenW * 0.38f - 20.dp) / gridSize
                       else             (screenW / 2 - 24.dp) / gridSize

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(Modifier.fillMaxSize()) {
            // ── Turn banner ──────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(if (isMyTurn) InkBlue else InkGray.copy(0.15f))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    if (isMyTurn) "🎯  È IL TUO TURNO — Spara!" else "⏳  Aspetta la mossa avversaria...",
                    color = if (isMyTurn) Color.White else InkGray,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isLandscape) {
                // ── Landscape: 3-column layout ───────────────────────────────
                Row(Modifier.fillMaxSize().weight(1f).padding(8.dp)) {
                    // Left: my grid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GridLabel("🗺️ La tua griglia")
                        GridBox(active = false) {
                            HandDrawnGrid(
                                gridSize  = gridSize, cellSizeDp = gridCellW,
                                shipCells = myShipCells, hitCells = hitOnMeCells,
                                missCells = missOnMeCells, sunkCells = sunkOnMeCells
                            )
                        }
                    }

                    // Center: fleet panel
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        FleetPanel(enemyFleet)
                    }

                    // Right: enemy grid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GridLabel("🎯 Flotta nemica")
                        GridBox(active = isMyTurn) {
                            HandDrawnGrid(
                                gridSize  = gridSize, cellSizeDp = gridCellW,
                                hitCells  = enemyHitCells, missCells = enemyMissCells,
                                sunkCells = enemySunkCells,
                                onCellTap = if (isMyTurn) { pos -> vm.shoot(pos) } else null
                            )
                        }
                    }
                }
            } else {
                // ── Portrait: top grids, bottom fleet ────────────────────────
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // My grid
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GridLabel("🗺️ La tua griglia")
                        GridBox(active = false) {
                            HandDrawnGrid(
                                gridSize  = gridSize, cellSizeDp = gridCellW,
                                shipCells = myShipCells, hitCells = hitOnMeCells,
                                missCells = missOnMeCells, sunkCells = sunkOnMeCells
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    // Enemy grid
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GridLabel("🎯 Flotta nemica")
                        GridBox(active = isMyTurn) {
                            HandDrawnGrid(
                                gridSize  = gridSize, cellSizeDp = gridCellW,
                                hitCells  = enemyHitCells, missCells = enemyMissCells,
                                sunkCells = enemySunkCells,
                                onCellTap = if (isMyTurn) { pos -> vm.shoot(pos) } else null
                            )
                        }
                    }
                }
                // Fleet status
                FleetPanel(enemyFleet, Modifier.padding(horizontal = 10.dp))
            }
        }

        // ── Toast ────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showToast,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp),
            enter = slideInVertically() + fadeIn(),
            exit  = slideOutVertically() + fadeOut()
        ) {
            Surface(shape = RoundedCornerShape(50), color = InkBlue.copy(0.92f),
                shadowElevation = 8.dp) {
                Text(toastText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GridLabel(text: String) {
    Text(text, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
        color = InkGray, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun GridBox(active: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width  = if (active) 1.5.dp else 1.dp,
                color  = if (active) InkBlue.copy(0.5f) else GridLine,
                shape  = RoundedCornerShape(6.dp)
            )
            .background(Paper)
    ) {
        content()
        if (!active) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(0.03f)))
        }
    }
}

// ─── Fleet panel ──────────────────────────────────────────────────────────────
@Composable
private fun FleetPanel(fleet: List<FleetEntry>, modifier: Modifier = Modifier) {
    Surface(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(8.dp),
        color     = Color.White.copy(0.5f),
        border    = BorderStroke(1.dp, GridLine)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("Flotta nemica", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace, color = InkGray,
                modifier = Modifier.padding(bottom = 6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                fleet.forEach { entry -> FleetBadge(entry) }
            }
        }
    }
}

@Composable
private fun FleetBadge(entry: FleetEntry) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (entry.isSunk) SunkFill else ShipFill,
        border = BorderStroke(1.dp, if (entry.isSunk) InkRed.copy(0.4f) else InkBlue.copy(0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            (0 until entry.size).forEach { i ->
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(1.dp))
                    .background(
                        if (i < entry.hitCount) InkRed.copy(0.7f) else InkBlue.copy(0.25f)
                    ))
            }
            if (entry.isSunk) {
                Spacer(Modifier.width(2.dp))
                Text("✕", fontSize = 9.sp, color = InkRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Game Over screen ─────────────────────────────────────────────────────────
@Composable
fun GameOverScreen(won: Boolean, vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(if (won) "🏆" else "💀", fontSize = 80.sp)
            Text(if (won) "HAI VINTO!" else "HAI PERSO!",
                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (won) InkGreen else InkRed)
            Text(if (won) "Tutta la flotta nemica è affondata!" else "La tua flotta è stata distrutta...",
                fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = InkGray,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 30.dp))
            Button(
                onClick  = { vm.resetGame() },
                modifier = Modifier.height(52.dp).padding(horizontal = 20.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = InkBlue),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 Nuova partita", fontSize = 16.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
