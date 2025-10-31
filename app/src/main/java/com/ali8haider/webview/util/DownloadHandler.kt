@file:Suppress("DEPRECATION")

package com.ali8haider.webview.util

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object DownloadHandler {

    const val DOWNLOAD_PERMISSION_CODE = 101

    private var pendingDownload: PendingDownloadData? = null

    private data class PendingDownloadData(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String
    )

    /**
     * Main entry point for downloading files
     */
    fun downloadLink(
        activity: Activity,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        if (isPermissionAllowed(activity)) {
            download(activity, url, userAgent, contentDisposition, mimeType)
        } else {
            // Store download data for later when permission is granted
            pendingDownload = PendingDownloadData(url, userAgent, contentDisposition, mimeType)
            requestPermission(activity)
        }
    }

    /**
     * Check if storage permission is granted
     */
    private fun isPermissionAllowed(activity: Activity): Boolean {
        // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE for downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request storage permission
     */
    private fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_PERMISSION_CODE)
        }
    }

    /**
     * Handle permission result from MainActivity
     */
    fun handlePermissionResult(activity: Activity, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, proceed with pending download
            pendingDownload?.let {
                download(activity, it.url, it.userAgent, it.contentDisposition, it.mimeType)
                pendingDownload = null
            }
        } else {
            Toast.makeText(
                activity,
                "Storage permission is required to download files",
                Toast.LENGTH_LONG
            ).show()
            pendingDownload = null
        }
    }

    /**
     * Perform the actual download
     */
    private fun download(
        activity: Activity,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        try {
            // Show downloading message
            Toast.makeText(activity, "Downloading file...", Toast.LENGTH_LONG).show()

            // Get filename from URL or content disposition
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)

            // Create download request
            val request = DownloadManager.Request(url.toUri()).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)

                // Add cookies for authenticated downloads
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null) {
                    addRequestHeader("Cookie", cookies)
                }

                setDescription("Downloading file...")
                setTitle(filename)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                allowScanningByMediaScanner()
                setVisibleInDownloadsUi(true)
            }

            // Start download
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(activity, "Downloading: $filename", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(activity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}