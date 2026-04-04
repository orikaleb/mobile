package com.example.nexiride2.di

import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideSearchBusesUseCase(busRepository: BusRepository) = SearchBusesUseCase(busRepository)

    @Provides
    @Singleton
    fun provideGetBookingsUseCase(bookingRepository: BookingRepository) = GetBookingsUseCase(bookingRepository)

    @Provides
    @Singleton
    fun provideCancelBookingUseCase(bookingRepository: BookingRepository) = CancelBookingUseCase(bookingRepository)

    @Provides
    @Singleton
    fun provideCreateBookingUseCase(bookingRepository: BookingRepository) = CreateBookingUseCase(bookingRepository)
}

