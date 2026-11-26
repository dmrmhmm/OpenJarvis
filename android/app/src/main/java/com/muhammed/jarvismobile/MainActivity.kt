package com.muhammed.jarvismobile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var pendingWebPermission: PermissionRequest? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingFileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        pendingFileCallback = null
    }

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            // Matches OpenJarvis' existing Android/Tauri CORS origin.
            .setDomain("tauri.localhost")
            .setHttpAllowed(true)
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ) = assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    // Keep the app inside its packaged origin. External links are
                    // intentionally not opened from untrusted chat content.
                    return request.url.host != "tauri.localhost"
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val wantsAudio = request.resources.contains(
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    )
                    if (!wantsAudio || request.resources.size != 1) {
                        request.deny()
                        return
                    }
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    } else {
                        pendingWebPermission?.deny()
                        pendingWebPermission = request
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST)
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    pendingFileCallback?.onReceiveValue(null)
                    pendingFileCallback = filePathCallback
                    val acceptedType = fileChooserParams.acceptTypes
                        .firstOrNull { it.isNotBlank() } ?: "*/*"
                    filePicker.launch(acceptedType)
                    return true
                }
            }
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        webView.loadUrl("http://tauri.localhost/assets/www/index.html")
    }

    @Deprecated("Use the Activity Result API when migrating this legacy permission callback")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != AUDIO_PERMISSION_REQUEST) return

        val request = pendingWebPermission ?: return
        pendingWebPermission = null
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
            request.deny()
        }
    }

    override fun onDestroy() {
        pendingWebPermission?.deny()
        pendingWebPermission = null
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        super.onDestroy()
    }

    private companion object {
        const val AUDIO_PERMISSION_REQUEST = 4102
    }
}
