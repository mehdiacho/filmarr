package com.mehdiacho.filmarr.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Writes the `users/{uid}` profile doc (GRAPH_RECOMMENDER.md §11.1). Onboarding uses this to
 * persist the cold-start signal: chosen interest tags, the optional age bracket, and a marker
 * that onboarding is complete (so returning users skip it). Merge-writes only — never clobbers
 * server-managed fields.
 */
class FirestoreUserProfile(uid: String) {

    private val doc = FirebaseFirestore.getInstance().collection("users").document(uid)

    fun completeOnboarding(interestTags: List<String>, ageBracket: String?) {
        val data = hashMapOf<String, Any>(
            "interest_tags" to interestTags,
            "onboarded_at" to FieldValue.serverTimestamp(),
        )
        if (ageBracket != null && ageBracket != AgeBracket.UNKNOWN) {
            data["current_age_bracket"] = ageBracket
        }
        doc.set(data, SetOptions.merge())
    }
}
