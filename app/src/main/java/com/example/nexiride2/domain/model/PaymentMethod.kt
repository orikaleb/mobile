package com.example.nexiride2.domain.model

data class PaymentMethod(
    val id: String,
    val type: PaymentType,
    val name: String,
    val details: String,
    val isDefault: Boolean = false
)

enum class PaymentType {
    MOBILE_MONEY_MTN, MOBILE_MONEY_VODAFONE, MOBILE_MONEY_AIRTELTIGO,
    VISA, MASTERCARD, CASH_AT_STATION
}
