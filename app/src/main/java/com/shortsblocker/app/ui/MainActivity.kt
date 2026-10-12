package com.shortsblocker.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shortsblocker.app.data.BlockingPreferences
import com.shortsblocker.app.domain.SessionLock
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.service.BlockGuardForegroundService
import com.shortsblocker.app.ui.theme.ShortsBlockerTheme

class MainActivity : ComponentActivity() {
    private val sessionLock = SessionLock()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            BlockGuardForegroundService.start(applicationContext)
        }.onFailure { e ->
            android.util.Log.e("MainActivity", "Failed to start guard service in onCreate", e)
        }
        val pinManager = PinManager(applicationContext)
        val blockingPreferences = BlockingPreferences(applicationContext)

        setContent {
            val themeMode by blockingPreferences.themeModeFlow.collectAsState()
            ShortsBlockerTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        pinManager = pinManager,
                        blockingPreferences = blockingPreferences,
                        sessionLock = sessionLock,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sessionLock.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        sessionLock.onAppForegrounded()
    }
}

@Composable
fun AppNavGraph(
    pinManager: PinManager,
    blockingPreferences: BlockingPreferences,
    sessionLock: SessionLock,
) {
    val navController = rememberNavController()
    val startDestination = if (pinManager.isPinSet()) Routes.HOME else Routes.ONBOARDING
    val isSessionUnlocked by sessionLock.unlockedFlow.collectAsState()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = { navController.navigate(Routes.PIN_SETUP) })
        }
        composable(Routes.PIN_SETUP) {
            PinSetupScreen(
                pinManager = pinManager,
                onDone = {
                    sessionLock.unlock()
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                blockingPreferences = blockingPreferences,
                onOpenSettings = {
                    if (isSessionUnlocked) {
                        navController.navigate(Routes.SETTINGS)
                    } else {
                        navController.navigate(Routes.PIN_ENTRY)
                    }
                },
                onRequestDisable = { navController.navigate(Routes.DISABLE_PIN_ENTRY) },
            )
        }
        composable(Routes.PIN_ENTRY) {
            PinEntryScreen(
                pinManager = pinManager,
                onSuccess = {
                    sessionLock.unlock()
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.HOME)
                    }
                },
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
            if (!isSessionUnlocked) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.PIN_ENTRY) {
                        popUpTo(Routes.HOME)
                    }
                }
            } else {
                SettingsScreen(
                    blockingPreferences = blockingPreferences,
                    onChangePin = { navController.navigate(Routes.CHANGE_PIN) },
                )
            }
        }
        composable(Routes.CHANGE_PIN) {
            if (!isSessionUnlocked) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.PIN_ENTRY) {
                        popUpTo(Routes.HOME)
                    }
                }
            } else {
                ChangePinScreen(
                    pinManager = pinManager,
                    onPinChanged = {
                        navController.popBackStack()
                    },
                )
            }
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
    const val CHANGE_PIN = "change_pin"
}
