package com.ali8haider.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    private lateinit var mWebView: WebView

    //    private lateinit var myToolBar: Toolbar
    private lateinit var mFrameLayout: FrameLayout

    //    private lateinit var mProgressBar: ProgressBar
//    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
//    private lateinit var mOnScrollChangedListener: OnScrollChangedListener
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var myWebChromeClient: MyWebChromeClient
    private lateinit var myWebViewClient: MyWebViewClient
    private lateinit var hostname: String
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        myToolBar = findViewById(R.id.mToolbar)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
//        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
//        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this)
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient

        hostname = "https://m.youtube.com"

//        setupSwipeRefreshLayout()
        setupWebView()
        restoreSavedInstanceState(savedInstanceState)

        // Handle back button press to navigate within the webView
        onBackPressedDispatcher.addCallback(this, callback)

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {

        val webSettings: WebSettings = mWebView.settings

        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true  // for better web app support
        webSettings.mediaPlaybackRequiresUserGesture = false  // Enable media playback
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null) // hardware acceleration
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.userAgentString = mWebView.settings.userAgentString // for better compatibility
        webSettings.allowFileAccess = true // Allow file access
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allow mixed content
        webSettings.useWideViewPort = true;
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.setGeolocationEnabled(true)
        webSettings.loadWithOverviewMode = true
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.mediaPlaybackRequiresUserGesture = false


    }

    /*private fun setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(R.color.purple, R.color.green, R.color.blue, R.color.orange)
        mSwipeRefreshLayout.setOnRefreshListener {
            mWebView.reload()
        }
    }*/


    class MyWebViewClient(private val activity: MainActivity) : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//            activity.mSwipeRefreshLayout.isRefreshing = false
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
//            activity.mSwipeRefreshLayout.isRefreshing = false
            activity.saveUrl(url.toString())
            super.onPageFinished(view, url)
        }


    }

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
        }

    }


    override fun onSaveInstanceState(outState: Bundle) {
        mWebView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mWebView.restoreState(savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }


    fun saveUrl(url: String) {
        if (!url.contains("No Internet") && !url.startsWith("file:///android_asset/")) {
            val sharedPreferences: SharedPreferences = getSharedPreferences("url", MODE_PRIVATE)
            sharedPreferences.edit().also {
                it.putString("url", url)
                it.apply()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        // This code will execute when the activity becomes visible to the user.
        Log.d(tag, "onStart called - Activity is now visible")
        // You can perform actions here that need to happen when the activity starts,
        // such as registering listeners, starting animations, or updating UI elements.
        /*mSwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener {
            OnScrollChangedListener {
                if (mWebView.scrollY == 0) mSwipeRefreshLayout.setEnabled(true);
                else mSwipeRefreshLayout.setEnabled(false);
            }
        }*/
    }


    override fun onStop() {
        Log.d(tag, "onStop called")
//        mSwipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener)
        super.onStop()
    }


    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        val intent = getIntent()
        val url = intent.getStringExtra("url")

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            if (url != null) mWebView.loadUrl(hostname);
            else loadUrl();
        }
    }

    fun loadUrl() {
        val loadUrl: SharedPreferences = getSharedPreferences("SAVE_URL", Context.MODE_PRIVATE)
        mWebView.loadUrl(loadUrl.getString("URL", hostname).toString())
    }

    // Handle back button press to navigate within the webView
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // First check if we're in fullscreen mode
            if (myWebChromeClient.customView != null) {
                myWebChromeClient.onHideCustomView()
                return
            }

            // Then check WebView navigation
            if (mWebView.canGoBack()) {
                mWebView.goBack()
            } else {
                finish()
            }
        }
    }


}