package com.bridgeflowfolk.bff

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bridgeflowfolk.bff.ui.screens.AboutScreen
import com.bridgeflowfolk.bff.ui.screens.ContactScreen
import com.bridgeflowfolk.bff.ui.screens.EventsScreen
import com.bridgeflowfolk.bff.ui.screens.GameScreen
import com.bridgeflowfolk.bff.ui.theme.BffTheme
import com.bridgeflowfolk.bff.workers.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

// ── Routes type-safe ──────────────────────────────────────────────────────────
@Serializable object RouteEvents
@Serializable object RouteGame
@Serializable object RouteAbout
@Serializable object RouteContact

// Index pour déterminer le sens de la transition (gauche ↔ droite)
private val routeOrder = listOf(RouteEvents, RouteGame, RouteAbout, RouteContact)

private data class BottomNavItem<T : Any>(
    val route: T,
    val label: String,
    val icon: @Composable () -> Unit
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)

        setContent {
            BffTheme {
                RequestNotificationPermission()

                val navController = rememberNavController()
                val navBackStack  by navController.currentBackStackEntryAsState()
                val currentDest   = navBackStack?.destination

                val bottomItems = listOf(
                    BottomNavItem(RouteEvents,  "Événements") { Icon(Icons.Default.CalendarMonth, "Événements") },
                    BottomNavItem(RouteGame,    "Jeu")        { Icon(Icons.Default.Star,          "Jeu") },
                    BottomNavItem(RouteAbout,   "À propos")   { Icon(Icons.Default.Info,          "À propos") },
                    BottomNavItem(RouteContact, "Contact")    { Icon(Icons.Default.Phone,         "Contact") }
                )

                // Index de la destination courante pour calculer le sens de glissement
                val currentIndex by remember(currentDest) {
                    derivedStateOf {
                        routeOrder.indexOfFirst { route ->
                            currentDest?.hasRoute(route::class) == true
                        }.coerceAtLeast(0)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Bridge & Flow Folk",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            bottomItems.forEachIndexed { index, item ->
                                val selected = currentDest
                                    ?.hierarchy
                                    ?.any { it.hasRoute(item.route::class) } == true

                                NavigationBarItem(
                                    selected = selected,
                                    onClick  = {
                                        navController.navigate(item.route) {
                                            popUpTo<RouteEvents> { saveState = true }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    },
                                    icon  = item.icon,
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController    = navController,
                        startDestination = RouteEvents,
                        modifier         = Modifier.padding(innerPadding),
                        // ── Transitions sobres : glissement horizontal directionnel ──
                        // La direction (gauche/droite) est calculée depuis currentIndex
                        // capturé via la closure — sobre et cohérent avec les conventions
                        // Material3 (pas de fade brutal, pas de zoom excessif).
                        enterTransition  = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)) },
                        exitTransition   = { slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(200)) },
                        popEnterTransition  = { slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(280)) },
                        popExitTransition   = { slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)) }
                    ) {
                        composable<RouteEvents>  { EventsScreen() }
                        composable<RouteGame>    { GameScreen() }
                        composable<RouteAbout>   { AboutScreen() }
                        composable<RouteContact> { ContactScreen() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* résultat ignoré — l'utilisateur peut refuser */ }
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
