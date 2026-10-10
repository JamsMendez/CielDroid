package com.jamsmendez.cieldroid.scenes

import com.jamsmendez.cieldroid.domain.PreviewSettings
import com.jamsmendez.cieldroid.domain.SceneId
import java.net.URI

object ScenePolicy {
    const val ORIGIN = "https://appassets.androidplatform.net"
    fun entry(settings: PreviewSettings) = "$ORIGIN/assets/scenes/${settings.scene.assetId}/index.html?fps=${settings.fps.value}"
    fun script(scene: SceneId) = "$ORIGIN/assets/scenes/${scene.assetId}/js/${scene.readyScript}"
    /** Null means deny locally, never fall back to a network request. */
    fun resource(url: String): String? {
        val uri = try { URI(url) } catch (_: Exception) { return null }
        if (uri.scheme != "https" || uri.rawAuthority != "appassets.androidplatform.net" ||
            uri.rawFragment != null || '%' in url || '\\' in url) return null
        val path = uri.rawPath ?: return null
        if (!path.startsWith("/assets/")) return null
        val asset = path.removePrefix("/assets/")
        if (asset !in SceneAssets.paths) return null
        val query = uri.rawQuery
        if (query != null) {
            val entrypoint = SceneId.entries.any { asset == "scenes/${it.assetId}/index.html" }
            if (!entrypoint || query !in setOf("fps=30", "fps=60")) return null
        }
        return asset
    }

    fun navigation(url: String, settings: PreviewSettings): Boolean =
        url == entry(settings) && resource(url) != null
}
