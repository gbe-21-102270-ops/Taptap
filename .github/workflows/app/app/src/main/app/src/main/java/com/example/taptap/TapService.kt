package com.example.taptap

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class TapService : AccessibilityService() {

    companion object {
        private const val STRIP_WIDTH_DP = 28
        private const val TAP_TIMEOUT = 320L
        private const val DICT_URL = "https://en.dict.naver.com/"
    }

    private var overlay: View? = null
    private var tapCount = 0
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = Runnable {
        when (tapCount) {
            2 -> openEnDict()
            3 -> performGlobalAction(GLOBAL_ACTION_BACK)
        }
        tapCount = 0
    }

    override fun onServiceConnected() {
        if (overlay != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val widthPx = (STRIP_WIDTH_DP * resources.displayMetrics.density).toInt()

        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.TOP

        val view = View(this)
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                tapCount++
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, TAP_TIMEOUT)
            }
            true
        }

        overlay = view
        wm.addView(view, params)
    }

    private fun openEnDict() {
        val uri = Uri.parse(DICT_URL)

        val chrome = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(chrome)
            return
        } catch (_: Exception) {
        }

        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        overlay?.let { v ->
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
            } catch (_: Exception) {
            }
        }
        overlay = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
