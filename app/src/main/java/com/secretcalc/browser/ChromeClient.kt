package com.secretcalc.browser

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView

class ChromeClient(private val progressBar: ProgressBar? = null, private val tvTitle: TextView? = null) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressBar?.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
        progressBar?.progress = newProgress
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        tvTitle?.text = title ?: ""
    }
}
