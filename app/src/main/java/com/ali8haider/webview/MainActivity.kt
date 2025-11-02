package com.ali8haider.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ali8haider.webview.util.DownloadHandler
import com.ali8haider.webview.util.PermissionUtil
import com.ali8haider.webview.util.UrlHandler
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mWebView: WebView

    lateinit var mFrameLayout: FrameLayout
    lateinit var mProgressBar: ProgressBar
    lateinit var mToolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var myWebChromeClient: MyWebChromeClient
    private lateinit var myWebViewClient: MyWebViewClient
    private lateinit var hostname: String
    private var searchView: SearchView? = null
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

        initializeViews()

        hostname = getSavedHomePage() ?: getString(R.string.google)

        setActionBar()
        checkStoragePermission()
        setupWebView()
        restoreSavedInstanceState(savedInstanceState)
        setupSwipeRefreshLayout()
        setupDownloadListener()

        // Setup Hamburger Icon
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Handle back button press to navigate within the webView
        onBackPressedDispatcher.addCallback(this, callback)

    }

    private fun initializeViews() {
        mToolbar = findViewById(R.id.mToolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)

        navigationView.setNavigationItemSelectedListener(this)
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)
        // Set WebView clients
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_menu1_option1 -> {
                Toast.makeText(this, "Menu 1 - Option 1 Selected", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_menu1_option2 -> {
                Toast.makeText(this, "Menu 1 - Option 2 Selected", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_menu2_option1 -> {
                Toast.makeText(this, "Menu 2 - Option 1 Selected", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_menu2_option2 -> {
                Toast.makeText(this, "Menu 2 - Option 2 Selected", Toast.LENGTH_SHORT).show()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Suppress("unused")
    fun restHomePage() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_HOMEPAGE) }
        mWebView.loadUrl(getString(R.string.google))
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
        prefs.edit { putString(KEY_HOMEPAGE, url) }
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
//        updateHomeButton(url)
    }

    private fun setActionBar() {
        setSupportActionBar(mToolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowCustomEnabled(true)
        }
    }

//    fun updateHomeButton(url: String?) {
//        val isHomePage = url == hostname
//        supportActionBar?.setDisplayHomeAsUpEnabled(!isHomePage)
//        supportActionBar?.setHomeButtonEnabled(!isHomePage)
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        searchView = searchItem?.actionView as? SearchView

        searchView?.apply {
            queryHint = "Enter URL or search..."
            maxWidth = Integer.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        loadUrl(it)
                    }
                    searchView?.clearFocus()
                    menu?.findItem(R.id.action_search)?.collapseActionView()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })
        }

        return true
    }

    private fun loadUrl(query: String) {
        val url = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else if (query.contains(".")) {
            "https://$query"
        } else {
            "https://www.google.com/search?q=$query"
        }
        mWebView.loadUrl(url)
        Toast.makeText(this, "Loading: $url", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_home -> {
                restHomePage()
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

            R.id.action_exit -> {
                finish()
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
            prefs.edit { putString(KEY_LAST_URL, url) }
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

    // Handle back button press to navigate within the webView
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when {
                searchView?.isIconified == false -> {
                    searchView?.isIconified = true
                }

                drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                myWebChromeClient.customView != null -> {
                    myWebChromeClient.onHideCustomView()
                }

                mWebView.canGoBack() -> {
                    mWebView.goBack()
                }

                else -> {
                    finish()
                }
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