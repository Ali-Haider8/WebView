package com.ali8dev.webviewdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
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
import com.ali8dev.webviewdemo.util.AlertDialog
import com.ali8dev.webviewdemo.util.DownloadHandler
import com.ali8dev.webviewdemo.util.FileChooserHelper
import com.ali8dev.webviewdemo.util.MyWebChromeClient
import com.ali8dev.webviewdemo.util.MyWebViewClient
import com.ali8dev.webviewdemo.util.PermissionUtil
import com.ali8dev.webviewdemo.util.UrlHandler
import com.google.android.material.navigation.NavigationView
import java.io.BufferedReader
import java.io.InputStreamReader

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
    var allowBackNavigation = true
    var justClickedHome = false  // ADD THIS LINE
    private var searchView: SearchView? = null
    private var isFirstLoad = true
    private val mAlertDialog = AlertDialog()

    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkAvailable = true

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val PREFS_NAME = "WebViewPrefs"
        private const val KEY_HOMEPAGE = "homepage_url"
        private const val SAVE_URL_PREFS = "SAVE_URL"
        private const val KEY_LAST_URL = "URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hostname = getSavedHomePage() ?: getString(R.string.google)

        initializeViews()
        setActionBar()
        checkStoragePermission()
        setupWebView()
        setupNetworkMonitoring()
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

        // Initialize clients
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)

        // Set WebView clients
        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient
    }

    /**
     * Setup real-time network monitoring
     */
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        isNetworkAvailable = true
                        // Auto-reload if on NoInternet page
                        if (mWebView.url?.contains("NoInternet") == true) {
                            val lastUrl = getLastUrl()
                            if (lastUrl != null && !lastUrl.contains("NoInternet")) {
                                mWebView.loadUrl(lastUrl)
                            } else {
                                mWebView.loadUrl(hostname)
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    runOnUiThread {
                        isNetworkAvailable = false
                        loadNoInternetPage()
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network, capabilities: NetworkCapabilities
                ) {
                    runOnUiThread {
                        isNetworkAvailable = capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET
                        ) && capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED
                        )
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()

            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        }

        // Initial check
        isNetworkAvailable = isInternetAvailable()
    }

    /**
     * Check if internet is available
     */
    fun isInternetAvailable(): Boolean {
        val cm = connectivityManager ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION") val networkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION") return networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Load NoInternet page from assets
     */
    fun loadNoInternetPage() {
        mWebView.loadUrl("file:///android_asset/NoInternet.html")
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun readAssetFile(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error loading file: ${e.message}"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> resetHomePage()
            R.id.nav_privacy_policy -> {
                mAlertDialog.showAlertDialog(
                    this, getString(R.string.privacy_policy), readAssetFile(this, "privacy_policy.txt")
                )
            }

            R.id.nav_terms_service -> {
                mAlertDialog.showAlertDialog(
                    this, getString(R.string.terms_and_conditions), readAssetFile(this, "terms_and_conditions.txt")
                )
            }

            R.id.nav_settings -> {
                Toast.makeText(this, getString(R.string.settings), Toast.LENGTH_SHORT).show()
            }

            R.id.nav_about -> {
                Toast.makeText(this, getString(R.string.about), Toast.LENGTH_SHORT).show()
            }

            else -> return false
        }

        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun resetHomePage() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_HOMEPAGE)
        }

        // Set flags to prevent back navigation
        allowBackNavigation = false
        justClickedHome = true  // ADD THIS LINE

        // Load the homepage
        mWebView.loadUrl(getString(R.string.google))

        isFirstLoad = true
    }

    private fun saveHomePage(url: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_HOMEPAGE, url)
        }
    }

    private fun getSavedHomePage(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_HOMEPAGE, null)
    }

    fun setHomepageFromFirstLoad(url: String?) {
        if (isFirstLoad && url != null && !url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
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
            queryHint = context.getString(R.string.search_view_query_hint)
            maxWidth = Integer.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { loadUrl(it) }
                    clearFocus()
                    searchItem.collapseActionView()
                    return true
                }

                override fun onQueryTextChange(newText: String?) = false
            })
        }
    }

    private fun loadUrl(query: String) {
        // Check internet before loading
        if (!isInternetAvailable()) {
            Toast.makeText(
                this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT
            ).show()
            loadNoInternetPage()
            return
        }

        val url = when {
            query.startsWith("http://") || query.startsWith("https://") -> query
            query.contains(".") -> "https://$query"
            else -> "https://www.google.com/search?q=$query"
        }
        mWebView.loadUrl(url)
        Toast.makeText(this, getString(R.string.loading, url), Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_desktop_mode -> {
//
                true
            }

            R.id.action_refresh -> {
                if (isInternetAvailable() || mWebView.url?.contains("NoInternet") == true) {
                    mWebView.reload()
                } else {
                    Toast.makeText(
                        this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT
                    ).show()
                }
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
        val currentUrl = mWebView.url
        if (currentUrl.isNullOrEmpty() || currentUrl.contains("NoInternet")) {
            Toast.makeText(
                this, getString(R.string.no_page_to_share), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, currentUrl)
            type = "text/plain"
        }
        startActivity(
            Intent.createChooser(shareIntent, getString(R.string.share_via))
        )
    }

    // Add these methods to your MainActivity class

    /**
     * Open file chooser
     */
    @Suppress("DEPRECATION")
    fun openFileChooser(fileChooserParams: WebChromeClient.FileChooserParams?) {
        try {
            Log.d("MainActivity", "openFileChooser called")

            // Check if accept types include camera
            val acceptTypes = fileChooserParams?.acceptTypes
            val needsCamera = acceptTypes != null && acceptTypes.any { it.contains("image") }

            // Request camera permission if needed
            if (needsCamera && !FileChooserHelper.hasCameraPermission(this)) {
                Log.d("MainActivity", "Requesting camera permission")
                FileChooserHelper.requestCameraPermission(this, fileChooserParams)
                return
            }

            // Create and launch file chooser intent
            val intent = FileChooserHelper.createFileChooserIntent(this, fileChooserParams)
            Log.d("MainActivity", "Launching file chooser")
            startActivityForResult(intent, FileChooserHelper.FILE_CHOOSER_REQUEST_CODE)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening file chooser", e)
            Toast.makeText(this, getString(R.string.cannot_open_file_chooser), Toast.LENGTH_SHORT).show()
            myWebChromeClient.cancelFileChooser()
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("MainActivity", "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == FileChooserHelper.FILE_CHOOSER_REQUEST_CODE) {
            Log.d("MainActivity", "File chooser result received")
            myWebChromeClient.handleFileChooserResult(resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d("MainActivity", "onRequestPermissionsResult - requestCode: $requestCode")

        when (requestCode) {
            PermissionUtil.MY_PERMISSIONS_REQUEST_DOWNLOAD -> {
                DownloadHandler.handlePermissionResult(this, grantResults)
            }

            PermissionUtil.MY_PERMISSIONS_REQUEST_SMS -> {
                UrlHandler.handleSmsPermissionResult(this, grantResults)
            }

            FileChooserHelper.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Camera permission granted")
                    // Permission granted, open file chooser
                    val params = FileChooserHelper.getPendingFileChooserParams()
                    openFileChooser(params)
                } else {
                    Log.d("MainActivity", "Camera permission denied")
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                    myWebChromeClient.cancelFileChooser()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel any pending file chooser
        myWebChromeClient.cancelFileChooser()

        // Unregister network callback
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }

        // Clean up WebView
        mWebView.apply {
            stopLoading()
            destroy()
        }
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

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        mWebView.settings.apply {
            // Core settings
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // Media and content
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true

            // Cache settings
            cacheMode = WebSettings.LOAD_DEFAULT  // Changed from LOAD_CACHE_ELSE_NETWORK

            // Display settings
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

            // Security settings - DO NOT enable these for regular websites
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE  // Changed
            // REMOVE these two lines - they break modern websites:
            // allowFileAccessFromFileURLs = true
            // allowUniversalAccessFromFileURLs = true

            // Window and geolocation
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setGeolocationEnabled(true)

            // Performance improvements
            setRenderPriority(WebSettings.RenderPriority.HIGH)

//            userAgentString = "Mozilla/5.0 (Android 10; Mobile; rv:89.0) Gecko/89.0 Firefox/89.0"


//            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light
            )
            setOnRefreshListener {
                if (isInternetAvailable() || mWebView.url?.contains("NoInternet") == true) {
                    mWebView.reload()
                } else {
                    isRefreshing = false
                    Toast.makeText(
                        context, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mWebView.viewTreeObserver.addOnScrollChangedListener {
            mSwipeRefreshLayout.isEnabled = mWebView.scrollY == 0
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWebView.saveState(outState)
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
                if (!isInternetAvailable()) {
                    loadNoInternetPage()
                } else {
                    val lastUrl = getLastUrl()
                    mWebView.loadUrl(lastUrl ?: hostname)
                }
            }
        }
    }

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
                // Check allowBackNavigation FIRST
                allowBackNavigation && mWebView.canGoBack() -> {
                    mWebView.goBack()
                }

                else -> {
                    finish()
                }
            }
        }
    }


    /**
     * Save current URL to SharedPreferences
     */
    fun saveUrl(url: String) {
        if (!url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
            getSharedPreferences(SAVE_URL_PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_LAST_URL, url)
            }
        }
    }

    /**
     * Get last saved URL from SharedPreferences
     */
    private fun getLastUrl(): String? {
        return getSharedPreferences(SAVE_URL_PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_URL, null)
    }


    override fun onPause() {
        super.onPause()
        mWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mWebView.onResume()
    }
}