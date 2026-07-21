package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Query("SELECT * FROM unlocked_accessories")
    fun getAllAccessoriesFlow(): Flow<List<UnlockedAccessory>>

    @Query("SELECT * FROM unlocked_accessories")
    suspend fun getAllAccessories(): List<UnlockedAccessory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessories(accessories: List<UnlockedAccessory>)

    @Update
    suspend fun updateAccessory(accessory: UnlockedAccessory)

    @Query("SELECT * FROM unlocked_achievements")
    fun getAllAchievementsFlow(): Flow<List<UnlockedAchievement>>

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getAllAchievements(): List<UnlockedAchievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<UnlockedAchievement>)

    @Update
    suspend fun updateAchievement(achievement: UnlockedAchievement)
}
