package com.ali8dev.webviewdemo.util

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
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

        // Set screen orientation to landscape
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE


        // Hide the WebView
        activity.mWebView.visibility = View.GONE

        // Show fullscreen container
        activity.mFrameLayout.visibility = View.VISIBLE
        activity.mFrameLayout.addView(customView)

        // Hide action bar if present
        activity.supportActionBar?.hide()
    }

    override fun onHideCustomView() {
        if (customView == null) {
            return
        }

        // Set screen orientation back to portrait
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        // Hide fullscreen container
        activity.mFrameLayout.visibility = View.GONE
        activity.mFrameLayout.removeView(customView)
        customView = null

        // Show the WebView again
        activity.mWebView.visibility = View.VISIBLE

        // Show action bar if present
        activity.supportActionBar?.show()

        customViewCallback?.onCustomViewHidden()
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