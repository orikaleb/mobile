package com.example.nexiride2.presentation.search

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Approximate lat/lng of the Ghanaian cities used throughout the app.
 * Used to resolve the user's GPS fix to the closest known origin city
 * and to measure how plausible that match is.
 */
object CityCoordinates {
    /** Furthest a GPS fix can be from a supported city before we refuse to auto-fill. */
    const val MAX_MATCH_DISTANCE_KM = 75.0

    /** Rough bounding box of Ghana — used to reject fixes that aren't in the country. */
    private const val GHANA_MIN_LAT = 4.5
    private const val GHANA_MAX_LAT = 11.5
    private const val GHANA_MIN_LNG = -3.5
    private const val GHANA_MAX_LNG = 1.5

    private val map = mapOf(
        "Accra" to (5.6037 to -0.1870),
        "Kumasi" to (6.6885 to -1.6244),
        "Tamale" to (9.4008 to -0.8393),
        "Cape Coast" to (5.1054 to -1.2466),
        "Takoradi" to (4.8845 to -1.7554),
        "Sunyani" to (7.3349 to -2.3123),
        "Ho" to (6.6000 to 0.4713),
        "Koforidua" to (6.0940 to -0.2571),
        "Bolgatanga" to (10.7856 to -0.8513),
        "Wa" to (10.0601 to -2.5099),
        "Tema" to (5.6698 to -0.0166),
        "Sekondi" to (4.9344 to -1.7049),
        "Obuasi" to (6.2027 to -1.6663),
        "Nkawkaw" to (6.5500 to -0.7667),
        "Berekum" to (7.4544 to -2.5836),
        "Dambai" to (8.0667 to 0.1833),
        "Yendi" to (9.4427 to -0.0104),
        "Hohoe" to (7.1500 to 0.4667),
        "Akosombo" to (6.2667 to 0.0500),
        "Prestea" to (5.4333 to -2.1500)
    )

    /** All supported cities (useful for substring matches against a Geocoder result). */
    val names: Set<String> get() = map.keys

    /** True if [lat]/[lng] is inside Ghana's rough bounding box. */
    fun isInGhana(lat: Double, lng: Double): Boolean =
        lat in GHANA_MIN_LAT..GHANA_MAX_LAT && lng in GHANA_MIN_LNG..GHANA_MAX_LNG

    data class NearestMatch(val city: String, val distanceKm: Double)

    /** Returns the closest supported city and the distance in km. Null if unknown. */
    fun nearestWithDistance(lat: Double, lng: Double): NearestMatch? =
        map.entries.minByOrNull { (_, coord) ->
            val (cLat, cLng) = coord
            haversineKm(lat, lng, cLat, cLng)
        }?.let { (name, coord) ->
            val (cLat, cLng) = coord
            NearestMatch(name, haversineKm(lat, lng, cLat, cLng))
        }

    /** Case-insensitive exact match to a supported city. */
    fun matchExact(candidate: String?): String? {
        if (candidate.isNullOrBlank()) return null
        return map.keys.firstOrNull { it.equals(candidate.trim(), ignoreCase = true) }
    }

    /** Substring match — e.g. "Greater Accra Region" → "Accra". */
    fun matchContains(candidate: String?): String? {
        if (candidate.isNullOrBlank()) return null
        val c = candidate.lowercase()
        return map.keys.firstOrNull { c.contains(it.lowercase()) }
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
