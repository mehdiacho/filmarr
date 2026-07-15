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

    /** Firestore mirror — non-null while a user is signed in. */
    private var store: FirestoreLibraryStore? = null

    /** Append-only interaction log + hidden hearted list (the recommender's training substrate). */
    private var interactionLog: FirestoreInteractionLog? = null

    /** Profile writer for onboarding (interest tags, age bracket, onboarded marker). */
    private var profile: FirestoreUserProfile? = null

    /** Attach the signed-in user's cloud library; the snapshot listener becomes the source of truth. */
    fun bind(uid: String) {
        store?.stop()
        undoStack.clear()
        _library.value = emptyList()
        val s = FirestoreLibraryStore(uid)
        store = s
        interactionLog = FirestoreInteractionLog(uid)
        profile = FirestoreUserProfile(uid)
        s.listen { entries -> _library.value = entries }
    }

    /** Detach on sign-out; fall back to the local preview catalog. */
    fun unbind() {
        store?.stop()
        store = null
        interactionLog = null
        profile = null
        undoStack.clear()
        _library.value = MockCatalog.library
    }

    /**
     * Persist onboarding: interest tags + optional age bracket to the profile, and each chosen
     * "loved" film as an INTERESTED seed interaction — the cold-start seed set the recommender
     * propagates from on a brand-new account (GRAPH_RECOMMENDER.md §10).
     */
    fun completeOnboarding(genres: List<String>, lovedFilms: List<Film>, ageBracket: String? = null) {
        profile?.completeOnboarding(genres, ageBracket)
        lovedFilms.forEach { logInteraction(it, InteractionEvent.INTERESTED) }
    }

    /**
     * Append a swipe/gesture event to the interaction log. The caller resolves the
     * feed-dependent meaning (right = INTERESTED in discover vs WATCHED in build) before
     * this point, per base.py. No-op for mock films whose id isn't a TMDB numeric id.
     */
    fun logInteraction(film: Film, eventType: String, rating: Float? = null) {
        val id = film.id.toLongOrNull() ?: return
        interactionLog?.log(id, eventType, rating)
    }

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
        store?.put(updated)   // write-through; snapshot listener echoes it back
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
        val nowFavorite = !(_library.value.firstOrNull { it.film.id == film.id }?.favorite ?: false)
        upsert(film) { existing ->
            (existing ?: LibraryEntry(film, WatchStatus.NONE)).copy(favorite = nowFavorite)
        }
        // Double-tap "heart" — mirror into the hidden hearted list + log the event (§7).
        film.id.toLongOrNull()?.let { id ->
            if (nowFavorite) {
                interactionLog?.heart(id)
                interactionLog?.log(id, InteractionEvent.HEARTED)
            } else {
                interactionLog?.unheart(id)
            }
        }
    }

    /** Discover swipe right = interested → watchlist. */
    fun markInterested(film: Film) = addToWatchlist(film)

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        val current = _library.value
        _library.value = prev
        store?.let { s ->
            val removed = current.map { it.film.id }.toSet() - prev.map { it.film.id }.toSet()
            removed.forEach { s.delete(it) }
            s.reconcile(prev)
        }
        emit("Undone")
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    private fun emit(msg: String) { _snackbar.value = SnackEvent(msg, System.nanoTime()) }
    fun consumeSnackbar() { _snackbar.value = null }
}

data class SnackEvent(val message: String, val id: Long)
