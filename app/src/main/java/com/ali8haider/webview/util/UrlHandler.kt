package com.ali8haider.webview.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat

object UrlHandler {

    private var pendingSmsUrl: String? = null

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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

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
            val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android-dir/mms-sms"
                data = Uri.parse(url)
            }
            activity.startActivity(smsIntent)
        } catch (e: Exception) {
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


    /**
     * Check if URL should be handled by external app
     * @param activity Activity context
     * @param url URL to check
     * @return true if URL was handled by external app, false otherwise
     */
    fun checkUrl(activity: Activity, url: String): Boolean {
        val uri = Uri.parse(url)

        return when {
            url.startsWith("tel:") -> {
                phoneLink(activity, url)
                true
            }

            url.startsWith("sms:") -> {
                smsLink(activity, url)
                true
            }

            url.startsWith("mailto:") -> {
                email(activity, url)
                true
            }

            url.startsWith("geo:") || uri.host == "maps.google.com" -> {
                val mapUrl = url.replace("https://maps.google.com/maps?daddr=", "geo:")
                map(activity, mapUrl)
                true
            }

            url.contains("youtube") -> {
                openYoutube(activity, url)
                true
            }

            url.startsWith("whatsapp://") || url.contains("https://api.whatsapp.com/send?text=") || url.contains("wa.me") || url.contains("whatsapp.com") -> {
                openWhatsApp(activity, url)
                true
            }

            url.startsWith("fb://") || url.startsWith("fb-messenger://") -> {
                openFacebook(activity, url)
                true
            }

            url.startsWith("instagram://") -> {
                openInstagram(activity, url)
                true
            }

            url.startsWith("twitter://") || url.startsWith("x://") -> {
                openTwitter(activity, url)
                true
            }

            uri.host == "play.google.com" || url.startsWith("market://") -> {
                openGooglePlay(activity, url)
                true
            }

            else -> false
        }
    }

    /**
     * Handle phone number links
     */
    private fun phoneLink(activity: Activity, url: String) {
        try {
            val tel = Intent(Intent.ACTION_DIAL, Uri.parse(url))
            activity.startActivity(tel)
        } catch (e: Exception) {
            Toast.makeText(activity, "Unable to open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle email links
     */
    private fun email(activity: Activity, url: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(url)
            }
            activity.startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Unable to open email app", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle map/location links
     */
    private fun map(activity: Activity, url: String) {
        try {
            val mapIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(mapIntent)
            } else {
                // Fallback to browser if Google Maps not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                activity.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Unable to open maps", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle Google Play links
     */
    private fun openGooglePlay(activity: Activity, url: String) {
        try {
            val googlePlayIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (googlePlayIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(googlePlayIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Unable to open Google Play", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle YouTube links
     */
    private fun openYoutube(activity: Activity, url: String) {
        try {
            val youtubeIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }

            if (youtubeIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(youtubeIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Unable to open YouTube", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle WhatsApp links
     */
    private fun openWhatsApp(activity: Activity, url: String) {
        try {
            val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(whatsappIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle Facebook links
     */
    private fun openFacebook(activity: Activity, url: String) {
        try {
            val facebookIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(facebookIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Facebook is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle Instagram links
     */
    private fun openInstagram(activity: Activity, url: String) {
        try {
            val instagramIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(instagramIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Instagram is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle Twitter/X links
     */
    private fun openTwitter(activity: Activity, url: String) {
        try {
            val twitterIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(twitterIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Twitter/X is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}