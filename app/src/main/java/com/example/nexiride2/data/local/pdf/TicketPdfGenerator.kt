package com.example.nexiride2.data.local.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.withTranslation
import com.example.nexiride2.domain.model.Booking
import java.io.File
import java.io.FileOutputStream

class TicketPdfGenerator(
    private val context: Context
) {
    fun generatePdfToFile(booking: Booking): File {
        val dir = File(context.filesDir, "tickets").apply { mkdirs() }
        val file = File(dir, "${booking.id}.pdf")

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4-ish at 72dpi
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 60f
            canvas.drawText("Bus Ticket", 40f, y, titlePaint)
            y += 24f
            canvas.drawText("Reference: ${booking.referenceCode}", 40f, y, valuePaint)
            y += 28f

            fun row(label: String, value: String) {
                canvas.drawText("$label:", 40f, y, labelPaint)
                canvas.drawText(value, 160f, y, valuePaint)
                y += 18f
            }

            row("Route", "${booking.route.origin} → ${booking.route.destination}")
            row("Date", booking.route.date)
            row("Time", "${booking.route.departureTime} - ${booking.route.arrivalTime}")
            row("Operator", booking.route.bus.companyName)
            row(
                "Bus number",
                booking.route.bus.busNumber?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
            )
            row("Bus type", booking.route.bus.busType)
            row("Seats", booking.seats.joinToString { it.number })
            row("Passengers", booking.passengers.joinToString { it.name })
            row("Paid with", booking.paymentMethod)
            booking.baggage?.let {
                row("Baggage", "${it.numberOfBags} bags (${it.totalWeight} kg)")
            }
            y += 12f
            row("Total", "${booking.currency} ${"%.2f".format(booking.totalPrice)}")

            canvas.withTranslation(40f, y + 24f) {
                val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
                drawText("This is a demo PDF generated on-device.", 0f, 0f, small)
                drawText("Present this ticket (QR code available in-app).", 0f, 14f, small)
            }

            document.finishPage(page)

            FileOutputStream(file).use { out -> document.writeTo(out) }
            return file
        } finally {
            document.close()
        }
    }
}

