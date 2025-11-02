package com.ali8haider.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ali8haider.webview.util.DownloadHandler
import com.ali8haider.webview.util.PermissionUtil
import com.ali8haider.webview.util.UrlHandler

class MainActivity : AppCompatActivity() {

    lateinit var mWebView: WebView

    lateinit var mFrameLayout: FrameLayout
    lateinit var mProgressBar: ProgressBar
    lateinit var mToolbar: Toolbar
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var myWebChromeClient: MyWebChromeClient
    private lateinit var myWebViewClient: MyWebViewClient
    private lateinit var hostname: String
    private var isFirstLoad = true // Flag to track first load

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val PREFS_NAME = "WebViewPrefs"
        private const val KEY_HOMEPAGE = "homepage_url"
        private const val KEY_LAST_URL = "last_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.mToolBar)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)

        // THESE TWO LINES ARE CRITICAL
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient

//        hostname = getString(R.string.google)
        hostname = getSavedHomePage() ?: getString(R.string.google)
//        mWebView.loadUrl(hostname)

        setActionBar()
        checkStoragePermission()
        setupWebView()
        restoreSavedInstanceState(savedInstanceState)
        setupSwipeRefreshLayout()
        setupDownloadListener()
//        loadUrl()

        // Handle back button press to navigate within the webView
        onBackPressedDispatcher.addCallback(this, callback)

    }

    @Suppress("unused")
    fun restHomePage() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HOMEPAGE).apply()
        isFirstLoad = true
    }

    /*
        Get Save homepage URL from SharedPreferences
    */

    private fun getSavedHomePage(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOMEPAGE, null)

    }

    /*
    * Save hompage URL to SharedPreferences
    * */

    private fun saveHomePage(url: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOMEPAGE, url).apply()
    }

    /*
    * Get Last loaded URL from SharedPreferences
    * */

    private fun getLastUrl(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_URL, null)
    }

    /*
    * Set The homepage based on the First  URL Loaded
    * Call this from MyWebViewClient.onPageFinished()
    * */

    fun setHomepageFromFirstLoad(url: String?) {
        if (isFirstLoad && url != null && !url.contains("No Internet") && !url.startsWith("file:///android_asset/")) {
            hostname = url
            saveHomePage(url)
            isFirstLoad = false

            // Update home button immediately
        }
        updateHomeButton(url)
    }

    private fun setActionBar() {
        setSupportActionBar(mToolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowCustomEnabled(true)
        }
    }

    fun updateHomeButton(url: String?) {
        val isHomePage = url == hostname
        supportActionBar?.setDisplayHomeAsUpEnabled(!isHomePage)
        supportActionBar?.setHomeButtonEnabled(!isHomePage)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mWebView.loadUrl(hostname)
                true
            }

            R.id.action_refresh -> {
                mWebView.reload()
                true
            }

            R.id.action_share -> {
                shareCurrentPage()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareCurrentPage() {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, mWebView.url)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
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
        super.onRestoreInstanceState(savedInstanceState)
        mWebView.restoreState(savedInstanceState)
        isFirstLoad = false
    }


    fun saveUrl(url: String) {
        if (!url.contains("No Internet") && !url.startsWith("file:///android_asset/")) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_URL, url).apply()
        }
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState)
            isFirstLoad = false
        } else {
            val lastUrl = getLastUrl()
            if (lastUrl != null) mWebView.loadUrl(lastUrl)
            else mWebView.loadUrl(hostname)
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