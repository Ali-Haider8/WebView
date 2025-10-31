package com.ali8haider.webview.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri

object UrlHandler {

    private var pendingSmsUrl: String? = null

    /**
     * Check if the URL is non-HTTP/HTTPS and handle it, returning true if handled
     * @param activity Activity context
     * @param url URL to check and handle
     * @return true if URL was handled (non-HTTP), false otherwise
     */
    fun checkUrl(activity: Activity, url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            openExternalUrl(activity, url)
            return true
        }
        return false
    }
    /**
     * Open non-HTTP/HTTPS URLs in external apps
     * @param activity Activity context
     * @param url URL to open
     */
    fun openExternalUrl(activity: Activity, url: String) {
        try {
            // Handle SMS with permission check
            if (url.startsWith("sms:")) {
                smsLink(activity, url)
                return
            }

            // For all other non-HTTP URLs, try to open with intent
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                Toast.makeText(
                    activity, "No app found to open this link", Toast.LENGTH_SHORT
                ).show()
                Toast.makeText(activity, url, Toast.LENGTH_SHORT).show()

            }
        } catch (e: Exception) {
            Toast.makeText(
                activity, "Unable to open link: ${e.message}", Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    /**
     * Handle SMS links with permission check
     */
    private fun smsLink(activity: Activity, url: String) {
        if (PermissionUtil.isPermissionAllowed(activity, Manifest.permission.SEND_SMS)) {
            sms(activity, url)
        } else {
            pendingSmsUrl = url
            PermissionUtil.requestPermission(
                activity, Manifest.permission.SEND_SMS, PermissionUtil.MY_PERMISSIONS_REQUEST_SMS
            )
        }
    }

    /**
     * Open SMS app
     */
    private fun sms(activity: Activity, url: String) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val smsIntent = Intent(Intent.ACTION_VIEW).setDataAndType(url.toUri(),"vnd.android-dir/mms-sms")
            activity.startActivity(smsIntent)
        } catch (_: Exception) {
            Toast.makeText(activity, "Unable to open SMS app", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle SMS permission result
     */
    fun handleSmsPermissionResult(activity: Activity, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pendingSmsUrl?.let { url ->
                sms(activity, url)
                pendingSmsUrl = null
            }
        } else {
            Toast.makeText(
                activity, "SMS permission is required to send messages", Toast.LENGTH_LONG
            ).show()
            pendingSmsUrl = null
        }
    }

    // Handle openGooglePlay, openYoutube, openWhatsApp, openFacebook, openInstagram, openTwitter (truncated as per your document, assume they are already there)

    fun handleUrl(activity: Activity, context: Context, url: String): Boolean {
        // 1. Let UrlHandler handle special schemes (sms:, tel:, etc.)
        if (checkUrl(activity, url)) return true

        val uri = url.toUri()
        val host = uri.host?.lowercase() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        // Handle custom schemes like "snssdk1233://" for TikTok
        if (scheme == "snssdk1233") {
            return openTikTok(context, uri)
        }

        // Extract path segments for parsing (e.g., "/gsmarenateam/" -> ["gsmarenateam"])
        val pathSegments = uri.pathSegments

        // App-specific deep link construction
        val schemeUri = when {
            host.contains("instagram.com") || host.contains("l.instagram.com") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "instagram://user?username=$username".toUri()
                } else null
            }

            host.contains("facebook.com") || host.contains("fb.me") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "fb://profile/$username".toUri()
                } else null
            }

            host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "twitter:///user?screen_name=$username".toUri()
                } else null
            }

            host.contains("whatsapp.com") || host.contains("wa.me") -> {
                if (pathSegments.size == 1) {
                    val phone = pathSegments[0]
                    "whatsapp://send?phone=$phone".toUri()
                } else null
            }

            host.contains("youtube.com") || host.contains("youtu.be") -> {
                if (pathSegments.size >= 2 && (pathSegments[0] == "user" || pathSegments[0] == "channel")) {
                    pathSegments[1]
                    "vnd.youtube://www.youtube.com/${uri.path}".toUri()
                } else null
            }

            host.contains("tiktok.com") -> {
                if (pathSegments.isNotEmpty() && pathSegments[0].startsWith("@")) {
                    val username = pathSegments[0].substring(1)
                    "tiktok://user?username=$username".toUri()
                } else null
            }

            host.contains("pinterest.com") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "pinterest://user/$username".toUri()
                } else null
            }

            host.contains("linkedin.com") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "linkedin://profile/$username".toUri()
                } else null
            }

            host.contains("spotify.com") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "spotify://user/$username".toUri()
                } else null
            }

            host.contains("telegram.me") -> {
                if (pathSegments.size == 1) {
                    val username = pathSegments[0]
                    "tg://resolve?domain=$username".toUri()
                } else null
            }

            host.contains("play.google.com") && scheme.contains("store") || url.startsWith("market://") ->
                if (pathSegments.size == 1) {
                    val appPackageName = pathSegments[0]
                    "market://details?id=$appPackageName".toUri()
                } else null

            url.startsWith("mailto:") -> url.toUri()

            else -> null
        }

        if (schemeUri != null) {
            val schemeIntent = Intent(Intent.ACTION_VIEW, schemeUri)
            if (isAppInstalled(context, schemeIntent)) {
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
            host.contains("pinterest.com") -> "com.pinterest"
            host.contains("linkedin.com") -> "com.linkedin.android"
            host.contains("spotify.com") -> "com.spotify.music"
            host.contains("telegram.me") -> "org.telegram.messenger"
            host.contains("play.google.com") -> "com.android.vending"
            url.startsWith("mailto:") -> null
            else -> null
        }

        if (packageName != null) {
            val appIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(packageName) }
            if (isAppInstalled(context, appIntent)) {
                context.startActivity(appIntent)
                return true
            }
        }

        // Let WebView load it
        return false
    }

    private fun openTikTok(context: Context, uri: Uri): Boolean {
        val packageName = "com.zhiliaoapp.musically"  // TikTok package
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(packageName) }
        if (isAppInstalled(context, appIntent)) {
            context.startActivity(appIntent)
            return true
        }

        // Fallback to HTTPS version if available in params
        val fallbackUrl = uri.getQueryParameter("params_url") ?: "https://www.tiktok.com"
        val fallbackUri = fallbackUrl.toUri()
        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply { setPackage(packageName) }
        if (isAppInstalled(context, fallbackIntent)) {
            context.startActivity(fallbackIntent)
            return true
        }

        return false
    }

    private fun isAppInstalled(context: Context, intent: Intent): Boolean {
        return try {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        } catch (_: Exception) {
            false
        }
    }

    // Handle openGooglePlay, openYoutube, openWhatsApp, openFacebook, openInstagram, openTwitter (truncated as per your document, assume they are already there)
}