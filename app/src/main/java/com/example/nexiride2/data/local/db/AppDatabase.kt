package com.example.nexiride2.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadedTicketEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedTicketDao(): DownloadedTicketDao
}

