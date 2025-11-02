package com.ali8haider.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.ali8haider.webview.util.UrlHandler

class MyWebViewClient(
    private val activity: MainActivity,
    private val context: Context
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
        Log.d("WebViewUrl", url)
        return UrlHandler.handleUrl(activity, context, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return UrlHandler.handleUrl(activity, context, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//        activity.mSwipeRefreshLayout.isRefreshing = true
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        activity.mSwipeRefreshLayout.isRefreshing = false
        activity.saveUrl(url.toString())

        // set homepage from first load AND update home button
        activity.setHomepageFromFirstLoad(url)

        super.onPageFinished(view, url)
    }
}