# Play Store Accessibility Permission Review Notes

Apps that use the Accessibility API for something other than screen-reading purposes (e.g., screen readers) fall under Google Play's **Restricted Use** policy for accessibility, and need a separate declaration and review pass before publishing.

## What to prepare before submitting
1. In **Play Console > App content > Accessibility permission**, clearly describe this app's purpose as "content blocking / digital wellbeing."
2. **Demo video**: screen-record from turning the accessibility service on through an actual Shorts/Reels/TikTok screen being blocked. The reviewer needs to be able to confirm the core feature doesn't work without the accessibility permission.
3. **Explain why no other API works.** `UsageStatsManager` and `DeviceAdmin` can't distinguish between screens *within* an app (Shorts vs. long-form); only accessibility API node-tree inspection makes that possible. State this explicitly.
4. **Privacy policy URL** is required — publish the contents of this repo's [PRIVACY_POLICY.md](./PRIVACY_POLICY.md) somewhere on the web and link to it.
5. The app needs a screen that explains *why* the accessibility permission is needed **before** requesting it (a pre-permission disclosure screen) — `OnboardingScreen` serves this role here.

## Common reasons for rejection
- **Too broad a package scope.** Keep `accessibility_service_config.xml`'s `packageNames` limited to the apps that actually need it (YouTube/Instagram/TikTok/browsers) — targeting all apps significantly increases the chance of rejection. This project is already scoped this way ([accessibility_service_config.xml](../app/src/main/res/xml/accessibility_service_config.xml)).
- **Suspicion of transmitting/storing screen content.** The code requests no `INTERNET` permission at all, and the log (`BlockEventLogger`) stores only the package name and rule ID — call this out explicitly in the review notes.
- **Bundling unrelated features with the accessibility permission.** This service must actually be used only for blocking, and not be combined with ads/analytics SDKs.

## A separate note on sideloading
When the APK is installed directly outside the Play Store, Android 13+ requires a "restricted settings" unlock step to enable the accessibility service (Settings > Apps > this app > Allow restricted settings). This is an OS behavior unrelated to Play review, but the user still needs to be told about it during onboarding.
