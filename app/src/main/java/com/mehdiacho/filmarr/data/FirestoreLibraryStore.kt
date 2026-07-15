package com.mehdiacho.filmarr.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Persists a user's library under `users/{uid}/library/{filmId}`. The film is denormalized into
 * each doc so the library renders without a TMDB round-trip. A snapshot listener streams changes
 * (including this device's own optimistic writes) back into [FilmRepository].
 */
class FirestoreLibraryStore(uid: String) {

    private val col = FirebaseFirestore.getInstance()
        .collection("users").document(uid).collection("library")

    private var registration: ListenerRegistration? = null

    fun listen(onEntries: (List<LibraryEntry>) -> Unit) {
        registration?.remove()
        registration = col.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            onEntries(snapshot.documents.mapNotNull { it.toEntry() })
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    fun put(entry: LibraryEntry) {
        col.document(entry.film.id).set(entry.toMap())
    }

    fun delete(filmId: String) {
        col.document(filmId).delete()
    }

    /** Replace the whole remote set with [target] — used to mirror an undo. */
    fun reconcile(target: List<LibraryEntry>) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        target.forEach { batch.set(col.document(it.film.id), it.toMap()) }
        batch.commit()
    }
}

private fun LibraryEntry.toMap(): Map<String, Any?> = mapOf(
    "filmId" to film.id,
    "title" to film.title,
    "year" to film.year,
    "genres" to film.genres,
    "rating" to film.rating,
    "runtimeMin" to film.runtimeMin,
    "overview" to film.overview,
    "cast" to film.cast,
    "posterPath" to film.posterPath,
    "backdropPath" to film.backdropPath,
    "status" to status.name,
    "userRating" to userRating?.toDouble(),
    "review" to review,
    "favorite" to favorite,
    "loggedLabel" to loggedLabel,
    "monthLabel" to monthLabel,
    "updatedAt" to FieldValue.serverTimestamp(),
)

private fun DocumentSnapshot.toEntry(): LibraryEntry? {
    val id = getString("filmId") ?: id
    val title = getString("title") ?: return null
    @Suppress("UNCHECKED_CAST")
    val genres = (get("genres") as? List<String>) ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val cast = (get("cast") as? List<String>) ?: emptyList()
    val film = Film(
        id = id,
        title = title,
        year = (getLong("year") ?: 0L).toInt(),
        genres = genres,
        rating = getDouble("rating") ?: 0.0,
        runtimeMin = (getLong("runtimeMin") ?: 0L).toInt(),
        overview = getString("overview").orEmpty(),
        cast = cast,
        posterPath = getString("posterPath"),
        backdropPath = getString("backdropPath"),
    )
    val status = getString("status")?.let { runCatching { WatchStatus.valueOf(it) }.getOrNull() } ?: WatchStatus.NONE
    return LibraryEntry(
        film = film,
        status = status,
        userRating = getDouble("userRating")?.toFloat(),
        review = getString("review"),
        favorite = getBoolean("favorite") ?: false,
        loggedLabel = getString("loggedLabel"),
        monthLabel = getString("monthLabel"),
    )
}
