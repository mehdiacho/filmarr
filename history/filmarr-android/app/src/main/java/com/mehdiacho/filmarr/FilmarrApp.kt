package com.mehdiacho.filmarr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehdiacho.filmarr.data.FilmRepository
import com.mehdiacho.filmarr.data.auth.AuthManager
import com.mehdiacho.filmarr.navigation.Dest
import com.mehdiacho.filmarr.ui.components.FilmarrBottomNav
import com.mehdiacho.filmarr.ui.components.UndoSnackbarHost
import com.mehdiacho.filmarr.ui.screens.AskScreen
import com.mehdiacho.filmarr.ui.screens.AuthScreen
import com.mehdiacho.filmarr.ui.screens.BlitzScreen
import com.mehdiacho.filmarr.ui.screens.BuildScreen
import com.mehdiacho.filmarr.ui.screens.DiscoverScreen
import com.mehdiacho.filmarr.ui.screens.FilmDetailScreen
import com.mehdiacho.filmarr.ui.screens.LetterboxdImportScreen
import com.mehdiacho.filmarr.ui.screens.LibraryScreen
import com.mehdiacho.filmarr.ui.screens.OnboardingScreen
import com.mehdiacho.filmarr.ui.screens.OverlapScreen
import com.mehdiacho.filmarr.ui.screens.ProfileScreen
import com.mehdiacho.filmarr.ui.screens.SearchScreen
import com.mehdiacho.filmarr.ui.screens.SettingsScreen
import com.mehdiacho.filmarr.ui.screens.TasteGraphScreen
import com.mehdiacho.filmarr.ui.screens.TonightScreen
import com.mehdiacho.filmarr.ui.theme.FilmarrColors

/** Routes that show the persistent bottom nav (§2.1 / §5). */
private val NAV_ROUTES = setOf(
    Dest.DISCOVER, Dest.BUILD, Dest.LIBRARY, Dest.PROFILE,
    Dest.FILM_DETAIL, Dest.BLITZ, Dest.TONIGHT, Dest.TASTE_GRAPH, Dest.OVERLAP, Dest.SEARCH,
)

/**
 * Routes that require an account. Guests may browse Search + Film detail (ratings, recommendations)
 * but everything personal — the swipe feeds, the library, the profile — prompts sign-in. A local
 * guest profile with on-device logging is a later addition.
 */
private val AUTH_REQUIRED = setOf(
    Dest.DISCOVER, Dest.BUILD, Dest.LIBRARY, Dest.PROFILE,
    Dest.BLITZ, Dest.TONIGHT, Dest.TASTE_GRAPH, Dest.OVERLAP, Dest.ASK, Dest.LETTERBOXD,
)

@Composable
fun FilmarrApp() {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val current = backEntry?.destination?.route

    // Bind the library to the signed-in user's cloud data; fall back to local preview when signed out.
    val user by AuthManager.user.collectAsStateWithLifecycle()
    val signedIn = user != null
    LaunchedEffect(user?.uid) {
        val uid = user?.uid
        if (uid != null) FilmRepository.bind(uid) else FilmRepository.unbind()
    }

    // Guarded tab switch: gated destinations bounce a guest to the auth gate.
    fun guardedTab(route: String) {
        if (route in AUTH_REQUIRED && !signedIn) nav.navigate(Dest.AUTH) else nav.switchTab(route)
    }

    Box(Modifier.fillMaxSize().background(FilmarrColors.Bg)) {
        NavHost(
            navController = nav,
            // Signed-out users land on the gate; Search is reachable from there as a guest.
            startDestination = if (signedIn) Dest.DISCOVER else Dest.AUTH,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Dest.ONBOARDING) {
                OnboardingScreen(onDone = { nav.navigate(Dest.DISCOVER) { popUpTo(Dest.ONBOARDING) { inclusive = true } } })
            }
            composable(Dest.AUTH) {
                AuthScreen(
                    onBack = { if (!nav.popBackStack()) nav.navigate(Dest.SEARCH) },
                    // New accounts go through onboarding (seeds the graph); returning users skip it.
                    onAuthed = { isNewUser ->
                        val dest = if (isNewUser) Dest.ONBOARDING else Dest.DISCOVER
                        nav.navigate(dest) { popUpTo(Dest.AUTH) { inclusive = true } }
                    },
                    onGuest = { nav.navigate(Dest.SEARCH) { popUpTo(Dest.AUTH) { inclusive = true } } },
                )
            }

            composable(Dest.DISCOVER) {
                DiscoverScreen(onOpenFilm = { nav.pushFilm(it) }, onOpenAsk = { nav.navigate(Dest.ASK) })
            }
            composable(Dest.BUILD) { BuildScreen(onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.LIBRARY) { LibraryScreen(onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.PROFILE) {
                ProfileScreen(
                    onOpenSettings = { nav.navigate(Dest.SETTINGS) },
                    onOpenGraph = { nav.navigate(Dest.TASTE_GRAPH) },
                    onOpenOverlap = { nav.navigate(Dest.OVERLAP) },
                    onOpenLetterboxd = { nav.navigate(Dest.LETTERBOXD) },
                    onOpenAuth = { nav.navigate(Dest.AUTH) },
                    onSignedOut = { nav.navigate(Dest.AUTH) { popUpTo(nav.graph.startDestinationId) { inclusive = true } } },
                )
            }
            composable(Dest.FILM_DETAIL) { entry ->
                val id = entry.arguments?.getString("filmId") ?: return@composable
                FilmDetailScreen(
                    filmId = id, onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) },
                    canLog = signedIn, onRequireAuth = { nav.navigate(Dest.AUTH) },
                )
            }
            composable(Dest.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) }, onOpenAsk = { nav.navigate(Dest.ASK) },
                    canLog = signedIn, onRequireAuth = { nav.navigate(Dest.AUTH) },
                )
            }
            composable(Dest.BLITZ) { BlitzScreen(onBack = { nav.popBackStack() }) }
            composable(Dest.TONIGHT) { TonightScreen(onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.ASK) { AskScreen(onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.OVERLAP) { OverlapScreen(onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.TASTE_GRAPH) { TasteGraphScreen(onBack = { nav.popBackStack() }, onOpenFilm = { nav.pushFilm(it) }) }
            composable(Dest.LETTERBOXD) { LetterboxdImportScreen(onBack = { nav.popBackStack() }) }
            composable(Dest.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenLetterboxd = { nav.navigate(Dest.LETTERBOXD) },
                    onOpenAuth = { nav.navigate(Dest.AUTH) },
                    onAccountDeleted = {
                        nav.navigate(Dest.AUTH) { popUpTo(nav.graph.startDestinationId) { inclusive = true } }
                    },
                )
            }
        }

        if (current in NAV_ROUTES) {
            // Undo snackbar floats just above the nav bar.
            UndoSnackbarHost(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 96.dp),
            )
            Box(Modifier.align(Alignment.BottomCenter)) {
                FilmarrBottomNav(
                    current = current,
                    onNavigate = { guardedTab(it) },
                    onQuickLog = { nav.navigate(Dest.SEARCH) },
                    onQuickLogLongPress = { guardedTab(Dest.TONIGHT) },
                )
            }
        }
    }
}

private fun NavHostController.pushFilm(id: String) = navigate(Dest.filmDetail(id))

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
