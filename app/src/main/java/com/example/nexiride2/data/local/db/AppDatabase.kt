package com.example.nexiride2.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadedTicketEntity::class, RouteCacheEntity::class, WalletEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedTicketDao(): DownloadedTicketDao
    abstract fun routeCacheDao(): RouteCacheDao
    abstract fun walletDao(): WalletDao
}

