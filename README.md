# LockedIn

> **Fork of [`JacobX0126/shorts-blocker-android`](https://github.com/JacobX0126/shorts-blocker-android)** (Apache-2.0), hardened for stricter self-control. All credit for the original rule engine, PIN system, and accessibility detection design goes to the upstream author. See [Changes from upstream](#changes-from-upstream).

An on-device Android app that selectively blocks short-form video content — **YouTube Shorts and Instagram Reels** — while leaving everything else in those same apps untouched: long-form YouTube videos, Instagram DMs, and the normal feed all still work.

## Changes from upstream

LockedIn starts from the upstream codebase and hardens it for a "hard block, no escape" personal-use setup:

- **Rebrand** — app display name is LockedIn (package name `com.shortsblocker.app` kept for safe upgrades).
- **Watchdog lock** (this fork's main addition) — a watchdog layer detects when the accessibility service is switched off in system Settings and raises a non-dismissible full-screen lock wall until protection is restored. *In progress — see commit history.*
- **Default scope** — only YouTube + Instagram blocking are enabled by default; TikTok/Facebook/Snapchat rules ship in the rule file but stay off unless toggled.
- **Planned** — battery-optimization exemption flow + OEM autostart hints; `FLAG_SECURE` on PIN/lock screens.

## Why I built this

Short-form feeds are designed around infinite scroll, so blocking the whole app also blocks the things you actually need (checking a message, looking something up), which makes a full block hard to live with. On the other hand, the screen-time limiters most phones ship with can only throttle an app as a whole — they can't single out one tab inside it. This project started from wanting a self-control tool that works at the **screen level**: tell short-form content apart from everything else in the same app, and only block that.

Two problems turned out to be the hard part.

1. **Deciding whether the screen on top right now is short-form.** The app reads the on-screen view tree through Android's `AccessibilityService` API, matches it against `viewId` regex rules first, and — for cases like a bottom tab bar where several tabs share the same `viewId` pattern — also requires a match on `text`/`contentDescription` to cut down false positives. This decision logic (`RuleEvaluator`) is written against an `AccessibleNode` interface rather than the real `AccessibilityNodeInfo`, so the whole thing can be unit-tested with fake node trees on the JVM, no device needed (`RuleEvaluatorTest.kt`, 16 cases).
2. **Keeping that decision off the main thread's critical path.** Tree traversal is bounded by node count (max 500) and depth (max 60), which caps the per-event processing cost. A node-recycling leak (from early-return branches skipping `AccessibilityNodeInfo.recycle()`) also showed up as a failing test before it ever became a real memory problem.

The reasoning behind specific design choices — how rules get updated, how PIN recovery works without email, and why the app is upfront with the user about what it can't guarantee — is written up in [`docs/DECISIONS.md`](docs/DECISIONS.md).

## Key Features

- **Selective short-form blocking** — detects the YouTube Shorts / Instagram Reels / TikTok / Facebook Reels player and tab, and backs out of it automatically. TikTok and Snapchat are blocked outright (`FULL` mode); the rest use whitelist-based partial blocking (`PARTIAL` mode)
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
| Language | Kotlin |
| UI | Jetpack Compose, Material3, Navigation Compose |
| Screen detection | Android `AccessibilityService` API |
| Secure storage | `androidx.security.crypto` (`EncryptedSharedPreferences`, Android Keystore), PBKDF2WithHmacSHA256 |
| Background persistence | Foreground Service (`specialUse`), `BOOT_COMPLETED` `BroadcastReceiver` |
| Data format | JSON (`org.json`) — rule definitions, local event log |
| Testing | JUnit4 — the rule engine is pure Kotlin, so it's unit-tested with no framework dependencies |
| Build | Gradle (Kotlin DSL), AGP 8.5.2 |

## Project Structure

```
app/src/main/java/com/shortsblocker/app/
├── rules/      # Decision engine — AccessibleNode, BlockRule, RuleEvaluator, RuleRepository, etc. (pure Kotlin, framework-independent)
├── service/    # AccessibilityService, foreground service, boot receiver, block enforcement (BACK/HOME)
├── overlay/    # On-screen notice shown when content is blocked
├── logging/    # On-device-only event log (ring buffer, max 50 entries)
├── security/   # PIN + recovery-question storage and verification (PBKDF2 hash + EncryptedSharedPreferences)
├── prefs/      # Master and per-app on/off state
└── ui/         # Compose screens (onboarding, PIN setup/entry/recovery, home, settings)

app/src/main/assets/block_rules.json   # Bundled block rule definitions (no remote updates)
app/src/test/                          # RuleEvaluator unit tests
docs/                                  # Design decisions, technical spec, privacy policy, Play Store review notes
```

## Build & Run

1. Open the project root (`ShortsBlocker/`) in Android Studio.
2. Once Gradle sync finishes, connect a physical device or emulator (API 26+) and run.
3. On first launch: onboarding → grant **Accessibility** permission → grant **Display over other apps** permission → set a PIN and recovery question.
4. To run just the unit tests:
   ```bash
   ./gradlew test
   ```

## Known Limitations

- No app can prevent a user from turning the accessibility service itself off under device Settings > Accessibility — that's an Android OS-level limit, not something an app can override. The in-app PIN only protects **this app's Settings screen**, and the onboarding screen tells the user that plainly. In other words, this app is designed as a soft-commitment tool for self-control, not a hard lock.
- If a target app changes its internal view IDs, the corresponding rule can stop matching (this is recorded locally as `Unmatched` so it's visible rather than silent). When that happens, `block_rules.json` needs an update and the app needs to be rebuilt.
- Facebook's block rule may have lower detection accuracy due to how its view IDs are structured (this is called out in the Settings screen).

For the full functional spec and test cases, see [`docs/TECHNICAL_SPEC.md`](docs/TECHNICAL_SPEC.md).
