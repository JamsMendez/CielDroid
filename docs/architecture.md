# Original scene preview: implemented, device proof pending

The Compose launcher now connects the four original FULL scenes to persisted scene
and 30/60 FPS selection. Each scene, FPS change or retry creates a new session and
WebView. Build and deterministic tests are not proof of Android rendering; the
native scenarios have only been compiled, not executed on a device.

## Review path and ownership

| Boundary | Owner and contract |
| --- | --- |
| `PreviewViewModel` | Activity-scoped factory captures application context. One application-owned `preview` DataStore delegate per file; repository observation uses `viewModelScope`. Controls wait for `loaded`; read failures remain visible and writes are not optimistically acknowledged. |
| `CielDroidApp` | Observes settings/error with lifecycle. New immutable generation on settings change, retry or composition recreation; manual pause survives activity recreation. Generation numbers are ViewModel-local, not globally monotonic owner IDs. Future modes are explanatory text, not controls. |
| `ScenePreview` | Keyed Compose owner with one controller and runtime. Fixed-height, full-width native viewport in inset-aware scrollable content; actual layout drives original resize listeners, without fabricated viewport JS. |
| `PreviewController` | Combines viewport visibility, RESUMED lifecycle and manual pause. Sends finite bridge commands and native pause/resume effects. Timer runs only while visible, unpaused and loading; hidden time cancels it and return grants a fresh timeout window. |
| `WebViewSceneRuntime` | Owns one WebView and AssetLoader. Captured immutable generation on all console/resource callbacks; release cancels queued callbacks, stops loading, pauses, detaches and destroys. No shared WebView or global `pauseTimers`. |

Controller disposal is idempotent and terminal. Lifecycle background transitions
pause immediately; scroll visibility and manual pause are applied by Compose.
Renderer death is handled by detaching/destroying the dead view and keeping an
observable generation error. Retry is explicit and creates a new owner, retaining
persisted settings. No commands execute after runtime release.

## Security and original bytes

`SceneAssets.paths` remains the immutable 56-file manifest inventory.
`ScenePolicy.entry/navigation/resource` is the canonical URL authority: only the
controlled HTTPS origin, GET and manifest resources reach AssetLoader, with exact
current-entry navigation. Every rejected/unknown request returns a locally denied
response, never null network fallthrough. Favicon/unknown browser-only requests
are denied without declaring a required scene asset broken. Missing allowlisted
resources and failed HTML transforms are persistent, visible failures.

Only inventory HTML passes through `SceneHtml.serve`, requiring exactly the single
expected original CSP before substitution with self-only script/style/font rules.
Stored asset bytes, ordered relative references, inventory and verifier are
unchanged. No unsafe-inline/eval, JS interface, Internet permission or remote
content is added. File/content access, mixed content, DOM/database storage,
popups, downloads, geolocation, permission requests and file pickers are denied.

## Readiness and persistence

`SceneSession.reduce` receives the originating generation, never a replacement
owner's generation. Exact draw-ready and alert-bridge-ready markers must both
come from their expected original script URLs. Page-finished is not readiness.
Trusted script ERROR messages become persistent failures, excluding the exact
original synthetic ERROR transport probe. This catches the original processing
and raphael nebula WebGL compile/link/unavailable/initialization diagnostics and
render-loop errors; late readiness cannot erase an error or degraded rendering.
Readiness says nothing about GPU fidelity or visual comparison.

`PreferencesSettingsRepository` preserves independent keys and propagates write
failures. The ViewModel displays failed writes while continuing to show the
repository's last observed persisted values. Read failures display an unavailable
state rather than claiming default settings succeeded. The failed observation
completes: storage repair and repository recreation are required for recovery.
SceneId and PreviewFps remain closed domains; no future-surface settings exist.

## Verification and C14 device checklist

Host tests cover policy/CSP/session/persistence plus controller visibility,
stale callbacks, manual pause, timeout suspension, readiness/error/disposal
cleanup, fresh generations and ViewModel not-loaded/write-failure behavior.
Android-specific CSP and WebView behavior use compilation and structural checks
because no device runner is available; artificial instrumentation RED was not
claimed. `BootstrapTest` checks truthful UI; `ScenePreviewTest` exercises all four
FULL scenes at both rates, retry, manual pause and activity recreation. Paused
retry/recreation assert retained Resume control and real WebView replacement,
then resume before awaiting both readiness markers. Only same-model retry checks
a changed generation; recreation does not assume cross-model ID monotonicity. Compilation
is not execution, recreation is not a physical orientation/accessibility test,
and console readiness is not a screenshot comparison.

With authorized API 35 and an available later API runtime, device acceptance should
run connected tests and manually
check original FULL visual fidelity (especially both WebGL nebula scenes),
30/60 cadence, large fonts, dark/light themes, insets, scroll visibility, resizing,
rotation, background/return, hidden-loading timeout suspension and renderer death.
Compare originals rather than placeholder imagery. Independently verify merged
permissions and untouched original hashes. WebKit 1.13.0 and lifecycle Compose
runtime/ViewModel 2.9.2 are pinned; DataStore 1.1.7 and coroutines 1.10.2 remain.

See the [test plan](test-plan.md) for the acceptance matrix and evidence history,
[permissions/lifecycle](permissions-and-lifecycle.md) for source-mapped boundaries,
and [renderer decision](renderer-decision.md) for future prototype alternatives.
Phase 0/1 code is complete, but device acceptance remains pending. Phase 2 overlay,
phase 3 wallpaper and phase 4 widget implementation and independent owner
configuration are deferred; no wallpaper renderer has been selected or proven viable. Publishing rights, remote CI and real device behavior remain
unverified. C14 adds no staging, publishing or SDK installs; its native review and local commit are pending.
