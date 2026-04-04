package com.example.nexiride2.data.local

import com.example.nexiride2.domain.model.*

object MockData {
    val cities = listOf("Accra", "Kumasi", "Tamale", "Cape Coast", "Takoradi", "Sunyani", "Ho", "Koforidua", "Bolgatanga", "Wa")

    val buses = listOf(
        Bus("b1", "VIP Jeoun Transport", null, "VIP", 45, listOf("AC", "WiFi", "USB Charging", "Reclining Seats"), 4.5f),
        Bus("b2", "STC Intercity", null, "Standard", 50, listOf("AC", "USB Charging"), 4.2f),
        Bus("b3", "OA Travel & Tours", null, "Luxury", 30, listOf("AC", "WiFi", "USB Charging", "TV", "Snacks"), 4.8f),
        Bus("b4", "Metro Mass Transit", null, "Standard", 60, listOf("AC"), 3.5f),
        Bus("b5", "VVIP Transport", null, "VIP", 40, listOf("AC", "WiFi", "USB Charging", "Reclining Seats", "Blanket"), 4.6f)
    )

    val routes = listOf(
        Route("r1", "Accra", "Kumasi", "06:00", "11:30", "5h 30m", 120.0, "GHS", buses[0],
            listOf(BusStop("s1","Accra (Circle)","06:00","06:00",null,1), BusStop("s2","Nkawkaw","08:30","08:45","15m",2), BusStop("s3","Kumasi (Kejetia)","11:30","11:30",null,3)),
            32, "2025-04-15"),
        Route("r2", "Accra", "Kumasi", "08:00", "13:00", "5h 00m", 150.0, "GHS", buses[2],
            listOf(BusStop("s4","Accra (Kaneshie)","08:00","08:00",null,1), BusStop("s5","Kumasi (Adum)","13:00","13:00",null,2)),
            18, "2025-04-15"),
        Route("r3", "Accra", "Tamale", "18:00", "06:00", "12h 00m", 200.0, "GHS", buses[4],
            listOf(BusStop("s6","Accra (Circle)","18:00","18:00",null,1), BusStop("s7","Kumasi","23:00","23:30","30m",2), BusStop("s8","Tamale","06:00","06:00",null,3)),
            25, "2025-04-15"),
        Route("r4", "Accra", "Cape Coast", "07:00", "10:00", "3h 00m", 80.0, "GHS", buses[1],
            listOf(BusStop("s9","Accra (Kaneshie)","07:00","07:00",null,1), BusStop("s10","Winneba","08:30","08:40","10m",2), BusStop("s11","Cape Coast","10:00","10:00",null,3)),
            40, "2025-04-15"),
        Route("r5", "Kumasi", "Tamale", "09:00", "15:00", "6h 00m", 130.0, "GHS", buses[3],
            listOf(BusStop("s12","Kumasi (Kejetia)","09:00","09:00",null,1), BusStop("s13","Techiman","11:00","11:15","15m",2), BusStop("s14","Tamale","15:00","15:00",null,3)),
            45, "2025-04-15"),
        Route("r6", "Accra", "Takoradi", "06:30", "10:30", "4h 00m", 100.0, "GHS", buses[0],
            listOf(BusStop("s15","Accra (Circle)","06:30","06:30",null,1), BusStop("s16","Cape Coast","08:30","08:45","15m",2), BusStop("s17","Takoradi","10:30","10:30",null,3)),
            28, "2025-04-15"),
        Route("r7", "Kumasi", "Accra", "14:00", "19:30", "5h 30m", 120.0, "GHS", buses[1],
            listOf(BusStop("s18","Kumasi (Kejetia)","14:00","14:00",null,1), BusStop("s19","Accra (Circle)","19:30","19:30",null,2)),
            35, "2025-04-15"),
        Route("r8", "Tamale", "Accra", "17:00", "05:00", "12h 00m", 200.0, "GHS", buses[4],
            listOf(BusStop("s20","Tamale","17:00","17:00",null,1), BusStop("s21","Kumasi","00:00","00:30","30m",2), BusStop("s22","Accra (Circle)","05:00","05:00",null,3)),
            20, "2025-04-15")
    )

    fun generateSeats(routeId: String): List<Seat> {
        val totalSeats = routes.find { it.id == routeId }?.bus?.totalSeats ?: 45
        val cols = 4; val rows = (totalSeats + cols - 1) / cols
        return (1..totalSeats).map { i ->
            val row = (i - 1) / cols + 1; val col = (i - 1) % cols
            val status = if (i % 7 == 0 || i % 11 == 0) SeatStatus.RESERVED else SeatStatus.AVAILABLE
            Seat("seat_${routeId}_$i", "$i", row, col, status, routes.find { it.id == routeId }?.price ?: 100.0)
        }
    }

    val currentUser = User("u1", "Kofi Mensah", "kofi@email.com", "+233 24 123 4567", null,
        listOf("r1", "r4"), listOf(
            PaymentMethod("pm1", PaymentType.MOBILE_MONEY_MTN, "MTN MoMo", "••• 4567", true),
            PaymentMethod("pm2", PaymentType.VISA, "Visa", "•••• 8901", false)
        ))

    val bookings = mutableListOf(
        Booking("bk1", "NXR-2025-001", routes[0], generateSeats("r1").filter { it.number == "12" || it.number == "13" },
            listOf(Passenger("Kofi Mensah", "+233241234567", "12"), Passenger("Ama Mensah", "+233241234568", "13")),
            BookingStatus.CONFIRMED, 240.0, "GHS", "MTN MoMo", "2025-04-10", "NXR-2025-001-CONF",
            BaggageInfo(2, 30.0)),
        Booking("bk2", "NXR-2025-002", routes[3], generateSeats("r4").filter { it.number == "5" },
            listOf(Passenger("Kofi Mensah", "+233241234567", "5")),
            BookingStatus.COMPLETED, 80.0, "GHS", "Visa •••• 8901", "2025-03-20", "NXR-2025-002-COMP"),
        Booking("bk3", "NXR-2025-003", routes[2], generateSeats("r3").filter { it.number == "8" },
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
