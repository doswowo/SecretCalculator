package com.secretcalc.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        initViews()
        initDatabase()
        initWebView()
        showHome()
        intent.getStringExtra(EXTRA_URL)?.let { loadUrl(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_URL)?.let { loadUrl(it) }
    }

    private fun applyTheme() {
        val night = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
            .getBoolean(KEY_NIGHT, false)
        AppCompatDelegate.setDefaultNightMode(
            if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun isDesktopMode(): Boolean =
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).getBoolean(KEY_DESKTOP, false)

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
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
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
        applyUserAgent()
        applyWebViewDark()

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

    private fun applyUserAgent() {
        webView.settings.userAgentString = if (isDesktopMode()) DESKTOP_UA
        else WebSettings.getDefaultUserAgent(this)
    }

    // 夜间模式下让网页内容也跟随变暗（Android 10+ 的算法变暗）
    private fun applyWebViewDark() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
        }
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
            var bookmarks = bookmarkDao.getAllOrdered()
            if (bookmarks.isEmpty()) {
                val defaults = listOf(
                    Pair("百度", "https://www.baidu.com"),
                    Pair("知乎", "https://www.zhihu.com"),
                    Pair("哔哩哔哩", "https://www.bilibili.com"),
                    Pair("微博", "https://weibo.com"),
                    Pair("京东", "https://www.jd.com"),
                    Pair("淘宝", "https://www.taobao.com")
                )
                var sort = 0L
                for ((title, url) in defaults) {
                    bookmarkDao.insert(
                        BookmarkEntry(url = url, title = title, timestamp = System.currentTimeMillis(), sortOrder = sort++)
                    )
                }
                bookmarks = bookmarkDao.getAllOrdered()
            }
            runOnUiThread {
                bookmarkGrid.removeAllViews()
                for (entry in bookmarks) {
                    bookmarkGrid.addView(createBookmarkItem(entry))
                }
                bookmarkGrid.addView(createAddItem())
            }
        }.start()
    }

    private fun createBookmarkItem(entry: BookmarkEntry): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 20, 12, 20)
            setBackgroundResource(R.drawable.tile_bg)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener { loadUrl(entry.url) }
            setOnLongClickListener { showBookmarkOptions(entry); true }
        }

        val icon = TextView(this).apply {
            text = entry.title.take(1)
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.accent_link))
        }

        val label = TextView(this).apply {
            text = entry.title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_main))
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

    private fun createAddItem(): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 20, 12, 20)
            setBackgroundResource(R.drawable.tile_bg)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener { showAddBookmarkDialog() }
        }

        val icon = TextView(this).apply {
            text = "＋"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_main))
        }

        val label = TextView(this).apply {
            text = "添加"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_main))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        item.addView(icon)
        item.addView(label)
        return item
    }

    private fun showBookmarkOptions(entry: BookmarkEntry) {
        val options = arrayOf("编辑", "删除", "前移", "后移")
        AlertDialog.Builder(this)
            .setTitle(entry.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditBookmarkDialog(entry)
                    1 -> Thread {
                        bookmarkDao.deleteById(entry.id)
                        runOnUiThread { loadBookmarks() }
                    }.start()
                    2 -> moveBookmark(entry, -1)
                    3 -> moveBookmark(entry, +1)
                }
            }
            .show()
    }

    private fun moveBookmark(entry: BookmarkEntry, direction: Int) {
        Thread {
            val list = bookmarkDao.getAllOrdered()
            val i = list.indexOfFirst { it.id == entry.id }
            val j = i + direction
            if (i < 0 || j < 0 || j >= list.size) {
                runOnUiThread {
                    Toast.makeText(this, if (direction < 0) "已经在最前面" else "已经在最后面", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }
            bookmarkDao.setSortOrder(list[i].id, list[j].sortOrder)
            bookmarkDao.setSortOrder(list[j].id, list[i].sortOrder)
            runOnUiThread { loadBookmarks() }
        }.start()
    }

    private fun showEditBookmarkDialog(entry: BookmarkEntry) {
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

    private fun showAddBookmarkDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val nameInput = EditText(this).apply { hint = "名称" }
        val urlInput = EditText(this).apply { hint = "网址（如 www.baidu.com）" }
        box.addView(nameInput)
        box.addView(urlInput)

        AlertDialog.Builder(this)
            .setTitle("添加收藏")
            .setView(box)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                var url = urlInput.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
                Thread {
                    if (bookmarkDao.findByUrl(url) == null) {
                        bookmarkDao.insert(
                            BookmarkEntry(
                                url = url,
                                title = name.ifBlank { url },
                                timestamp = System.currentTimeMillis(),
                                sortOrder = (bookmarkDao.maxSortOrder() ?: -1) + 1
                            )
                        )
                    }
                    runOnUiThread { loadBookmarks() }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadUrl(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> {
                val q = java.net.URLEncoder.encode(url, "UTF-8")
                "https://www.baidu.com/s?wd=$q"
            }
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
        popup.menu.add(0, 0, 0, "回到主页")
        popup.menu.add(0, 1, 1, if (isDesktopMode()) "切换为移动版页面" else "切换为桌面版页面")
        popup.menu.add(0, 2, 2, if (isNightMode()) "切换为日间模式" else "切换为夜间模式")
        popup.menu.add(0, 3, 3, "历史记录")
        popup.menu.add(0, 4, 4, "收藏夹")
        popup.menu.add(0, 5, 5, "添加收藏")
        popup.menu.add(0, 6, 6, "修改密码")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> showHome()
                1 -> {
                    val sp = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                    sp.edit().putBoolean(KEY_DESKTOP, !isDesktopMode()).apply()
                    applyUserAgent()
                    if (!showingHome) webView.reload()
                    Toast.makeText(this, if (isDesktopMode()) "已切换为桌面版页面" else "已切换为移动版页面", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    val sp = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                    val night = !isNightMode()
                    sp.edit().putBoolean(KEY_NIGHT, night).apply()
                    AppCompatDelegate.setDefaultNightMode(
                        if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
                3 -> startActivity(Intent(this, HistoryActivity::class.java))
                4 -> startActivity(Intent(this, BookmarkActivity::class.java))
                5 -> addBookmark()
                6 -> startActivity(Intent(this, ChangePasswordActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun isNightMode(): Boolean =
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).getBoolean(KEY_NIGHT, false)

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
                        timestamp = System.currentTimeMillis(),
                        sortOrder = (bookmarkDao.maxSortOrder() ?: -1) + 1
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

    companion object {
        const val EXTRA_URL = "open_url"
        const val PREFS_SETTINGS = "settings"
        const val KEY_NIGHT = "night_mode"
        const val KEY_DESKTOP = "desktop_mode"
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        fun applyTheme(activity: AppCompatActivity) {
            val night = activity.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(KEY_NIGHT, false)
            AppCompatDelegate.setDefaultNightMode(
                if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
