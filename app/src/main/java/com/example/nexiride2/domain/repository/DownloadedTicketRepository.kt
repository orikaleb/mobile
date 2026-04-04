package com.example.nexiride2.domain.repository

import kotlinx.coroutines.flow.Flow

data class DownloadedTicket(
    val bookingId: String,
    val referenceCode: String,
    val createdAtEpochMs: Long,
    val pdfPath: String
)

interface DownloadedTicketRepository {
    fun observeDownloadedTickets(): Flow<List<DownloadedTicket>>
    suspend fun getDownloadedTicket(bookingId: String): DownloadedTicket?
    suspend fun downloadTicketPdf(bookingId: String): Result<DownloadedTicket>
}

