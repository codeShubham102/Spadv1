package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gamepad_profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetGame: String,
    val profileJson: String,
    val isBuiltIn: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
