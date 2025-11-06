package com.ali8dev.webviewdemo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ali8dev.webviewdemo.adapter.FavoriteAdapter
import com.ali8dev.webviewdemo.database.Favorite
import com.ali8dev.webviewdemo.database.FavoriteDatabase

class FavoritesActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var favoriteAdapter: FavoriteAdapter
    private lateinit var favoriteDatabase: FavoriteDatabase
    private val favorites = mutableListOf<Favorite>()

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.favoritesToolbar)
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        favoriteDatabase = FavoriteDatabase(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.favorites)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter(
            favorites = favorites,
            onItemClick = { favorite ->
                openFavorite(favorite)
            },
            onDeleteClick = { favorite ->
                showDeleteConfirmation(favorite)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = favoriteAdapter
        }
    }

    private fun loadFavorites() {
        val loadedFavorites = favoriteDatabase.getAllFavorites()
        favorites.clear()
        favorites.addAll(loadedFavorites)
        favoriteAdapter.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (favorites.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun openFavorite(favorite: Favorite) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_URL, favorite.url)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun showDeleteConfirmation(favorite: Favorite) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_favorite))
            .setMessage(getString(R.string.delete_favorite_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteFavorite(favorite)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteFavorite(favorite: Favorite) {
        favoriteDatabase.removeFavorite(favorite.url)
        favoriteAdapter.removeFavorite(favorite)
        updateEmptyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}