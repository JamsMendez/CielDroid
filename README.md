# CielDroid — Compose bootstrap

C02 provides an Android 15+ (`minSdk 35`) launcher and a text catalog of the
four original FULL CieLinux scenes: processing, explorer, idle and raphael.
C03 adds scene/settings domain models, preferences persistence and ViewModel
foundations. The launcher remains a standalone catalog: no user-facing settings
controls, scene selection or preview rendering is integrated. Wallpaper, overlay
and widget behavior remain out of scope.

C04 prepares three unmodified original shared resources under
`app/src/main/assets/scenes/shared/`: `js/alert-overlay.js`, `fonts/OFL.txt`
and `fonts/ArchivoBlack-Regular.ttf`. [The inventory](docs/source-assets.json)
catalogs all 56 originals / 940651 bytes; it is unchanged for C08b.
C08b byte checks match this inventory, committed upstream and production APK.
C05 adds the whole original idle Full/Mini component: 11 unchanged text files
(175960 bytes) under `app/src/main/assets/scenes/idle/`. Full and Mini share
these files; Full is default; query or hash `variant=mini` selects Mini;
query `fps=30` selects 30 fps, otherwise 60. The canvas
“Idle mini” label does not make this a Mini-only component.
C06b completes the whole unchanged Explorer component under
`app/src/main/assets/scenes/explorer/`: 12 text files / 209240 bytes / 3665 lines.
It adds `index.html`, `styles.css`, `js/animate.js`, `js/main.js`,
`js/render-loop.js` and `js/see-through-hook.js` (57160 bytes / 1074 lines)
to C06a's six model/effects JS files. Full/Mini share these files, not a Mini-only
canvas: Full is default; query or hash `variant=mini` selects Mini; `fps=30`
selects 30 fps, otherwise 60. Static closure covers ten JS files, CSS and existing
shared overlay/font, with no observed idle dependency; it is not dynamic proof.
C07 adds the whole original processing Full/Mini component under
`app/src/main/assets/scenes/processing/`: 12 text files / 153772 bytes / 3402 lines.
Full is default; query or hash `variant=mini` selects Mini in the same files;
query `fps=30` selects 30 fps, otherwise 60. The “Processing mini” canvas label
is not a Mini-only import. Static closure covers ten JS files, CSS and the shared
overlay/font; it is not execution, GPU proof or Android compatibility.
C07 imported 38 originals / 687578 bytes (37 text, one TTF).
C08a prepares ten whole original JS files under `app/src/main/assets/scenes/raphael/`:
`js/{config,math,hexadecagon,sphere,scene-data,feathers,glyphs,glyph-rings,digits,central-core}.js`
(122670 bytes / 1940 newline-counted vendor lines).
C08b adds eight whole original rendering/entry files: `index.html`, `styles.css`,
`js/{nebula,sprites,layers,see-through-hook,render-loop,main}.js`
(130403 bytes / 2732 vendor lines), completing Raphael's 18 text files /
253073 bytes / 4672 lines. The cumulative candidate has all 56 originals /
940651 bytes (55 text, one TTF): complete asset inventory, not a complete app.
This is the Raphael scene, not the external Raphaël library. Full/Mini share files;
Full is default; query or hash `variant=mini` selects Mini; query `fps=30`
selects 30 fps, otherwise 60. Static closure covers 16 JS files, CSS and shared
overlay/font; it is not execution, Android compatibility or GPU/visual proof.
Preserve original bytes, CielWin headers, ordering and seeded RNG; runtime awaits C13.
The launcher remains catalog-only: no controls, runtime or preview integration.
The JavaScript's CielWin provenance header is preserved. The font's SIL OFL does
not license the JavaScript; the vendor-component size exception applies only to
these natural components, not native-code budgets or public JavaScript licensing.
Original qrc-only CSPs remain at `idle/index.html:5`, `explorer/index.html:5`
and `processing/index.html:5`, plus `raphael/index.html:5`; import does not establish
Android rendering compatibility. Idle's static references cover all nine JS files, CSS, shared alert
overlay and Archivo Black/OFL; this bounded dependency
closure is not dynamic runtime proof.

C10 imports secure URL policy, a literal 56-path allowlist, served-only CSP adaptation
and generation/source/both-marker session readiness foundations, with five test methods.
Full remains implicit: no Mini selector; default settings are processing at 30 fps.
The launcher is still catalog-only, with no UI controls or runtime integration.
URL policy alone does not enforce GET, no-network fallback, no JavaScript interface
or WebView lifecycle: those remain future adapter responsibilities. Readiness markers
are not pixels/GPU proof. Original asset qrc CSPs, headers and license bytes stay intact.

C11 imports the unchanged owner controller and three host test methods (137 Kotlin
lines). It coordinates visibility, resumed-loading timeout and generation-owned cleanup;
main-thread ownership is a caller precondition, not enforced. Tests do not assert
visibility callback ordering. Independent full C11 verification passed all three
controller methods within 16 fresh host tests (15 functional, one arithmetic), with
zero failures/errors/skips. The qualified verification record below retains I05's
failed final strict audit. Native consent, final identity and authorized local commit
remain pending; C12's adapter and C13's UI are not integrated. Readiness is not pixels/GPU proof.

## Build and verification

Use the checked-in wrapper and installed SDK package `platforms;android-37.0` / Build Tools 36.0.0.
The daemon requires JDK 25; JDK 26 launcher compatibility is historical evidence.
Gradle 9.6.0 / AGP 9.4.1 are pinned. AGP uses built-in Kotlin 2.2.10; the
Compose compiler matches Kotlin, independently of the Compose BOM.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
```

Historical C03 evidence only: the four isolated checks passed, including eight
freshly executed host tests: seven functional domain/repository/ViewModel tests
and one arithmetic example.
There were no test failures, errors or skips; lint reported 0 errors/34 warnings.
The arithmetic example is not functional coverage. Later untracked source,
tests and assets are preserved, but are not evidence for C03. Test APK compilation
is not execution. With an authorized API 35+ device, run
`./gradlew :app:connectedDebugAndroidTest` and check launch, light/dark themes,
large fonts, insets, scrolling, rotation and lifecycle. No device/AVD acceptance
has been established; visual/GPU/WebGL, cadence, accessibility and lifecycle
acceptance remain pending.

Historical C04 only: isolated provenance, packaging and four checks passed: eight fresh
host tests (seven functional), no failures/errors/skips; lint 0 errors/34 warnings.
The tests are not asset coverage: separate byte checks match the three originals
against committed upstream, the catalog and the production APK. `OFL.txt:21`
retains upstream whitespace; C04's staged-change whitespace check exited 2.
Historical C05: provenance, packaging and four checks passed: eight fresh host tests
(seven functional), no failures/errors/skips; lint 0 errors/34 warnings.
All 14 originals match the catalog, committed upstream and production APK bytes;
these byte checks are separate from host tests, which are not asset coverage.
The non-writing I01 check confirmed exact tested source/index identity after the
verifier's initial `git write-tree` call. Earlier Git metadata immutability is
unverified; no source/staging mutation was observed. That check reran no builds.
Historical C06a: four isolated checks passed: eight fresh host tests (seven functional),
no failures/errors/skips; lint 0 errors/34 warnings. Separate checks match all
20 originals against committed upstream, catalog and APK; source/index unchanged.
Later, 117 common Git objects had mtime-only changes with equal SHA/size;
the writer is unknown. This is not a globally immutable metadata history.
Historical C06b: four isolated checks passed: eight fresh host tests (seven functional),
no failures/errors/skips; lint 0 errors/34 warnings. Separate checks match all
26 originals against committed upstream, catalog and APK; source/index unchanged.
Historical C07: four separate offline wrapper checks exited 0: assemble 36 tasks executed,
forced host tests 24, lint 9 executed/18 up-to-date, test APK 30 executed/20 up-to-date.
Eight fresh host tests (seven functional) had no failures/errors/skips;
lint reported 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Separate byte checks match all 38 originals / 687578 bytes against the catalog,
committed upstream and production APK; test APK assets: zero. Host tests are not
asset coverage. Production/test manifest boundaries were attested: production
min35/target37, sole MainActivity launcher, no services or Internet/overlay/wallpaper
permissions; AndroidX app-local signature permission/provider/receiver remain.
The separate test manifest uses AndroidJUnitRunner and REORDER_TASKS.
Four actual JDK 25 daemon requests were attested, distinct from the JDK 26 launcher.
Test APK compilation is not device execution; preDebugAndroidTestBuild SKIPPED
and NO-SOURCE tasks are not JUnit skips. The strict final metadata audit exited 1:
only the common `.git` root directory had mtime-only drift; writer unknown.
All 88 candidate source/index entries, HEAD, 14-path staged status and pure tree
were unchanged, as were metadata-file bytes/size/mtime/mode and linked metadata.
A transient lock is only a possible mechanism, not proven causality. Human accepted
qualified I02 continuation; passing checks stand. Native review and local commit
closed at `e51ad24`. The final C07 audit observed zero current metadata drift;
it does not erase the failed I02 strict audit or establish globally immutable history.
Historical C08a: four separate offline wrapper checks exited 0: assemble 36 tasks executed,
forced host tests 24, lint 9 executed/18 up-to-date, test APK 30 executed/20 up-to-date.
Eight fresh host tests (seven functional, one arithmetic) had no failures/errors/skips;
lint reported 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Separate catalog, committed-upstream and production-APK byte checks match all
48 originals / 810248 bytes, including ten whole Raphael JS files / 122670 bytes /
1940 newlines; test APK assets: zero. The eight future Raphael rendering/entry files /
130403 bytes were absent in the isolated candidate only. Host tests are not asset coverage.
Binary manifests attest production min35/target37, sole MainActivity launcher and no
services or Internet/overlay/wallpaper permissions; AndroidX app-local signature
permission, startup provider and DUMP-protected receiver mean it is not permission-free.
The separate test manifest uses AndroidJUnitRunner and REORDER_TASKS. Four actual
JDK 25 daemon requests were attested, distinct from the JDK 26 launcher. Test APK
compilation is not device/instrumentation execution; preDebugAndroidTestBuild SKIPPED
and NO-SOURCE tasks are not JUnit skips. The strict current audit exited 0: all 98
source/index entries, HEAD, staged status and pure tree were unchanged, with zero
current drift in common/linked metadata files and directories. Outputs were confined
to ignored build/cache roots; pre-existing `.atl` was unchanged. This current pass does
not erase C05's unknown pre-write-tree history, C06a's unknown object-mtime writer or
I02's failed historical directory-mtime gate and human-qualified continuation.
C08a functional checks, provenance and attestation passed; native review/local commit
closed at `6e1f6f0`. This does not approve C08b.
Historical C08b: four separate isolated offline wrapper checks exited 0: assemble 36 tasks executed,
forced host tests 24, lint 9 executed/18 up-to-date, test APK 30 executed/20 up-to-date.
Eight fresh host tests (seven functional, one arithmetic) had no failures/errors/skips;
lint reported 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Separate catalog, committed-upstream and production-APK byte checks match exactly
56 originals / 940651 bytes (55 text, one TTF), with no extra assets; test APK assets: zero.
Host tests are not asset coverage. Binary manifests attest `com.jamsmendez.cieldroid`,
min35/target37, sole MainActivity launcher, no services or Internet/overlay/wallpaper
permissions; AndroidX signature permission, startup provider and DUMP-protected receiver
mean production is not permission-free. Test package/target, AndroidJUnitRunner and
REORDER_TASKS are separate. Four actual JDK 25 daemon requests were attested,
distinct from the JDK 26 launcher. Test APK compilation is not device execution;
preDebugAndroidTestBuild was SKIPPED in lint/test-APK checks; NO-SOURCE tasks are not
JUnit skips. Strict current audit exited 0: all 106 source/index entries, HEAD, staged
status and pure tree unchanged; 674 common and nine linked metadata files/directories,
including roots and Git pointer, had zero current drift. Only authorized ignored
build/cache outputs changed; pre-existing `.atl` was unchanged. Working-root/app-directory
mtime/size changes from cache creation are not source/Git metadata drift, nor proof of
filesystem-wide immutability. Historical C05/C06a/I02 qualifications above still apply.
C08b final identity verification and native approval/exact acknowledgement (burned)
closed in local commit `9fc80fb`; this does not approve C09.
Component import alone proves neither Full visual behavior nor GPU compatibility.
C09 includes the unchanged [asset verifier](tools/verify_scene_assets.py),
[source audit](docs/source-audit.md) and [CI workflow](.github/workflows/android.yml).
C09's first isolated validators (default and `--source`) exited 0: 56 files /
940651 bytes / 57 references; source mode proves checkout equality, not a revision.
Separate catalog/committed-upstream/production-APK checks match all 56 originals;
test APK assets: zero. All 13 characterization cases and four offline wrapper
checks passed: eight fresh host tests (seven functional), no failures/errors/skips;
lint 0 errors/24 warnings. Binary manifests and four JDK 25 daemon requests were
attested; production is not permission-free, and test compilation is not execution.
I03's strict audit exited 1 solely for common `.git` root directory mtime drift;
writer unknown. All 109 source/index entries, HEAD, staged status, pure tree and
other Git metadata were unchanged; outputs were only ignored build/cache files.
Human accepted candidate-specific qualified continuation, not a retroactive pass
or blanket waiver. Final input verification and native approval/exact acknowledgement
(burned) closed in local commit `195440b`; the failed I03 gate and unknown writer remain.
C10 controlled RED compiled successfully, then failed its sole fresh CSP test
with ComparisonFailure (expected self, actual qrc) at SceneCoreTest.kt:20; strict audit passed.
The fresh focused GREEN rerun passed one test, zero failures/errors/skips, including
four scenes and malformed-CSP rejection assertions, not the full suite. Independent
C10 verification then ran four separate offline wrapper checks, all exit 0: assemble
18 executed/18 up-to-date, forced host tests 24 executed, lint 9 executed/18 up-to-date,
test APK 30 executed/20 up-to-date. Fresh host execution passed 13 tests (12 functional,
one arithmetic), including all five core methods, with zero failures/errors/skips.
Lint reported 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Both asset validators exited 0. Separate byte checks match all 56 originals / 940651
bytes against the catalog, actual committed upstream `a31faf9` and production APK,
with no extras; test APK assets: zero. Host tests are not asset or GPU proof.
Binary manifests attest production `com.jamsmendez.cieldroid`, min35/target37, sole
MainActivity launcher, no services or Internet/overlay/wallpaper permissions.
AndroidX signature permission, startup provider and DUMP-protected receiver mean
production is not permission-free; test `com.jamsmendez.cieldroid.test` targets the
production package with AndroidJUnitRunner and REORDER_TASKS (min35/target37).
Four actual JDK 25 daemon request/context pairs were attested, distinct from launcher
26.0.2.1. Test APK compilation is not device execution; preDebugAndroidTestBuild
SKIPPED in lint/test-APK and NO-SOURCE tasks are not JUnit skips.
Initial strict audit passed; FINAL strict audit/acceptance FAILED exit 1: common `.git`
root DIRECTORY and three existing object FILE mtimes only (objects `5e9ed1…`,
`7f61df…`, `c7222b…`); file SHA and all size/mode/nlink values equal, writer UNKNOWN.
All 114 candidate source/index entries, HEAD, seven staged paths, pure tree, raw indexes,
future 11, six preservation backups and pre-existing `.atl` were unchanged. Parent-owned
main-ledger interim edits were outside the candidate, not Git-writer attribution.
Human selected `continue_c10_qualified_root_and_object_mtimes`: ONLY this C10 qualified
continuation, not retroactive PASS, blanket future waiver, repairs, passing-check reruns
or publishing. C05/C06a/I02/I03 historical qualifications remain; the final failed gate
is preserved. C10 final input verification, native approval and exact acknowledgement
(burned) closed in local commit `1352f49`; device acceptance remains pending.
No UI/runtime integration is proved, and I04 grants no new candidate waiver.
Final UI documentation restoration belongs to C13/C14, not this slice.

C11 independent verification ran four separate offline wrapper checks once each, all
exit 0: assemble 18 executed/18 up-to-date, forced host tests 24 executed, lint 9
executed/18 up-to-date, test APK 30 executed/20 up-to-date. All three controller and
five core methods passed within 16 fresh tests (15 functional, one arithmetic), with
zero failures/errors/skips. Lint: 0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt).
Both validators exited 0; separate checks match exactly 56 originals / 940651 bytes
against the catalog, actual committed upstream `a31faf9` and production APK, with no
extras; test APK assets: zero. Four original qrc CSPs at line 5 and OFL.txt:21 remain intact.
Binary manifests attest `com.jamsmendez.cieldroid`, min35/target37, sole MainActivity
launcher, no services or Internet/overlay/wallpaper permissions. AndroidX signature
permission/startup provider/DUMP-protected receiver mean production is not permission-free;
test AndroidJUnitRunner and REORDER_TASKS remain separate. Four actual daemon-25
request/context pairs (PID 372902): 25381/25398, 25433/25450, 25487/25504, 25539/25556;
release 25.0.3 differs from launcher 26.0.2.1. Test APK compilation is not device execution;
preDebugAndroidTestBuild was SKIPPED in lint/test-APK; NO-SOURCE is not a JUnit skip.
Initial strict audit passed; FINAL strict audit/acceptance FAILED exit 1: common `.git`
root DIRECTORY and three existing object FILE mtimes only, equal file SHA and all
size/mode/nlink values, writer UNKNOWN. Both 116-entry candidate source/index sets,
HEAD, four staged paths, pure tree, raw indexes, future nine, three preservation and
six prior backups, ledger, local.properties, `.atl` and nine linked metadata entries
were unchanged. Parent paused ledger/source/Git writes throughout, not writer attribution.
Human selected `continue_c11_qualified_root_and_object_mtimes`: ONLY this C11 qualified
continuation, preserving passes and FAILED/UNKNOWN history; no inherited I04 waiver,
retroactive PASS, future waiver, repairs, passing-check reruns or publishing.
C05/C06a/I02/I03/I04 qualifications remain; a later current PASS/native approval cannot
erase I05. Native current consent, final identity and authorized local commit remain pending.
Saved isolated `build/verification/c11-independent-{acceptance,proof,provenance,manifests}.json`
and logs/exits hold the evidence. Host tests/readiness are not asset/pixels/GPU proof;
device, visual/GPU/WebGL, cadence, accessibility and runtime lifecycle gates remain pending.

## Scope and distribution limits

The application ID is `com.jamsmendez.cieldroid`; compile/target SDK are 37.
The source manifest requests no permissions or services. Public stability and
publishing eligibility of the exact AGP/API versions remain unverified, as does
distribution licensing for upstream scene JavaScript. No remote CI execution
is established. CI pins four actions to researched immutable commits, retains JDK 25
and the wrapper, and compiles the example instrumentation APK without device execution.
Google's published stable-channel SDK metadata confirms `android-37.0` revision 2
and Build Tools 36.0.0; this is not remote execution or AGP compatibility proof.
The Ubuntu 24.04 image still floats; Java/SDK/Gradle inputs are not fully immutable.
Retained setup-java v4 is deprecated: a maintenance risk, not an authorized major upgrade.
Do not infer publication readiness from local builds. See [contributor constraints](AGENTS.md).
