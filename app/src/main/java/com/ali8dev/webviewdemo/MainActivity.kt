package com.ali8dev.webviewdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ali8dev.webviewdemo.database.FavoriteDatabase
import com.ali8dev.webviewdemo.util.AlertDialog
import com.ali8dev.webviewdemo.util.DownloadHandler
import com.ali8dev.webviewdemo.util.FileChooserHelper
import com.ali8dev.webviewdemo.util.MyWebChromeClient
import com.ali8dev.webviewdemo.util.MyWebViewClient
import com.ali8dev.webviewdemo.util.PermissionUtil
import com.ali8dev.webviewdemo.util.UrlHandler
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.messaging.FirebaseMessaging
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Views
    lateinit var mWebView: WebView
    lateinit var mFrameLayout: FrameLayout
    lateinit var mProgressBar: ProgressBar
    lateinit var mToolbar: Toolbar
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var searchView: SearchView? = null

    // WebView clients
    private lateinit var myWebChromeClient: MyWebChromeClient
    private lateinit var myWebViewClient: MyWebViewClient

    // State
    private var isDesktopMode = false
    private var desktopModeMenuItem: MenuItem? = null
    private var favoriteMenuItem: MenuItem? = null
    var isNetworkAvailable = true


    // Database
    private lateinit var favoriteDatabase: FavoriteDatabase

    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val mAlertDialog = AlertDialog()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val PREFS_NAME = "WebViewPrefs"
        private const val KEY_LAST_URL = "last_url"
        private const val KEY_DESKTOP_MODE = "desktop_mode"
        private const val DEFAULT_URL = "https://google.com/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupToolbar()
        setupWebView()
        setupNetworkMonitoring()
        setupNavigationDrawer()
        setupBackPressHandler()
        checkStoragePermission()
        askNotificationPermission()

        // Initialize favorites database
        favoriteDatabase = FavoriteDatabase(this)

        // Load content
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState)
        } else {
            // Check if coming from FavoritesActivity
            val urlFromFavorites = intent.getStringExtra(FavoritesActivity.EXTRA_URL)
            if (urlFromFavorites != null) {
                mWebView.loadUrl(urlFromFavorites)
            } else {
                loadInitialUrl()
            }
        }

        Firebase.analytics.logEvent("launch app") {
            param("item_name", "My Awesome Product")
        }

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TAG", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // الحصول على الرمز الجديد
            val token = task.result

            // طباعته (للتجربة) أو إرساله للخادم الخاص بك
            Log.d("TAG", "FCM Token: $token")
            // sendTokenToServer(token)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle URL from favorites
        val urlFromFavorites = intent.getStringExtra(FavoritesActivity.EXTRA_URL)
        if (urlFromFavorites != null) {
            mWebView.loadUrl(urlFromFavorites)
        }
    }

    private fun initializeViews() {
        mToolbar = findViewById(R.id.mToolbar)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mFrameLayout = findViewById(R.id.fullscreenContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)

        // Initialize WebView clients
        myWebChromeClient = MyWebChromeClient(this)
        myWebViewClient = MyWebViewClient(this, this)

        mWebView.webChromeClient = myWebChromeClient
        mWebView.webViewClient = myWebViewClient

        // Setup swipe refresh
        mSwipeRefreshLayout.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
            setOnRefreshListener {
                if (isNetworkAvailable || mWebView.url?.contains("NoInternet") == true) {
                    mWebView.reload()
                } else {
                    isRefreshing = false
                    showToast(getString(R.string.no_internet_connection))
                }
            }
        }

        // Enable swipe refresh only at top of page
        mWebView.viewTreeObserver.addOnScrollChangedListener {
            mSwipeRefreshLayout.isEnabled = mWebView.scrollY == 0
        }

        // Setup download listener
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            DownloadHandler.downloadLink(this, url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(mToolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Load desktop mode preference
        loadDesktopModePreference()

        mWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setGeolocationEnabled(true)
        }

        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        applyDesktopMode()
    }

    private fun loadDesktopModePreference() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstRun = !prefs.contains(KEY_DESKTOP_MODE)

        isDesktopMode = if (isFirstRun && isTablet()) {
            saveDesktopModePreference(true)
            true
        } else {
            prefs.getBoolean(KEY_DESKTOP_MODE, false)
        }
    }

    private fun isTablet(): Boolean {
        val screenLayout = resources.configuration.screenLayout and
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyDesktopMode() {
        val settings = mWebView.settings
        val urlToReload = mWebView.url

        if (isDesktopMode) {
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

        } else {
            settings.userAgentString = WebSettings.getDefaultUserAgent(this)
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            mWebView.setInitialScale(0)
        }

        mWebView.clearCache(true)
        mWebView.clearFormData()
        mWebView.clearHistory()
        mWebView.clearSslPreferences()

        if (!urlToReload.isNullOrEmpty() && urlToReload != "about:blank") {
            mWebView.loadUrl(urlToReload)
        } else {
            mWebView.loadUrl(DEFAULT_URL)
        }
    }

    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        saveDesktopModePreference(isDesktopMode)
        applyDesktopMode()
        updateDesktopModeMenuItem()

        val message = if (isDesktopMode) "Desktop mode enabled" else "Mobile mode enabled"
        showToast(message)
    }

    private fun saveDesktopModePreference(enabled: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_DESKTOP_MODE, enabled)
        }
    }

    private fun updateDesktopModeMenuItem() {
        desktopModeMenuItem?.isChecked = isDesktopMode
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        isNetworkAvailable = true
                        if (mWebView.url?.contains("NoInternet") == true) {
                            val lastUrl = getLastUrl() ?: DEFAULT_URL
                            if (!lastUrl.contains("NoInternet")) {
                                mWebView.loadUrl(lastUrl)
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
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        }

        isNetworkAvailable = checkInternetConnection()
    }

    @Suppress("ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE")
    private fun checkInternetConnection(): Boolean {
        val cm = connectivityManager ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION") cm.activeNetworkInfo?.isConnected ?: false
        }
    }

    fun loadNoInternetPage() {
        mWebView.loadUrl("file:///android_asset/NoInternet.html")
    }

    private fun loadInitialUrl() {
        if (!isNetworkAvailable) {
            loadNoInternetPage()
        } else {
            mWebView.loadUrl(getLastUrl() ?: DEFAULT_URL)
        }
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
        when (item.itemId) {
            R.id.nav_home -> mWebView.loadUrl(DEFAULT_URL)
            R.id.nav_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_privacy_policy -> showTextDialog(
                getString(R.string.privacy_policy), "privacy_policy.txt"
            )
            R.id.nav_terms_service -> showTextDialog(
                getString(R.string.terms_and_conditions), "terms_and_conditions.txt"
            )
            R.id.nav_settings -> showToast(getString(R.string.settings))
            R.id.nav_about -> showAboutDialog()
            else -> return false
        }

        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showTextDialog(title: String, fileName: String) {
        val content = readAssetFile(this, fileName)
        mAlertDialog.showAlertDialog(this, title, content)
    }

    private fun readAssetFile(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            }
        } catch (e: Exception) {
            "Error loading file: ${e.message}"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        setupSearchView(menu)
        desktopModeMenuItem = menu?.findItem(R.id.action_desktop_mode)
        favoriteMenuItem = menu?.findItem(R.id.action_favorite)
        updateDesktopModeMenuItem()
        updateFavoriteMenuItem()
        return true
    }

    private fun setupSearchView(menu: Menu?) {
        val searchItem = menu?.findItem(R.id.action_search)
        searchView = (searchItem?.actionView as? SearchView)?.apply {
            queryHint = getString(R.string.search_view_query_hint)
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

        mWebView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (searchView?.isIconified == false) {
                    searchView?.isIconified = true
                    searchItem?.collapseActionView()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun loadUrl(query: String) {
        if (!isNetworkAvailable) {
            showToast(getString(R.string.no_internet_connection))
            loadNoInternetPage()
            return
        }

        val url = when {
            query.startsWith("http://") || query.startsWith("https://") -> query
            query.contains(".") -> "https://$query"
            else -> "https://www.google.com/search?q=$query"
        }
        mWebView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            R.id.action_desktop_mode -> {
                toggleDesktopMode()
                true
            }
            R.id.action_refresh -> {
                if (isNetworkAvailable || mWebView.url?.contains("NoInternet") == true) {
                    mWebView.reload()
                } else {
                    showToast(getString(R.string.no_internet_connection))
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

    private fun toggleFavorite() {
        val currentUrl = mWebView.url
        val currentTitle = mWebView.title

        if (currentUrl.isNullOrEmpty() || currentUrl.contains("NoInternet") ||
            currentUrl.startsWith("file:///android_asset/")) {
            showToast(getString(R.string.no_page_to_share))
            return
        }

        if (favoriteDatabase.isFavorite(currentUrl)) {
            favoriteDatabase.removeFavorite(currentUrl)
            showToast(getString(R.string.favorite_removed))
        } else {
            val title = currentTitle ?: "Untitled"
            favoriteDatabase.addFavorite(title, currentUrl)
            showToast(getString(R.string.favorite_added))
        }

        updateFavoriteMenuItem()
    }

    private fun updateFavoriteMenuItem() {
        val currentUrl = mWebView.url ?: return

        if (currentUrl.contains("NoInternet") || currentUrl.startsWith("file:///android_asset/")) {
            favoriteMenuItem?.isVisible = false
            return
        }

        favoriteMenuItem?.isVisible = true

        if (favoriteDatabase.isFavorite(currentUrl)) {
            favoriteMenuItem?.setIcon(R.drawable.ic_star)
            favoriteMenuItem?.title = getString(R.string.remove_from_favorites)
        } else {
            favoriteMenuItem?.setIcon(R.drawable.ic_star_border)
            favoriteMenuItem?.title = getString(R.string.add_to_favorites)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        updateFavoriteMenuItem()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun shareCurrentPage() {
        val currentUrl = mWebView.url
        if (currentUrl.isNullOrEmpty() || currentUrl.contains("NoInternet")) {
            showToast(getString(R.string.no_page_to_share))
            return
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, currentUrl)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }

    // Add this method to MainActivity.kt

    private fun showAboutDialog() {
        // Get app version
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        val appName = getString(R.string.app_name)
        val devEmail = "ali88dev@gmail.com" // Your email from terms_and_conditions.txt

        // Create custom dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)

        // Find views
        val appNameText = dialogView.findViewById<TextView>(R.id.aboutAppName)
        val versionText = dialogView.findViewById<TextView>(R.id.aboutVersion)
        val emailText = dialogView.findViewById<TextView>(R.id.aboutEmail)
        val updateButton = dialogView.findViewById<Button>(R.id.aboutUpdateButton)

        // Set values
        appNameText.text = appName
        versionText.text = "Version: $versionName"
        emailText.text = "Developer: $devEmail"

        // Create dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_app))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.close)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Handle update button click
        updateButton.setOnClickListener {
            val packageName = applicationContext.packageName
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                // If Play Store not available, open in browser
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
        }

        dialog.show()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // تم منح الإذن
        } else {
            // تم رفض الإذن، يمكنك توضيح أهمية الإشعارات للمستخدم
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // (TIRAMISU = Android 13)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // الإذن ممنوح مسبقاً
            } else {
                // اطلب الإذن
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    searchView?.isIconified == false -> searchView?.isIconified = true
                    mDrawerLayout.isDrawerOpen(GravityCompat.START) ->
                        mDrawerLayout.closeDrawer(GravityCompat.START)
                    myWebChromeClient.customView != null ->
                        myWebChromeClient.onHideCustomView()
                    mWebView.canGoBack() -> mWebView.goBack()
                    else -> finish()
                }
            }
        })
    }

    @Suppress("DEPRECATION")
    fun openFileChooser(fileChooserParams: WebChromeClient.FileChooserParams?) {
        try {
            val acceptTypes = fileChooserParams?.acceptTypes
            val needsCamera = acceptTypes?.any { it.contains("image") } ?: false

            if (needsCamera && !FileChooserHelper.hasCameraPermission(this)) {
                FileChooserHelper.requestCameraPermission(this, fileChooserParams)
                return
            }

            val intent = FileChooserHelper.createFileChooserIntent(this, fileChooserParams)
            startActivityForResult(intent, FileChooserHelper.FILE_CHOOSER_REQUEST_CODE)
        } catch (_: Exception) {
            showToast(getString(R.string.cannot_open_file_chooser))
            myWebChromeClient.cancelFileChooser()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileChooserHelper.FILE_CHOOSER_REQUEST_CODE) {
            myWebChromeClient.handleFileChooserResult(resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionUtil.MY_PERMISSIONS_REQUEST_DOWNLOAD ->
                DownloadHandler.handlePermissionResult(this, grantResults)
            PermissionUtil.MY_PERMISSIONS_REQUEST_SMS ->
                UrlHandler.handleSmsPermissionResult(this, grantResults)
            FileChooserHelper.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileChooser(FileChooserHelper.getPendingFileChooserParams())
                } else {
                    showToast("Camera permission denied")
                    myWebChromeClient.cancelFileChooser()
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    fun saveUrl(url: String) {
        if (!url.contains("NoInternet") && !url.startsWith("file:///android_asset/")) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_LAST_URL, url)
            }
        }
    }

    private fun getLastUrl(): String? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_URL, null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWebView.saveState(outState)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        mWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mWebView.onResume()
        updateFavoriteMenuItem()
    }

    override fun onDestroy() {
        super.onDestroy()
        myWebChromeClient.cancelFileChooser()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        favoriteDatabase.close()
        mWebView.apply {
            stopLoading()
            destroy()
        }
    }
}