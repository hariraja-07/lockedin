# Privacy Policy (Draft)

**Shorts Blocker** is a personal self-control app that operates under the following principles.

## Information collected
None. The app has no server and does not request the `INTERNET` permission. No data — screen content, viewing history, account information, location, advertising identifiers, or anything else — ever leaves the device.

## What the accessibility service processes
It inspects only the **screen structure** (view IDs, tab/button text) of YouTube, Instagram, TikTok, and the browser apps the user has selected, in real time, to decide whether the content is short-form.
- This decision happens **only on-device** and its result is not stored (except for the "Local decision log" described below).
- The actual content shown on screen — videos, photos, message text, etc. — is never read, stored, or transmitted anywhere.

## Local decision log
The app keeps up to the last 50 block/no-match events, on-device only. Each entry contains only the **app package name, the matched rule ID (or "UNMATCHED"), and a timestamp** — never screen content. This log is deleted when the app is uninstalled and is never transmitted externally.

## PIN and recovery question
The PIN and recovery question/answer that protect the Settings screen are stored only as salted hashes in on-device encrypted storage (`EncryptedSharedPreferences`). They are never stored in plaintext or transmitted off the device.

## Purpose of each permission

| Permission | Purpose |
|---|---|
| Accessibility service | Detect and immediately block Shorts/Reels/TikTok screens |
| Display over other apps | Show a brief notice when content is blocked |
| Notifications | Persistent notification indicating the blocking service is running |
| Start on boot | Automatically restart the blocking service after a device reboot |

## Contact
This app is distributed as a personal project. Contact information will be added to this document at the time of distribution.

*Last updated: draft — contact and business information to be filled in before public release.*
