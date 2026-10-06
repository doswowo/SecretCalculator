package com.secretcalc.browser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        BrowserActivity.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db")
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2)
            .build()
        historyDao = db.historyDao()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(this) { entry ->
            startActivity(Intent(this, BrowserActivity::class.java).apply {
                putExtra(BrowserActivity.EXTRA_URL, entry.url)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
        recyclerView.adapter = adapter
        loadHistory()
    }

    private fun loadHistory() {
        Thread {
            val list = historyDao.getAll()
            runOnUiThread { adapter.submitList(list) }
        }.start()
    }

    fun onClearHistory(view: View) {
        Thread {
            historyDao.clearAll()
            runOnUiThread {
                adapter.submitList(emptyList())
                Toast.makeText(this, "历史已清除", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}
