package com.ali8dev.webviewdemo.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    const val MY_PERMISSIONS_REQUEST_SMS = 11
    const val MY_PERMISSIONS_REQUEST_DOWNLOAD = 12

    /**
     * Check if a permission is granted
     * @param context Application context
     * @param permission Permission to check (e.g., Manifest.permission.WRITE_EXTERNAL_STORAGE)
     * @return true if permission is granted, false otherwise
     */
    fun isPermissionAllowed(context: Context, permission: String): Boolean {
        // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE for downloads
        if (permission == android.Manifest.permission.WRITE_EXTERNAL_STORAGE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            return true
        }

        // Getting the permission status
        val result = ContextCompat.checkSelfPermission(context, permission)

        // If permission is granted returning true
        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request a permission
     * @param activity Activity context
     * @param permission Permission to request
     * @param permissionNumber Request code for the permission
     */
    fun requestPermission(activity: Activity, permission: String, permissionNumber: Int) {
        // Request permission only if needed
        // Skip request for Android 10+ when requesting storage permission
        if (permission == android.Manifest.permission.WRITE_EXTERNAL_STORAGE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            return
        }

        // And finally ask for the permission
        ActivityCompat.requestPermissions(activity, arrayOf(permission), permissionNumber)
    }

    /**
     * Handle permission result with toast message
     * @param activity Activity context
     * @param grantResults Permission grant results
     * @param permissionName Name of permission for user-friendly message
     */
    @Suppress("unused")
    fun handlePermissionResult(activity: Activity, grantResults: IntArray, permissionName: String) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "$permissionName permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "$permissionName permission denied", Toast.LENGTH_LONG).show()
        }
    }
}