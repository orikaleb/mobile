package com.example.nexiride2.domain.model

data class Bus(
    val id: String,
    val companyName: String,
    val companyLogoUrl: String? = null,
    val busType: String,
    val totalSeats: Int,
    val amenities: List<String> = emptyList(),
    val rating: Float = 0f
)
