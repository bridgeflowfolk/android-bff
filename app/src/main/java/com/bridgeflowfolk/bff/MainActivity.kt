package com.bridgeflowfolk.bff

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bridgeflowfolk.bff.ui.screens.AboutScreen
import com.bridgeflowfolk.bff.ui.screens.ContactScreen
import com.bridgeflowfolk.bff.ui.screens.EventsScreen
import com.bridgeflowfolk.bff.ui.theme.BffTheme
import com.bridgeflowfolk.bff.workers.SyncWorker
import dagger.hilt.android.AndroidEntryPoint

// ─── Routes ──────────────────────────────────────────────────────────────────
sealed class Screen(val route: String, val label: String) {
    object Events  : Screen("events",  "Événements")
    object About   : Screen("about",   "À propos")
    object Contact : Screen("contact", "Contact")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Démarre la sync périodique WorkManager
        SyncWorker.schedule(this)

        setContent {
            BffTheme {
                // Demande permission notifications (Android 13+)
                RequestNotificationPermission()

                val navController = rememberNavController()
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentDest = navBackStack?.destination

                val bottomItems = listOf(Screen.Events, Screen.About, Screen.Contact)

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
                            bottomItems.forEach { screen ->
                                val selected = currentDest?.hierarchy
                                    ?.any { it.route == screen.route } == true

                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        when (screen) {
                                            Screen.Events  -> Icon(Icons.Default.CalendarMonth, screen.label)
                                            Screen.About   -> Icon(Icons.Default.Info, screen.label)
                                            Screen.Contact -> Icon(Icons.Default.Phone, screen.label)
                                        }
                                    },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Events.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Events.route)  { EventsScreen() }
                        composable(Screen.About.route)   { AboutScreen() }
                        composable(Screen.Contact.route) { ContactScreen() }
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
        ) { /* résultat ignoré : l'app fonctionne sans notifs */ }

        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
