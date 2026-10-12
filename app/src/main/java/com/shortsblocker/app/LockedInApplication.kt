package com.shortsblocker.app

import android.app.Application
import android.os.StrictMode
import android.util.Log

class LockedInApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupStrictMode()
        setupUncaughtExceptionHandler()
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log exception type and message only — strictly NO personal data, screen content, or sensitive info
                Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.javaClass.simpleName}: ${throwable.message}")
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val TAG = "LockedInApp"
    }
}
