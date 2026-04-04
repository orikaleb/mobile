package com.example.nexiride2.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tickets")
data class DownloadedTicketEntity(
    @PrimaryKey val bookingId: String,
    val referenceCode: String,
    val createdAtEpochMs: Long,
    val pdfPath: String
)

