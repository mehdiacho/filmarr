package com.mehdiacho.filmarr.data.recommender

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Backend surface. Auth is the signed-in user's Firebase ID token, attached as a Bearer header by
 * an OkHttp interceptor (see [RecommenderRepository]). `mode` is "discover" or "library" — the two
 * feeds, one backbone (GRAPH_RECOMMENDER.md §7).
 */
interface RecommenderService {
    @GET("recommend")
    suspend fun recommend(
        @Query("mode") mode: String,
        @Query("n") n: Int = 20,
        @Query("explain") explain: Boolean = false,
    ): RecommendResponse

    /**
     * Permanently delete the caller's account: all Firestore data under users/{uid} and the
     * Firebase Auth user. Runs server-side with Admin credentials, so it can clear the append-only
     * interaction log (create-only for clients) and needs no recent-login re-auth.
     */
    @DELETE("account")
    suspend fun deleteAccount(): Response<Unit>
}
