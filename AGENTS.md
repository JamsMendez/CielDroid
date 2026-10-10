# CielDroid bootstrap constraints

C03 adds settings/domain, preferences persistence and ViewModel foundations to
C02's Compose launcher and scene-name catalog. User-facing settings controls,
preview rendering, overlay, wallpaper and widget integration remain out of scope.
C04 added three original shared resources; C05 adds the whole unchanged idle
Full/Mini component (11 text files, 175960 bytes), not a Mini-only scene.
Full is default; query or hash `variant=mini` selects Mini in the same files;
query `fps=30` selects 30 fps, otherwise 60.
C06b completes Explorer: 12 unchanged text files / 209240 bytes / 3665 lines.
Six entry/style/driver files (57160 bytes / 1074 lines), listed in README, join
C06a's six model/effects JS files. Full/Mini share files; Full is default, query
or hash `variant=mini` selects Mini; `fps=30` selects 30 fps, otherwise 60.
Preserve CielWin headers and Explorer's original qrc-only CSP (`index.html:5`).
C07 adds the whole processing Full/Mini component: 12 unchanged text files /
153772 bytes / 3402 lines. Full is default; query or hash `variant=mini` selects
Mini in the same files; query `fps=30` selects 30 fps, otherwise 60.
Preserve CielWin headers and processing's original qrc-only CSP (`index.html:5`).
C07 imported 38 originals / 687578 bytes (37 text, one TTF).
C08a prepares ten whole original Raphael JS files (listed in README):
122670 bytes / 1940 newline-counted vendor lines. C08b adds eight whole originals
(listed in README), 130403 bytes / 2732 lines, completing Raphael's 18 text files /
253073 bytes / 4672 lines. Cumulative candidate: 56 originals / 940651 bytes
(55 text, one TTF), complete asset inventory, not a complete app/runtime integration.
C08b byte checks match the unchanged catalog, committed upstream and production APK.
This is the scene, not the external Raphaël library. Full/Mini share files; Full is
default; query or hash `variant=mini` selects Mini; query `fps=30` selects 30 fps,
otherwise 60. Preserve Raphael's original qrc-only CSP (`index.html:5`).
Its static 16-JS/CSS/shared-overlay/font closure is not execution or Android/GPU proof.
Preserve original bytes, CielWin headers, ordering and seeded RNG.
Processing's static ten-JS/CSS/shared-overlay/font closure is not execution,
GPU proof or Android compatibility; runtime integration awaits C13.
The launcher stays catalog-only. Retain the qrc-only CSP at `idle/index.html:5`;
static dependency closure is not runtime proof or Android compatibility.
Future untracked files must survive; they are not verification for this slice.

C10 adds secure URL policy/allowlist, served-only CSP and session readiness foundations
with five test methods, not UI controls or runtime integration. Full is implicit,
without a Mini selector; settings default to processing at 30 fps. URL policy does
not enforce GET, no-network fallback, no JavaScript interface or WebView lifecycle;
the future adapter must enforce these. Exact generation/source/both-marker readiness
is not pixels/GPU proof. Keep original asset qrc CSPs, headers and license bytes intact.

C11 imports the unchanged owner controller and three test methods: 137 Kotlin lines.
Resumed-loading timeout and cleanup are foundations, not WebView/UI integration.
Main-thread ownership is a caller precondition; visibility callback order is not
asserted by these tests. Independent full C11 verification passed all three controller
methods: 16 fresh host tests (15 functional), zero failures/errors/skips; four offline
checks passed. README records provenance, manifests, daemon evidence and I05 qualification.
Initial strict audit passed; FINAL strict audit/acceptance FAILED exit 1 for common root
DIRECTORY and three object FILE mtimes only, equal SHA/size/mode/nlink, UNKNOWN writer.
Human accepted ONLY C11 qualified continuation; no retroactive PASS or future waiver.
Native consent/final identity/local commit remain pending, as do C12 adapter/C13 UI;
the launcher stays catalog-only and readiness is not pixels/GPU proof. Device gates remain pending.

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
compatibility is historical. SDK package `platforms;android-37.0` / Build Tools 36.0.0 are required.
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
Historical C06a: four checks passed: eight fresh tests (seven functional), no failures,
errors or skips; lint 0 errors/34 warnings. Separate checks match all 20 originals
against committed upstream, catalog and APK; source/index unchanged. Later, 117
common Git objects had mtime-only changes with equal SHA/size; writer unknown.
Do not claim globally immutable Git metadata history.
Historical C06b: four checks passed: eight fresh tests (seven functional), no failures,
errors or skips; lint 0 errors/34 warnings. Separate checks match all 26 originals
against committed upstream, catalog and APK; source/index unchanged.
Historical C07: four separate offline wrapper checks exited 0; task counts and attested
production/test manifest boundaries are recorded in README. Eight fresh host tests
(seven functional) had no failures/errors/skips; lint 0 errors/24 warnings
(23 UnusedResources, one ObsoleteSdkInt), distinct from historical counts above.
Separate catalog/committed-upstream/production-APK byte checks match all 38 originals /
687578 bytes; test APK assets: zero. Host tests are not asset coverage. Four actual
JDK 25 daemon requests were attested, distinct from the JDK 26 launcher.
Test APK compilation is not device execution; preDebugAndroidTestBuild SKIPPED
and NO-SOURCE tasks are not JUnit skips. Strict final metadata audit exited 1 solely
for common `.git` root directory mtime-only drift; writer unknown, transient lock
only a possible mechanism. All 88 source/index entries, HEAD, 14-path staged status,
pure tree, metadata-file bytes/size/mtime/mode and linked metadata were unchanged.
Human accepted qualified I02 continuation; passes stand. Native review/local commit
closed at e51ad24. Final C07 audit observed zero current metadata drift, not globally
immutable history; the failed I02 strict audit and unknown writer remain qualified.
Historical C08a: four separate offline wrapper checks exited 0; task counts and binary manifest
boundaries are recorded in README. Eight fresh host tests (seven functional, one
arithmetic) had no failures/errors/skips; lint 0 errors/24 warnings (23 UnusedResources,
one ObsoleteSdkInt). Separate catalog/committed-upstream/production-APK byte checks
match all 48 originals / 810248 bytes; test APK assets: zero, not host asset coverage.
Eight future Raphael rendering/entry files / 130403 bytes were absent only isolated.
Four actual JDK 25 daemon requests were attested, distinct from the JDK 26 launcher.
AndroidX signature permission/startup provider/DUMP-protected receiver mean production
is not permission-free; test AndroidJUnitRunner/REORDER_TASKS remain separate.
Strict current audit exited 0: all 98 source/index entries, HEAD, staged status and pure
tree unchanged; common/linked metadata files and directories had zero current drift.
Only ignored build/cache outputs changed; pre-existing `.atl` was unchanged. This does
not erase C05's unknown pre-write-tree history, C06a's unknown object-mtime writer or
I02's failed historical directory-mtime gate and human-qualified continuation.
C08a functional checks, provenance and attestation passed; native review/local commit
closed at 6e1f6f0, not C08b approval.
Historical C08b: four separate isolated offline wrapper checks exited 0: assemble 36 tasks executed,
forced host tests 24, lint 9 executed/18 up-to-date, test APK 30 executed/20 up-to-date.
Eight fresh host tests (seven functional, one arithmetic) had no failures/errors/skips;
lint 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Separate catalog/committed-upstream/production-APK byte checks match exactly all
56 originals / 940651 bytes (55 text, one TTF), no extras; test APK assets: zero.
Host tests are not asset coverage. Binary manifests attest `com.jamsmendez.cieldroid`,
min35/target37, sole MainActivity launcher, no services or Internet/overlay/wallpaper
permissions. AndroidX signature permission/startup provider/DUMP-protected receiver
mean production is not permission-free; test package/target, AndroidJUnitRunner and
REORDER_TASKS remain separate. Four actual JDK 25 daemon requests were attested,
distinct from the JDK 26 launcher. Test APK compilation is not device execution;
preDebugAndroidTestBuild was SKIPPED in lint/test-APK checks; NO-SOURCE is not a JUnit skip.
Strict current audit exited 0: all 106 source/index entries, HEAD, staged status and
pure tree unchanged; 674 common and nine linked metadata files/directories, including
roots and Git pointer, had zero current drift. Only authorized ignored build/cache
outputs changed; pre-existing `.atl` was unchanged. Working-root/app-directory mtime/size
changes from cache creation are not source/Git metadata drift or filesystem-wide
immutability. Historical C05/C06a/I02 qualifications above remain valid.
C08b final identity verification/native approval/exact acknowledgement (burned) and
local commit closed at 9fc80fb, not C09 approval.
Whole-component import is not Full visual/GPU proof; no runtime controls are added.
C09 includes the unchanged [verifier](tools/verify_scene_assets.py),
[audit](docs/source-audit.md) and [CI](.github/workflows/android.yml). Both first isolated
validators, separate committed-upstream/catalog/APK provenance, 13 characterization
cases and four offline wrapper checks passed; manifests and four JDK 25 requests
were attested. Eight fresh host tests (seven functional) had no failures/errors/skips;
lint 0 errors/24 warnings. I03 strict audit exited 1 solely for common `.git` root
directory mtime drift, writer unknown; 109 source/index entries and other Git metadata
were unchanged. Human accepted candidate-specific qualified continuation, not a gate
pass or blanket waiver. Final input verification/native approval/exact acknowledgement
(burned) and local commit closed at 195440b; I03 remains a failed historical gate.
C10 controlled RED compiled successfully, then failed its sole fresh CSP test
with ComparisonFailure (expected self, actual qrc) at SceneCoreTest.kt:20; strict audit passed.
The fresh focused GREEN rerun passed one test, zero failures/errors/skips, including
four scenes and malformed-CSP rejection assertions, not the full suite. Independent
full C10 verification subsequently passed four offline checks, 13 fresh host tests
(12 functional, one arithmetic; all five core methods; zero failures/errors/skips),
both validators and exact 56-original/940651-byte catalog/committed-upstream/APK checks.
README records task counts, lint 0 errors/24 warnings, manifests and four daemon-25 contexts.
Initial strict audit passed, but FINAL strict audit/acceptance FAILED exit 1: common
`.git` root DIRECTORY plus three existing object FILE mtimes only, equal file SHA and
all size/mode/nlink values; writer UNKNOWN. Candidate 114 entries/index/HEAD/staged seven/
pure tree/raw indexes/future 11/backups six/`.atl` were unchanged; parent-owned main-ledger
edits outside the candidate are not Git attribution. Human accepted ONLY C10 qualified
continuation (`continue_c10_qualified_root_and_object_mtimes`), not retroactive PASS,
blanket waiver, repairs, passing-check reruns or publishing. C05/C06a/I02/I03 remain
qualified. Final input verification/native approval/exact acknowledgement (burned) and local
commit closed at `1352f49`; device acceptance remains pending. I04 grants no new waiver;
host tests and readiness are not asset/pixels/GPU proof.
Final UI documentation restoration remains C13/C14 work, not this slice.
The arithmetic example is not functional.
Build output and test APK assembly do not prove device behavior. Report failures
and skipped checks explicitly. Device, visual/GPU/WebGL, cadence, accessibility
and lifecycle acceptance remain pending.
Public toolchain stability, upstream distribution licensing and remote CI are
unverified; do not publish on the strength of a local build.
