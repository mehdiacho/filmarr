package com.mehdiacho.filmarr.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository backing the mock phase. Cross-screen state (library, undo, snackbar,
 * streak) lives here as a singleton; screens read via collectAsStateWithLifecycle and mutate
 * through the action functions. Firestore drops in behind this same surface later.
 */
object FilmRepository {

    private val _library = MutableStateFlow(MockCatalog.library)
    val library: StateFlow<List<LibraryEntry>> = _library.asStateFlow()

    private val _snackbar = MutableStateFlow<SnackEvent?>(null)
    val snackbar: StateFlow<SnackEvent?> = _snackbar.asStateFlow()

    private val _streak = MutableStateFlow(MockCatalog.profileStats.streakDays)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val undoStack = ArrayDeque<List<LibraryEntry>>()

    val watched: List<LibraryEntry> get() = _library.value.filter { it.status == WatchStatus.WATCHED }
    val watchlist: List<LibraryEntry> get() = _library.value.filter { it.status == WatchStatus.WATCHLIST }

    fun statusOf(filmId: String): WatchStatus =
        _library.value.firstOrNull { it.film.id == filmId }?.status ?: WatchStatus.NONE

    fun isLogged(filmId: String): Boolean = statusOf(filmId) == WatchStatus.WATCHED

    private fun pushUndo() { undoStack.addLast(_library.value); if (undoStack.size > 10) undoStack.removeFirst() }

    private fun upsert(film: Film, transform: (LibraryEntry?) -> LibraryEntry) {
        pushUndo()
        val current = _library.value.toMutableList()
        val idx = current.indexOfFirst { it.film.id == film.id }
        val updated = transform(if (idx >= 0) current[idx] else null)
        if (idx >= 0) current[idx] = updated else current.add(0, updated)
        _library.value = current
    }

    fun logWatched(film: Film, rating: Float? = null, review: String? = null) {
        upsert(film) { existing ->
            (existing ?: LibraryEntry(film, WatchStatus.WATCHED)).copy(
                status = WatchStatus.WATCHED,
                userRating = rating ?: existing?.userRating,
                review = review ?: existing?.review,
                monthLabel = existing?.monthLabel ?: "THIS MONTH",
                loggedLabel = existing?.loggedLabel ?: "•",
            )
        }
        emit("Logged ${film.title}")
    }

    fun addToWatchlist(film: Film) {
        upsert(film) { existing -> (existing ?: LibraryEntry(film, WatchStatus.WATCHLIST)).copy(status = WatchStatus.WATCHLIST) }
        emit("Added ${film.title} to watchlist")
    }

    fun markWatching(film: Film) {
        upsert(film) { existing -> (existing ?: LibraryEntry(film, WatchStatus.WATCHING)).copy(status = WatchStatus.WATCHING) }
        emit("Marked ${film.title} as watching")
    }

    fun toggleFavorite(film: Film) {
        upsert(film) { existing ->
            (existing ?: LibraryEntry(film, WatchStatus.NONE)).let { it.copy(favorite = !it.favorite) }
        }
    }

    /** Discover swipe right = interested → watchlist. */
    fun markInterested(film: Film) = addToWatchlist(film)

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        _library.value = prev
        emit("Undone")
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    private fun emit(msg: String) { _snackbar.value = SnackEvent(msg, System.nanoTime()) }
    fun consumeSnackbar() { _snackbar.value = null }
}

data class SnackEvent(val message: String, val id: Long)
