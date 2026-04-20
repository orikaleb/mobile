package com.example.nexiride2.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallet WHERE userId = :userId LIMIT 1")
    suspend fun getByUser(userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)
}
