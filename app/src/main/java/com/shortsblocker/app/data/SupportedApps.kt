package com.shortsblocker.app.data

data class SupportedApp(val packageName: String, val displayName: String, val note: String? = null)

/** Apps the user can individually toggle blocking for, in Settings. */
object SupportedApps {
    val ALL = listOf(
        SupportedApp("com.google.android.youtube", "YouTube"),
        SupportedApp("com.instagram.android", "Instagram"),
        SupportedApp("com.zhiliaoapp.musically", "TikTok"),
        SupportedApp("com.facebook.katana", "Facebook", note = "Detection accuracy may be lower"),
        SupportedApp("com.snapchat.android", "Snapchat", note = "Blocks the entire app"),
    )
}
