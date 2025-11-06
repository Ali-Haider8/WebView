package com.ali8dev.webviewdemo.util

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.ali8dev.webviewdemo.MainActivity

@Suppress("DEPRECATION")
class MyWebChromeClient(private val activity: MainActivity) : WebChromeClient() {

    var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation = 0
    private var originalSystemUiVisibility = 0

    // File upload support - make it public so MainActivity can access it
    var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            onHideCustomView()
            return
        }

        customView = view
        customViewCallback = callback

        // Save original orientation and UI visibility
        originalOrientation = activity.requestedOrientation
        originalSystemUiVisibility = activity.window.decorView.systemUiVisibility

        // Hide action bar first
        activity.supportActionBar?.hide()

        // Hide the WebView and toolbar
        activity.mWebView.visibility = View.GONE
        activity.mToolbar.visibility = View.GONE

        // Set fullscreen flags BEFORE adding view
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system UI
        hideSystemUI()

        // Set screen orientation to landscape
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Show fullscreen container
        activity.mFrameLayout.visibility = View.VISIBLE
        activity.mFrameLayout.addView(customView)
    }

    override fun onHideCustomView() {
        if (customView == null) {
            return
        }

        // Hide fullscreen container
        activity.mFrameLayout.visibility = View.GONE
        activity.mFrameLayout.removeView(customView)
        customView = null

        // Show the WebView and toolbar again
        activity.mWebView.visibility = View.VISIBLE
        activity.mToolbar.visibility = View.VISIBLE

        // Show action bar
        activity.supportActionBar?.show()

        // Clear fullscreen flags
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore screen orientation
        activity.requestedOrientation = originalOrientation

        // Show system UI again
        showSystemUI()

        customViewCallback?.onCustomViewHidden()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.apply {
                hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LOW_PROFILE)
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above
            activity.window.setDecorFitsSystemWindows(true)
            activity.window.insetsController?.show(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)

        if (newProgress < 100) {
            activity.mProgressBar.visibility = View.VISIBLE
            activity.mProgressBar.progress = newProgress
        } else {
            activity.mProgressBar.progress = 0
        }
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val newWebView = WebView(activity)
        newWebView.webViewClient = MyWebViewClient(activity, activity)

        val transport = resultMsg.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg.sendToTarget()

        return true
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        activity.supportActionBar?.subtitle = title.toString()
        super.onReceivedTitle(view, title)
    }

    // File chooser for Android 5.0+ (API 21+)
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("WebChrome", "onShowFileChooser called")

        // Cancel any existing callback
        if (this.filePathCallback != null) {
            Log.d("WebChrome", "Canceling previous callback")
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
        }

        // Store the new callback
        this.filePathCallback = filePathCallback
        Log.d("WebChrome", "New callback stored")

        try {
            activity.openFileChooser(fileChooserParams)
            return true
        } catch (e: Exception) {
            Log.e("WebChrome", "Error opening file chooser", e)
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            e.printStackTrace()
            return false
        }
    }

    // Handle the file chooser result
    fun handleFileChooserResult(resultCode: Int, data: android.content.Intent?) {
        Log.d("WebChrome", "handleFileChooserResult called - resultCode: $resultCode")

        if (filePathCallback == null) {
            Log.e("WebChrome", "filePathCallback is null!")
            return
        }

        val results = FileChooserHelper.handleActivityResult(resultCode, data)
        Log.d("WebChrome", "Results: ${results?.size ?: 0} files")

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null

        Log.d("WebChrome", "Callback completed and cleared")
    }

    // Cancel file chooser
    fun cancelFileChooser() {
        Log.d("WebChrome", "cancelFileChooser called")
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }
}