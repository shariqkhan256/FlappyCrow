package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val totalCoins: Int = 0,
    val selectedAccessory: String = "none"
)
