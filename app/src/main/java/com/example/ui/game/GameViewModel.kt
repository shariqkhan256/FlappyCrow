package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.UnlockedAccessory
import com.example.data.database.UnlockedAchievement
import com.example.data.database.UserStats
import com.example.data.repository.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GameScreen {
    Menu,
    Game,
    Customize,
    Achievements,
    Settings,
    GameOver
}

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    // Current active screen in our custom state-based router
    var currentScreen by mutableStateOf(GameScreen.Menu)
        private set

    // Settings
    var isSoundEnabled by mutableStateOf(true)
        private set
    var isVibrationEnabled by mutableStateOf(true)
        private set

    // Game Session Stats
    var lastRunScore by mutableStateOf(0)
        private set
    var lastRunCoinsCollected by mutableStateOf(0)
        private set
    var isNewHighScore by mutableStateOf(false)
        private set

    // Real-time flows from database
    val userStats: StateFlow<UserStats> = repository.userStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats()
        )

    val accessories: StateFlow<List<UnlockedAccessory>> = repository.accessories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val achievements: StateFlow<List<UnlockedAchievement>> = repository.achievements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.initializeDataIfNeeded()
        }
    }

    fun navigateTo(screen: GameScreen) {
        currentScreen = screen
    }

    fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
    }

    fun toggleVibration() {
        isVibrationEnabled = !isVibrationEnabled
    }

    // Save game results
    fun handleGameOver(score: Int, coinsCollected: Int) {
        lastRunScore = score
        lastRunCoinsCollected = coinsCollected
        
        viewModelScope.launch {
            val stats = userStats.value
            isNewHighScore = score > stats.highScore
            repository.updateHighScoreAndCoins(score, coinsCollected)
            
            // Check for cumulative chimney achievement (city_explorer)
            if (score >= 15) {
                repository.unlockAchievement("city_explorer")
            }
            
            navigateTo(GameScreen.GameOver)
        }
    }

    // Purchase an accessory
    fun buyAccessory(accessoryId: String) {
        viewModelScope.launch {
            val success = repository.purchaseAccessory(accessoryId)
            if (success) {
                repository.selectAccessory(accessoryId)
            }
        }
    }

    // Select an accessory
    fun selectAccessory(accessoryId: String) {
        viewModelScope.launch {
            repository.selectAccessory(accessoryId)
        }
    }

    // Trigger wings flap achievement
    fun onFirstFlap() {
        viewModelScope.launch {
            repository.unlockAchievement("first_flight")
        }
    }

    // Factory for constructing ViewModel with Repository
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = GameRepository(database.gameDao())
                    return GameViewModel(application, repository) as T
                }
            }
    }
}
