package com.ali8dev.webviewdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button

    private val splashDuration = 1000L // 3 seconds
    private var hasCheckedConnection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize views
        logoImageView = findViewById(R.id.splashLogo)
        progressBar = findViewById(R.id.splashProgressBar)
        statusTextView = findViewById(R.id.splashStatusText)
        retryButton = findViewById(R.id.retryButton)

        // Setup retry button
        retryButton.setOnClickListener {
            checkConnectionAndProceed()
        }

        // Start the splash sequence
        checkConnectionAndProceed()
    }

    private fun checkConnectionAndProceed() {
        hasCheckedConnection = true

        // Hide retry button and show progress
        retryButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        statusTextView.text = getString(R.string.checking_connection)

        // Check internet connection
        if (isNetworkAvailable()) {
            statusTextView.text = getString(R.string.loading)
            // Connection available - proceed to MainActivity after delay
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()
            }, splashDuration)
        } else {
            // No connection - show error and retry option
            showNoConnectionState()
        }
    }

    private fun showNoConnectionState() {
        progressBar.visibility = View.GONE
        statusTextView.apply {
            text = getString(R.string.no_internet_connection)
//            setTextColor(ContextCompat.getColor(this@SplashActivity, android.R.color.holo_red_dark))
        }
        retryButton.visibility = View.VISIBLE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent going back from splash screen
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finishAffinity() // Close the app completely
    }
}