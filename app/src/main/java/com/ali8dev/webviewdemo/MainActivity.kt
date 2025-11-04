package com.ali8dev.webviewdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
    var justClickedHome = false
    private var searchView: SearchView? = null
    private var isFirstLoad = true
    private val mAlertDialog = AlertDialog()

    // Desktop mode variables
    private var isDesktopMode = false
    private var desktopModeMenuItem: MenuItem? = null

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
        private const val KEY_DESKTOP_MODE = "desktop_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hostname = getSavedHomePage() ?: getString(R.string.google)

        initializeViews()
        setupTouchListenersForSearchView()
        setActionBar()
        checkStoragePermission()

        // Load desktop mode preference and detect tablet
        loadDesktopModePreference()

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
     * Check if device is tablet or has large screen
     */
    private fun isTablet(): Boolean {
        val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * Load desktop mode preference or detect tablet
     */
    private fun loadDesktopModePreference() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if this is first time opening the app
        val isFirstRun = !prefs.contains(KEY_DESKTOP_MODE)

        if (isFirstRun && isTablet()) {
            // First time on tablet - enable desktop mode by default
            isDesktopMode = true
            saveDesktopModePreference(true)
        } else {
            // Load saved preference
            isDesktopMode = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        }
    }

    /**
     * Save desktop mode preference
     */
    private fun saveDesktopModePreference(enabled: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_DESKTOP_MODE, enabled)
        }
    }

    /**
     * Toggle desktop mode on/off
     */
    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        saveDesktopModePreference(isDesktopMode)
        applyDesktopMode()
        updateDesktopModeMenuItem()

        // Reload current page to apply changes
        mWebView.reload()

        val message = if (isDesktopMode) "Desktop mode enabled" else "Desktop mode disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Apply desktop mode settings to WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun applyDesktopMode() {
//        mWebView.settings.apply {
        if (isDesktopMode) {
            // Desktop mode settings

//                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
//                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            mWebView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            mWebView.settings.useWideViewPort = true
            mWebView.settings.loadWithOverviewMode = true

            // Let the page scale naturally (don't force zoom)
            mWebView.setInitialScale(0)

            // Enable zoom controls for desktop mode
            mWebView.settings.setSupportZoom(true)
            mWebView.settings.builtInZoomControls = true
            mWebView.settings.displayZoomControls = false


        } else {
            // Mobile mode settings (default)
            mWebView.settings.userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity)
            mWebView.settings.useWideViewPort = true
            mWebView.settings.loadWithOverviewMode = true

            // Reset scale
            mWebView.setInitialScale(0)

            // Keep zoom controls enabled
            mWebView.settings.setSupportZoom(true)
            mWebView.settings.builtInZoomControls = true
            mWebView.settings.displayZoomControls = false


        }
//        }
    }

    /**
     * Update desktop mode menu item checkbox state
     */
    private fun updateDesktopModeMenuItem() {
        desktopModeMenuItem?.isChecked = isDesktopMode
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

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        }

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

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
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
            this, mDrawerLayout, mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
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
                    this, getString(R.string.privacy_policy),
                    readAssetFile(this, "privacy_policy.txt")
                )
            }

            R.id.nav_terms_service -> {
                mAlertDialog.showAlertDialog(
                    this, getString(R.string.terms_and_conditions),
                    readAssetFile(this, "terms_and_conditions.txt")
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

        allowBackNavigation = false
        justClickedHome = true

        mWebView.loadUrl(getString(R.string.google))
        isFirstLoad = true
    }

    private fun saveHomePage(url: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_HOMEPAGE, url)
        }
    }

    private fun getSavedHomePage(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOMEPAGE, null)
    }

    fun setHomepageFromFirstLoad(url: String?) {
        if (isFirstLoad && url != null && !url.contains("NoInternet") &&
            !url.startsWith("file:///android_asset/")
        ) {
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

        // Setup desktop mode menu item
        desktopModeMenuItem = menu?.findItem(R.id.action_desktop_mode)
        updateDesktopModeMenuItem()

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

            // Add this: Set focus change listener to close when focus is lost
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    clearFocus()
                    searchItem.collapseActionView()
                }
            }
        }
        setupWebViewTouchListener(searchItem)
    }

    private fun setupWebViewTouchListener(searchItem: MenuItem?) {
        mWebView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                // Re-enable focusing only after user interaction
                val enableFocusJS = """
                (function() {
                    const inputs = document.querySelectorAll('input, textarea');
                    inputs.forEach(el => {
                        if (el._focus) el.focus = el._focus;
                    });
                    // Stop the MutationObserver if it exists
                    if (window._focusObserver) {
                        window._focusObserver.disconnect();
                        window._focusObserver = null;
                    }
                })();
            """
                mWebView.evaluateJavascript(enableFocusJS, null)
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (searchView?.isIconified == false) {
                    searchView?.isIconified = true
                    searchItem?.collapseActionView()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    // Add this to your onCreate() method after initializing views
    private fun setupTouchListenersForSearchView() {
        // Get the root view
        val rootView = findViewById<View>(android.R.id.content)

        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (searchView?.isIconified == false) {
                    searchView?.isIconified = true
                    val searchItem = mToolbar.menu?.findItem(R.id.action_search)
                    searchItem?.collapseActionView()
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            // Get the search view coordinates
            val searchViewCoords = IntArray(2)
            searchView?.getLocationOnScreen(searchViewCoords)

            val x = ev.rawX
            val y = ev.rawY

            // Check if touch is outside search view
            if (searchView != null && !searchView!!.isIconified) {
                val searchViewWidth = searchView!!.width
                val searchViewHeight = searchView!!.height

                // If touch is outside search view bounds
                if (x < searchViewCoords[0] || x > searchViewCoords[0] + searchViewWidth ||
                    y < searchViewCoords[1] || y > searchViewCoords[1] + searchViewHeight
                ) {

                    // Close search view
                    searchView?.isIconified = true
                    val searchItem = mToolbar.menu?.findItem(R.id.action_search)
                    searchItem?.collapseActionView()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loadUrl(query: String) {
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
                true
            }

            R.id.action_desktop_mode -> {
                toggleDesktopMode()
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
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }

    @Suppress("DEPRECATION")
    fun openFileChooser(fileChooserParams: WebChromeClient.FileChooserParams?) {
        try {
            Log.d("MainActivity", "openFileChooser called")

            val acceptTypes = fileChooserParams?.acceptTypes
            val needsCamera = acceptTypes != null && acceptTypes.any { it.contains("image") }

            if (needsCamera && !FileChooserHelper.hasCameraPermission(this)) {
                Log.d("MainActivity", "Requesting camera permission")
                FileChooserHelper.requestCameraPermission(this, fileChooserParams)
                return
            }

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

        myWebChromeClient.cancelFileChooser()

        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }

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
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
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
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setGeolocationEnabled(true)
            setRenderPriority(WebSettings.RenderPriority.HIGH)
        }

        // 👇 prevent auto keyboard on load
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS



        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        applyDesktopMode()
    }


    private fun setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
            setOnRefreshListener {
                if (isInternetAvailable() || mWebView.url?.contains("NoInternet") == true) {
                    mWebView.reload()
                } else {
                    isRefreshing = false
                    Toast.makeText(
                        context, getString(R.string.no_internet_connection),
                        Toast.LENGTH_SHORT
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

                allowBackNavigation && mWebView.canGoBack() -> {
                    mWebView.goBack()
                }

                else -> {
                    finish()
                }
            }
        }
    }

    fun saveUrl(url: String) {
        if (!url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
            getSharedPreferences(SAVE_URL_PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_LAST_URL, url)
            }
        }
    }

    private fun getLastUrl(): String? {
        return getSharedPreferences(SAVE_URL_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_URL, null)
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