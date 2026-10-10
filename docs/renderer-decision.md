# Preview renderer established; wallpaper decision deferred

Use the implemented, owner-local WebView for the activity preview only. **No wallpaper renderer has been selected or proven viable.** Phase 3 needs a measured prototype, not an extrapolation from preview compilation.

## Evidence boundaries

- [Source audit](source-audit.md): the originals combine JavaScript Canvas2D with WebGL nebula rendering in processing and raphael. Mini ignores the FULL pause command and relies on a Linux luminance-key compositor; it is not Android transparency support.
- [Compiled implementation](../app/src/main/java/com/jamsmendez/cieldroid/scenes/WebViewSceneRuntime.kt): local HTTPS AssetLoader, constrained CSP, one WebView per generation. Compilation and console markers do not prove pixels, GPU behavior or cadence.
- [WallpaperService.Engine API](https://developer.android.com/reference/android/service/wallpaper/WallpaperService.Engine): supplies SurfaceHolder and surface/visibility/destruction callbacks. A normal [WebView](https://developer.android.com/reference/android/webkit/WebView) is a View; these APIs do not document direct attachment to an arbitrary wallpaper SurfaceHolder.
- W01 spike (unmeasured): engine surface → private `VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY` VirtualDisplay → `Presentation` hosting the unchanged WebViewSceneRuntime. On the API 35 phone, in the system live-wallpaper preview (portrait only; that activity locks orientation), the idle scene reached READY in about 1 s, the screenshot showed the original scene full-screen, and Back released the display and engine with no CSP/WebGL/crash logcat lines. Landscape resize, FPS, CPU/GPU, memory, battery and multi-engine behavior are **not measured** (W02/W03).

## Alternatives for phase 3

| Candidate | Benefit | Unproven work / boundary |
| --- | --- | --- |
| Preview WebView | Preserves existing JS, Canvas2D and WebGL pipelines | Appropriate for an activity View hierarchy; not a wallpaper surface implementation. |
| Native Canvas / OpenGL ES | Render directly through engine-owned surface lifecycle | Port JS animation, layout, text/font metrics, effects and scheduling; GL nebula and shader behavior need parity tests. |
| Partial compositor port | Reuse selected scene logic while replacing presentation | Define ownership of each layer and coordinate transforms; compositing, alpha, synchronization and retained JS execution still need engineering. Not an automatic faithful port. |
| Offscreen WebView + bitmap capture | Potential reuse of original pages | Capture/copy/upload overhead, hardware/WebGL capture correctness and background execution are unresolved. Claiming 60 FPS without benchmarks is unacceptable. |
| Offscreen GL texture/surface pipeline | Potential GPU-native composition | Requires explicit context, synchronization and surface integration; does not make a WebView an attachable engine renderer. |

Original nebula shaders request fragment `highp`; GPU precision support, shader compilation/linking, context loss and silent blank output need testing. Port boundaries include shared alert overlay, Canvas2D geometry, typography, URL scene/FPS contract and fixed-deadline scheduler. Preserve bundled originals; adaptations belong in separate implementation files. A partial port must document any fidelity loss rather than substitute placeholder art.

## Smallest deciding prototype (future work, not this change)

1. On API 35 and a later available API, render one representative Canvas2D FULL scene and one WebGL-nebula scene to an engine-owned surface with the candidate pipeline. Include resize, surface destruction/recreation and visibility pause.
2. Compare original pixels and collect 30/60 target versus delivered FPS, frame-time distribution and dropped frames, CPU/GPU time, capture-to-present latency, memory/allocations and thermal behavior. Record resolution, device, WebView/GPU versions and warm-up duration.
3. Measure battery/energy under equal brightness and duration against idle and activity-preview baselines; include hidden/background cost. Set acceptance budgets before selecting a renderer.
4. Run two simultaneous engines (preview and applied wallpaper), independent configurations, repeated create/destroy, context loss and stale-callback rejection. Verify resource counts return to baseline and invisible engines stop work.

Only measured results can justify a wallpaper choice. Overlay and widget decisions are also deferred; see [permissions/lifecycle](permissions-and-lifecycle.md) and [device acceptance](test-plan.md).
