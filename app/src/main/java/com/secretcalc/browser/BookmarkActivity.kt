package com.secretcalc.browser

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_bookmark.*

class BookmarkActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db").build()
        bookmarkDao = db.bookmarkDao()

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BookmarkAdapter(this) { entry ->
            loadUrl(entry.url)
        }
        recyclerView.adapter = adapter

        loadBookmarks()
    }

    private fun loadBookmarks() {
        Thread {
            val list = bookmarkDao.getAll()
            runOnUiThread {
                adapter.submitList(list)
            }
        }.start()
    }

    private fun loadUrl(url: String) {
        val result = Intent().apply { putExtra("url", url) }
        setResult(RESULT_OK, result)
        finish()
    }
}
