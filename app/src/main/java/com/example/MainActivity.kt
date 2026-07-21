package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.game.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Construct ViewModel with custom Room Repository factory
        val viewModel: GameViewModel by viewModels {
            GameViewModel.provideFactory(application)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        GameNavigationOrchestrator(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun GameNavigationOrchestrator(viewModel: GameViewModel) {
    val stats by viewModel.userStats.collectAsState()
    val accessories by viewModel.accessories.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    // Smooth screen crossfade transitions
    Crossfade(
        targetState = viewModel.currentScreen,
        label = "screen_routing"
    ) { screen ->
        when (screen) {
            GameScreen.Menu -> {
                MainMenuScreen(
                    stats = stats,
                    onNavigate = { target -> viewModel.navigateTo(target) }
                )
            }

            GameScreen.Game -> {
                FlappyCrowGame(
                    selectedAccessory = stats.selectedAccessory,
                    isSoundEnabled = viewModel.isSoundEnabled,
                    isVibrationEnabled = viewModel.isVibrationEnabled,
                    onGameOver = { score, coins ->
                        viewModel.handleGameOver(score, coins)
                    }
                )
            }

            GameScreen.Customize -> {
                CustomizeScreen(
                    stats = stats,
                    accessories = accessories,
                    onSelect = { id -> viewModel.selectAccessory(id) },
                    onBuy = { id -> viewModel.buyAccessory(id) },
                    onBack = { viewModel.navigateTo(GameScreen.Menu) }
                )
            }

            GameScreen.Achievements -> {
                AchievementsScreen(
                    achievements = achievements,
                    onBack = { viewModel.navigateTo(GameScreen.Menu) }
                )
            }

            GameScreen.Settings -> {
                SettingsScreen(
                    isSoundEnabled = viewModel.isSoundEnabled,
                    isVibrationEnabled = viewModel.isVibrationEnabled,
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleVibration = { viewModel.toggleVibration() },
                    onBack = { viewModel.navigateTo(GameScreen.Menu) }
                )
            }

            GameScreen.GameOver -> {
                GameOverScreen(
                    score = viewModel.lastRunScore,
                    highScore = stats.highScore,
                    coinsCollected = viewModel.lastRunCoinsCollected,
                    isNewHighScore = viewModel.isNewHighScore,
                    onTryAgain = {
                        viewModel.navigateTo(GameScreen.Game)
                    },
                    onMainMenu = {
                        viewModel.navigateTo(GameScreen.Menu)
                    }
                )
            }
        }
    }
}
