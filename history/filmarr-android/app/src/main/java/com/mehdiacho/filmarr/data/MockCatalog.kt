package com.mehdiacho.filmarr.data

/**
 * Placeholder catalog so every screen is populated before TMDB is wired.
 * Titles/gradients mirror the design kit (§1.2) plus invented extras.
 */
object MockCatalog {

    val films: List<Film> = listOf(
        Film("neon-harbor", "Neon Harbor", 2023, listOf("Sci-Fi", "Thriller"), 7.8, 128,
            "A dock worker in a flooded megacity smuggles memories for the people who can no longer afford their own.",
            cast = listOf("M. Voss", "L. Reyes", "D. Okafor")),
        Film("saltwater", "Saltwater", 2021, listOf("Drama", "Romance"), 7.2, 112,
            "Two estranged siblings return to the coast to sell their late mother's house and find she left them more than property.",
            cast = listOf("A. Bello", "K. Sun")),
        Film("ember-ash", "Ember & Ash", 2018, listOf("War", "Drama"), 8.0, 141,
            "A field nurse and a deserter cross a burning countryside, keeping each other honest about what survival costs.",
            cast = listOf("R. Haas", "T. Milo")),
        Film("velvet-static", "Velvet Static", 2024, listOf("Mystery"), 8.4, 118,
            "A radio engineer in a coastal town starts hearing a voice between stations — one that knows things it shouldn't. A slow-burn mystery about static, memory, and the people we tune out.",
            cast = listOf("M. Voss", "L. Reyes", "D. Okafor")),
        Film("quiet-year", "The Quiet Year", 2019, listOf("Drama", "Indie"), 7.6, 98,
            "A commune records one calendar year of small decisions before the first frost changes everything.",
            cast = listOf("N. Park")),
        Film("low-tide", "Low Tide", 2022, listOf("Thriller", "Crime"), 7.1, 106,
            "Four teenagers find money buried on the beach and spend a summer learning who they'll become to keep it.",
            cast = listOf("J. Cole", "S. Amin")),
        Film("paper-moths", "Paper Moths", 2020, listOf("Animation", "Fantasy"), 7.9, 92,
            "A bookbinder's apprentice folds paper creatures that come alive only in the margins of other people's stories.",
            cast = listOf("Voice: E. Frost")),
        Film("north-signal", "North Signal", 2017, listOf("Sci-Fi", "Drama"), 7.4, 133,
            "The last operator of an arctic listening post decides whether to answer a transmission that took forty years to arrive.",
            cast = listOf("H. Gray")),
        Film("cinder-lane", "Cinder Lane", 2016, listOf("Horror", "Mystery"), 6.9, 101,
            "A street that only appears on old maps pulls a cartographer back to the town that raised her.",
            cast = listOf("P. Dunne")),
        Film("gold-static", "Gold Static", 2015, listOf("Crime", "Noir"), 7.7, 119,
            "A fixer for a fading film studio has one week to make a scandal disappear, and one conscience left to lose.",
            cast = listOf("V. Marsh")),
        Film("blue-hour", "Blue Hour", 2023, listOf("Romance", "Drama"), 7.3, 104,
            "Two night-shift workers keep meeting in the ten blue minutes before dawn and pretend it's a coincidence.",
            cast = listOf("C. Idris", "F. Lin")),
        Film("dry-season", "Dry Season", 2018, listOf("Drama"), 7.5, 127,
            "A well-digger travels a drought-struck region trading water stories for shelter.",
            cast = listOf("O. Sane")),
    )

    fun byId(id: String): Film = films.first { it.id == id }

    /** Discover deck — "stuff you'd like". */
    val discoverDeck: List<Film> = listOf(
        byId("neon-harbor"), byId("saltwater"), byId("north-signal"),
        byId("paper-moths"), byId("blue-hour"), byId("velvet-static"),
    )

    /** Build feed — "stuff you've probably already seen". */
    val buildFeed: List<BuildCandidate> = listOf(
        BuildCandidate(byId("ember-ash"), "Likely seen · 2018", "War · Drama · matched your 2018 era"),
        BuildCandidate(byId("gold-static"), "Likely seen · 2015", "Crime · Noir · you rate this era highly"),
        BuildCandidate(byId("cinder-lane"), "Likely seen · 2016", "Horror · Mystery · popular that year"),
        BuildCandidate(byId("dry-season"), "Likely seen · 2018", "Drama · matched your 2018 era"),
    )

    /** Diary / library seed. */
    val library: List<LibraryEntry> = listOf(
        LibraryEntry(byId("velvet-static"), WatchStatus.WATCHED, 4.5f,
            "A slow, gorgeous heartbreak. The harbor scene alone…", favorite = true,
            loggedLabel = "14", monthLabel = "JULY 2026"),
        LibraryEntry(byId("low-tide"), WatchStatus.WATCHED, 3.5f,
            "Tense little thing. Stuck the landing.", loggedLabel = "09", monthLabel = "JULY 2026"),
        LibraryEntry(byId("quiet-year"), WatchStatus.WATCHED, 4.0f, null,
            loggedLabel = "02", monthLabel = "JULY 2026"),
        LibraryEntry(byId("gold-static"), WatchStatus.WATCHED, 5.0f,
            "Perfect noir. Marsh has never been better.", favorite = true,
            loggedLabel = "28", monthLabel = "JUNE 2026"),
        LibraryEntry(byId("dry-season"), WatchStatus.WATCHED, 3.0f, null,
            loggedLabel = "21", monthLabel = "JUNE 2026"),
        LibraryEntry(byId("ember-ash"), WatchStatus.WATCHED, 4.5f, null,
            loggedLabel = "12", monthLabel = "JUNE 2026"),
        LibraryEntry(byId("north-signal"), WatchStatus.WATCHLIST),
        LibraryEntry(byId("blue-hour"), WatchStatus.WATCHLIST),
        LibraryEntry(byId("paper-moths"), WatchStatus.WATCHING),
    )

    /** Backlog Blitz — popular films you likely saw. */
    val blitz: List<Film> = listOf(
        byId("ember-ash"), byId("gold-static"), byId("cinder-lane"), byId("dry-season"),
        byId("north-signal"), byId("low-tide"),
    )

    val onboardingGenres: List<Genre> = listOf(
        Genre("Sci-Fi", true), Genre("Drama"), Genre("Thriller", true), Genre("Horror"),
        Genre("Comedy"), Genre("Indie", true), Genre("Animation"), Genre("Mystery", true),
        Genre("Romance"), Genre("A24", true), Genre("Noir"), Genre("Crime"),
    )

    val profileStats = ProfileStats(
        filmsLogged = 142, reviews = 38, avgRating = 3.8f,
        ratingHistogram = listOf(3, 6, 10, 22, 34, 18),
        topGenres = listOf("Drama" to 1.0f, "Thriller" to 0.72f, "Sci-Fi" to 0.61f, "Mystery" to 0.48f, "Crime" to 0.33f),
        streakDays = 6,
    )

    val tasteGraph: List<GraphNode> = listOf(
        GraphNode("A24 core", NodeKind.CLUSTER),
        GraphNode("Slow-burn mystery", NodeKind.CLUSTER),
        GraphNode("90s sci-fi", NodeKind.CLUSTER),
        GraphNode("Velvet Static", NodeKind.LIKED),
        GraphNode("Neon Harbor", NodeKind.RECO),
    )

    val askSuggestions = listOf(
        "That submarine thriller from the 90s",
        "Something like Velvet Static but funnier",
        "A quiet film for a rainy Sunday",
    )

    val moods = listOf("Cozy", "Tense", "Weird", "Big & loud")
}
