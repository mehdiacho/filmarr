package com.mehdiacho.filmarr.data.recommender

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Backend recommender surface. Auth is the signed-in user's Firebase ID token, attached as a
 * Bearer header by an OkHttp interceptor (see [RecommenderRepository]). `mode` is "discover"
 * or "library" — the two feeds, one backbone (GRAPH_RECOMMENDER.md §7).
 */
interface RecommenderService {
    @GET("recommend")
    suspend fun recommend(
        @Query("mode") mode: String,
        @Query("n") n: Int = 20,
        @Query("explain") explain: Boolean = false,
    ): RecommendResponse
}
