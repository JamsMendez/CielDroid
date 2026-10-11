package com.jamsmendez.cieldroid.scenes

import java.util.Collections

/**
 * Literal CielScenes submodule files (app/src/main/assets/scenes) the loader may serve;
 * checked by tools/verify_scene_assets.py. Never authorize directory prefixes: other
 * submodule files (e.g. README.md) are packaged but never served.
 */
object SceneAssets {
    val paths: Set<String> = Collections.unmodifiableSet(setOf(
        "scenes/explorer/index.html",
        "scenes/explorer/js/animate.js",
        "scenes/explorer/js/config.js",
        "scenes/explorer/js/earth.js",
        "scenes/explorer/js/glyphs.js",
        "scenes/explorer/js/main.js",
        "scenes/explorer/js/math.js",
        "scenes/explorer/js/render-loop.js",
        "scenes/explorer/js/rings.js",
        "scenes/explorer/js/rising-sparks.js",
        "scenes/explorer/js/see-through-hook.js",
        "scenes/explorer/styles.css",
        "scenes/idle/index.html",
        "scenes/idle/js/animate.js",
        "scenes/idle/js/config.js",
        "scenes/idle/js/earth.js",
        "scenes/idle/js/glyphs.js",
        "scenes/idle/js/main.js",
        "scenes/idle/js/math.js",
        "scenes/idle/js/render-loop.js",
        "scenes/idle/js/rings.js",
        "scenes/idle/js/see-through-hook.js",
        "scenes/idle/styles.css",
        "scenes/processing/index.html",
        "scenes/processing/js/config.js",
        "scenes/processing/js/layers.js",
        "scenes/processing/js/main.js",
        "scenes/processing/js/math.js",
        "scenes/processing/js/nebula.js",
        "scenes/processing/js/render-loop.js",
        "scenes/processing/js/scene-data.js",
        "scenes/processing/js/see-through-hook.js",
        "scenes/processing/js/sphere.js",
        "scenes/processing/js/sprites.js",
        "scenes/processing/styles.css",
        "scenes/raphael/index.html",
        "scenes/raphael/js/central-core.js",
        "scenes/raphael/js/config.js",
        "scenes/raphael/js/digits.js",
        "scenes/raphael/js/feathers.js",
        "scenes/raphael/js/glyph-rings.js",
        "scenes/raphael/js/glyphs.js",
        "scenes/raphael/js/hexadecagon.js",
        "scenes/raphael/js/layers.js",
        "scenes/raphael/js/main.js",
        "scenes/raphael/js/math.js",
        "scenes/raphael/js/nebula.js",
        "scenes/raphael/js/render-loop.js",
        "scenes/raphael/js/scene-data.js",
        "scenes/raphael/js/see-through-hook.js",
        "scenes/raphael/js/sphere.js",
        "scenes/raphael/js/sprites.js",
        "scenes/raphael/styles.css",
        "scenes/shared/fonts/ArchivoBlack-Regular.ttf",
        "scenes/shared/fonts/OFL.txt",
        "scenes/shared/js/alert-overlay.js",
    ))
}

/** Separate host adaptations served beside the originals; never part of the original inventory. */
object SceneAdaptations {
    const val MINI_TRANSPARENT = "adaptations/mini-transparent.css"
    val paths: Set<String> = Collections.unmodifiableSet(setOf(MINI_TRANSPARENT))
    /** Everything the asset loader may serve. */
    val served: Set<String> = Collections.unmodifiableSet(SceneAssets.paths + paths)
}
