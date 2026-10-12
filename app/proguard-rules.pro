# LockedIn Proguard / R8 Keep Rules

# Keep JSON models parsed via RuleSetParser and BlockEventLogger
-keepclassmembers class com.shortsblocker.app.rules.BlockRule { *; }
-keepclassmembers class com.shortsblocker.app.rules.NodeMatcher { *; }
-keepclassmembers class com.shortsblocker.app.rules.RuleSet { *; }
-keepclassmembers class com.shortsblocker.app.data.BlockEventLogger$Event { *; }
-keep class com.shortsblocker.app.data.SupportedApp { *; }

# Security classes (prevent stripping/inlining of critical security and PBKDF2 logic)
-keep class com.shortsblocker.app.security.** { *; }

# WorkManager Worker classes instantiated via reflection
-keep class com.shortsblocker.app.service.GuardReviveWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# Keep Compose Composable annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# AndroidX Security / Google Tink compile-time annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
