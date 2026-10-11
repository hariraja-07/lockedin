# Design Decisions

## Combining detection strategies
- **Primary: `viewId` regex.** Elements that appear uniquely on a screen — the player, the viewer, the feed container — can be reliably matched by `viewId` alone.
- **Secondary: text / contentDescription conditions.** For something like a bottom tab bar, where several tabs share the same `viewId` pattern (e.g. `bottom_bar_item`, `tab_bar`), `viewId` alone can't tell a "Shorts tab" from a "Home tab." Those rules additionally require a `requireTextRegex` / `requireContentDescRegex` match to cut down false positives.
- The whitelist always wins — if a screen contains both a block-target node and a whitelist node at the same time, it's unconditionally allowed.

## Rule update mechanism: bundled resources only (final)
Since this is a personal self-control app, there's no separate server or hosting to maintain — rules live entirely in `assets/block_rules.json`. When YouTube or Instagram change their view IDs, the rule file gets updated and the app gets rebuilt. `RuleEvaluator` logs match failures (`Unmatched`) locally, so if failures start piling up, the user can tell it's time to update the rules themselves.

## PIN recovery method: security question (final)
To keep everything on-device with no server or email dependency, PIN setup also stores a **local security question and answer** instead of email verification. Both the question and answer are hashed with PBKDF2 (salted, 120,000 iterations) and never stored in plaintext. A successful recovery routes to a screen for setting a brand-new PIN.

## OS-level limits (disclosed, not hidden)
The in-app PIN only protects this app's own Settings screen. Turning the service itself off under **device Settings > Accessibility** is something no app can prevent — that's Android OS policy. LockedIn's answer is a **watchdog**: the guard foreground service polls the service's enabled state every ~3s and, if it goes dark while master blocking is on, immediately raises a full-screen non-dismissible overlay (`LockoutOverlayController`) with a deep link back to accessibility settings. A legitimate pause is still possible through the app's PIN-gated master switch (watchdog state `PausedByUser` shows no lock wall). The onboarding screen (`onboarding_soft_lock_notice`) explains both halves to the user. Safe-mode boot + adb can still remove the app — this is a hardened self-control tool, not an MDM lock.
