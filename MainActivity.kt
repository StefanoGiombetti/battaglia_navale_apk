package com.battaglianavale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.battaglianavale.game.GameViewModel
import com.battaglianavale.models.GamePhase
import com.battaglianavale.ui.screens.*
import com.battaglianavale.ui.theme.BattagliaNavaleTheme
import com.battaglianavale.ui.theme.Paper

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BattagliaNavaleTheme {
                BattagliaNavaleApp(vm)
            }
        }
    }
}

@Composable
fun BattagliaNavaleApp(vm: GameViewModel) {
    val phase by vm.phase.collectAsState()

    AnimatedContent(
        targetState = phase,
        modifier = Modifier.fillMaxSize().background(Paper),
        transitionSpec = {
            (fadeIn() + slideInHorizontally { it / 10 }).togetherWith(
                fadeOut() + slideOutHorizontally { -it / 10 }
            )
        },
        label = "phase_transition"
    ) { currentPhase ->
        when (currentPhase) {
            is GamePhase.Setup              -> SetupScreen(vm)
            is GamePhase.Placement          -> PlacementScreen(vm)
            is GamePhase.WaitingForOpponent -> WaitingScreen()
            is GamePhase.Playing            -> GameScreen(vm)
            is GamePhase.GameOver           -> GameOverScreen(currentPhase.won, vm)
        }
    }
}
