package com.theveloper.pixelplay.presentation.youtubemusic

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity

class YouTubeMusicLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "YouTube Music sign in"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 12, 20, 12)
        }
        root.addView(TextView(this).apply {
            text = "Sign in to Google, then return here and tap Connect. Your session is encrypted on this device."
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 8, 4, 12)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "Connect YouTube Music account"
            setOnClickListener {
                val cookies = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
                val hasAuth = cookies.split(';').any {
                    val key = it.trim().substringBefore('=')
                    key == "SAPISID" || key == "__Secure-3PAPISID"
                }
                if (!hasAuth) {
                    setText("Sign in first, then tap Connect")
                    return@setOnClickListener
                }
                YouTubeMusicSessionStore.save(this@YouTubeMusicLoginActivity, cookies)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
        web.loadUrl("https://music.youtube.com/")
    }
}
