package com.example.nexiride2.domain.model

data class Route(
    val id: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val price: Double,
    val currency: String = "GHS",
    val bus: Bus,
    val stops: List<BusStop> = emptyList(),
    val availableSeats: Int,
    val date: String
)
