package com.battaglianavale.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battaglianavale.game.GameViewModel
import com.battaglianavale.models.GameSettings
import com.battaglianavale.models.ShipConfig
import com.battaglianavale.ui.theme.*

@Composable
fun SetupScreen(vm: GameViewModel) {
    val peers        by vm.net.discoveredPeers.collectAsState()
    val status       by vm.net.connectionStatus.collectAsState()
    val isConnected  by vm.net.isConnected.collectAsState()
    val isHost       by vm.net.isHost.collectAsState()
    val settings     by vm.settings.collectAsState()

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // ── Title ────────────────────────────────────────────────────────
            Text("⚓ Battaglia Navale",
                fontSize   = 28.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = InkBlue)
            Text("avvicina i due dispositivi",
                fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkGray)
            Spacer(Modifier.height(24.dp))

            // ── Connection card ───────────────────────────────────────────────
            PaperCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                            .background(if (isConnected) InkGreen else InkGray.copy(alpha = 0.5f)))
                        Spacer(Modifier.width(8.dp))
                        Text(status, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
                    }

                    if (!isConnected) {
                        Spacer(Modifier.height(12.dp))
                        if (peers.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = InkBlue)
                                Spacer(Modifier.width(8.dp))
                                Text("Ricerca in corso...", fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace, color = InkGray)
                            }
                        } else {
                            Text("Dispositivi trovati:", fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, color = InkGray)
                            Spacer(Modifier.height(8.dp))
                            peers.forEach { peer ->
                                Button(
                                    onClick = { vm.connectToPeer(peer) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors   = ButtonDefaults.buttonColors(containerColor = ShipFill),
                                    shape    = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📱 ${peer.name}  →  Connetti",
                                        color = InkBlue, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("✅ ${if (isHost) "Sei l'host" else "Sei il guest"}",
                            color = InkGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    }
                }
            }

            // ── Settings (only shown when connected) ──────────────────────────
            if (isConnected) {
                Spacer(Modifier.height(16.dp))
                PaperCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚙️ Impostazioni",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace, color = InkBlue)
                            Spacer(Modifier.weight(1f))
                            if (isHost) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = InkBlue.copy(alpha = 0.12f)
                                ) {
                                    Text("HOST", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
                                }
                            }
                        }
                        Divider(color = GridLine, modifier = Modifier.padding(vertical = 10.dp))

                        if (isHost) {
                            // Grid size slider
                            Text("Griglia: ${settings.gridSize}×${settings.gridSize}",
                                fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
                            Slider(
                                value = settings.gridSize.toFloat(),
                                onValueChange = { vm.updateSettings(settings.copy(gridSize = it.toInt())) },
                                valueRange = 6f..15f, steps = 8,
                                colors = SliderDefaults.colors(thumbColor = InkBlue, activeTrackColor = InkBlue)
                            )
                            Spacer(Modifier.height(8.dp))
                            Divider(color = GridLine)
                            Spacer(Modifier.height(8.dp))
                            Text("Navi:", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkGray)
                            Spacer(Modifier.height(8.dp))

                            settings.shipConfigs.forEachIndexed { idx, cfg ->
                                ShipConfigRow(cfg) { newCfg ->
                                    val updated = settings.shipConfigs.toMutableList()
                                    updated[idx] = newCfg
                                    vm.updateSettings(settings.copy(shipConfigs = updated))
                                }
                                Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { vm.sendSettings() },
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.buttonColors(containerColor = InkBlue),
                                shape    = RoundedCornerShape(8.dp)
                            ) {
                                Text("📡 Invia impostazioni al guest",
                                    fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            // Guest: show received
                            Text("In attesa impostazioni dall'host...",
                                fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                                color = InkGray, modifier = Modifier.padding(bottom = 8.dp))
                            Text("Griglia: ${settings.gridSize}×${settings.gridSize}",
                                fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
                            Spacer(Modifier.height(6.dp))
                            settings.shipConfigs.forEach { cfg ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ShipIcon(cfg.size, small = true)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${cfg.name} ×${cfg.count}",
                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Start placement button ────────────────────────────────────
                Button(
                    onClick  = { vm.startPlacement() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = InkBlue),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("🗺️  Posiziona le navi  →",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Ship config row ─────────────────────────────────────────────────────────
@Composable
private fun ShipConfigRow(cfg: ShipConfig, onUpdate: (ShipConfig) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShipIcon(cfg.size, small = false)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(cfg.name, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = InkBlue)
            Text("size ${cfg.size}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = InkGray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (cfg.count > 0) onUpdate(cfg.copy(count = cfg.count - 1)) }) {
                Text("−", fontSize = 20.sp, color = InkBlue)
            }
            Text("${cfg.count}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = InkBlue,
                modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { if (cfg.count < 5) onUpdate(cfg.copy(count = cfg.count + 1)) }) {
                Text("+", fontSize = 20.sp, color = InkBlue)
            }
        }
    }
}

// ─── Ship icon row ────────────────────────────────────────────────────────────
@Composable
fun ShipIcon(size: Int, small: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (small) 1.dp else 2.dp)) {
        repeat(size) {
            Box(
                Modifier
                    .size(if (small) 12.dp else 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(InkBlue)
                    .border(0.5.dp, InkBlue.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    }
}

// ─── Paper card ──────────────────────────────────────────────────────────────
@Composable
fun PaperCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        color     = Color.White.copy(alpha = 0.75f),
        shadowElevation = 2.dp,
        border    = BorderStroke(1.dp, GridLine)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
