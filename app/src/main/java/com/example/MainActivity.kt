package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = (application as StrokoApplication).container
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = Splash) {
                        composable<Splash> {
                            SplashScreen(
                                onNavigateToMain = {
                                    navController.navigate(Main) {
                                        popUpTo(Splash) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<Main> {
                            MainScreen(
                                onNavigateToCreate = { navController.navigate(CreateRoom) },
                                onNavigateToJoin = { navController.navigate(JoinRoom) },
                                onNavigateToPacks = { navController.navigate(WordPacks) },
                                onNavigateToSettings = { navController.navigate(Settings) },
                                onNavigateToAbout = { navController.navigate(About) }
                            )
                        }
                        composable<CreateRoom> {
                            CreateRoomScreen(
                                hostManager = appContainer.hostManager,
                                wordPackRepository = appContainer.wordPackRepository,
                                settingsRepository = appContainer.settingsRepository,
                                onBack = { navController.popBackStack() },
                                onGameCreated = { port, username ->
                                    navController.navigate(Game("127.0.0.1", port, username, true)) {
                                        popUpTo(Main)
                                    }
                                }
                            )
                        }
                        composable<JoinRoom> {
                            JoinRoomScreen(
                                nsdHelper = appContainer.nsdHelper,
                                settingsRepository = appContainer.settingsRepository,
                                onBack = { navController.popBackStack() },
                                onJoin = { host, port, username ->
                                    navController.navigate(Game(host, port, username, false)) {
                                        popUpTo(Main)
                                    }
                                }
                            )
                        }
                        composable<Game> { backStackEntry ->
                            val gameArgs = backStackEntry.toRoute<Game>()
                            GameScreenWrapper(
                                appContainer = appContainer,
                                args = gameArgs,
                                onNavigateMain = { 
                                    navController.navigate(Main) {
                                        popUpTo(0)
                                    } 
                                }
                            )
                        }
                        composable<WordPacks> {
                            WordPacksScreen(
                                repository = appContainer.wordPackRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<Settings> {
                            SettingsScreen(
                                repository = appContainer.settingsRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<About> {
                            AboutScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
