package com.jamsmendez.cieldroid.scenes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import com.jamsmendez.cieldroid.domain.PreviewSettings
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

/** A single owner's single generation. Interception never returns network fallthrough. */
@SuppressLint("SetJavaScriptEnabled")
class WebViewSceneRuntime(
    context: Context,
    private val generation: Long,
    private val settings: PreviewSettings,
    private val emit: (Long, SceneEvent) -> Unit,
    /** Overlay only: the WebView composites true alpha instead of its default opaque white. */
    private val transparentBackground: Boolean = false,
) {
    private val released = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    val view = WebView(context)
    private val loader = WebViewAssetLoader.Builder()
        .setDomain("appassets.androidplatform.net")
        .addPathHandler("/assets/") { path ->
            // The caller's policy is primary; repeat the inventory check at the IO boundary.
            if (path !in SceneAdaptations.served) denied()
            else try {
                val bytes = context.applicationContext.assets.open(path).use { it.readBytes() }
                val body = if (path.endsWith("/index.html")) {
                    SceneHtml.serve(bytes.toString(Charsets.UTF_8), settings.variant).toByteArray(Charsets.UTF_8)
                } else bytes
                val mime = when (path.substringAfterLast('.')) {
                    "html" -> "text/html"
                    "js" -> "application/javascript"
                    "css" -> "text/css"
                    "ttf" -> "font/ttf"
                    else -> "text/plain"
                }
                WebResourceResponse(mime, if (mime.startsWith("font/")) null else "UTF-8", ByteArrayInputStream(body))
            } catch (_: Exception) {
                post(SceneEvent.Failure("Required local asset could not be served: $path"))
                denied(500, "Local asset failure")
            }
        }.build()

    init {
        view.settings.apply {
            javaScriptEnabled = true // Original local scripts only, enforced by policy and served CSP.
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            domStorageEnabled = false
            databaseEnabled = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true) // Route popup attempts to explicit denial below.
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            setGeolocationEnabled(false)
        }
        if (transparentBackground) view.setBackgroundColor(Color.TRANSPARENT) // Before any load.
        view.setDownloadListener { _, _, _, _, _ -> /* No downloads. */ }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                if (released.get() || request.method != "GET") return denied()
                val url = request.url.toString()
                if (ScenePolicy.resource(url) == null ||
                    (request.isForMainFrame && !ScenePolicy.navigation(url, settings))) return denied()
                return loader.shouldInterceptRequest(request.url) ?: run {
                    post(SceneEvent.Failure("Required local asset handler returned no response"))
                    denied(500, "Local asset failure")
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                released.get() || request.method != "GET" || !ScenePolicy.navigation(request.url.toString(), settings)

            @Deprecated("Legacy callback remains fail closed")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                released.get() || !ScenePolicy.navigation(url, settings)

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!ScenePolicy.navigation(url, settings)) {
                    view.stopLoading()
                    post(SceneEvent.Failure("Unexpected main-frame navigation"))
                }
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (ScenePolicy.resource(request.url.toString()) != null) {
                    post(SceneEvent.Failure("Required local resource failed to load"))
                }
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (ScenePolicy.resource(request.url.toString()) != null) {
                    post(SceneEvent.Failure("Required local resource returned ${response.statusCode}"))
                }
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                post(SceneEvent.Failure("Unexpected TLS request"))
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (!released.get()) {
                    emit(generation, SceneEvent.Failure("Renderer process ended. Retry to create a new preview."))
                    release()
                }
                return true
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if (released.get()) return true
                val source = message.sourceId()
                val text = message.message()
                val trustedScript = ScenePolicy.resource(source)?.endsWith(".js") == true
                // Originals deliberately emit synthetic ERROR probes: these are not script failures.
                val synthetic = text == "CIELINUX_DIAGNOSTICS_JS_ERROR synthetic transport probe"
                if (trustedScript && message.messageLevel() == ConsoleMessage.MessageLevel.ERROR && !synthetic) {
                    post(SceneEvent.Failure("Scene script failure: $text"))
                } else {
                    post(SceneEvent.Console(source, text))
                }
                return true
            }
            override fun onCreateWindow(view: WebView, dialog: Boolean, gesture: Boolean, result: android.os.Message) = false
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, false, false)
            }
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                callback.onReceiveValue(null)
                return true
            }
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel(); return true
            }
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel(); return true
            }
            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
                result.cancel(); return true
            }
        }
        view.onPause()
    }

    fun load() { if (!released.get()) view.loadUrl(ScenePolicy.entry(settings)) }
    fun command(command: SceneCommand) {
        if (!released.get()) view.evaluateJavascript(command.javascript(), null)
    }
    fun visible(value: Boolean) {
        if (released.get()) return
        if (value) view.onResume() else view.onPause()
        // Never use global pauseTimers: another runtime owner may be active.
    }
    private fun post(event: SceneEvent) {
        handler.post { if (!released.get()) emit(generation, event) }
    }
    fun release() {
        if (!released.compareAndSet(false, true)) return
        handler.removeCallbacksAndMessages(null)
        view.stopLoading()
        view.onPause()
        (view.parent as? ViewGroup)?.removeView(view)
        view.webChromeClient = null
        view.webViewClient = WebViewClient()
        view.removeAllViews()
        view.destroy()
    }
    private fun denied(code: Int = 403, reason: String = "Local denial") = WebResourceResponse(
        "text/plain", "UTF-8", code, reason, mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)),
    )
}
