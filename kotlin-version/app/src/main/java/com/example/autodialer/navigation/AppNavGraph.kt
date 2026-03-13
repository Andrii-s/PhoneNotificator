package com.example.autodialer.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autodialer.ui.debtors.DebtorsScreen
import com.example.autodialer.ui.settings.ConfirmLogoutScreen
import com.example.autodialer.ui.settings.SettingsScreen
import com.example.autodialer.ui.splash.SplashScreen

// =============================================================================
// Route definitions
// =============================================================================

/**
 * Sealed class that represents all destinations in the app.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Settings : Screen("settings")
    object ConfirmLogout : Screen("confirm_logout")

    /**
     * Debtors screen. Accepts an optional [ARG_AUDIO_FILE_PATH] query parameter
     * so the Settings screen can forward the selected audio file path.
     */
    object Debtors : Screen("debtors") {
        const val ARG_AUDIO_FILE_PATH = "audioFilePath"

        /** Builds the full route string with the encoded [audioFilePath]. */
        fun createRoute(audioFilePath: String): String =
            "debtors?$ARG_AUDIO_FILE_PATH=${Uri.encode(audioFilePath)}"
    }
}

// =============================================================================
// Navigation graph
// =============================================================================

/**
 * Top-level navigation graph for AutoDialer.
 *
 * Graph:
 *  Splash ──► Settings ──► Debtors
 *                     └──► ConfirmLogout
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val activity = LocalContext.current as? Activity

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToDebtors = { audioFilePath ->
                    navController.navigate(Screen.Debtors.createRoute(audioFilePath))
                },
                onNavigateToConfirmLogout = {
                    navController.navigate(Screen.ConfirmLogout.route)
                },
            )
        }

        // ── Debtors ───────────────────────────────────────────────────────────
        composable(
            route = "${Screen.Debtors.route}?${Screen.Debtors.ARG_AUDIO_FILE_PATH}={${Screen.Debtors.ARG_AUDIO_FILE_PATH}}",
            arguments = listOf(
                navArgument(Screen.Debtors.ARG_AUDIO_FILE_PATH) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments
                ?.getString(Screen.Debtors.ARG_AUDIO_FILE_PATH)
                .orEmpty()
            // Decode the URI-encoded path received from SettingsScreen.
            val audioFilePath = Uri.decode(rawPath)

            DebtorsScreen(
                audioFilePath = audioFilePath,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Confirm Logout ────────────────────────────────────────────────────
        composable(Screen.ConfirmLogout.route) {
            ConfirmLogoutScreen(
                onConfirm = {
                    activity?.finish()
                },
                onDismiss = {
                    navController.popBackStack()
                },
            )
        }
    }
}
