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
    private lateinit var mToolbar: Toolbar
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
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

        hostname = getSavedHomePage() ?: getString(R.string.google)

        initializeViews()
        setActionBar()
        checkStoragePermission()
        setupWebView()
        restoreSavedInstanceState(savedInstanceState)
        setupSwipeRefreshLayout()
        setupDownloadListener()
        setupNavigationDrawer()
        setupBackPressHandler()
    }

    private fun initializeViews() {
        mToolbar = findViewById(R.id.mToolbar)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)

        // Initialize clients HERE
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)

        // Set WebView clients
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, mDrawerLayout, mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val message = when (item.itemId) {
            R.id.nav_menu1_option1 -> "Menu 1 - Option 1 Selected"
            R.id.nav_menu1_option2 -> "Menu 1 - Option 2 Selected"
            R.id.nav_menu2_option1 -> "Menu 2 - Option 1 Selected"
            R.id.nav_menu2_option2 -> "Menu 2 - Option 2 Selected"
            else -> return false
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun resetHomePage() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { remove(KEY_HOMEPAGE) }
        mWebView.loadUrl(getString(R.string.google))
        isFirstLoad = true
    }

    private fun saveHomePage(url: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_HOMEPAGE, url) }
    }

    private fun getLastUrl(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_URL, null)
    }

    fun saveUrl(url: String) {
        if (!url.contains("No Internet") && !url.startsWith("file:///android_asset/")) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_LAST_URL, url) }
        }
    }

    /*
        Get Save homepage URL from SharedPreferences
    */

    private fun getSavedHomePage(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOMEPAGE, null)
    }

    fun setHomepageFromFirstLoad(url: String?) {
        if (isFirstLoad && url != null && !url.contains("No Internet") && !url.startsWith("file:///android_asset/")) {
            hostname = url
            saveHomePage(url)
            isFirstLoad = false
        }
    }

    private fun setActionBar() {
        setSupportActionBar(mToolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowCustomEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        setupSearchView(menu)
        return true
    }

    private fun setupSearchView(menu: Menu?) {
        val searchItem = menu?.findItem(R.id.action_search)
        searchView = (searchItem?.actionView as? SearchView)?.apply {
            queryHint = "Enter URL or search..."
            maxWidth = Integer.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { loadUrl(it) }
                    clearFocus()
                    searchItem.collapseActionView()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean = false
            })
        }
    }

    private fun loadUrl(query: String) {
        val url = when {
            query.startsWith("http://") || query.startsWith("https://") -> query
            query.contains(".") -> "https://$query"
            else -> "https://www.google.com/search?q=$query"
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
                resetHomePage()
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
        mWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setGeolocationEnabled(true)
            loadWithOverviewMode = true
        }
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
            setOnRefreshListener { mWebView.reload() }
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


    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        when {
            savedInstanceState != null -> {
                mWebView.restoreState(savedInstanceState)
                isFirstLoad = false
            }

            else -> {
                val lastUrl = getLastUrl()
                mWebView.loadUrl(lastUrl ?: hostname)
            }
        }
    }

    // Handle back button press to navigate within the webView
    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when {
                searchView?.isIconified == false -> {
                    searchView?.isIconified = true
                }

                mDrawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    mDrawerLayout.closeDrawer(GravityCompat.START)
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