package com.mehdiacho.filmarr.data.tmdb

import com.mehdiacho.filmarr.BuildConfig
import com.mehdiacho.filmarr.data.Film
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap

/** TMDB image CDN — path fragments from the API become full URLs here. */
object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"
    fun poster(path: String?, size: String = "w500"): String? = path?.let { "$BASE$size$it" }
    fun backdrop(path: String?, size: String = "w780"): String? = path?.let { "$BASE$size$it" }
}

/** Full poster/backdrop URLs for a [Film], or null while art is unknown. */
fun Film.posterUrl(size: String = "w500"): String? = TmdbImages.poster(posterPath, size)
fun Film.backdropUrl(size: String = "w780"): String? = TmdbImages.backdrop(backdropPath, size)

/**
 * Single entry point for TMDB data. Owns the Retrofit client, a lazily-loaded genre id→name map,
 * and an in-memory [Film] cache so any screen can resolve a film by id (see [cached]) without
 * refetching. Every film this repository emits is cached.
 */
object TmdbRepository {

    private val service: TmdbService by lazy { buildService() }
    private val cache = ConcurrentHashMap<String, Film>()

    @Volatile private var genreMap: Map<Int, String> = emptyMap()
    private val genreMutex = Mutex()

    private fun buildService(): TmdbService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_READ_TOKEN}")
                    .addHeader("accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbService::class.java)
    }

    /** A previously-fetched film, if we've seen it this session. */
    fun cached(id: String): Film? = cache[id]

    private suspend fun ensureGenres() {
        if (genreMap.isNotEmpty()) return
        genreMutex.withLock {
            if (genreMap.isNotEmpty()) return
            genreMap = runCatching { service.genres().genres.associate { it.id to it.name } }
                .getOrDefault(emptyMap())
        }
    }

    private fun TmdbMovie.toFilm(): Film {
        val film = Film(
            id = id.toString(),
            title = title ?: name ?: "Untitled",
            year = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
            genres = genreIds.mapNotNull { genreMap[it] },
            rating = voteAverage,
            runtimeMin = 0,
            overview = overview.orEmpty(),
            posterPath = posterPath,
            backdropPath = backdropPath,
        )
        cache[film.id] = film
        return film
    }

    private fun TmdbMovieDetail.toFilm(): Film {
        val film = Film(
            id = id.toString(),
            title = title ?: "Untitled",
            year = releaseDate?.take(4)?.toIntOrNull() ?: 0,
            genres = genres.map { it.name },
            rating = voteAverage,
            runtimeMin = runtime ?: 0,
            overview = overview.orEmpty(),
            cast = credits?.cast?.mapNotNull { it.name }?.take(6) ?: emptyList(),
            posterPath = posterPath,
            backdropPath = backdropPath,
        )
        cache[film.id] = film
        return film
    }

    private suspend fun fetchPage(fetch: suspend () -> TmdbPage): List<Film> = withContext(Dispatchers.IO) {
        ensureGenres()
        runCatching { fetch().results.filter { it.posterPath != null }.map { it.toFilm() } }
            .getOrDefault(emptyList())
    }

    /** Discover deck — "stuff you'd like". */
    suspend fun discover(page: Int = 1): List<Film> = fetchPage { service.trending(page) }

    /** Build feed — popular titles the user has probably already seen. */
    suspend fun buildFeed(page: Int = 1): List<Film> = fetchPage { service.popular(page) }

    /** Backlog blitz — critically acclaimed, likely-seen titles. */
    suspend fun topRated(page: Int = 1): List<Film> = fetchPage { service.topRated(page) }

    suspend fun search(query: String): List<Film> {
        if (query.isBlank()) return emptyList()
        return fetchPage { service.search(query) }
    }

    suspend fun recommendations(id: String): List<Film> {
        val numeric = id.toIntOrNull() ?: return emptyList()
        return fetchPage { service.recommendations(numeric) }
    }

    /** Full detail (runtime, named genres, cast). Falls back to the cached list-item film on error. */
    suspend fun detail(id: String): Film? = withContext(Dispatchers.IO) {
        val numeric = id.toIntOrNull() ?: return@withContext cached(id)
        ensureGenres()
        runCatching { service.detail(numeric).toFilm() }.getOrNull() ?: cached(id)
    }
}
