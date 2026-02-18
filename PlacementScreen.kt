package com.battaglianavale.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battaglianavale.game.GameViewModel
import com.battaglianavale.models.PlacedShip
import com.battaglianavale.models.Position
import com.battaglianavale.models.ShipOrientation
import com.battaglianavale.ui.components.*
import com.battaglianavale.ui.theme.*

@Composable
fun PlacementScreen(vm: GameViewModel) {
    val settings  by vm.settings.collectAsState()
    val gridSize   = settings.gridSize
    val config     = LocalConfiguration.current
    val screenW    = config.screenWidthDp.dp
    val cellSizeDp = (screenW - 60.dp - 16.dp) / gridSize

    // Build flat list of ships to place
    val shipsToPlace = remember(settings) {
        settings.shipConfigs.flatMap { cfg -> (0 until cfg.count).map { cfg.name to cfg.size } }
    }

    var currentIdx    by remember { mutableIntStateOf(0) }
    var orientation   by remember { mutableStateOf(ShipOrientation.HORIZONTAL) }
    var hoverPos      by remember { mutableStateOf<Position?>(null) }
    var placedShips   by remember { mutableStateOf<List<PlacedShip>>(emptyList()) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }

    val isComplete    = currentIdx >= shipsToPlace.size
    val currentShip   = if (!isComplete) shipsToPlace[currentIdx] else null
    val allOccupied   = placedShips.flatMap { it.positions }
    val previewCells  = remember(hoverPos, orientation, currentShip) {
        if (hoverPos == null || currentShip == null) emptyList()
        else shipPositions(hoverPos!!, currentShip.second, orientation == ShipOrientation.HORIZONTAL)
    }
    val previewValid  = remember(previewCells, allOccupied, gridSize) {
        hoverPos != null && currentShip != null &&
        isValidPlacement(hoverPos!!, currentShip.second,
            orientation == ShipOrientation.HORIZONTAL, gridSize, allOccupied)
    }

    // Auto-clear error
    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            kotlinx.coroutines.delay(1500)
            errorMsg = null
        }
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Header ───────────────────────────────────────────────────────
            Text("⚓ Posiziona le tue navi",
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = InkBlue)
            Spacer(Modifier.height(4.dp))
            AnimatedContent(targetState = currentShip, label = "ship") { ship ->
                if (ship != null)
                    Text("Stai piazzando: ${ship.first} (${ship.second} celle)",
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = InkGray)
                else
                    Text("✅ Tutte le navi piazzate!",
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = InkGreen)
            }
            AnimatedVisibility(visible = errorMsg != null) {
                Text("⚠️ ${errorMsg ?: ""}",
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkRed)
            }

            Spacer(Modifier.height(12.dp))

            // ── Grid ─────────────────────────────────────────────────────────
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GridLine, RoundedCornerShape(8.dp))
                    .background(Paper)
            ) {
                HandDrawnGrid(
                    gridSize     = gridSize,
                    cellSizeDp   = cellSizeDp,
                    shipCells    = allOccupied,
                    previewCells = previewCells,
                    previewValid = previewValid,
                    onCellTap    = { pos ->
                        if (!isComplete && currentShip != null) {
                            if (isValidPlacement(pos, currentShip.second,
                                    orientation == ShipOrientation.HORIZONTAL, gridSize, allOccupied)) {
                                val positions = shipPositions(pos, currentShip.second,
                                    orientation == ShipOrientation.HORIZONTAL)
                                placedShips = placedShips + PlacedShip(
                                    name = currentShip.first, size = currentShip.second, positions = positions)
                                currentIdx++
                                hoverPos = null
                            } else {
                                errorMsg = "Posizione non valida!"
                            }
                        }
                    },
                    onCellHover  = { pos -> hoverPos = pos }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Controls ─────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isComplete) {
                    OutlinedButton(
                        onClick = { orientation = orientation.toggled },
                        border  = BorderStroke(1.dp, InkBlue),
                        shape   = RoundedCornerShape(8.dp)
                    ) {
                        Text(orientation.label, fontFamily = FontFamily.Monospace, color = InkBlue)
                    }
                }
                if (placedShips.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            placedShips = placedShips.dropLast(1)
                            if (currentIdx > 0) currentIdx--
                        },
                        border = BorderStroke(1.dp, InkRed.copy(alpha = 0.5f)),
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Text("↩ Annulla", fontFamily = FontFamily.Monospace, color = InkRed)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Ship tokens row ───────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(shipsToPlace) { idx, ship ->
                    val state = when {
                        idx < currentIdx  -> TokenState.PLACED
                        idx == currentIdx -> TokenState.CURRENT
                        else              -> TokenState.PENDING
                    }
                    ShipToken(ship.first, ship.second, state)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Confirm button ────────────────────────────────────────────────
            AnimatedVisibility(visible = isComplete) {
                Button(
                    onClick  = { vm.confirmPlacement(placedShips) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = InkGreen),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("✅ Conferma e inizia!",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ─── Ship token ───────────────────────────────────────────────────────────────
private enum class TokenState { PLACED, CURRENT, PENDING }

@Composable
private fun ShipToken(name: String, size: Int, state: TokenState) {
    val bg     = when (state) { TokenState.CURRENT -> InkBlue.copy(0.08f); else -> Color.Transparent }
    val border = when (state) { TokenState.CURRENT -> InkBlue.copy(0.4f);  else -> Color.Transparent }
    val alpha  = if (state == TokenState.PLACED) 0.4f else 1f

    Column(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .then(if (alpha < 1f) Modifier.alpha(alpha) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(size) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp))
                    .background(when (state) {
                        TokenState.PLACED  -> InkGray.copy(0.3f)
                        TokenState.CURRENT -> ShipFill
                        TokenState.PENDING -> InkBlue.copy(0.15f)
                    }))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(name, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            color = if (state == TokenState.CURRENT) InkBlue else InkGray,
            maxLines = 1)
    }
}

// ─── Waiting screen ───────────────────────────────────────────────────────────
@Composable
fun WaitingScreen() {
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CircularProgressIndicator(color = InkBlue, strokeWidth = 3.dp)
            Text("In attesa che l'avversario\nfinisca di posizionare le navi...",
                fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = InkBlue,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("⚓", fontSize = 48.sp)
        }
    }
}

// Extension to add alpha directly inline
private fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer { this.alpha = alpha })
