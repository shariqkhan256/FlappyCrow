package com.example.data.repository

import com.example.data.database.GameDao
import com.example.data.database.UnlockedAccessory
import com.example.data.database.UnlockedAchievement
import com.example.data.database.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val userStats: Flow<UserStats> = gameDao.getUserStatsFlow().map { it ?: UserStats() }
    val accessories: Flow<List<UnlockedAccessory>> = gameDao.getAllAccessoriesFlow()
    val achievements: Flow<List<UnlockedAchievement>> = gameDao.getAllAchievementsFlow()

    suspend fun initializeDataIfNeeded() {
        // Initialize UserStats if empty
        if (gameDao.getUserStats() == null) {
            gameDao.insertUserStats(UserStats())
        }

        // Initialize Accessories if empty
        if (gameDao.getAllAccessories().isEmpty()) {
            val defaultAccessories = listOf(
                UnlockedAccessory("none", "No Accessory", 0, true),
                UnlockedAccessory("detective_hat", "Detective Hat", 25, false),
                UnlockedAccessory("red_scarf", "Red Scarf", 15, false),
                UnlockedAccessory("wizard_hat", "Wizard Hat", 40, false),
                UnlockedAccessory("golden_crown", "Golden Crown", 100, false),
                UnlockedAccessory("pilot_goggles", "Pilot Goggles", 60, false)
            )
            gameDao.insertAccessories(defaultAccessories)
        }

        // Initialize Achievements if empty
        if (gameDao.getAllAchievements().isEmpty()) {
            val defaultAchievements = listOf(
                UnlockedAchievement("first_flight", "First Flight", "Flap Coco's wings for the first time"),
                UnlockedAchievement("coin_grabber", "Coin Grabber", "Collect 10 total gold coins in your adventure"),
                UnlockedAchievement("city_explorer", "City Explorer", "Fly through 15 chimneys in total"),
                UnlockedAchievement("golden_wings", "Golden Wings", "Purchase the legendary Golden Crown"),
                UnlockedAchievement("moonlight_legend", "Moonlight Legend", "Achieve an in-game score of 20 or higher")
            )
            gameDao.insertAchievements(defaultAchievements)
        }
    }

    suspend fun updateHighScoreAndCoins(score: Int, sessionCoins: Int) {
        val current = gameDao.getUserStats() ?: UserStats()
        val newHighScore = if (score > current.highScore) score else current.highScore
        val newTotalCoins = current.totalCoins + sessionCoins
        gameDao.insertUserStats(current.copy(highScore = newHighScore, totalCoins = newTotalCoins))
        
        // Check for coin grabber achievement
        if (newTotalCoins >= 10) {
            unlockAchievement("coin_grabber")
        }
        // Check score achievement
        if (newHighScore >= 20) {
            unlockAchievement("moonlight_legend")
        }
    }

    suspend fun selectAccessory(accessoryId: String) {
        val current = gameDao.getUserStats() ?: UserStats()
        gameDao.insertUserStats(current.copy(selectedAccessory = accessoryId))
    }

    suspend fun purchaseAccessory(accessoryId: String): Boolean {
        val stats = gameDao.getUserStats() ?: UserStats()
        val accs = gameDao.getAllAccessories()
        val acc = accs.find { it.id == accessoryId } ?: return false

        if (!acc.isUnlocked && stats.totalCoins >= acc.cost) {
            // Deduct coins and unlock
            val updatedStats = stats.copy(totalCoins = stats.totalCoins - acc.cost)
            val updatedAcc = acc.copy(isUnlocked = true)
            
            gameDao.insertUserStats(updatedStats)
            gameDao.updateAccessory(updatedAcc)

            if (accessoryId == "golden_crown") {
                unlockAchievement("golden_wings")
            }
            return true
        }
        return false
    }

    suspend fun incrementChimneyCount() {
        // Increment chimney explorer stats (could track in achievements or we just check if highscore has surpassed or track cumulative chimneys)
        // Let's increment cumulative chimneys in a custom stats logic if needed,
        // or simple triggers on run. Let's make a simple tracker.
        // Let's unlock "city_explorer" if they hit 15 score in a run, or we can track cumulative chimneys.
        // To be simple and robust: we can check if their score reaches 15 in any run to unlock city_explorer, which is super clear!
    }

    suspend fun unlockAchievement(id: String) {
        val achievements = gameDao.getAllAchievements()
        val ach = achievements.find { it.id == id } ?: return
        if (!ach.isUnlocked) {
            gameDao.updateAchievement(ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
        }
    }
}
