package com.example.nexiride2.di

import android.content.Context
import androidx.room.Room
import com.example.nexiride2.data.local.db.AppDatabase
import com.example.nexiride2.data.local.db.DownloadedTicketDao
import com.example.nexiride2.data.local.db.RouteCacheDao
import com.example.nexiride2.data.local.db.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDownloadedTicketDao(db: AppDatabase): DownloadedTicketDao = db.downloadedTicketDao()

    @Provides
    fun provideRouteCacheDao(db: AppDatabase): RouteCacheDao = db.routeCacheDao()

    @Provides
    fun provideWalletDao(db: AppDatabase): WalletDao = db.walletDao()
}

