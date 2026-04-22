package com.example.nexiride2.domain.model

data class Bus(
    val id: String,
    val companyName: String,
    val companyLogoUrl: String? = null,
    val busType: String,
    /** Fleet / registration (e.g. GR 1234-22). May be null for older stored routes. */
    val busNumber: String? = null,
    val totalSeats: Int,
    val amenities: List<String> = emptyList(),
    val rating: Float = 0f
)
