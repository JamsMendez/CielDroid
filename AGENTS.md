# CielDroid bootstrap constraints

C03 adds settings/domain, preferences persistence and ViewModel foundations to
C02's Compose launcher and scene-name catalog. User-facing settings controls,
preview rendering, overlay, wallpaper and widget integration remain out of scope.
C04 added three original shared resources; C05 adds the whole unchanged idle
Full/Mini component (11 text files, 175960 bytes), not a Mini-only scene.
Full is default; `?variant=mini` selects Mini in the same files.
C06a imports only six whole unchanged Explorer model/effects JS files (152080 bytes),
listed in README: no entry HTML, CSS or drivers, no executed JS or Full/Mini closure.
Preserve config/rising-sparks CielWin headers byte-exact; Explorer's qrc CSP is absent.
The candidate is 20 originals / 476646 bytes (19 text, one TTF), not the intended
56-file / 940651-byte catalog. Six Explorer files await C06b (57160 bytes);
30 processing/raphael files / 406845 bytes remain out. Closure awaits C06b/C13.
The launcher stays catalog-only. Retain the qrc-only CSP at `idle/index.html:5`;
static dependency closure is not runtime proof or Android compatibility.
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
not ambient future sources. JDK 25 is required for the daemon; JDK 26 launcher
compatibility is historical. SDK platform 37 / Build Tools 36.0.0 are required.
Historically, C03's four checks passed: eight fresh host tests (seven functional),
no failures, errors or skips and lint 0 errors/34 warnings.
C04's four checks also passed;
eight fresh tests (seven functional) are not asset coverage. Separate checks match
the three originals against committed upstream, catalog and production APK bytes.
Keep inherited `OFL.txt:21` whitespace: C04's staged-change check exited 2.
C04 evidence is historical. Historical C05: four checks passed: eight fresh tests (seven
functional), no failures/errors/skips; lint 0 errors/34 warnings. Separate byte
checks match all 14 originals against committed upstream, catalog and APK.
Non-writing I01 confirmed exact source/index identity after an initial `write-tree`;
prior Git metadata immutability is unknown. No source/staging mutation was observed.
The font OFL is not a JS license grant; vendor exceptions do not waive native caps.
C06a's four checks passed: eight fresh tests (seven functional), no failures,
errors or skips; lint 0 errors/34 warnings. Separate checks match all 20 originals
against committed upstream, catalog and APK; source/index unchanged. Shared Git
object mtimes changed later without byte changes; their writer remains unproven.
The full asset verifier requires all 56 files and first applies at C09.
Final UI documentation restoration remains C13/C14 work, not this slice.
The arithmetic example is not functional.
Build output and test APK assembly do not prove device behavior. Report failures
and skipped checks explicitly. Device launch, accessibility and lifecycle remain pending.
Public toolchain stability, upstream distribution licensing and remote CI are
unverified; do not publish on the strength of a local build.
