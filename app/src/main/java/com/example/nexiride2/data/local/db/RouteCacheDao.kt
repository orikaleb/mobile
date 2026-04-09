package com.example.nexiride2.data.local.db

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query

@Dao
interface RouteCacheDao {

    @Query("SELECT * FROM route_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): RouteCacheEntity?

    @Upsert
    suspend fun upsert(entity: RouteCacheEntity)
}
