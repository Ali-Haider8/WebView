package com.ali8dev.webviewdemo.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Favorite(
    val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

class FavoriteDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "favorites.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_FAVORITES = "favorites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_URL = "url"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_URL TEXT NOT NULL UNIQUE,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        onCreate(db)
    }

    /**
     * Add a favorite to the database
     */
    fun addFavorite(title: String, url: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_URL, url)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        return db.insert(TABLE_FAVORITES, null, values)
    }

    /**
     * Remove a favorite by URL
     */
    fun removeFavorite(url: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_FAVORITES, "$COLUMN_URL = ?", arrayOf(url))
    }

    /**
     * Check if a URL is in favorites
     */
    fun isFavorite(url: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FAVORITES,
            arrayOf(COLUMN_ID),
            "$COLUMN_URL = ?",
            arrayOf(url),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    /**
     * Get all favorites ordered by timestamp (newest first)
     */
    fun getAllFavorites(): List<Favorite> {
        val favorites = mutableListOf<Favorite>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_FAVORITES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val favorite = Favorite(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                )
                favorites.add(favorite)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return favorites
    }

    /**
     * Delete all favorites
     */
    fun clearAllFavorites(): Int {
        val db = writableDatabase
        return db.delete(TABLE_FAVORITES, null, null)
    }
}