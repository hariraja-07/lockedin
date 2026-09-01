package com.shortsblocker.app.prefs

data class SupportedApp(val packageName: String, val displayName: String, val note: String? = null)

/** Apps the user can individually toggle blocking for, in Settings. */
object SupportedApps {
    val ALL = listOf(
        SupportedApp("com.google.android.youtube", "YouTube"),
        SupportedApp("com.instagram.android", "Instagram"),
        SupportedApp("com.zhiliaoapp.musically", "TikTok"),
        SupportedApp("com.facebook.katana", "Facebook", note = "감지 정확도 낮을 수 있음"),
        SupportedApp("com.snapchat.android", "Snapchat", note = "앱 전체 차단"),
    )
}
