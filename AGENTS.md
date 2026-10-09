# CielDroid bootstrap constraints

C03 adds settings/domain, preferences persistence and ViewModel foundations to
C02's Compose launcher and scene-name catalog. User-facing settings controls,
preview rendering, overlay, wallpaper and widget integration remain out of scope.
C04 adds only three unmodified original shared resources and the intended full
56-file inventory catalog; the 53 scene-specific resources/entrypoints remain
outside the committed slice. Shared resources are not integrated with the catalog
launcher. Future untracked files must survive; they are not verification for
this slice.

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
also works. SDK platform 37 / Build Tools 36.0.0 are required. Historically, C03's
four checks passed: eight fresh host tests (seven functional), no failures,
errors or skips and lint 0 errors/34 warnings. C04's four checks also passed;
eight fresh tests (seven functional) are not asset coverage. Separate checks match
the three originals against committed upstream, catalog and production APK bytes.
Keep inherited `OFL.txt:21` whitespace: the whole-tree whitespace check reports it.
The full asset verifier requires all 56 files and first applies at C09.
The arithmetic example is not functional.
Build output and test APK assembly do not prove device behavior. Report failures
and skipped checks explicitly. Device launch, accessibility and lifecycle remain pending.
Public toolchain stability, upstream distribution licensing and remote CI are
unverified; do not publish on the strength of a local build.
