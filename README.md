# CielDroid — Compose bootstrap

C02 provides an Android 15+ (`minSdk 35`) launcher and a text catalog of the
four original FULL CieLinux scenes: processing, explorer, idle and raphael.
C03 adds scene/settings domain models, preferences persistence and ViewModel
foundations. The launcher remains a standalone catalog: no user-facing settings
controls, scene selection or preview rendering is integrated. Wallpaper, overlay
and widget behavior remain out of scope.

## Build and verification

Use the checked-in wrapper and installed SDK platform 37 / Build Tools 36.0.0.
The daemon criterion requires JDK 25; a local JDK 26 launcher also works.
Gradle 9.6.0 / AGP 9.4.1 are pinned. AGP uses built-in Kotlin 2.2.10; the
Compose compiler matches Kotlin, independently of the Compose BOM.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
```

The four isolated C03 checks passed, including eight freshly executed host tests:
seven functional domain/repository/ViewModel tests and one arithmetic example.
There were no test failures, errors or skips; lint reported 0 errors/34 warnings.
The arithmetic example is not functional coverage. Later untracked source,
tests and assets are preserved, but are not evidence for C03. Test APK compilation
is not execution. With an authorized API 35+ device, run
`./gradlew :app:connectedDebugAndroidTest` and check launch, light/dark themes,
large fonts, insets, scrolling, rotation and lifecycle. No device/AVD acceptance
has been established.

## Scope and distribution limits

The application ID is `com.jamsmendez.cieldroid`; compile/target SDK are 37.
The source manifest requests no permissions or services. Public stability and
publishing eligibility of the exact AGP/API versions remain unverified, as does
distribution licensing for upstream scene JavaScript. No remote CI execution
is established, and no CI workflow is included in this slice. Do not infer
publication readiness from local builds. See [contributor constraints](AGENTS.md).
