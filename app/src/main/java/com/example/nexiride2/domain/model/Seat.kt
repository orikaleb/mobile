package com.example.nexiride2.domain.model

data class Seat(
    val id: String,
    val number: String,
    val row: Int,
    val column: Int,
    val status: SeatStatus,
    val price: Double = 0.0,
    val type: SeatType = SeatType.STANDARD
)

enum class SeatStatus { AVAILABLE, RESERVED, SELECTED }
enum class SeatType { STANDARD, VIP, WINDOW, AISLE }
