package com.mehdiacho.filmarr.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Append-only swipe log under `users/{uid}/interactions/{autoId}` plus the hidden
 * `users/{uid}/hearted/{movieId}` list. This is the recommender's training substrate
 * (GRAPH_RECOMMENDER.md §8, §11.1): every event is a labeled, timestamped fact the
 * backend's `services/state.py` reads to seed propagation.
 *
 * Doc shape matches what the backend expects and what firestore.rules enforces:
 *   { movie_id: int, event_type: string, ts: timestamp, rating?: number,
 *     user_age_bracket_at_event: string }
 * The rules allow `create` only (never update/delete), so history stays clean — this
 * class only ever adds documents.
 */
class FirestoreInteractionLog(uid: String) {

    private val db = FirebaseFirestore.getInstance()
    private val interactions = db.collection("users").document(uid).collection("interactions")
    private val hearted = db.collection("users").document(uid).collection("hearted")

    /** Append one swipe/gesture event. [rating] only for RATED (focus-mode 5-star). */
    fun log(movieId: Long, eventType: String, rating: Float? = null, ageBracket: String = AgeBracket.UNKNOWN) {
        val data = hashMapOf<String, Any>(
            "movie_id" to movieId,
            "event_type" to eventType,
            "ts" to FieldValue.serverTimestamp(),
            "user_age_bracket_at_event" to ageBracket,
        )
        if (rating != null) data["rating"] = rating.toDouble()
        interactions.add(data)
    }

    fun heart(movieId: Long) {
        hearted.document(movieId.toString()).set(mapOf("ts" to FieldValue.serverTimestamp()))
    }

    fun unheart(movieId: Long) {
        hearted.document(movieId.toString()).delete()
    }
}

/** Event vocabulary — must stay in lockstep with base.py `InteractionType` values. */
object InteractionEvent {
    const val WATCHED = "watched"
    const val NOT_WATCHED = "not_watched"
    const val WATCHING = "watching"
    const val PLAN_TO_WATCH = "plan_to_watch"
    const val INTERESTED = "interested"
    const val ON_THE_FENCE = "on_the_fence"
    const val HEARTED = "hearted"
    const val RATED = "rated"
}

/** Coarse life-stage brackets — must stay in lockstep with base.py `AgeBracket` values. */
object AgeBracket {
    const val TEEN = "13_17"
    const val YOUNG_ADULT = "18_24"
    const val ADULT = "25_34"
    const val MID = "35_44"
    const val MATURE = "45_54"
    const val SENIOR = "55_plus"
    const val UNKNOWN = "unknown"
}
