package com.secretcalc.browser

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_bookmark.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db").build()
        historyDao = db.historyDao()

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(this) { entry ->
            loadUrl(entry.url)
        }
        recyclerView.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {
        Thread {
            val list = historyDao.getAll()
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

    fun onClearHistory(view: android.view.View) {
        Thread {
            historyDao.clearAll()
            runOnUiThread {
                adapter.submitList(emptyList())
                Toast.makeText(this, "历史已清除", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}
