# Verification evidence and pending device acceptance

Phase 0/1 code is complete; C15 ran instrumentation and a human manual checklist on one API 35 phone, but automated visual/pixel, cadence and accessibility acceptance remain pending. Host tests and test APK compilation cannot establish visual fidelity, WebGL support or lifecycle behavior on Android. Phase 2–4 modes are not implemented.

## Recorded evidence

| Boundary | Observed result / limitation |
| --- | --- |
| C15 device | motorola moto g84 5G, Android 15 / API 35, Adreno 619 / OpenGL ES 3.2; committed tree `42dc3b1`. `connectedDebugAndroidTest --offline` could not run (UTP `com.android.tools.utp:gradle-work-action:32.4.1` not in the offline Gradle cache); offline-built APKs installed with `adb install -r` and run via `adb shell am instrument -w -r com.jamsmendez.cieldroid.test/androidx.test.runner.AndroidJUnitRunner`: OK (3 tests: `BootstrapTest.launcherShowsTruthfulPreviewAndFutureModeStatus`, `ScenePreviewTest.retryReplacesOwnerAndRecreationRetainsPersistedSettings`, `ScenePreviewTest.fourOriginalScenesReachBothReadinessMarkersAtBothRates`). No app crash, CSP, Uncaught or WebGL logcat errors. Human manual checklist (launch, scene switching, FPS, pause/resume/retry, background/return, rotation, themes) reported OK. Known deferred issue: scene element scale/sizing on the phone, consistent with upstream desktop-sized layout (viewport `width=device-width,initial-scale=1`, no adapter zoom overrides); original bytes preserved. Readiness markers and human observation are not automated visual/pixel, cadence or accessibility proof. |
| C13 (C02–C13 chain) | Independent isolated verification: four offline checks exit 0 (assemble, forced host tests, lint, test APK); 16 fresh host tests (15 functional, one arithmetic), zero failures/errors/skips; production APK keeps 56 assets / 940651 bytes; test APK packages `BootstrapTest` and `ScenePreviewTest`, compiled only, never executed. T3/T3b/T4 rows below are pre-chain full-working-tree history. |
| T3 deterministic core | Writer observed compiled-stub RED: 9 tests, 6 assertion failures; GREEN: 12 tests. Independent verifier forced real execution: 12 tests, 11 functional, zero failures/skips. |
| T3b controller/ViewModel | Writer observed 4 new assertion failures among 16 tests, then GREEN: 16 tests. Independent `mv1duoki-c-j7ny` freshly executed 16 host tests: 15 functional plus the arithmetic example, zero failures/errors/skips. This is JUnit test count, not a scene/FPS parameter-case total. |
| Independent T3b checks | All five succeeded: `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`, `./gradlew :app:assembleDebugAndroidTest`, `python3 tools/verify_scene_assets.py`. Instrumentation compiled, not executed. |
| Packaging/assets | Actual production APK min35/target37, sole MainActivity, no services/Internet/overlay/wallpaper. All 56 packaged assets byte-equal; inventory totals 940651 bytes and 57 local references. |
| Diagnostics | Latest C14 lint (same as C13): 0 errors / 8 warnings (7 UnusedResources, 1 ObsoleteSdkInt); one Kotlin deprecation warning for `WebViewSceneRuntime` `databaseEnabled`. Historical T3b/T4 lint: 0 errors / 18 warnings: pinned-version notices, launcher qualifier and unused colors. Builds also reported unstripped native libraries, protobuf Unsafe and deprecated WebView database configuration warnings. The earlier invalid onNode import compile incident was corrected; it was not behavioral RED. |
| T4 ordering followup | Source-based risk, not an observed device failure: fresh views start paused. Retry and recreation now check replacement and retained Resume control, resume before awaiting both READY markers. Retry checks a fresh same-model generation; recreation checks actual WebView identity without cross-model monotonicity assumptions. No invented instrumentation RED/GREEN; structural checking and test APK compilation are the applicable exception. |

[ScenePreviewTest](../app/src/androidTest/java/com/jamsmendez/cieldroid/ScenePreviewTest.kt) retains all four FULL scenes at 30 and 60 FPS and retry/recreation coverage. [BootstrapTest](../app/src/androidTest/java/com/jamsmendez/cieldroid/BootstrapTest.kt) checks truthful UI. Readiness is initialization, not pixels. Host tests cover policy/CSP/session, persistence, stale events, timeout suspension, disposal and ViewModel storage failures.

T4 observed all four focused checks succeeding:

| Command | T4 execution evidence |
| --- | --- |
| `./gradlew :app:assembleDebugAndroidTest` | 50 actionable tasks: 4 executed, 46 up-to-date; changed instrumentation Kotlin compiled and test APK repackaged. |
| `./gradlew :app:testDebugUnitTest` | 24 actionable tasks, all up-to-date; no new host-test execution claimed. |
| `./gradlew :app:lintDebug` | 27 actionable tasks: 3 executed, 24 up-to-date; instrumentation model/analysis reran, report remained up-to-date with 0 errors / 18 warnings. |
| `python3 tools/verify_scene_assets.py` | Executed successfully: 56 files, 940651 bytes, 57 local references; SHA256 and destination inventory verified. |

T4-only historical result: structural checking resolved all 35 local Markdown links, including linked class paths, and confirmed resume-before-READY ordering plus retained four-scene/two-FPS loops. Production assembly and packaged-byte proof remain the independent T3b evidence, not a new T4 claim. C14 independent verification reported resolving 44 relative links in README.md, AGENTS.md and docs/*.md; the C15 docs re-check resolved all 43 relative links in the current tree.

## Device acceptance matrix

Use API 35 **and an available later API**, recording device/API, WebView and GPU versions. Historical through C14: no devices, AVDs or adb were available, and connected tests, installation, launch and manual checks were skipped. C15 covered one API 35 physical device (see Recorded evidence); a later API, screenshots, pixel comparison and runtime measurements remain pending.

| Area | Required scenarios / evidence |
| --- | --- |
| FULL visuals and rates | processing, explorer, idle, raphael × 30/60 (eight combinations per device). Compare original reference pixels/animation, font and layering; collect screenshots only from real runs. |
| Pause/timeout/retry | Visible versus scrolled-hidden, manual pause, retry while paused, hidden/manual-paused loading, visible 15-second timeout and fresh timeout window after return. Verify new owner, retained settings and persistent errors. |
| Activity lifecycle | Background/return, recreation, physical rotation/resize and repeated disposal. Manual pause survives recreation; no hidden-owner work or stale callbacks. |
| Security | Strict served CSP, manifest-only local HTTPS, denied external/unknown requests and exact trusted sourceIds for both markers. Reject forged readiness; real script failures persist. |
| GPU robustness | processing/raphael nebula pixels, highp support, unsupported WebGL, shader compile/link failure, context loss, silent blank output and renderer-process death/retry. Console READY alone is insufficient. |
| Accessibility/layout | Large fonts/display scaling, TalkBack labels/focus, light/dark themes, system insets, small/large viewports and scroll interaction. |
| Performance/resources | Delivered cadence/frame times at 30/60, CPU/GPU, memory growth through scene/rate/retry cycles, background cost and release returning resources to baseline. Wallpaper metrics need a separate phase 3 prototype. |

## Prepare a runtime later (instructions, not actions performed)

Only after authorization, use Android Studio **Device Manager → Create Virtual Device**, choose a hardware profile and API 35+ system image, then start that AVD. Include API 35 and a later available API in acceptance. Emulator GPU results do not replace physical-GPU fidelity testing. Alternatively, enable developer options and USB debugging on an API 35+ physical device, connect it, and approve that computer's debugging prompt; do not expose remote debugging or store credentials in the repository.

Optional CLI equivalents below assume SDK tools on PATH and a compatible authorized host. Downloading images and creating/launching an AVD are future actions, not part of T4 or C14. Select an available later API image separately for the second pass.

```sh
sdkmanager --list
sdkmanager "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n CielDroid_API35 -k "system-images;android-35;google_apis;x86_64"
emulator -list-avds
emulator -avd CielDroid_API35
adb devices -l
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jamsmendez.cieldroid/.MainActivity
./gradlew :app:connectedDebugAndroidTest
```

On non-x86 hosts choose a supported architecture in Device Manager rather than assuming the example image fits. Inspect connected test reports under `app/build/reports/androidTests/connected/`; record failures/skips and manual evidence separately. Never label test APK compilation as execution.

## Remaining delivery gates

C02–C15 are local commits on the feature branch (C14 closed at `42dc3b1`, C15 at `c36b0dc`). No staging/publishing/SDK installation or emulator creation/launch occurred in T4 or C14. CI is authored but not remotely executed. Distribution licensing and public stability/publishing eligibility of the exact SDK/AGP pins remain unresolved. See [source audit](source-audit.md), [renderer decision](renderer-decision.md), [lifecycle boundaries](permissions-and-lifecycle.md) and the local ODD ledger `odd/tasks/android-scene-preview.md` (untracked, not part of the repository tree).
