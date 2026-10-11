# Original scene resources preserved for Android

> **Current model:** the scenes are the CielScenes git submodule at
> `app/src/main/assets/scenes` (pinned by its gitlink), not a copy. Check it with
> `git submodule update --init app/src/main/assets/scenes` and
> `python3 tools/verify_scene_assets.py`; adopt upstream changes with
> `git submodule update --remote app/src/main/assets/scenes`. The verifier no longer
> reads per-file hashes or supports `--source`. Everything below is historical.

Historical T1 copied the four original CieLinux scene trees without changing bytes,
headers, layout, CSP or scheduling. This is an audited resource baseline, **not a
working Android preview**. Android host implementation belongs to later units.

## Verify the copy (historical)

```sh
python3 tools/verify_scene_assets.py --source /home/jamsmendez/Documents/OS/CosmicLinux/CieLinux
./gradlew :app:assembleDebug
```

`source-assets.json` records all 56 source/destination paths, individual byte
counts and SHA256 hashes: **940,651 bytes**, including the font and OFL notice.
Copied text accounts for 16,022 lines; copied resources are separate from authored
audit/verifier work. The unchanged stdlib verifier checks schema, canonical paths,
source layout, catalog SHA256/size and selected five-tree destination inventory,
plus local HTML script/style/preload and CSS URL/import references. Totals are
catalog-derived, not hardcoded to 56 files, 940651 bytes or 57 references. Default
mode reads no upstream; `--source` compares checkout bytes/inventory, not committed
revision, APK or provenance metadata. Those attestations require separate checks.
It does not prove JS/dynamic dependencies, CSP, runtime or licensing, nor enforce
ancestor-symlink, hard-link-count or regular-file pre-read hardening.
The original byte-preserving resource/documentation unit adds no rendering
behavior, so artificial RED/GREEN tests are inapplicable.

Historical T1 verification (not fresh C09 evidence):
- Asset verifier: exit 0; 56 files, 940,651 bytes, 57 local references; hashes,
  selected inventories and checkout byte equality passed (49 scripts, 4 CSS, 4 fonts).
- `./gradlew :app:assembleDebug`: exit 0; BUILD SUCCESSFUL in 1s;
  32 actionable tasks (3 executed, 29 up-to-date), configuration cache reused.
  Kotlin and Java main compilation remained NO-SOURCE. No device test ran.

## C09 isolated evidence and I03 qualification

Both first `python3 -B tools/verify_scene_assets.py` runs (default and `--source`
with the checkout above) exited 0: 56 files / 940651 bytes / 57 references
(49 scripts, 4 CSS, 4 fonts). Source mode proves checkout equality only; separate
checks match all 56 canonical regular nonsymlink nlink1 originals to the unchanged
catalog, committed upstream `a31faf9` and production APK, with no extras; test APK
assets: zero. All 13 characterization cases passed (one control, 12 rejections);
duplicate destinations fail the layout invariant, not a dedicated duplicate guard.
Four separate offline wrapper checks exited 0: assemble 36 executed, forced host
24, lint 9 executed/18 up-to-date, test APK 30 executed/20 up-to-date. Eight fresh
host tests (seven functional, one arithmetic) had no failures/errors/skips; lint
0 errors/24 warnings (23 UnusedResources, one ObsoleteSdkInt). Binary manifests
and four actual JDK 25 daemon requests were attested, distinct from launcher 26.
Production is not permission-free; test compilation is not device execution.
I03 strict audit exited 1 solely for common `.git` root DIRECTORY mtime-only drift,
writer UNKNOWN. All 109 source/index entries, HEAD, five-path staged status, pure
tree and other Git metadata were unchanged; only ignored build/cache outputs changed.
Human accepted candidate-specific qualified continuation, not a retroactive strict
pass or blanket waiver. Final input verification, native review and local commit remain pending.

## Provenance and authorization

- Read-only source checkout: `/home/jamsmendez/Documents/OS/CosmicLinux/CieLinux`.
- Referenced revision and observed local HEAD:
  `a31faf964ff376dc3443c9939377e9d76f36a95f`.
- Read-only Git status showed no tracked modifications. It did show three
  unrelated untracked files: `.atl/.skill-registry.cache.json`,
  `.atl/skill-registry.md`, and `odd/plans/versioned-releases.md`.
  Therefore the **tracked tree is clean, not the entire checkout**. No remote
  fetch was performed; local HEAD is not evidence of current GitHub HEAD.
- Before copying, each selected working-tree file was compared byte-for-byte
  with `git show REV:path`. All 56 matched that committed revision. Destinations
  are unmodified copies, not Android-adapted derivatives. Future modifications
  must be identified separately rather than attributed to this committed source.
- The user explicitly authorized copying scenes/resources for CielDroid. This
  project reuse authorization does not establish general public distribution
  rights, ownership of all material, or a public upstream software license.
- No general `LICENSE*` file was found. Scene headers cite reference-only,
  Git-excluded `docs/great-sage` material, absent from this checkout; do not
  reconstruct or import it. Examples: `scenes/processing/js/config.js:1-12`,
  `scenes/explorer/js/config.js:1`, `scenes/idle/js/rings.js:1`, and
  `scenes/raphael/js/config.js:1-2`. Their existing headers remain untouched.
- `scenes/shared/fonts/OFL.txt:1-9,48-81` identifies the Archivo Black Project
  Authors and SIL OFL 1.1, including notice/redistribution conditions. Both font
  and complete OFL are retained. The font notice does not license scene code.
  **Distribution licensing remains unresolved**; do not invent ownership or
  extend the font license to other resources.

## Concrete source contract

All path:line references below are relative to the upstream checkout at the
recorded revision, not Android implementation claims.

| Finding | Evidence and consequence |
|---|---|
| All scenes render with Canvas2D | `scenes/processing/js/config.js:15-17`, `scenes/explorer/js/config.js:12-13`, `scenes/idle/js/config.js:11-12`, `scenes/raphael/js/config.js:21-23` obtain a `2d` context. |
| Processing and raphael additionally use WebGL | `scenes/processing/js/nebula.js:169` and `scenes/raphael/js/nebula.js:209` request `webgl` for the nebula. WebGL support/failure paths need runtime verification on Android. |
| Mini and full share entrypoints | `src/scene-host.cpp:8-16,26-30` maps the same scene `index.html` to mini (`variant=mini`) and full URLs, with explicit `fps=30` or `fps=60`; no separate mini resource tree exists. |
| Native default is 30, JavaScript default is 60 | `src/scene-host.h:12,60` defaults the native rate to 30; `src/scene-host.cpp:58` normalizes other native rates to 30. Every scene's `js/render-loop.js:27-31` chooses 30 only when the URL's numeric `fps` equals 30; otherwise 60, including absent/invalid input. Android must validate and pass an explicit rate rather than rely on inconsistent defaults. |
| URL parsing controls variant and initial rate | Every scene's `js/render-loop.js:33-48` reads query/hash variant, defaults to full and initializes the frame interval. The original fixed-deadline rAF scheduler is at `:148-181`. A rate change is not an existing bridge command. |
| Mini ignores pause | Every scene's `js/render-loop.js:134` returns immediately for mini in `setWallpaperPaused`; full holds callbacks and re-arms on resume (`:131-146`). Android ownership/lifecycle needs explicit future adaptation; bridge pause alone does not cover mini. |
| Transparency is host-dependent | `src/view.qml:59-66` forces black WebEngine background and applies a mini-only Qt shader; `src/lumakey.frag:1-14` sets alpha to `max(r,g,b)` with premultiplied color. This is a Linux luminance-key technique, not an Android transparency guarantee. |
| Existing bridge is narrowly scoped | `scenes/shared/js/alert-overlay.js:907-939` routes show/hide/pause/resume and parses the JSON string in `cielinuxAlertCommand`. It does **not** implement proposed select/fps/viewport commands. Native URL switching is separate (`src/scene-host.cpp:70-82`). |
| Dependency ordering and shared layout are preserved | `scenes/processing/index.html:8-23`, `scenes/explorer/index.html:8-22`, `scenes/idle/index.html:8-21`, `scenes/raphael/index.html:8-29` load local CSS, ordered JS and `../shared/js/alert-overlay.js`. Each scene's `styles.css:3-5` loads `../shared/fonts/ArchivoBlack-Regular.ttf`. |
| Original CSP is Qt-specific | Each scene's `index.html:5` allows `qrc:` scripts/styles/fonts, not the future Android HTTPS origin. Copy verification passing does not mean these pages will render in Android WebView unchanged. |

## Android references and future boundaries

Primary references for later Android design (not runtime proof):

- [Load local content in WebView](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content):
  `WebViewAssetLoader` serves app-local assets through an HTTPS origin such as
  `https://appassets.androidplatform.net/assets/`, with request interception.
  This motivates local-only allowlisting and a deliberate CSP adaptation in T3,
  not remote page loading or permissive `file://` access. No AssetLoader or CSP
  changes have been implemented in T1.
- [WallpaperService.Engine](https://developer.android.com/reference/android/service/wallpaper/WallpaperService.Engine):
  Engine supplies a surface holder and surface/visibility/lifecycle callbacks
  (`getSurfaceHolder`, `onSurfaceCreated`, `onSurfaceChanged`,
  `onSurfaceDestroyed`, `onVisibilityChanged`, `onDestroy`). These document
  native wallpaper rendering responsibilities, **not** a verified direct
  WebView attachment technique or achievable offscreen WebView FPS. Wallpaper,
  overlay and widget implementation remain outside this feature.

These references do not prove Android Canvas/WebGL parity, offscreen rendering,
frame cadence, transparency or lifecycle behavior. At the resource-copy/T1 audit,
no connected device was used and the package had no launcher Activity or main
Kotlin/Java implementation. That baseline preceded the T2 bootstrap and T3/T3b
preview implementation in the earlier full working tree, not this C09 slice.
The current [README](../README.md) describes a catalog-only launcher, without
preview or selection controls; runtime integration and architecture await C13.
Compilation and host verification do not establish device acceptance; build
success cannot substitute for those runtime checks.

## Copy boundary

Only four `index.html`/`styles.css` files with their complete JS trees and the
three shared alert/font/notice files were copied. No desktop icons, Qt/Linux
source, excluded reference material, credentials or unrelated integration files
were imported. Upstream was not modified. Git is initialized on
`feature/preview-review-chain`; C01–C08 have reviewed local commits, ending at
`9fc80fb`. No push, PR or merge occurred; C09 checks passed with I03 qualification
above; final input verification, native review and local commit remain pending.
