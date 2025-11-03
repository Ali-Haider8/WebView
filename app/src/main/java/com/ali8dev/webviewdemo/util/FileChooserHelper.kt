package com.ali8dev.webviewdemo.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileChooserHelper {

    const val FILE_CHOOSER_REQUEST_CODE = 200
    const val CAMERA_PERMISSION_REQUEST_CODE = 201

    private var cameraPhotoUri: Uri? = null
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null

    /**
     * Create file chooser intent
     */
    fun createFileChooserIntent(
        activity: Activity,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Intent {
        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, intent)
            putExtra(Intent.EXTRA_TITLE, "Select File")

            // Add camera option if accept types include images
            val acceptTypes = fileChooserParams?.acceptTypes
            if (acceptTypes != null && acceptTypes.any { it.contains("image") }) {
                val cameraIntents = createCameraIntents(activity)
                if (cameraIntents.isNotEmpty()) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents)
                }
            }
        }

        return chooserIntent
    }

    /**
     * Create camera intent for capturing photos
     */
    private fun createCameraIntents(activity: Activity): Array<Intent> {
        val cameraIntents = mutableListOf<Intent>()

        try {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = createImageFile(activity)

            if (photoFile != null) {
                cameraPhotoUri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    photoFile
                )
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                cameraIntents.add(captureIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cameraIntents.toTypedArray()
    }

    /**
     * Create a temporary image file
     */
    private fun createImageFile(activity: Activity): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Handle activity result from file chooser
     */
    fun handleActivityResult(resultCode: Int, data: Intent?): Array<Uri>? {
        if (resultCode != Activity.RESULT_OK) {
            cameraPhotoUri = null
            return null
        }

        // Check if result is from camera
        if (data == null || data.data == null) {
            cameraPhotoUri?.let {
                val results = arrayOf(it)
                cameraPhotoUri = null
                return results
            }
            return null
        }

        // Handle single file selection
        data.data?.let { uri ->
            cameraPhotoUri = null
            return arrayOf(uri)
        }

        // Handle multiple file selection
        data.clipData?.let { clipData ->
            val results = Array(clipData.itemCount) { i ->
                clipData.getItemAt(i).uri
            }
            cameraPhotoUri = null
            return results
        }

        cameraPhotoUri = null
        return null
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(
        activity: Activity,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ) {
        pendingFileChooserParams = fileChooserParams
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Get pending file chooser params
     */
    fun getPendingFileChooserParams(): WebChromeClient.FileChooserParams? {
        val params = pendingFileChooserParams
        pendingFileChooserParams = null
        return params
    }

    /**
     * Clear pending data
     */
    fun clearPendingData() {
        cameraPhotoUri = null
        pendingFileChooserParams = null
    }
}