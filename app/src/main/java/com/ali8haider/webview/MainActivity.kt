package com.ali8haider.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ali8haider.webview.util.DownloadHandler
import com.ali8haider.webview.util.PermissionUtil
import com.ali8haider.webview.util.UrlHandler

class MainActivity : AppCompatActivity() {

    lateinit var mWebView: WebView

    //    private lateinit var myToolBar: Toolbar
    lateinit var mFrameLayout: FrameLayout
    lateinit var mProgressBar: ProgressBar
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var myWebChromeClient: MyWebChromeClient
    private lateinit var myWebViewClient: MyWebViewClient
    private lateinit var hostname: String

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        myToolBar = findViewById(R.id.mToolbar)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)

        // THESE TWO LINES ARE CRITICAL
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient

//        hostname = getString(R.string.webviewLink)
        hostname = "https://m.gsmarena.com/oppo_find_x9_pro-14094.php"
        mWebView.loadUrl(hostname)

        checkStoragePermission()
        setupWebView()
        restoreSavedInstanceState(savedInstanceState)
        setupSwipeRefreshLayout()
        setupDownloadListener()
        loadUrl()

        // Handle back button press to navigate within the webView
        onBackPressedDispatcher.addCallback(this, callback)

    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupDownloadListener() {
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            DownloadHandler.downloadLink(this, url, userAgent, contentDisposition, mimeType)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {

        val webSettings: WebSettings = mWebView.settings

        /*web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
web.getSettings().setPluginsEnabled(true);
web.getSettings().setSupportMultipleWindows(false);
web.getSettings().setSupportZoom(true);
web.setVerticalScrollBarEnabled(false);
web.setHorizontalScrollBarEnabled(false);
web.getSettings().setBuiltInZoomControls(true);
web.getSettings().setLoadWithOverviewMode(true);
web.getSettings().setUseWideViewPort(true);
web.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
web.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
web.getSettings().setAllowFileAccess(true);
web.getSettings().setAppCacheEnabled(true);*/

        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true  // for better web app support
        webSettings.mediaPlaybackRequiresUserGesture = false  // Enable media playback
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.userAgentString = mWebView.settings.userAgentString // for better compatibility
        webSettings.allowFileAccess = true // Allow file access
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allow mixed content
        webSettings.useWideViewPort = true
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.setGeolocationEnabled(true)
        webSettings.loadWithOverviewMode = true
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.mediaPlaybackRequiresUserGesture = false
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null) // hardware acceleration

    }

    private fun setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light
        )
        mSwipeRefreshLayout.setOnRefreshListener {
            mWebView.reload()
        }
        mWebView.viewTreeObserver.addOnScrollChangedListener {
            mSwipeRefreshLayout.isEnabled = mWebView.scrollY == 0
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

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        val intent = getIntent()
        val url = intent.getStringExtra("url")

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState)
        } else {
            if (url != null) mWebView.loadUrl(hostname)
            else loadUrl()
        }
    }

    fun loadUrl() {
        val loadUrl: SharedPreferences = getSharedPreferences("SAVE_URL", Context.MODE_PRIVATE)
        mWebView.loadUrl(loadUrl.getString("URL", hostname).toString())
    }

    // Handle back button press to navigate within the webView
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
//             First check if we're in fullscreen mode
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionUtil.MY_PERMISSIONS_REQUEST_DOWNLOAD -> {
                DownloadHandler.handlePermissionResult(this, grantResults)
            }

            PermissionUtil.MY_PERMISSIONS_REQUEST_SMS -> {
                UrlHandler.handleSmsPermissionResult(this, grantResults)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}