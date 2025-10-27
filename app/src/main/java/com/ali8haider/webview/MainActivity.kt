package com.ali8haider.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.pm.PackageInfoCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


public class MainActivity : AppCompatActivity() {

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null


    private lateinit var mWebView: WebView

    //    private lateInit val myToolBar: Toolbar
    private val tag = "MainActivity"
    lateinit var myToolBar: Toolbar
    private lateinit var mFrameLayout: FrameLayout
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mOnScrollChangedListener: OnScrollChangedListener
    private lateinit var myWebChromeClient: MyWebChromeClient

    //    private lateinit var mNestedScrollView: NestedScrollView
    private lateinit var hostname: String
    private lateinit var url: String
    private lateinit var uri: Uri

    companion object {

        // Unique request code for permission request
        private const val PERMISSION_REQUEST_CODE = 123
    }


    @SuppressLint("SetJavaScriptEnabled", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

//        requestPermissions()

        myWebChromeClient = MyWebChromeClient(this)


        hostname = "https://m.youtube.com"
        myToolBar = findViewById(R.id.mToolbar)
        mFrameLayout = findViewById(R.id.mProgressBarContainer)
        mProgressBar = findViewById(R.id.mProgressBar)
        mWebView = findViewById(R.id.mWebView)
        mSwipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout)
//        mNestedScrollView = findViewById(R.id.mNestedScrollView)

        setSupportActionBar(myToolBar)
        mWebView.webChromeClient = myWebChromeClient

        mSwipeRefreshLayout.setColorSchemeResources(R.color.purple, R.color.green, R.color.blue, R.color.orange)
        mSwipeRefreshLayout.setOnRefreshListener {
            mWebView.reload()
        }

        mProgressBar.max = 100

        val appName = getString(R.string.app_name)
        supportActionBar?.title = appName
        myToolBar.setTitleTextColor(Color.BLACK)
        myToolBar.setSubtitleTextColor(Color.BLACK)

//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /*val upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        upArrow?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.black))
            supportActionBar?.setHomeAsUpIndicator(wrappedDrawable)
        }*/


        // Handle back button press to navigate within the webView
        // Handle back button press to navigate within the webView
        onBackPressedDispatcher.addCallback(this, callback)

        // Get the version name and version code
        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        val verCode = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        Toast.makeText(this, "Version Name: $versionName\nVersion Code: $verCode", Toast.LENGTH_SHORT).show()

        val webSettings: WebSettings = mWebView.settings

        // Configure webView settings
//        mWebView.addJavascriptInterface(this, "Android") // Add a JavaScript interface for communication
        mWebView.settings.javaScriptEnabled = true // Enable JavaScript
        mWebView.settings.domStorageEnabled = true // Enable Dom storage
        mWebView.settings.allowFileAccess = true // Allow file access
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allow mixed content
        mWebView.settings.builtInZoomControls = true // Enable built-in zoom controls
        mWebView.settings.displayZoomControls = false // Hide zoom controls
        webSettings.useWideViewPort = true;
//        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.javaScriptCanOpenWindowsAutomatically = true;
        webSettings.setSupportMultipleWindows(true)
//        webSettings.databaseEnabled = true
        webSettings.setGeolocationEnabled(true)
//        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());
        webSettings.loadWithOverviewMode = true
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
        mWebView.settings.mediaPlaybackRequiresUserGesture = false


        // Load a URL
//        mWebView.loadUrl(hostname)

        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            // Handle the download here
            val request = DownloadManager.Request(Uri.parse(url))

            // Extract filename from contentDisposition or URL
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)

            request.apply {
                setTitle(filename)
                setDescription("Downloading file...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                allowScanningByMediaScanner() // Optional: Make the file visible to media scanners
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started: $filename", Toast.LENGTH_SHORT).show()
        }

        val intent = getIntent()
        val url = intent.getStringExtra("url")

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            if (url != null) mWebView.loadUrl(hostname);
            else loadUrl();
        }

        mWebView.webViewClient = object : WebViewClient() {

            // Handel URL navigation within the webView
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {


//                if (url.startsWith("file:") || uri.host != null && uri.host?.endsWith(hostname) == false) {
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//                    startActivity(intent)
////                view?.context?.startActivity(intent)
//                    return false
//                } else {
//                    Toast.makeText(this@MainActivity, "This is a local URL", Toast.LENGTH_SHORT).show()
//                    return true
//                uri = Uri.parse(url)
//                url = request?.url.toString()
                view?.loadUrl(request?.url.toString())
                return super.shouldOverrideUrlLoading(view, request)

            }


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                mSwipeRefreshLayout.isRefreshing = false
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                mSwipeRefreshLayout.isRefreshing = false
//                mNestedScrollView.scrollTo(0, 0)
                saveUrl(url.toString())
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
            }


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
            activity.myToolBar.visibility = View.GONE

            Log.d("WebChrome", "Showing fullscreen container")
            activity.mFrameLayout.visibility = View.VISIBLE
            activity.mFrameLayout.addView(
                customView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            Log.d("WebChrome", "Setting fullscreen flags")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )

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
            activity.myToolBar.visibility = View.VISIBLE  // ADD THIS LINE

            // Restore original UI state
            activity.window.decorView.systemUiVisibility = originalSystemUiVisibility
            activity.requestedOrientation = originalOrientation

            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            activity.mProgressBar.visibility = View.VISIBLE
            activity.mProgressBar.progress = newProgress
            if (newProgress == 100) {
                activity.mProgressBar.visibility = View.INVISIBLE
                activity.mSwipeRefreshLayout.isEnabled = true
            }
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            activity.myToolBar.subtitle = title
            super.onReceivedTitle(view, title)
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
        }

        var filePathCallback: ValueCallback<Array<Uri>>? = null

        override fun onShowFileChooser(
            webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
        ): Boolean {
            this.filePathCallback = filePathCallback
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            return true
        }

        companion object {
            const val FILE_CHOOSER_REQUEST_CODE = 1001
        }/*
//
//
//        private val activityRef = WeakReference(activity)
//
//        private var customView: View? = null
//        private var customViewCallback: CustomViewCallback? = null
//
//        private var originalOrientation = 0
//        private var originalSystemUiVisibility = 0



        override fun getDefaultVideoPoster(): Bitmap? {
            return activityRef.get()?.run {
                BitmapFactory.decodeResource(applicationContext.resources, 2130837573)
            }
        }

        override fun onHideCustomView() {
            activityRef.get()?.run {
                (window.decorView as ViewGroup).removeView(customView)
                customView = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
                requestedOrientation = originalOrientation
            }
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
        }

        override fun onShowCustomView(view: View?, viewCallback: CustomViewCallback?) {
            if (customView != null) {
                onHideCustomView()
                return
            }
            customView = view
            activityRef.get()?.run {
                originalSystemUiVisibility = window.decorView.systemUiVisibility
                originalOrientation = requestedOrientation
                customViewCallback = viewCallback
                (window.decorView as ViewGroup).addView(
                    customView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                window.decorView.systemUiVisibility = 3846
            }
        }

//        var objMainActivity: MainActivity = MainActivity() // Create an object
        *//*  var fullScreen: View? = null
          private var customView: View? = null
          private var customViewCallback: CustomViewCallback? = null
          private var originalOrientation: Int = 0
          private var originalSystemUiVisibility: Int = 0

          override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {

  //            mWebView.setVisibility(View.GONE);
  //
  //            if(fullScreen != null)
  //            {
  //                ((FrameLayout)getWindow().getDecorView()).removeView(fullScreen);
  //            }
  //
  //            fullScreen = view;
  //            ((FrameLayout)getWindow().getDecorView()).addView(fullScreen, new FrameLayout.LayoutParams(-1, -1));
  //            fullScreen.setVisibility(View.VISIBLE);


              if (customView != null) {
                  onHideCustomView()
                  return
              }

              customView = view
              customViewCallback = callback
              originalOrientation = activity.requestedOrientation
              originalSystemUiVisibility = activity.window.decorView.systemUiVisibility

              // Add the custom view to the activity's content
              val decorView = activity.window.decorView as FrameLayout
              decorView.addView(
                  customView, FrameLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                  )
              )

              // Set fullscreen flags and orientation
              activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
              activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

              *//**//*fullScreen?.visibility = View.GONE
            if (fullScreen != null) {
                (objMainActivity.window.decorView as FrameLayout).removeView(fullScreen)
            }

            (objMainActivity.window.decorView as FrameLayout).addView(fullScreen, FrameLayout.LayoutParams(-1, -1))
            fullScreen!!.visibility = View.VISIBLE
            super.onShowCustomView(view, callback)*//**//*
        }

        override fun onHideCustomView() {
            if (customView == null) {
                return
            }

            // Remove the custom view
            val decorView = activity.window.decorView as FrameLayout
            decorView.removeView(customView)

            // Restore original flags and orientation
            activity.window.decorView.systemUiVisibility = originalSystemUiVisibility
            activity.requestedOrientation = originalOrientation

            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null

            *//**//*fullScreen?.visibility = View.GONE
            objMainActivity.mWebView.visibility = View.VISIBLE
            super.onHideCustomView()*//**//*
        }*//*

        override fun onReceivedTitle(view: WebView?, title: String?) {
//            objMainActivity.myToolBar.setSubtitle(title)
            super.onReceivedTitle(view, title)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
//            view.context.activityCallback<MainActivity> {
//                onProgress(progress)
//            }

//            objMainActivity.mProgressBar.visibility = View.VISIBLE
//            objMainActivity.mProgressBar.progress = newProgress
            if (newProgress == 100) {
//                objMainActivity.mProgressBar.visibility = View.INVISIBLE
//                objMainActivity.mSwipeRefreshLayout.isEnabled = true
            }
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
//                val drawable: Drawable = BitmapDrawable(resources, icon)
//                supportActionBar?.setHomeAsUpIndicator(drawable)
        }

        var filePathCallback: ValueCallback<Array<Uri>>? = null


        override fun onShowFileChooser(
            webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
        ): Boolean {
            this.filePathCallback = filePathCallback

            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            // Ensure you handle camera/gallery options if needed by modifying the intent
            // For example, if you want to allow camera capture:
            // val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            // chooserIntent.putExtra(Intent.EXTRA_INTENT, intent)
            // chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            // activity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)

            activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            return true
        }

        companion object {
            const val FILE_CHOOSER_REQUEST_CODE = 1001
        }*/

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MyWebChromeClient.FILE_CHOOSER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                myWebChromeClient.filePathCallback?.onReceiveValue(results)
            } else {
                myWebChromeClient.filePathCallback?.onReceiveValue(null) // User cancelled
            }
            myWebChromeClient.filePathCallback = null // Clear the callback
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
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString("url", url)
            editor.apply()
        }
    }


    override fun onStart() {
        super.onStart()
        // This code will execute when the activity becomes visible to the user.
        Log.d(tag, "onStart called - Activity is now visible")
        // You can perform actions here that need to happen when the activity starts,
        // such as registering listeners, starting animations, or updating UI elements.
        mSwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener {
            ViewTreeObserver.OnScrollChangedListener {
                if (mWebView.scrollY == 0) mSwipeRefreshLayout.setEnabled(true);
                else mSwipeRefreshLayout.setEnabled(false);
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause called")
    }

    override fun onStop() {
        Log.d(tag, "onStop called")
//        if (url.startsWith("file:") || uri.host != null && uri.host?.endsWith(hostname) == false) {

//        } else
        mSwipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy called")
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
//                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle orientation change if needed
        Log.d(tag, "Configuration changed: ${newConfig.orientation}")

    }

    fun loadUrl() {
        val loadUrl: SharedPreferences = getSharedPreferences("SAVE_URL", Context.MODE_PRIVATE)
        mWebView.loadUrl(loadUrl.getString("URL", hostname).toString())
    }

    /*private fun requestPermissions() {

        // List of permissions the app may need
        val permissions = arrayOf(
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.CAMERA,
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )

        // Filter out the permissions that are not yet granted
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        // If there are permissions that need to be requested, ask the user for them
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(), // Convert list to array
                PERMISSION_REQUEST_CODE // Pass the request code
            )
        } else {
            // All permissions are already granted
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }*/
    // Callback function that handles the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {

            // Combine permissions with their corresponding grant results
            val deniedPermissions = permissions.zip(grantResults.toTypedArray()).filter { it.second != PackageManager.PERMISSION_GRANTED } // Filter out the denied ones
                .map { it.first } // Get the permission names

            if (deniedPermissions.isEmpty()) {
                // All permissions granted
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                // Some permissions were denied, show them in a Toast
                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_LONG).show()
            }
        }
    }


}