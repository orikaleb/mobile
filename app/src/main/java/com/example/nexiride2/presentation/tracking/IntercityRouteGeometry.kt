package com.example.nexiride2.presentation.tracking

import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Approximate intercity polylines for Ghana (N1 / coastal / north trunk).
 * Used when live tracking; falls back to Accra–Kumasi (N6) if the pair is unknown.
 */
data class TrackedIntercityRoute(
    val waypoints: List<LatLng>,
    val stopLabels: List<String>
)

private fun s(raw: String) = raw.lowercase(Locale.US).trim()

private fun isAccra(x: String) = x.contains("accra") || x.contains("tudu") || x.contains("circle") || x.contains("kaneshie")
private fun isKumasi(x: String) = x.contains("kumasi") || x.contains("kejetia") || x.contains("adum")
private fun isCape(x: String) = x.contains("cape")
private fun isTamale(x: String) = x.contains("tamale")
private fun isWinneba(x: String) = x.contains("winneba")

// Accra → Kumasi (N1 / N6) — same as previous hardcoded path
private val accraKumasiPoints = listOf(
    LatLng(5.6037, -0.1870),
    LatLng(5.6420, -0.2310),
    LatLng(5.7200, -0.3050),
    LatLng(5.8080, -0.3617),
    LatLng(5.9200, -0.4100),
    LatLng(6.0427, -0.4531),
    LatLng(6.1300, -0.5200),
    LatLng(6.1892, -0.5731),
    LatLng(6.2766, -0.6498),
    LatLng(6.3500, -0.7000),
    LatLng(6.5576, -0.7617),
    LatLng(6.6100, -0.9800),
    LatLng(6.6247, -1.2272),
    LatLng(6.6400, -1.4500),
    LatLng(6.6885, -1.6244)
)
private val accraKumasiStops = listOf(
    "Accra (Tudu)", "Achimota", "Ofankor", "Nsawam", "Aburi Hills", "Suhum", "Kukurantumi", "Apedwa",
    "Bunso Jct", "Anyinam", "Nkawkaw", "Juaso", "Konongo", "Obuasi Jct", "Kumasi (Kejetia)"
)

// Accra → Cape Coast (coastal trunk — approximate)
private val accraCapePoints = listOf(
    LatLng(5.6037, -0.1870),
    LatLng(5.5000, -0.3500),
    LatLng(5.3600, -0.6200), // Winneba area
    LatLng(5.2000, -0.9000),
    LatLng(5.1053, -1.2466)  // Cape Coast
)
private val accraCapeStops = listOf("Accra", "Kasoa corridor", "Winneba", "Mankesim", "Cape Coast")

// Accra → Tamale (rough inland / eastern corridor)
private val accraTamalePoints = listOf(
    LatLng(5.6037, -0.1870),
    LatLng(5.8000, -0.4000),
    LatLng(6.6800, -1.5500), // Kumasi belt
    LatLng(7.4000, -1.1000), // tech corridor
    LatLng(8.2000, -0.8000),
    LatLng(9.4000, -0.8500)  // Tamale
)
private val accraTamaleStops = listOf("Accra", "Eastern", "Kumasi area", "Yeji", "Kintampo", "Tamale")

// Kumasi → Tamale
private val kumasiTamalePoints = listOf(
    LatLng(6.6885, -1.6244),
    LatLng(7.2000, -1.3000),
    LatLng(8.2000, -0.9000),
    LatLng(9.4000, -0.8500)
)
private val kumasiTamaleStops = listOf("Kumasi (Kejetia)", "Techiman", "Kintampo", "Tamale")

object IntercityRouteGeometry {

    fun forBooking(origin: String, destination: String): TrackedIntercityRoute {
        val o = s(origin)
        val d = s(destination)
        if (o.isEmpty() && d.isEmpty()) return forward(accraKumasiPoints, accraKumasiStops)

        // Specific pairs
        return when {
            isAccra(o) && isKumasi(d) -> forward(accraKumasiPoints, accraKumasiStops)
            isKumasi(o) && isAccra(d) -> forward(accraKumasiPoints.reversed(), accraKumasiStops.reversed())

            isAccra(o) && isCape(d) -> forward(accraCapePoints, accraCapeStops)
            isCape(o) && isAccra(d) -> forward(accraCapePoints.reversed(), accraCapeStops.reversed())

            isAccra(o) && isTamale(d) -> forward(accraTamalePoints, accraTamaleStops)
            isTamale(o) && isAccra(d) -> forward(accraTamalePoints.reversed(), accraTamaleStops.reversed())

            isKumasi(o) && isTamale(d) -> forward(kumasiTamalePoints, kumasiTamaleStops)
            isTamale(o) && isKumasi(d) -> forward(
                kumasiTamalePoints.reversed(),
                kumasiTamaleStops.reversed()
            )

            // Winneba / smaller hops on coastal
            isAccra(o) && isWinneba(d) -> forward(
                accraCapePoints.take(3),
                accraCapeStops.take(3)
            )
            isWinneba(o) && isAccra(d) -> forward(
                accraCapePoints.take(3).reversed(),
                accraCapeStops.take(3).reversed()
            )

            else -> forward(accraKumasiPoints, accraKumasiStops)
        }
    }

    private fun forward(waypoints: List<LatLng>, labels: List<String>): TrackedIntercityRoute {
        val names = if (labels.size == waypoints.size) labels else
            waypoints.mapIndexed { i, _ -> labels.getOrElse(i) { "Stop ${i + 1}" } }
        return TrackedIntercityRoute(waypoints, names)
    }
}

/** Sum of segment lengths along a polyline (km). */
fun polylineLengthKm(waypoints: List<LatLng>): Int {
    if (waypoints.size < 2) return 0
    var sum = 0.0
    for (i in 0 until waypoints.size - 1) {
        sum += haversineKm(waypoints[i], waypoints[i + 1])
    }
    return sum.roundToInt().coerceAtLeast(1)
}
