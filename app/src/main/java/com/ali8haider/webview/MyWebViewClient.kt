package com.ali8haider.webview
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ali8haider.webview.util.UrlHandler

class MyWebViewClient(
    private val activity: MainActivity,
    private val context: Context
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrl(url)
    }

    private fun handleUrl(url: String): Boolean {
        // 1. Let UrlHandler handle special schemes (sms:, tel:, etc.)
        if (UrlHandler.checkUrl(activity, url)) return true

        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        // Handle custom schemes like "snssdk1233://" for TikTok
        if (scheme == "snssdk1233") {
            return openTikTok(uri)
        }

        // Extract path segments for parsing (e.g., "/gsmarenateam/" -> ["gsmarenateam"])
        val pathSegments = uri.pathSegments

        // App-specific deep link construction
        val schemeUri = when {
            host.contains("instagram.com") || host.contains("l.instagram.com") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    Uri.parse("instagram://user?username=$username")
                } else null
            }
            host.contains("facebook.com") || host.contains("fb.me") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    Uri.parse("fb://profile/$username")
                } else null
            }
            host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    Uri.parse("twitter:///user?screen_name=$username")
                } else null
            }
            host.contains("whatsapp.com") || host.contains("wa.me") -> {
                if (pathSegments.size == 1) {
                    val phone = pathSegments[0]
                    Uri.parse("whatsapp://send?phone=$phone")
                } else null
            }
            host.contains("youtube.com") || host.contains("youtu.be") -> {
                if (pathSegments.size >= 2 && (pathSegments[0] == "user" || pathSegments[0] == "channel")) {
                    val id = pathSegments[1]
                    Uri.parse("vnd.youtube://www.youtube.com/${uri.path}")
                } else null
            }
            host.contains("tiktok.com") -> {
                if (pathSegments.size >= 1 && pathSegments[0].startsWith("@")) {
                    val username = pathSegments[0].substring(1)
                    Uri.parse("tiktok://user?username=$username")
                } else null
            }
            else -> null
        }

        if (schemeUri != null) {
            val schemeIntent = Intent(Intent.ACTION_VIEW, schemeUri)
            if (isAppInstalled(schemeIntent)) {
                context.startActivity(schemeIntent)
                return true
            }
        }

        // Fallback: Try direct HTTPS with setPackage (if app handles domain)
        val packageName = when {
            host.contains("instagram.com") -> "com.instagram.android"
            host.contains("facebook.com") || host.contains("fb.me") -> "com.facebook.katana"
            host.contains("twitter.com") || host.contains("x.com") -> "com.twitter.android"
            host.contains("whatsapp.com") || host.contains("wa.me") -> "com.whatsapp"
            host.contains("youtube.com") || host.contains("youtu.be") -> "com.google.android.youtube"
            host.contains("tiktok.com") -> "com.zhiliaoapp.musically"  // or "com.ss.android.ugc.aweme" for some regions
            else -> null
        }

        if (packageName != null) {
            val appIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(packageName) }
            if (isAppInstalled(appIntent)) {
                context.startActivity(appIntent)
                return true
            }
        }

        // Let WebView load it
        return false
    }

    private fun openTikTok(uri: Uri): Boolean {
        val packageName = "com.zhiliaoapp.musically"  // TikTok package
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(packageName) }
        if (isAppInstalled(appIntent)) {
            context.startActivity(appIntent)
            return true
        }

        // Fallback to HTTPS version if available in params
        val fallbackUrl = uri.getQueryParameter("params_url") ?: "https://www.tiktok.com"
        val fallbackUri = Uri.parse(fallbackUrl)
        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply { setPackage(packageName) }
        if (isAppInstalled(fallbackIntent)) {
            context.startActivity(fallbackIntent)
            return true
        }

        return false
    }

    private fun isAppInstalled(intent: Intent): Boolean {
        return try {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        } catch (e: Exception) {
            false
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        activity.mSwipeRefreshLayout.isRefreshing = true
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        activity.mSwipeRefreshLayout.isRefreshing = false
        activity.saveUrl(url.toString())
        super.onPageFinished(view, url)
    }



}