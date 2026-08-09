package ai.blueview.weather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.blueview.weather.ui.screens.about.AboutScreen
import ai.blueview.weather.ui.screens.home.HomeScreen
import ai.blueview.weather.ui.screens.settings.SettingsScreen

private object Routes {
    const val HOME     = "home"
    const val SETTINGS = "settings"
    const val ABOUT    = "about"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSettings = { nav.navigate(Routes.SETTINGS) },
                onNavigateToAbout    = { nav.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { nav.popBackStack() })
        }
    }
}
