package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_accessories")
data class UnlockedAccessory(
    @PrimaryKey val id: String,
    val name: String,
    val cost: Int,
    val isUnlocked: Boolean = false
)
