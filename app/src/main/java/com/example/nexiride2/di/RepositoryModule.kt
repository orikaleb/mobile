package com.example.nexiride2.di

import android.content.Context
import com.example.nexiride2.data.connectivity.NetworkStatusProvider
import com.example.nexiride2.data.supabase.SupabaseBookingRepository
import com.example.nexiride2.data.supabase.SupabaseBusRepository
import com.example.nexiride2.data.local.db.DownloadedTicketDao
import com.example.nexiride2.data.local.db.RouteCacheDao
import com.example.nexiride2.data.local.pdf.TicketPdfGenerator
import com.example.nexiride2.data.repository.CachingBusRepository
import com.example.nexiride2.data.repository.DownloadedTicketRepositoryImpl
import com.example.nexiride2.data.repository.WalletRepositoryImpl
import com.example.nexiride2.data.supabase.SupabaseAuthRepository
import com.example.nexiride2.data.supabase.SupabaseNotificationRepository
import com.example.nexiride2.data.supabase.SupabaseUserRepository
import com.google.gson.Gson
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.repository.DownloadedTicketRepository
import com.example.nexiride2.domain.repository.NotificationRepository
import com.example.nexiride2.domain.repository.UserRepository
import com.example.nexiride2.domain.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(impl: SupabaseAuthRepository): AuthRepository = impl

    @Provides
    @Singleton
    fun provideBusRepository(
        supabaseBusRepository: SupabaseBusRepository,
        routeCacheDao: RouteCacheDao,
        gson: Gson,
        networkStatus: NetworkStatusProvider
    ): BusRepository {
        return CachingBusRepository(
            remote = supabaseBusRepository,
            routeCacheDao = routeCacheDao,
            gson = gson,
            networkStatus = networkStatus
        )
    }

    @Provides
    @Singleton
    fun provideBookingRepository(impl: SupabaseBookingRepository): BookingRepository = impl

    @Provides
    @Singleton
    fun provideNotificationRepository(impl: SupabaseNotificationRepository): NotificationRepository = impl

    @Provides
    @Singleton
    fun provideUserRepository(impl: SupabaseUserRepository): UserRepository = impl

    @Provides
    @Singleton
    fun provideWalletRepository(impl: WalletRepositoryImpl): WalletRepository = impl

    @Provides
    @Singleton
    fun provideTicketPdfGenerator(@ApplicationContext context: Context): TicketPdfGenerator {
        return TicketPdfGenerator(context)
    }

    @Provides
    @Singleton
    fun provideDownloadedTicketRepository(
        bookingRepository: BookingRepository,
        dao: DownloadedTicketDao,
        pdfGenerator: TicketPdfGenerator
    ): DownloadedTicketRepository {
        return DownloadedTicketRepositoryImpl(
            bookingRepository = bookingRepository,
            dao = dao,
            pdfGenerator = pdfGenerator
        )
    }
}

