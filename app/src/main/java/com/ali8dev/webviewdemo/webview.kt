package com.ali8dev.webviewdemo

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class webview : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mWebview2: WebView = findViewById(R.id.mWebView2)
        mWebview2.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // ... existing settings ...

            // ADD THESE for local file access:
            allowFileAccess = true
            allowContentAccess = true  // ← ADD THIS
            allowFileAccessFromFileURLs = true  // ← ADD THIS (for older Android)
            allowUniversalAccessFromFileURLs = true  // ← ADD THIS (for older Android)
        }
        mWebview2.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mWebview2.loadUrl("file:///android_asset/privacy_policy.html")
//        mWebview2.loadUrl("https://www.google.com")
    }
}