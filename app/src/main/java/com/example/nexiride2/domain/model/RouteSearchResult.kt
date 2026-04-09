package com.example.nexiride2.domain.model

data class RouteSearchResult(
    val routes: List<Route>,
    val fromCache: Boolean
)
