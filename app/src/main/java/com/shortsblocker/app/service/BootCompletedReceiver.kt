package com.shortsblocker.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val restartAction = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!restartAction) return
        runCatching {
            BlockGuardForegroundService.start(context)
        }.onFailure { e ->
            Log.e("BootCompletedReceiver", "Failed to start guard service on boot/replace", e)
        }
    }
}
