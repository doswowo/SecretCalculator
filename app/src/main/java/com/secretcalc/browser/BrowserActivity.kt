package com.secretcalc.browser

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnStop: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var homeView: View
    private lateinit var bookmarkGrid: GridLayout

    private lateinit var db: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var bookmarkDao: BookmarkDao

    private var currentUrl = ""
    private var showingHome = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        initViews()
        initDatabase()
        initWebView()
        showHome()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnStop = findViewById(R.id.btnStop)
        btnHome = findViewById(R.id.btnHome)
        tvTitle = findViewById(R.id.tvTitle)
        btnMenu = findViewById(R.id.btnMenu)
        homeView = findViewById(R.id.homeView)
        bookmarkGrid = findViewById(R.id.bookmarkGrid)

        btnBack.setOnClickListener {
            when {
                showingHome -> finish()
                webView.canGoBack() -> webView.goBack()
                else -> showHome()
            }
        }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnRefresh.setOnClickListener {
            if (showingHome) showHome() else webView.reload()
        }
        btnStop.setOnClickListener { webView.stopLoading() }
        btnHome.setOnClickListener { showHome() }
        btnMenu.setOnClickListener { showMenu() }

        urlInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) loadUrl(url)
                true
            } else false
        }
    }

    private fun initDatabase() {
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "browser_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        historyDao = db.historyDao()
        bookmarkDao = db.bookmarkDao()
    }

    private fun initWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                urlInput.setText(url)
                currentUrl = url ?: ""
                progressBar.visibility = View.VISIBLE
                btnRefresh.visibility = View.GONE
                btnStop.visibility = View.VISIBLE
                showingHome = false
                homeView.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                btnRefresh.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                updateNavButtons()
                url?.let { saveHistory(it, view?.title ?: "") }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
            }
        }

        webView.webChromeClient = ChromeClient(progressBar, tvTitle)
    }

    private fun showHome() {
        showingHome = true
        homeView.visibility = View.VISIBLE
        webView.visibility = View.GONE
        tvTitle.text = "浏览器"
        urlInput.setText("")
        currentUrl = ""
        loadBookmarks()
        updateNavButtons()
    }

    private fun loadBookmarks() {
        Thread {
            val bookmarks = bookmarkDao.getAll()
            runOnUiThread {
                bookmarkGrid.removeAllViews()
                val defaults = listOf(
                    Pair("Google", "https://www.google.com"),
                    Pair("百度", "https://www.baidu.com"),
                    Pair("Bing", "https://www.bing.com"),
                    Pair("知乎", "https://www.zhihu.com"),
                    Pair("微博", "https://weibo.com"),
                    Pair("GitHub", "https://github.com")
                )
                val all = if (bookmarks.isEmpty()) defaults else bookmarks.map { Pair(it.title, it.url) }
                for ((title, url) in all) {
                    bookmarkGrid.addView(createBookmarkItem(title, url))
                }
            }
        }.start()
    }

    private fun createBookmarkItem(title: String, url: String): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 20, 12, 20)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener { loadUrl(url) }
        }

        val icon = TextView(this).apply {
            text = title.take(1)
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#007AFF"))
        }

        val label = TextView(this).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#333333"))
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        item.addView(icon)
        item.addView(label)
        return item
    }

    private fun loadUrl(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> "https://www.google.com/search?q=$url"
        }
        showingHome = false
        homeView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(finalUrl)
        urlInput.setText(finalUrl)
        urlInput.clearFocus()
    }

    private fun saveHistory(url: String, title: String) {
        Thread {
            historyDao.insert(HistoryEntry(url = url, title = title, timestamp = System.currentTimeMillis()))
        }.start()
    }

    private fun updateNavButtons() {
        btnBack.alpha = if (showingHome || webView.canGoBack()) 1f else 0.3f
        btnForward.alpha = if (webView.canGoForward()) 1f else 0.3f
    }

    private fun showMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.menu.add(0, 1, 0, "历史记录")
        popup.menu.add(0, 2, 1, "收藏夹")
        popup.menu.add(0, 3, 2, "添加收藏")
        popup.menu.add(0, 4, 3, "修改密码")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> startActivity(Intent(this, HistoryActivity::class.java))
                2 -> startActivity(Intent(this, BookmarkActivity::class.java))
                3 -> addBookmark()
                4 -> startActivity(Intent(this, ChangePasswordActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun addBookmark() {
        if (currentUrl.isEmpty()) {
            Toast.makeText(this, "当前没有可收藏的页面", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val exists = bookmarkDao.findByUrl(currentUrl)
            if (exists == null) {
                bookmarkDao.insert(
                    BookmarkEntry(
                        url = currentUrl,
                        title = tvTitle.text.toString().ifBlank { currentUrl },
                        timestamp = System.currentTimeMillis()
                    )
                )
                runOnUiThread { Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show() }
            } else {
                runOnUiThread { Toast.makeText(this, "已在收藏夹中", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            showingHome -> super.onBackPressed()
            webView.canGoBack() -> webView.goBack()
            else -> showHome()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
