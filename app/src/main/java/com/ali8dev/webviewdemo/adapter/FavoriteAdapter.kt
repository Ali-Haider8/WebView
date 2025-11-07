package com.ali8dev.webviewdemo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ali8dev.webviewdemo.R
import com.ali8dev.webviewdemo.database.Favorite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteAdapter(
    private var favorites: MutableList<Favorite>,
    private val onItemClick: (Favorite) -> Unit,
    private val onDeleteClick: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.favoriteTitle)
        val urlTextView: TextView = itemView.findViewById(R.id.favoriteUrl)
        val dateTextView: TextView = itemView.findViewById(R.id.favoriteDate)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favorites[position]

        holder.titleTextView.text = favorite.title
        holder.urlTextView.text = shortenUrl(favorite.url)
        holder.dateTextView.text = formatDate(favorite.timestamp)

        holder.itemView.setOnClickListener {
            onItemClick(favorite)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(favorite)
        }
    }

    override fun getItemCount(): Int = favorites.size

    /**
     * Shorten URL for display
     */
    private fun shortenUrl(url: String): String {
        val maxLength = 50
        return if (url.length > maxLength) {
            val shortened = url.take(maxLength)
            "$shortened..."
        } else {
            url
        }
    }

    /**
     * Format timestamp to readable date and time
     */
    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        return "Date: ${dateFormat.format(date)}"
    }

    /**
     * Update the list of favorites
     */
    fun updateFavorites(newFavorites: List<Favorite>) {
        favorites.clear()
        favorites.addAll(newFavorites)
        notifyDataSetChanged()
    }

    /**
     * Remove a favorite from the list
     */
    fun removeFavorite(favorite: Favorite) {
        val position = favorites.indexOf(favorite)
        if (position != -1) {
            favorites.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}