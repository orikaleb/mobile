package com.example.nexiride2.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadedTicketEntity)

    @Query("SELECT * FROM downloaded_tickets ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<DownloadedTicketEntity>>

    @Query("SELECT * FROM downloaded_tickets WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getByBookingId(bookingId: String): DownloadedTicketEntity?
}

