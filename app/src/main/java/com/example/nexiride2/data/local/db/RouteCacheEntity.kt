package com.example.nexiride2.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_cache")
data class RouteCacheEntity(
    @PrimaryKey val cacheKey: String,
    val routesJson: String,
    val updatedAtEpochMs: Long
)
