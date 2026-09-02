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
The in-app PIN only protects this app's own Settings screen. Turning the service itself off under **device Settings > Accessibility** is something no app can prevent — that's Android OS policy. The onboarding screen (`onboarding_soft_lock_notice`) states this explicitly to the user — this app is a soft-commitment tool that supports self-control, not a "hard lock."
