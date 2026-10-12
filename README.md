# LockedIn

**Block YouTube Shorts & Instagram Reels on Android — hard.** A watchdog-backed accessibility blocker: Shorts/Reels bounce you out instantly, and if anyone flips the accessibility service off in Settings, a non-dismissible lock wall takes over the screen until protection is restored.

> **Fork of [`JacobX0126/shorts-blocker-android`](https://github.com/JacobX0126/shorts-blocker-android)** (Apache-2.0), hardened for stricter self-control. All credit for the original rule engine, PIN system, and accessibility detection design goes to the upstream author. See [Changes from upstream](#changes-from-upstream).

An on-device Android app that selectively blocks short-form video content — **YouTube Shorts and Instagram Reels** — while leaving everything else in those same apps untouched: long-form YouTube videos, Instagram DMs, and the normal feed all still work.

## Changes from upstream

LockedIn starts from the upstream codebase and hardens it for a "hard block, no escape" personal-use setup:

- **Rebrand** — app display name is LockedIn (package name `com.shortsblocker.app` kept for safe upgrades).
- **Watchdog lock** (this fork's main addition) — a watchdog layer detects when the accessibility service is switched off in system Settings and raises a non-dismissible full-screen lock wall until protection is restored.
- **Default scope** — only YouTube + Instagram blocking are enabled by default; TikTok/Facebook/Snapchat rules ship in the rule file but stay off unless toggled.
- **Battery/OEM hardening** — battery-optimization exemption deep-link + OEM autostart hints so the watchdog survives aggressive task killers.
- **FLAG_SECURE** on the PIN screens and the lock wall (no screenshots / recents previews of either).

## Why I built this

Short-form feeds are designed around infinite scroll, so blocking the whole app also blocks the things you actually need (checking a message, looking something up), which makes a full block hard to live with. On the other hand, the screen-time limiters most phones ship with can only throttle an app as a whole — they can't single out one tab inside it. This project started from wanting a self-control tool that works at the **screen level**: tell short-form content apart from everything else in the same app, and only block that.

Two problems turned out to be the hard part.

1. **Deciding whether the screen on top right now is short-form.** The app reads the on-screen view tree through Android's `AccessibilityService` API, matches it against `viewId` regex rules first, and — for cases like a bottom tab bar where several tabs share the same `viewId` pattern — also requires a match on `text`/`contentDescription` to cut down false positives. This decision logic (`RuleEvaluator`) is written against an `AccessibleNode` interface rather than the real `AccessibilityNodeInfo`, so the whole thing can be unit-tested with fake node trees on the JVM, no device needed (`RuleEvaluatorTest.kt`, 16 cases).
2. **Keeping that decision off the main thread's critical path.** Tree traversal is bounded by node count (max 500) and depth (max 60), which caps the per-event processing cost. A node-recycling leak (from early-return branches skipping `AccessibilityNodeInfo.recycle()`) also showed up as a failing test before it ever became a real memory problem.

The reasoning behind specific design choices — how rules get updated, how PIN recovery works without email, and why the app is upfront with the user about what it can't guarantee — is written up in [`docs/DECISIONS.md`](docs/DECISIONS.md).

## Key Features

- **Selective short-form blocking** — detects the YouTube Shorts / Instagram Reels / TikTok / Facebook Reels player and tab, and backs out of it automatically. YouTube and Instagram use whitelist-based partial blocking (`PARTIAL` mode) enabled by default; TikTok, Snapchat, and Facebook rules are included but disabled by default (opt-in via Settings)
- **Browser URL blocking** — also catches short-form URLs (`youtube.com/shorts`, `instagram.com/reels`, etc.) opened in Chrome, Samsung Internet, Firefox, or Edge
- **PIN + recovery question** — the Settings screen is PIN-protected; if the PIN is forgotten, it can be recovered on-device via a security question, no email involved. Both the PIN and the recovery answer are stored only as PBKDF2 hashes (salted, 120,000 iterations) inside `EncryptedSharedPreferences` (AES-256, backed by the Android Keystore) — plaintext is never persisted
- **Two-step protection on the master switch** — turning blocking on is instant, but turning it off requires a confirmation dialog *and* a correct PIN, so it can't be disabled on impulse
- **Per-app toggles** — Settings lets you turn blocking on/off independently for YouTube, Instagram, TikTok, Facebook, and Snapchat
- **On-device event log** — keeps the last 50 block/no-match decisions locally (package name, matched rule ID, timestamp only) — never screen content or personal data
- **Survives a reboot** — a `BOOT_COMPLETED` receiver restarts the foreground service after the device restarts, so blocking stays active without reopening the app

## How It Works

```
AccessibilityEvent
      │
      ▼
ShortsBlockAccessibilityService  ── checks BlockingPreferences (master + per-app on/off)
      │
      ▼
AccessibilityNodeAdapter  (wraps the real AccessibilityNodeInfo as an AccessibleNode)
      │
      ▼
RuleEvaluator  ── evaluates against block_rules.json, loaded by RuleRepository
      │            (whitelist takes priority, viewId/text/contentDescription matching, bounded BFS traversal)
      ▼
BlockDecision (Block / Allow / Unmatched)
      │
      ▼
BlockActionExecutor  ── performs BACK, escalates to HOME if still blocked 200ms later, debounces repeat triggers for 1s
      │
      ▼
BlockOverlayController (brief on-screen notice)  +  BlockEventLogger (local decision log)
```

Every decision is made entirely on-device. The rule set (`assets/block_rules.json`) ships inside the app and is never fetched from or updated by a remote server.

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.06.00), Material3, Navigation Compose |
| Screen detection | Android `AccessibilityService` API |
| Secure storage | `androidx.security.crypto` (`EncryptedSharedPreferences`, Android Keystore), PBKDF2WithHmacSHA256 |
| Background persistence | Foreground Service (`specialUse`), WorkManager `GuardReviveWorker`, `BOOT_COMPLETED` `BroadcastReceiver` |
| Data format | JSON (`org.json`) — rule definitions, local event log |
| Testing | JUnit4, Kotlin Coroutines Test — rule engine is pure Kotlin unit-tested on JVM |
| Build | Gradle (Kotlin DSL), AGP 8.7.3, compileSdk 35 / targetSdk 35 |

## Project Structure

```
app/src/main/java/com/shortsblocker/app/
├── app/        # Application class, StrictMode & uncaught exception handling
├── domain/     # ProtectionStatus, ProtectionStatusProvider, SessionLock, WatchdogDecision, WatchdogMonitor
├── data/       # BlockingPreferences, SupportedApps, BlockEventLogger, RuleRepository
├── rules/      # Decision engine — AccessibleNode, BlockRule, RuleEvaluator, etc. (pure Kotlin, framework-independent)
├── service/    # AccessibilityService, foreground service, boot receiver, block enforcement (BACK/HOME), WorkManager
├── overlay/    # On-screen notice shown when content is blocked, lockout overlay
├── security/   # PIN + recovery storage and verification (PBKDF2 hash, constant-time compare, brute force defense)
└── ui/         # Compose screens (onboarding wizard, PIN setup/entry/recovery, home, settings), ViewModels, LockoutActivity
    └── theme/

app/src/main/assets/block_rules.json   # Bundled block rule definitions (no remote updates)
app/src/test/                          # Unit tests (RuleEvaluator, WatchdogDecision, HashUtils, PinSecurity, etc.)
docs/                                  # Design decisions, technical spec, privacy policy, Play Store review notes
```

## Build & Run

0. Clone repository:
   ```bash
   git clone https://github.com/hariraja-07/lockedin.git
   cd lockedin
   ```
1. Open the project root (`shorts-blocker/`) in Android Studio.
2. Once Gradle sync finishes, connect a physical device or emulator (API 26+) and run.
3. On first launch: onboarding → grant **Accessibility** permission → grant **Display over other apps** permission → set a PIN and recovery question.
4. To run just the unit tests:
   ```bash
   ./gradlew test
   ```

## Known Limitations

- Android still allows the accessibility service itself to be switched off under device Settings > Accessibility — no app can prevent that. LockedIn's watchdog detects the switch within seconds and covers the screen with a lock wall until the service is re-enabled, which raises the friction a lot, but a user willing to boot into safe mode + adb (or factory reset) can still remove the app. This is a hardened self-control tool, not an enterprise MDM lock.
- If a target app changes its internal view IDs, the corresponding rule can stop matching (this is recorded locally as `Unmatched` so it's visible rather than silent). When that happens, `block_rules.json` needs an update and the app needs to be rebuilt.
- Facebook's block rule may have lower detection accuracy due to how its view IDs are structured (this is called out in the Settings screen).

For the full functional spec and test cases, see [`docs/TECHNICAL_SPEC.md`](docs/TECHNICAL_SPEC.md).
