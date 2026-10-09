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
catalogs the intended complete 56-file / 940651-byte set, not the C06b subset.
C05 adds the whole original idle Full/Mini component: 11 unchanged text files
(175960 bytes) under `app/src/main/assets/scenes/idle/`. Full and Mini share
these files; Full is the default and `?variant=mini` selects Mini. The canvas
“Idle mini” label does not make this a Mini-only component.
C06b completes the whole unchanged Explorer component under
`app/src/main/assets/scenes/explorer/`: 12 text files / 209240 bytes / 3665 lines.
It adds `index.html`, `styles.css`, `js/animate.js`, `js/main.js`,
`js/render-loop.js` and `js/see-through-hook.js` (57160 bytes / 1074 lines)
to C06a's six model/effects JS files. Full/Mini share these files, not a Mini-only
canvas: Full is default; query or hash `variant=mini` selects Mini; `fps=30`
selects 30 fps, otherwise 60. Static closure covers ten JS files, CSS and existing
shared overlay/font, with no observed idle dependency; it is not dynamic proof.
The candidate is 26 originals / 533806 bytes (25 text, one TTF);
30 processing/raphael files / 406845 bytes remain out. Entry/driver closure is
complete, but JS is not executed; Android runtime integration awaits C13.
The launcher remains catalog-only: no controls, runtime or preview integration.
The JavaScript's CielWin provenance header is preserved. The font's SIL OFL does
not license the JavaScript; the vendor-component size exception applies only to
these natural components, not native-code budgets or public JavaScript licensing.
Original qrc-only CSPs (`idle/index.html:5`, `explorer/index.html:5`) remain; import does not
establish Android rendering compatibility. Idle's static references cover all nine JS
files, CSS, shared alert overlay and Archivo Black/OFL; this bounded dependency
closure is not dynamic runtime proof.

## Build and verification

Use the checked-in wrapper and installed SDK platform 37 / Build Tools 36.0.0.
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
has been established; GPU, accessibility and lifecycle acceptance remain pending.

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
Shared Git object mtimes changed later without byte changes; writer is unproven.
C06b's four isolated checks passed: eight fresh host tests (seven functional),
no failures/errors/skips; lint 0 errors/34 warnings. Separate checks match all
26 originals against committed upstream, catalog and APK; source/index unchanged.
Component import alone proves neither Full visual behavior nor GPU compatibility.
`tools/verify_scene_assets.py` requires all 56 assets and first applies at C09.
Final UI documentation restoration belongs to C13/C14, not this slice.

## Scope and distribution limits

The application ID is `com.jamsmendez.cieldroid`; compile/target SDK are 37.
The source manifest requests no permissions or services. Public stability and
publishing eligibility of the exact AGP/API versions remain unverified, as does
distribution licensing for upstream scene JavaScript. No remote CI execution
is established, and no CI workflow is included in this slice. Do not infer
publication readiness from local builds. See [contributor constraints](AGENTS.md).
