package com.mehdiacho.filmarr.data

/**
 * Domain model. During the mock phase these carry placeholder art (gradientKey → PosterGradients);
 * once TMDB is wired, [posterPath]/[backdropPath] populate and the gradients become fallbacks.
 */
data class Film(
    val id: String,
    val title: String,
    val year: Int,
    val genres: List<String>,
    val rating: Double,          // TMDB vote average, 0..10
    val runtimeMin: Int,
    val overview: String,
    val cast: List<String> = emptyList(),
    val posterPath: String? = null,
    val backdropPath: String? = null,
) {
    val gradientKey: String get() = id
    val genreLine: String get() = genres.take(2).joinToString(" · ")
}

enum class WatchStatus { NONE, WATCHED, WATCHING, WATCHLIST }

/** A film in the user's library with their personal signal. */
data class LibraryEntry(
    val film: Film,
    val status: WatchStatus,
    val userRating: Float? = null,   // 0.5..5.0 in half steps
    val review: String? = null,
    val favorite: Boolean = false,
    val loggedLabel: String? = null, // e.g. "14" within a month section (mock)
    val monthLabel: String? = null,  // e.g. "JULY 2026"
)

/** Build-feed candidate — a film we suspect the user has already seen. */
data class BuildCandidate(
    val film: Film,
    val confidence: String,   // "Likely seen"
    val reason: String,       // "War · Drama · matched your 2018 era"
)

data class Genre(val name: String, val selected: Boolean = false)

/** Taste-graph node for the recommender visualisation (§3 Taste graph, §3 Overlap). */
data class GraphNode(
    val label: String,
    val kind: NodeKind,
)

enum class NodeKind { YOU, LIKED, CLUSTER, RECO, FRIEND }

/** One AI conversation turn (§2.10 / Ask filmarr). */
data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val filmResults: List<Film> = emptyList(),
)

/** Profile stats (§3 Profile). */
data class ProfileStats(
    val filmsLogged: Int,
    val reviews: Int,
    val avgRating: Float,
    val ratingHistogram: List<Int>, // 6 buckets
    val topGenres: List<Pair<String, Float>>, // name → 0..1 share
    val streakDays: Int,
)
