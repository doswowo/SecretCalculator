package com.secretcalc.browser

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long
)

@Dao
interface HistoryDao {
    @Insert
    fun insert(entry: HistoryEntry)

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): List<HistoryEntry>

    @Query("DELETE FROM history")
    fun clearAll()
}

@Entity(tableName = "bookmarks")
data class BookmarkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long
)

@Dao
interface BookmarkDao {
    @Insert
    fun insert(entry: BookmarkEntry)

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAll(): List<BookmarkEntry>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    fun findByUrl(url: String): BookmarkEntry?

    @Query("DELETE FROM bookmarks WHERE url = :url")
    fun delete(url: String)
}

@Database(entities = [HistoryEntry::class, BookmarkEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
