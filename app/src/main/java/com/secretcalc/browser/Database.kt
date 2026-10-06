package com.secretcalc.browser

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    val timestamp: Long,
    val sortOrder: Long = 0
)

@Dao
interface BookmarkDao {
    @Insert
    fun insert(entry: BookmarkEntry)

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAll(): List<BookmarkEntry>

    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC")
    fun getAllOrdered(): List<BookmarkEntry>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    fun findByUrl(url: String): BookmarkEntry?

    @Query("SELECT MAX(sortOrder) FROM bookmarks")
    fun maxSortOrder(): Long?

    @Query("UPDATE bookmarks SET url = :url, title = :title WHERE id = :id")
    fun update(id: Long, url: String, title: String)

    @Query("UPDATE bookmarks SET sortOrder = :sortOrder WHERE id = :id")
    fun setSortOrder(id: Long, sortOrder: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    fun delete(url: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    fun deleteById(id: Long)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bookmarks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE bookmarks SET sortOrder = id")
    }
}

@Database(entities = [HistoryEntry::class, BookmarkEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "browser_db")
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
    }
}
