package com.ali8haider.webview
import android.content.pm.ActivityInfo
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout

@Suppress("DEPRECATION")
class MyWebChromeClient(private val activity: MainActivity) : WebChromeClient() {

    var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation = 0
    private var originalSystemUiVisibility = 0

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {

        Log.d("WebChrome", "onShowCustomView called")

        if (customView != null) {
            Log.d("WebChrome", "customView already exists, hiding it")
            onHideCustomView()
            return
        }

        customView = view
        customViewCallback = callback
        originalOrientation = activity.requestedOrientation
        originalSystemUiVisibility = activity.window.decorView.systemUiVisibility

        Log.d("WebChrome", "Hiding WebView and Toolbar")
        activity.mWebView.visibility = View.GONE
//            activity.myToolBar.visibility = View.GONE

        Log.d("WebChrome", "Showing fullscreen container")
        activity.mFrameLayout.visibility = View.VISIBLE
        activity.mFrameLayout.addView(
            customView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        Log.d("WebChrome", "Setting fullscreen flags")
        activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        Log.d("WebChrome", "Setting landscape orientation")
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onHideCustomView() {
        if (customView == null) return

        // Remove fullscreen view
        activity.mFrameLayout.removeView(customView)
        activity.mFrameLayout.visibility = View.GONE

        // Show WebView and Toolbar again
        activity.mWebView.visibility = View.VISIBLE
//            activity.myToolBar.visibility = View.VISIBLE  // ADD THIS LINE

        // Restore original UI state
        activity.window.decorView.systemUiVisibility = originalSystemUiVisibility
        activity.requestedOrientation = originalOrientation

        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {/*activity.mProgressBar.visibility = View.VISIBLE
            activity.mProgressBar.progress = newProgress
            if (newProgress == 100) {
                activity.mProgressBar.visibility = View.INVISIBLE
                activity.mSwipeRefreshLayout.isEnabled = true
            }*/
        super.onProgressChanged(view, newProgress)


        if (newProgress < 100) {
            activity.mProgressBar.visibility = View.VISIBLE
            activity.mProgressBar.progress = newProgress
        } else {
            activity.mProgressBar.visibility = View.GONE
        }
    }

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        // Create a new WebView for the new window
        val newWebView = WebView(activity)
        newWebView.webViewClient = MyWebViewClient(activity, activity)

        // Send the new WebView back via the message
        val transport = resultMsg.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg.sendToTarget()

        return true
    }


}