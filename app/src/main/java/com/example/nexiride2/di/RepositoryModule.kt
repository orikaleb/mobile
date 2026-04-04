package com.example.nexiride2.di

import android.content.Context
import com.example.nexiride2.data.local.pdf.TicketPdfGenerator
import com.example.nexiride2.data.repository.DownloadedTicketRepositoryImpl
import com.example.nexiride2.data.repository.*
import com.example.nexiride2.data.local.db.DownloadedTicketDao
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
    fun provideBusRepository(): BusRepository = MockBusRepository()

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

