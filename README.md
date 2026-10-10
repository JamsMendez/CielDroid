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
At C08b the launcher was catalog-only: no controls, runtime or preview integration.
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
At C10 the launcher was still catalog-only, with no UI controls or runtime integration.
URL policy alone does not enforce GET, no-network fallback, no JavaScript interface
or WebView lifecycle: those are adapter responsibilities, compiled in C12. Readiness markers
are not pixels/GPU proof. Original asset qrc CSPs, headers and license bytes stay intact.

C11 imports the unchanged owner controller and three host test methods (137 Kotlin
lines). It coordinates visibility, resumed-loading timeout and generation-owned cleanup;
main-thread ownership is a caller precondition, not enforced. Tests do not assert
visibility callback ordering. Independent full C11 verification passed all three
controller methods within 16 fresh host tests (15 functional, one arithmetic), with
zero failures/errors/skips. The qualified verification record below retains I05's
failed final strict audit. Native approval/exact acknowledgement (burned) and local
commit closed at `fddae4c`; C13 later integrates the UI. Readiness is not pixels/GPU proof.

C12 imports the unchanged, compiled
[WebView adapter](app/src/main/java/com/jamsmendez/cieldroid/scenes/WebViewSceneRuntime.kt)
(175 Kotlin lines) for one owner generation. C13 integrates it into the launcher through
ScenePreview; C14 owns consolidated docs and device gates. `androidx.webkit`
1.13.0 was already pinned in C02; C12 adds no dependencies or permissions.

| Concern | What the code enforces |
|---------|------------------------|
| Loading | `WebViewAssetLoader` serves only `SceneAssets` paths; `index.html` gets the served CSP |
| Requests | Non-GET, non-policy or released requests get an empty 403/500; never network fallthrough |
| Navigation | Policy-rejected URLs are overridden; an unexpected main-frame start is stopped and failed |
| Errors | Policy-resource errors and TLS errors (cancelled) emit `Failure`; renderer loss fails and releases |
| Web features | File/content access, mixed content, DOM storage, database, cache and geolocation off |
| Chrome | Popups, permissions, geolocation, file chooser and JS dialogs are denied or cancelled |
| Bridge | No JavaScript interface; commands use `evaluateJavascript` of `SceneCommand` only |
| Lifecycle | Starts paused; per-view resume/pause, no global timers; idempotent generation-gated release |

Independent isolated C12 verification passed four offline wrapper checks (assemble 36
executed, forced host 24, lint 9/18 up-to-date, test APK 30/20 up-to-date): 16 fresh host
tests (15 functional, one arithmetic), zero failures/errors/skips; lint 0 errors/24 warnings
(23 UnusedResources, one ObsoleteSdkInt), none in the adapter; one Kotlin deprecation
warning for `databaseEnabled` (`WebViewSceneRuntime.kt:58`). Both validators and exact
56-original/940651-byte catalog/committed-upstream/APK checks passed; test APK assets: zero.
FINAL strict audit FAILED (I06) for the common `.git` root DIRECTORY mtime only, writer
UNKNOWN; human accepted ONLY C12 qualified continuation, not a retroactive PASS or waiver.
Native approval/exact acknowledgement (burned) and local commit closed at `7e205e8`.
No adapter test or instrumentation was executed; nothing is proven on device. Readiness is not pixels proof; GPU/WebGL, cadence and lifecycle gates remain pending.

C13 integrates the launcher with the preview: the restored
[launcher](app/src/main/java/com/jamsmendez/cieldroid/ui/CielDroidApp.kt) exposes
persisted scene/FPS selection, Pause/Resume and Retry, and keys one
[ScenePreview](app/src/main/java/com/jamsmendez/cieldroid/ui/ScenePreview.kt) per
generation. ScenePreview owns one C11 `PreviewController` and one C12
`WebViewSceneRuntime`, released on dispose. `BootstrapTest` replaces the template
`ExampleInstrumentedTest`; it and `ScenePreviewTest` are instrumentation tests that are
compiled only, with no device execution. Independent isolated C13 verification passed
four offline wrapper checks (assemble 36 executed, forced host 24, lint 9/18, test APK
30/20), 16 fresh host tests (15 functional) with zero failures/errors/skips and lint
0 errors/8 warnings (7 UnusedResources, 1 ObsoleteSdkInt), none in C13 files. The test
APK packages both tests; the production APK keeps 56 assets / 940651 bytes. Native
approval/exact acknowledgement (burned) and local commit closed at `1dab560`. Device, visual/GPU/WebGL,
cadence, lifecycle and accessibility acceptance remain pending; readiness is not pixels proof.

C14 consolidates the evidence documentation: [architecture](docs/architecture.md),
[permissions and lifecycle](docs/permissions-and-lifecycle.md),
[renderer decision](docs/renderer-decision.md) and [test plan](docs/test-plan.md),
audited against the committed C03–C13 code and the C13 check results. No device or adb
was available during C14; its device gates were executed in C15. Independent isolated C14 verification
passed four offline wrapper checks (assemble 36 executed, forced host 24, lint 9/18, test
APK 30/20), 16 fresh host tests with zero failures/errors/skips, lint 0 errors/8 warnings
and 56 assets / 940651 bytes; the comment-only `rules.keep` has no effect (minify off).
Native approval/exact acknowledgement (burned) and local commit closed at `42dc3b1`.

C15 device acceptance ran on a motorola moto g84 5G (Android 15 / API 35, Adreno 619 /
OpenGL ES 3.2) against committed tree `42dc3b1`. `connectedDebugAndroidTest --offline`
could not run (UTP `com.android.tools.utp:gradle-work-action:32.4.1` is not in the offline
Gradle cache), so offline-built APKs were installed with `adb install -r` and run through
`am instrument`: OK (3 tests) — `BootstrapTest.launcherShowsTruthfulPreviewAndFutureModeStatus`,
`ScenePreviewTest.retryReplacesOwnerAndRecreationRetainsPersistedSettings` and
`ScenePreviewTest.fourOriginalScenesReachBothReadinessMarkersAtBothRates`. Logcat showed no
app crash, CSP, Uncaught or WebGL errors. The human manual checklist (launch, scene switching,
FPS, pause/resume/retry, background/return, rotation, themes) was reported OK. Known issue,
deferred: scene element scale/sizing on the phone, consistent with the upstream CieLinux
desktop-sized layout (viewport `width=device-width,initial-scale=1`, no adapter zoom
overrides); original bytes are preserved. Readiness markers and human observation are not
automated visual/pixel, cadence or accessibility proof; accessibility, cadence measurement,
security audit, licensing, remote CI and publishing remain pending. C15 closed at
`c36b0dc` (native approved).

F01b fixes two C13 lifecycle advisories in `ScenePreview`. The owner observer now signals
`ownerResumed(resumed && inViewport)` in both directions immediately, so a pause/resume pair
coalesced before recomposition no longer leaves the preview paused; the effect applies pauses
before resumes, so a same-frame manual unpause plus viewport exit cannot transiently resume.
The viewport exposes `stateDescription` running/paused (semantics only, no new control).
`ScenePreviewLifecycleTest` drives a `LifecycleRegistry` owner: before the fix the coalesced
STARTED→RESUMED test failed its assertion (expected running, actual paused); after it, both
tests passed and full `am instrument` gave OK (5 tests) on the same API 35 phone, with no app
FATAL/Uncaught/CSP logcat lines. The viewport-exit ordering is reasoned, not separately tested.
Single device only; readiness and state semantics are not pixel, GPU or cadence proof.

F01d1 adds an accessibility semantics baseline (no visual redesign, no new controls). Section
titles are headings, scene chips share a `selectableGroup`, `preview-status` is a polite live
region, and the viewport exposes a resource-backed description (scene + FPS) and Running/Paused
state on a wrapper `Box`: the interop view replaces an `AndroidView`'s own platform node, so
semantics set there never reached TalkBack. The decorative WebView is
`IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS`. `AccessibilityTest` RED: 4/4 assertion failures
(missing heading, selectable group, live region, description); GREEN 4/4, full `am instrument`
OK (9 tests) on the API 35 phone, no app FATAL/Uncaught/CSP logcat. Gaps: no automated ATF
scan (needs a dependency), no manual TalkBack pass, no translations, no reduced-motion handling.

W01 adds an unmeasured live-wallpaper spike, not an adopted renderer. `SceneWallpaperService`
renders its engine surface through a private `VirtualDisplay` (`VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY`,
no permission) and a `Presentation` built from the service context, hosting one
`WebViewSceneRuntime` + `PreviewController` per generation (fixed idle at 30 fps, no configuration
UI). The `BIND_WALLPAPER` service and its `scene_wallpaper.xml` metadata are the only manifest
addition. In the system live-wallpaper preview on the API 35 phone (never applied), the scene
pixels were visible, readiness reached READY, visibility paused it, and closing the preview
destroyed the surface and engine and released the display, with no FATAL/CSP/Uncaught/WebGL
logcat. The preview activity stayed portrait, so resize/landscape is not yet exercised. Nothing
is measured (W02/W03 pending); single device only.

W04a hardens the engine and gives the wallpaper its own settings. Each generation is built under a
`Teardown` release stack, so a failure part-way through releases what already exists in reverse
order and leaves no generation. A surface size change resizes the existing `VirtualDisplay` and swaps
its surface only when the Surface object changed (`DisplayPlan.SwapSurface`); the Presentation and
WebView stay alive. `wallpaper_scene` / `wallpaper_fps` (default idle at 30 fps) are stored apart
from `preview_scene` / `preview_fps` in the same DataStore, exposed through the repository and
ViewModel without UI. The engine follows them and rebuilds its generation on change. FPS, energy and
landscape are still unmeasured (deferred by the user); single device; no wallpaper UI yet (W04b).

W04b adds a "Live wallpaper" section below the preview: its own scene and FPS chips (independent of
the preview chips) and a "Set as wallpaper" button. The button opens the system live wallpaper
preview (`ACTION_CHANGE_LIVE_WALLPAPER` for `SceneWallpaperService`); you confirm it there, and
CielDroid never applies a wallpaper itself. If no picker exists, an inline message is shown. A
recreated wallpaper surface is now detected from `onSurfaceCreated`, because the holder keeps one
`Surface` object. Honest limits: one measurement of the idle scene showed about 12.6 of 30 fps;
energy, landscape and processing at 60 fps are unmeasured; single device (moto g84).

W05 adds a home screen snapshot widget (`widget/SceneWidgetProvider`). It shows one still frame of
the live wallpaper scene/FPS, rendered off-screen by `SceneSnapshotRenderer` (ImageReader → private
`CielSnapshot` VirtualDisplay → Presentation → its own WebView), released in reverse order on every
path. It refreshes only when a widget is added or resized and when the wallpaper scene/FPS changes;
there are no periodic updates and it is not animated by design. The PNG (≤ 512 px, one per scene+fps)
is cached in app-internal `filesDir`. On timeout (≤ 8 s) or scene error it shows the app icon and
scene name; a tap opens CielDroid. You add the widget yourself from the launcher; single device.

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

`adb` lives at `$ANDROID_HOME/platform-tools/adb`. When the UTP runner is unavailable
offline, install both APKs with `adb install -r` and run
`adb shell am instrument -w -r com.jamsmendez.cieldroid.test/androidx.test.runner.AndroidJUnitRunner`.

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
erase I05. Native approval/exact acknowledgement (burned) and local commit closed at `fddae4c`.
Saved isolated `build/verification/c11-independent-{acceptance,proof,provenance,manifests}.json`
and logs/exits hold the evidence. Host tests/readiness are not asset/pixels/GPU proof;
device, visual/GPU/WebGL, cadence, accessibility and runtime lifecycle gates remain pending.

## Scope and distribution limits

The application ID is `com.jamsmendez.cieldroid`; compile/target SDK are 37.
The source manifest requests no permissions or services. Public stability and
publishing eligibility of the exact AGP/API versions remain unverified, as does
distribution licensing for upstream scene JavaScript. No remote CI execution
is established. CI pins four actions to researched immutable commits, retains JDK 25
and the wrapper, and compiles the instrumentation test APK without device execution.
Google's published stable-channel SDK metadata confirms `android-37.0` revision 2
and Build Tools 36.0.0; this is not remote execution or AGP compatibility proof.
The Ubuntu 24.04 image still floats; Java/SDK/Gradle inputs are not fully immutable.
Retained setup-java v4 is deprecated: a maintenance risk, not an authorized major upgrade.
Do not infer publication readiness from local builds. See [contributor constraints](AGENTS.md).
