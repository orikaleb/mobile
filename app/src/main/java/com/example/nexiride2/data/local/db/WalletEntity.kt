package com.example.nexiride2.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val userId: String,
    val balanceGhs: Double,
    val updatedAtEpochMs: Long
)
