package com.ali8haider.webview

//import android.R
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView
//    private lateInit val myToolBar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val myToolBar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(myToolBar)

        val appName = getString(R.string.app_name)
        supportActionBar?.title = appName
        myToolBar.setTitleTextColor(Color.BLACK)
        myToolBar.setSubtitleTextColor(Color.BLACK)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        upArrow?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.black))
            supportActionBar?.setHomeAsUpIndicator(wrappedDrawable)
        }


        onBackPressedDispatcher.addCallback(this, callback)


        myWebView = findViewById(R.id.mWebView)

        // Configure webView settings
        myWebView.settings.javaScriptEnabled = true // Enable JavaScript
        myWebView.settings.domStorageEnabled = true // Enable Dom storage

        // Set A WebViewClient to handle page navigation and redirects with webView
        myWebView.webViewClient = WebViewClient()

        // Load a URL
        myWebView.loadUrl("https://www.google.com")

        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val pageTitle = view?.title
                myToolBar.setSubtitle(pageTitle)

            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Handle back button press to navigate within the webView
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (myWebView.canGoBack())
                myWebView.goBack()
            else
                finish()
        }
    }


}