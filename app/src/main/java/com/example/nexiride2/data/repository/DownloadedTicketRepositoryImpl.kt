package com.example.nexiride2.data.repository

import com.example.nexiride2.data.local.db.DownloadedTicketDao
import com.example.nexiride2.data.local.db.DownloadedTicketEntity
import com.example.nexiride2.data.local.pdf.TicketPdfGenerator
import com.example.nexiride2.domain.repository.BookingRepository
import com.example.nexiride2.domain.repository.DownloadedTicket
import com.example.nexiride2.domain.repository.DownloadedTicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadedTicketRepositoryImpl @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val dao: DownloadedTicketDao,
    private val pdfGenerator: TicketPdfGenerator
) : DownloadedTicketRepository {

    override fun observeDownloadedTickets(): Flow<List<DownloadedTicket>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getDownloadedTicket(bookingId: String): DownloadedTicket? {
        return dao.getByBookingId(bookingId)?.toDomain()
    }

    override suspend fun downloadTicketPdf(bookingId: String): Result<DownloadedTicket> {
        val booking = bookingRepository.getBookingById(bookingId).getOrElse {
            return Result.failure(it)
        }
        val file = pdfGenerator.generatePdfToFile(booking)
        val entity = DownloadedTicketEntity(
            bookingId = bookingId,
            referenceCode = booking.referenceCode,
            createdAtEpochMs = System.currentTimeMillis(),
            pdfPath = file.absolutePath
        )
        dao.upsert(entity)
        return Result.success(entity.toDomain())
    }
}

private fun DownloadedTicketEntity.toDomain() = DownloadedTicket(
    bookingId = bookingId,
    referenceCode = referenceCode,
    createdAtEpochMs = createdAtEpochMs,
    pdfPath = pdfPath
)

