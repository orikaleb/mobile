package com.example.nexiride2.domain.model

data class BusStop(
    val id: String,
    val name: String,
    val arrivalTime: String,
    val departureTime: String,
    val layoverDuration: String? = null,
    val order: Int
)
