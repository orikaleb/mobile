package com.example.nexiride2.di

import android.content.Context
import com.example.nexiride2.data.connectivity.NetworkStatusProvider
import com.example.nexiride2.data.local.db.DownloadedTicketDao
import com.example.nexiride2.data.local.db.RouteCacheDao
import com.example.nexiride2.data.local.pdf.TicketPdfGenerator
import com.example.nexiride2.data.repository.CachingBusRepository
import com.example.nexiride2.data.repository.DownloadedTicketRepositoryImpl
import com.example.nexiride2.data.repository.*
import com.google.gson.Gson
import com.example.nexiride2.domain.repository.*
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
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        return MockAuthRepository(
            tokenStore = SecureTokenStore(context)
        )
    }

    @Provides
    @Singleton
    fun provideMockBusRepository(): MockBusRepository = MockBusRepository()

    @Provides
    @Singleton
    fun provideBusRepository(
        mockBusRepository: MockBusRepository,
        routeCacheDao: RouteCacheDao,
        gson: Gson,
        networkStatus: NetworkStatusProvider
    ): BusRepository {
        return CachingBusRepository(
            remote = mockBusRepository,
            routeCacheDao = routeCacheDao,
            gson = gson,
            networkStatus = networkStatus
        )
    }

    @Provides
    @Singleton
    fun provideBookingRepository(): BookingRepository = MockBookingRepository()

    @Provides
    @Singleton
    fun provideNotificationRepository(): NotificationRepository = MockNotificationRepository()

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = MockUserRepository()

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

