package com.secretcalc.browser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class BookmarkActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        bookmarkDao = db.bookmarkDao()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BookmarkAdapter(this) { }
        recyclerView.adapter = adapter
        loadBookmarks()
    }

    private fun loadBookmarks() {
        Thread {
            val list = bookmarkDao.getAll()
            runOnUiThread { adapter.submitList(list) }
        }.start()
    }
}
