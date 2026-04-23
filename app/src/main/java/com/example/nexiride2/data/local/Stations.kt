package com.example.nexiride2.data.local

/**
 * Canonical boarding / drop-off station name per supported city.
 * Used to seed route stops with real-world terminal names and to enrich
 * the ticket UI with the actual station a passenger should show up at.
 *
 * If a city isn't listed we fall back to the raw city name so the UI
 * never renders an empty "—" column.
 */
object Stations {
    private val cityToStation: Map<String, String> = mapOf(
        "Accra"       to "Circle VIP Terminal",
        "Kumasi"      to "Kejetia Main Station",
        "Tamale"      to "Aboabo Transport Yard",
        "Cape Coast"  to "Kotokuraba Station",
        "Takoradi"    to "Market Circle Station",
        "Sunyani"     to "Sunyani Central Station",
        "Ho"          to "Ho Civic Centre Terminal",
        "Koforidua"   to "Koforidua New Juabeng Station",
        "Bolgatanga"  to "Bolga Main Station",
        "Wa"          to "Wa Central Bus Yard",
        "Tema"        to "Community 1 Main Terminal",
        "Sekondi"     to "Sekondi Station",
        "Obuasi"      to "Obuasi Central Station",
        "Nkawkaw"     to "Nkawkaw Lorry Station",
        "Berekum"     to "Berekum Main Terminal",
        "Dambai"      to "Dambai Ferry Terminal",
        "Yendi"       to "Yendi Central Station",
        "Hohoe"       to "Hohoe Central Terminal",
        "Akosombo"    to "Akosombo Tourist Terminal",
        "Prestea"     to "Prestea Station"
    )

    /** Returns the station name for [city], or [city] itself as a safe fallback. */
    fun forCity(city: String): String =
        cityToStation[city.trim()] ?: city
}
