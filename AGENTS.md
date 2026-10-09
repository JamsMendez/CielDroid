# CielDroid bootstrap constraints

This milestone is a Compose launcher and scene-name catalog only. Preview,
settings, overlay, wallpaper and widget integration remain outside C02.
Future untracked files must survive; they are not verification for this slice.

## Change boundaries

- Preserve `com.jamsmendez.cieldroid`, Android 15 / minSdk 35 and original assets.
- Use one writer, English technical artifacts and pinned dependencies.
- AGP 9 uses built-in Kotlin; align the Compose compiler with Kotlin, not the
  Compose BOM. Do not add `org.jetbrains.kotlin.android`.
- Do not present unavailable features as working controls or add permissions,
  network content or services without authorization.
- No staging, commits, remotes, publishing, SDK installs or persistent machine
  changes without separate authorization. Preserve unrelated files.

## Verification boundary

Use the wrapper commands in [README](README.md) against the isolated candidate,
not ambient future sources. JDK 25 is required for the daemon; a JDK 26 launcher
also works. SDK platform 37 / Build Tools 36.0.0 are required. C02 checks
passed; example tests are not functional bootstrap coverage. Build output
and test APK assembly do not prove device behavior. Report failures and skipped
checks explicitly. Device launch, accessibility and lifecycle remain pending.
Public toolchain stability, upstream distribution licensing and remote CI are
unverified; do not publish on the strength of a local build.
