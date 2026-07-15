package com.mehdiacho.filmarr.data.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** TMDB v3 REST surface. Auth is a v4 bearer token attached by an OkHttp interceptor. */
interface TmdbService {
    @GET("trending/movie/week")
    suspend fun trending(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/popular")
    suspend fun popular(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/top_rated")
    suspend fun topRated(@Query("page") page: Int = 1): TmdbPage

    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): TmdbPage

    @GET("movie/{id}")
    suspend fun detail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "credits",
    ): TmdbMovieDetail

    @GET("movie/{id}/recommendations")
    suspend fun recommendations(@Path("id") id: Int, @Query("page") page: Int = 1): TmdbPage

    @GET("genre/movie/list")
    suspend fun genres(): TmdbGenreList
}
