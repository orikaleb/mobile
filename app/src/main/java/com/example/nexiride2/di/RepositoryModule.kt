package com.example.nexiride2.di

import android.content.Context
import com.example.nexiride2.data.connectivity.NetworkStatusProvider
import com.example.nexiride2.data.firebase.FirebaseAuthRepository
import com.example.nexiride2.data.firebase.FirestoreAdminRepository
import com.example.nexiride2.data.firebase.FirestoreBookingRepository
import com.example.nexiride2.data.firebase.FirestoreBusRepository
import com.example.nexiride2.data.firebase.FirestoreDriverRepository
import com.example.nexiride2.data.firebase.FirestoreNotificationRepository
import com.example.nexiride2.data.firebase.FirestoreUserRepository
import com.example.nexiride2.data.local.db.DownloadedTicketDao
import com.example.nexiride2.data.local.db.RouteCacheDao
import com.example.nexiride2.data.local.pdf.TicketPdfGenerator
import com.example.nexiride2.data.repository.CachingBusRepository
import com.example.nexiride2.data.repository.DownloadedTicketRepositoryImpl
import com.example.nexiride2.data.repository.WalletRepositoryImpl
import com.google.gson.Gson
import com.example.nexiride2.domain.repository.AdminRepository
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.repository.DownloadedTicketRepository
import com.example.nexiride2.domain.repository.DriverRepository
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
    fun provideAuthRepository(impl: FirebaseAuthRepository): AuthRepository = impl

    @Provides
    @Singleton
    fun provideBusRepository(
        firestoreBusRepository: FirestoreBusRepository,
        routeCacheDao: RouteCacheDao,
        gson: Gson,
        networkStatus: NetworkStatusProvider
    ): BusRepository {
        return CachingBusRepository(
            remote = firestoreBusRepository,
            routeCacheDao = routeCacheDao,
            gson = gson,
            networkStatus = networkStatus
        )
    }

    @Provides
    @Singleton
    fun provideBookingRepository(impl: FirestoreBookingRepository): BookingRepository = impl

    @Provides
    @Singleton
    fun provideNotificationRepository(impl: FirestoreNotificationRepository): NotificationRepository = impl

    @Provides
    @Singleton
    fun provideUserRepository(impl: FirestoreUserRepository): UserRepository = impl

    @Provides
    @Singleton
    fun provideAdminRepository(impl: FirestoreAdminRepository): AdminRepository = impl

    @Provides
    @Singleton
    fun provideDriverRepository(impl: FirestoreDriverRepository): DriverRepository = impl

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

