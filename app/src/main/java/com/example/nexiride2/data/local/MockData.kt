package com.example.nexiride2.data.local

import com.example.nexiride2.domain.model.*

object MockData {
    /**
     * Template date on each route document; the app’s search / booking always applies
     * the user’s selected calendar day, so the same bus line is bookable for any future day.
     */
    private const val INVENTORY_DATE = "2026-01-01"

    /** Major hubs + regional — every ordered pair (A→B, A≠B) gets at least one bus in [routes]. */
    val cities = listOf(
        "Accra", "Kumasi", "Tamale", "Cape Coast", "Takoradi", "Sunyani", "Ho", "Koforidua",
        "Bolgatanga", "Wa", "Tema", "Sekondi", "Obuasi", "Nkawkaw", "Berekum", "Dambai",
        "Yendi", "Hohoe", "Akosombo", "Prestea"
    )

    val buses = listOf(
        Bus("b1", "VIP Jeoun Transport", null, "VIP", "GR-2011-21", 45, listOf("AC", "WiFi", "USB Charging", "Reclining Seats"), 4.5f),
        Bus("b2", "STC Intercity", null, "Standard", "GR-8844-10", 50, listOf("AC", "USB Charging"), 4.2f),
        Bus("b3", "OA Travel & Tours", null, "Luxury", "GR-5520-19", 30, listOf("AC", "WiFi", "USB Charging", "TV", "Snacks"), 4.8f),
        Bus("b4", "Metro Mass Transit", null, "Standard", "GR-1102-15", 60, listOf("AC"), 3.5f),
        Bus("b5", "VVIP Transport", null, "VIP", "GR-9901-32", 40, listOf("AC", "WiFi", "USB Charging", "Reclining Seats", "Blanket"), 4.6f)
    )

    /** Four daily departures per city pair: early, morning, afternoon, evening. */
    private val slotHours = listOf(6, 10, 14, 19)

    val routes: List<Route> = buildList {
        var idx = 0
        for (from in cities) {
            for (to in cities) {
                if (from == to) continue
                slotHours.forEachIndexed { slot, depH ->
                    idx++
                    val baseBus = buses[(idx + slot) % buses.size]
                    val busNumber = "GR-${String.format("%04d", idx % 10000)}-${listOf("A", "B", "C", "D", "E")[(idx + slot) % 5]}"
                    val bus = baseBus.copy(busNumber = busNumber)
                    val depM = ((idx + slot) * 7) % 60
                    val dep = "%02d:%02d".format(depH, depM)
                    val hours = 2 + ((idx + slot) % 10)
                    val mins = (slot % 4) * 15
                    val arrH = (depH + hours) % 24
                    val arr = "%02d:%02d".format(arrH, depM)
                    val duration = "${hours}h ${mins}m"
                    val base = 50.0 + (from.length + to.length) * 1.8 + (idx % 7) * 12.0 + slot * 8.0
                    val price = "%.0f".format(base).toDouble()
                    val seatsAvail = 28 + ((idx + slot) % 25)
                    add(
                        Route(
                            id = "nx_" + idx.toString().padStart(5, '0'),
                            origin = from,
                            destination = to,
                            departureTime = dep,
                            arrivalTime = arr,
                            duration = duration,
                            price = price,
                            currency = "GHS",
                            bus = bus,
                            stops = listOf(
                                BusStop("o$idx", Stations.forCity(from), dep, dep, null, 1),
                                BusStop("d$idx", Stations.forCity(to), arr, arr, null, 2)
                            ),
                            availableSeats = seatsAvail,
                            date = INVENTORY_DATE
                        )
                    )
                }
            }
        }
    }

    private fun routeAccraKumasi() = routes.first { it.origin == "Accra" && it.destination == "Kumasi" }
    private fun routeAccraCape() = routes.first { it.origin == "Accra" && it.destination == "Cape Coast" }
    private fun routeAccraTamale() = routes.first { it.origin == "Accra" && it.destination == "Tamale" }

    fun generateSeats(routeId: String): List<Seat> {
        val totalSeats = routes.find { it.id == routeId }?.bus?.totalSeats ?: 45
        val cols = 4
        return (1..totalSeats).map { i ->
            val row = (i - 1) / cols + 1; val col = (i - 1) % cols
            val status = if (i % 7 == 0 || i % 11 == 0) SeatStatus.RESERVED else SeatStatus.AVAILABLE
            Seat("seat_${routeId}_$i", "$i", row, col, status, routes.find { it.id == routeId }?.price ?: 100.0)
        }
    }

    val currentUser = User("u1", "Kofi Mensah", "kofi@email.com", "+233 24 123 4567", null,
        listOf(routeAccraKumasi().id, routeAccraCape().id), listOf(
            PaymentMethod("pm1", PaymentType.MOBILE_MONEY_MTN, "MTN MoMo", "••• 4567", true),
            PaymentMethod("pm2", PaymentType.VISA, "Visa", "•••• 8901", false)
        ))

    val bookings = mutableListOf(
        Booking("bk1", "NXR-2025-001", routeAccraKumasi(), generateSeats(routeAccraKumasi().id).filter { it.number == "12" || it.number == "13" },
            listOf(Passenger("Kofi Mensah", "+233241234567", "12"), Passenger("Ama Mensah", "+233241234568", "13")),
            BookingStatus.CONFIRMED, 240.0, "GHS", "MTN MoMo", "2025-04-10", "NXR-2025-001-CONF",
            BaggageInfo(2, 30.0)),
        Booking("bk2", "NXR-2025-002", routeAccraCape(), generateSeats(routeAccraCape().id).filter { it.number == "5" },
            listOf(Passenger("Kofi Mensah", "+233241234567", "5")),
            BookingStatus.COMPLETED, 80.0, "GHS", "Visa •••• 8901", "2025-03-20", "NXR-2025-002-COMP"),
        Booking("bk3", "NXR-2025-003", routeAccraTamale(), generateSeats(routeAccraTamale().id).filter { it.number == "8" },
            listOf(Passenger("Kofi Mensah", "+233241234567", "8")),
            BookingStatus.CANCELLED, 200.0, "GHS", "MTN MoMo", "2025-03-15", "NXR-2025-003-CXL")
    )

    val notifications = mutableListOf(
        AppNotification("n1", "Booking Confirmed!", "Your booking NXR-2025-001 for Accra → Kumasi on Apr 15 has been confirmed.", NotificationType.BOOKING_CONFIRMATION, "2025-04-10 14:30", false, "bk1"),
        AppNotification("n2", "Trip Reminder", "Your trip Accra → Kumasi departs tomorrow at 06:00. Don't forget your ticket!", NotificationType.TRIP_REMINDER, "2025-04-14 06:00", false, "bk1"),
        AppNotification("n3", "Refund Processed", "Your refund of GHS 200.00 for booking NXR-2025-003 has been processed.", NotificationType.REFUND, "2025-03-16 10:00", true, "bk3"),
        AppNotification("n4", "Special Offer! 🎉", "Get 20% off on all VIP routes this weekend. Use code: NEXIVIP20", NotificationType.PROMO, "2025-04-12 09:00", true),
        AppNotification("n5", "Schedule Update", "The 18:00 Accra → Tamale service has been delayed by 30 minutes.", NotificationType.DELAY, "2025-04-14 16:00", false, "bk1")
    )
}
