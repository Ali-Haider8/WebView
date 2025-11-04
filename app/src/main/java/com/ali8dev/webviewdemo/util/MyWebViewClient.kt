package com.ali8dev.webviewdemo.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ali8dev.webviewdemo.MainActivity

class MyWebViewClient(
    private val activity: MainActivity, private val context: Context
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return if (url != null) handleUrl(url) else false
    }

    private fun handleUrl(url: String): Boolean {
        // Handle captcha in external browser
        if (url.contains("captcha") || url.contains("challenge")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
            return true
        }

        // Let UrlHandler decide if it should open externally
        return UrlHandler.handleUrl(activity, context, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        // Check network only if not loading local assets
        if (!activity.isNetworkAvailable && !url.isNullOrEmpty() && !url.startsWith("file:///android_asset/")) {
            activity.loadNoInternetPage()
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        activity.mSwipeRefreshLayout.isRefreshing = false

        // Save URL if it's a valid web page
        if (!url.isNullOrEmpty() && !url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
            activity.saveUrl(url)
        }
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)

        // Only load error page for main frame errors
        if (request?.isForMainFrame == true) {
            activity.loadNoInternetPage()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView?, errorCode: Int, description: String?, failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        activity.loadNoInternetPage()
    }
}