package com.mehdiacho.filmarr.data.recommender

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response shape of the backend `GET /recommend` (see backend/app/main.py). */
@JsonClass(generateAdapter = true)
data class RecommendResponse(
    val mode: String = "discover",
    @Json(name = "config_fingerprint") val configFingerprint: String? = null,
    val items: List<RecItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RecItem(
    @Json(name = "movie_id") val movieId: Long,
    val score: Double = 0.0,
    val rank: Int = 0,
    /** Provenance ("because you liked X"), populated only when explain=true. */
    val why: List<RecWhy> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RecWhy(
    val via: Long = 0,
    val edge: String = "",
    val weight: Double = 0.0,
)
