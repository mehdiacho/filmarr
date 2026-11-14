package com.mehdiacho.filmarr.data.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Paged list response shared by popular / top-rated / trending / search endpoints. */
@JsonClass(generateAdapter = true)
data class TmdbPage(
    val page: Int = 1,
    val results: List<TmdbMovie> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 1,
)

/** A movie as it appears in list endpoints (no runtime / named genres). */
@JsonClass(generateAdapter = true)
data class TmdbMovie(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
)

/** Full movie detail (adds runtime + named genres + credits when appended). */
@JsonClass(generateAdapter = true)
data class TmdbMovieDetail(
    val id: Int,
    val title: String? = null,
    val overview: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
)

@JsonClass(generateAdapter = true)
data class TmdbCredits(val cast: List<TmdbCastMember> = emptyList())

@JsonClass(generateAdapter = true)
data class TmdbCastMember(val name: String? = null)

@JsonClass(generateAdapter = true)
data class TmdbGenre(val id: Int, val name: String)

@JsonClass(generateAdapter = true)
data class TmdbGenreList(val genres: List<TmdbGenre> = emptyList())
