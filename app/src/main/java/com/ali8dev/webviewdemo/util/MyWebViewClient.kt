package com.ali8dev.webviewdemo.util

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.ali8dev.webviewdemo.MainActivity

class MyWebViewClient(
    private val activity: MainActivity, private val context: Context
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        // Check internet connection before loading URL
        if (!isInternetAvailable()) {
            loadNoInternetPage(view)
            return true
        }

        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
        Log.d("WebViewUrl", url)
        return UrlHandler.handleUrl(activity, context, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false

        // Check internet connection before loading URL
        if (!isInternetAvailable()) {
            loadNoInternetPage(view)
            return true
        }

        return UrlHandler.handleUrl(activity, context, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        // Check internet connection when page starts loading
        if (!isInternetAvailable() && !url.isNullOrEmpty() && !url.startsWith("file:///android_asset/")) {
            loadNoInternetPage(view)
            return
        }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        activity.mSwipeRefreshLayout.isRefreshing = false

        // Only save URL if it's not the NoInternet page
        if (!url.isNullOrEmpty() && !url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
            activity.saveUrl(url)
            // set homepage from first load AND update home button
            activity.setHomepageFromFirstLoad(url)
        }

        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
    ) {
        // Load NoInternet page on error
        if (request?.isForMainFrame == true) {
            loadNoInternetPage(view)
        }
        super.onReceivedError(view, request, error)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView?, errorCode: Int, description: String?, failingUrl: String?
    ) {
        // Load NoInternet page on error
        loadNoInternetPage(view)
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    /**
     * Check if internet connection is available
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION") return networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Load the NoInternet.html page from assets
     */
    private fun loadNoInternetPage(view: WebView?) {
        view?.loadUrl("file:///android_asset/NoInternet.html")
    }
}