package com.mehdiacho.filmarr.navigation

/** Route registry. Root tabs + pushed destinations (§5 nav map). */
object Dest {
    // Root tabs
    const val DISCOVER = "discover"
    const val BUILD = "build"
    const val LIBRARY = "library"
    const val PROFILE = "profile"

    // Entry / auth
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"

    // Pushed
    const val FILM_DETAIL = "film/{filmId}"
    const val SEARCH = "search"
    const val BLITZ = "blitz"
    const val TONIGHT = "tonight"
    const val ASK = "ask"
    const val OVERLAP = "overlap"
    const val TASTE_GRAPH = "taste_graph"
    const val LETTERBOXD = "letterboxd_import"
    const val SETTINGS = "settings"

    fun filmDetail(id: String) = "film/$id"

    val rootTabs = setOf(DISCOVER, BUILD, LIBRARY, PROFILE)
}
