package com.example.nexiride2.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val profilePhotoUrl: String? = null,
    val savedRouteIds: List<String> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList()
)
