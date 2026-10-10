# Activity preview ownership and permission boundaries

The current preview has **no Internet permission, overlay permission or wallpaper service**. Future surfaces are not implemented. Build metadata is evidence about packaging, not running lifecycle behavior.

## Current package

[Source manifest](../app/src/main/AndroidManifest.xml) declares only MainActivity as the exported launcher, no permissions or services. Independent production APK manifest inspection (pre-chain T3b and C08b–C11, recorded in the [README](../README.md)) confirmed minSdk 35 / targetSdk 37, the sole activity and no Internet/overlay/wallpaper components. AndroidX contributes a signature-only `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, startup provider and DUMP-protected profile installer diagnostic receiver. These are not user-facing services or notification access. AndroidX instrumentation helper activities belong to the separate test APK, not production functionality.

Local HTTPS is an AssetLoader origin, not network use. [WebViewSceneRuntime](../app/src/main/java/com/jamsmendez/cieldroid/scenes/WebViewSceneRuntime.kt) denies unknown/non-GET requests without network fallthrough, file/content access, mixed content, downloads, popups, geolocation, file pickers and permission requests. [ScenePolicy](../app/src/main/java/com/jamsmendez/cieldroid/scenes/ScenePolicy.kt) and [SceneHtml](../app/src/main/java/com/jamsmendez/cieldroid/scenes/SceneHtml.kt) constrain the inventory and exact served-only CSP adaptation. Originals remain unchanged.

## Lifecycle source map

| Source | Implemented contract |
| --- | --- |
| [CielDroidApp](../app/src/main/java/com/jamsmendez/cieldroid/ui/CielDroidApp.kt) | Scene/FPS/retry key a new composition generation. Manual pause uses rememberSaveable for activity recreation; controls wait for loaded settings. |
| [ScenePreview](../app/src/main/java/com/jamsmendez/cieldroid/ui/ScenePreview.kt) | One controller/runtime pair per keyed owner. Combines RESUMED lifecycle, viewport intersection and manual pause; immediate background pause, observer removal and disposal. Real View sizing drives resize listeners. |
| [PreviewController](../app/src/main/java/com/jamsmendez/cieldroid/scenes/PreviewController.kt) | 15-second readiness timeout only while visible, resumed and unpaused in LOADING. Hiding cancels it; return starts a fresh window. Disposal is terminal/idempotent. |
| [SceneSession](../app/src/main/java/com/jamsmendez/cieldroid/scenes/SceneSession.kt) | Rejects stale generations and disposed callbacks. Requires both trusted draw/bridge markers; ERROR is persistent and cannot be cleared by late readiness. |
| [WebViewSceneRuntime](../app/src/main/java/com/jamsmendez/cieldroid/scenes/WebViewSceneRuntime.kt) | Fresh views start paused. Captures immutable generation; release removes queued callbacks, stops, pauses, detaches and destroys the view. Renderer death releases the view and reports error; explicit retry replaces owner. No global pauseTimers. |
| [PreviewViewModel](../app/src/main/java/com/jamsmendez/cieldroid/ui/PreviewViewModel.kt) / [PreferencesSettingsRepository](../app/src/main/java/com/jamsmendez/cieldroid/data/PreferencesSettingsRepository.kt) | Application-context singleton DataStore per file, ViewModel-owned observation; only preview_scene/preview_fps persisted. Atomic edits preserve other keys. Failed writes stay visible; read failure ends observation and needs repository recreation after storage repair. |

Generation numbers belong to a ViewModel, not a process-global identity contract. Recreation tests compare actual WebView instances instead of assuming monotonic IDs across different models. Rotation/settings retention, pause behavior and cleanup still require [device execution](test-plan.md).

## Future modes: separate investigation

- **Overlay (phase 2):** investigate special overlay permission, user consent/settings flow, background restrictions, any required foreground-service type and current Play policy. Do not assume `specialUse` is justified or request notification access. No permission/service is added here.
- **Wallpaper (phase 3):** engine-owned SurfaceHolder, visibility and surface lifecycle; each engine needs its own renderer and independent configuration. Renderer feasibility remains open in the [decision record](renderer-decision.md).
- **Widget (phase 4):** controlled snapshots through supported widget/RemoteViews mechanisms, not an arbitrary embedded WebView or continuously animated 30/60 FPS widget. Define refresh budgets and cleanup later.

Each future surface must independently pause/release resources and reject stale callbacks. Preview preferences do not implement all-mode configuration; never share a live WebView between activity, overlay, wallpaper engines or widgets.
