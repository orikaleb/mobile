package com.example.nexiride2.presentation.tracking

import com.google.android.gms.maps.model.LatLng
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance between two WGS84 points (km). */
fun haversineKm(a: LatLng, b: LatLng): Double {
    val earthKm = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    return earthKm * c
}
