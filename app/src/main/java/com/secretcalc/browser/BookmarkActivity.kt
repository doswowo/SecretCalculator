package com.secretcalc.browser

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class BookmarkActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        BrowserActivity.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db")
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        bookmarkDao = db.bookmarkDao()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BookmarkAdapter(this,
            onClick = { entry ->
                startActivity(Intent(this, BrowserActivity::class.java).apply {
                    putExtra(BrowserActivity.EXTRA_URL, entry.url)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            },
            onLongClick = { entry -> showOptions(entry) }
        )
        recyclerView.adapter = adapter
        loadBookmarks()
    }

    private fun showOptions(entry: BookmarkEntry) {
        val options = arrayOf("编辑", "删除")
        AlertDialog.Builder(this)
            .setTitle(entry.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(entry)
                    1 -> Thread {
                        bookmarkDao.deleteById(entry.id)
                        runOnUiThread { loadBookmarks() }
                    }.start()
                }
            }
            .show()
    }

    private fun showEditDialog(entry: BookmarkEntry) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val nameInput = EditText(this).apply { hint = "名称"; setText(entry.title) }
        val urlInput = EditText(this).apply { hint = "网址"; setText(entry.url) }
        box.addView(nameInput)
        box.addView(urlInput)

        AlertDialog.Builder(this)
            .setTitle("编辑收藏")
            .setView(box)
            .setPositiveButton("保存") { _, _ ->
                var url = urlInput.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
                Thread {
                    bookmarkDao.update(entry.id, url, nameInput.text.toString().trim().ifBlank { url })
                    runOnUiThread { loadBookmarks() }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadBookmarks() {
        Thread {
            val list = bookmarkDao.getAll()
            runOnUiThread { adapter.submitList(list) }
        }.start()
    }
}
