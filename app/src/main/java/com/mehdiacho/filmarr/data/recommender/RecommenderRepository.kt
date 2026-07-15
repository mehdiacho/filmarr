package com.mehdiacho.filmarr.data.recommender

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.mehdiacho.filmarr.BuildConfig
import com.mehdiacho.filmarr.data.Film
import com.mehdiacho.filmarr.data.tmdb.TmdbRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Client for the Cloud Run `/recommend` service. Feeds the discover / build decks when a backend
 * URL is configured (BuildConfig.RECOMMENDER_BASE_URL, from .env); otherwise a no-op so callers
 * fall back to TMDB. Every request carries the current Firebase ID token; the backend verifies it
 * and assembles the user's taste state from Firestore server-side.
 *
 * The endpoint returns TMDB movie ids; we resolve each to a [Film] via [TmdbRepository] so the
 * existing card UI renders unchanged.
 */
object RecommenderRepository {

    /** True once a deployed backend URL is set — until then the app runs on TMDB feeds. */
    val isConfigured: Boolean get() = BuildConfig.RECOMMENDER_BASE_URL.isNotBlank()

    private val service: RecommenderService? by lazy { if (isConfigured) buildService() else null }

    private fun buildService(): RecommenderService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                idToken()?.let { req.addHeader("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val base = BuildConfig.RECOMMENDER_BASE_URL.trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RecommenderService::class.java)
    }

    /** Blocking fetch of a fresh, non-forced ID token (runs on OkHttp's background thread). */
    private fun idToken(): String? = runCatching {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    }.getOrNull()

    /**
     * Recommendations for [mode] ("discover" | "library") as resolved [Film]s, in ranked order.
     * Returns empty (so the caller falls back to TMDB) when unconfigured, on any error, or when
     * the user has no seed history yet (cold start).
     */
    suspend fun recommend(mode: String, n: Int = 20): List<Film> {
        val svc = service ?: return emptyList()
        val resp = runCatching {
            withContext(Dispatchers.IO) { svc.recommend(mode = mode, n = n) }
        }.getOrNull() ?: return emptyList()

        return coroutineScope {
            resp.items
                .map { item -> async { TmdbRepository.detail(item.movieId.toString()) } }
                .awaitAll()
                .filterNotNull()
        }
    }

    /**
     * Permanently delete the signed-in user's account and all their data (server-side, via the
     * backend's Admin credentials). The caller is responsible for signing out locally on success.
     * Fails if no backend is configured — deletion must be total, so we never silently no-op.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val svc = service ?: return Result.failure(
            IllegalStateException("Account deletion needs the backend, which isn't configured."),
        )
        return runCatching {
            withContext(Dispatchers.IO) {
                val resp = svc.deleteAccount()
                if (!resp.isSuccessful) error("Server returned ${resp.code()}")
            }
        }
    }
}
