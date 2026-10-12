package com.shortsblocker.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.service.BlockGuardForegroundService
import com.shortsblocker.app.ui.theme.ShortsBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BlockGuardForegroundService.start(applicationContext)
        val pinManager = PinManager(applicationContext)
        val blockingPreferences = BlockingPreferences(applicationContext)

        setContent {
            ShortsBlockerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(pinManager = pinManager, blockingPreferences = blockingPreferences)
                }
            }
        }
    }
}

@Composable
fun AppNavGraph(pinManager: PinManager, blockingPreferences: BlockingPreferences) {
    val navController = rememberNavController()
    val startDestination = if (pinManager.isPinSet()) Routes.HOME else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = { navController.navigate(Routes.PIN_SETUP) })
        }
        composable(Routes.PIN_SETUP) {
            PinSetupScreen(
                pinManager = pinManager,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                blockingPreferences = blockingPreferences,
                onOpenSettings = { navController.navigate(Routes.PIN_ENTRY) },
                onRequestDisable = { navController.navigate(Routes.DISABLE_PIN_ENTRY) },
            )
        }
        composable(Routes.PIN_ENTRY) {
            PinEntryScreen(
                pinManager = pinManager,
                onSuccess = { navController.navigate(Routes.SETTINGS) },
                onForgot = { navController.navigate(Routes.PIN_RECOVERY) },
            )
        }
        composable(Routes.DISABLE_PIN_ENTRY) {
            PinEntryScreen(
                pinManager = pinManager,
                onSuccess = {
                    blockingPreferences.masterEnabled = false
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onForgot = { navController.navigate(Routes.PIN_RECOVERY) },
            )
        }
        composable(Routes.PIN_RECOVERY) {
            PinRecoveryScreen(
                pinManager = pinManager,
                onRecovered = {
                    navController.navigate(Routes.PIN_SETUP) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                blockingPreferences = blockingPreferences,
                onChangePin = { navController.navigate(Routes.PIN_SETUP) },
            )
        }
    }
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val PIN_SETUP = "pin_setup"
    const val HOME = "home"
    const val PIN_ENTRY = "pin_entry"
    const val DISABLE_PIN_ENTRY = "disable_pin_entry"
    const val PIN_RECOVERY = "pin_recovery"
    const val SETTINGS = "settings"
}
