package com.kanso.focus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kanso.focus.data.FocusSessionManager
import com.kanso.focus.ui.BlockOverlayActivity

class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusSessionManager.init(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == applicationContext.packageName) return

            FocusSessionManager.init(applicationContext)
            if (FocusSessionManager.isSessionActive.value &&
                FocusSessionManager.restrictedPackages.value.contains(pkg)
            ) {
                val overlayIntent = Intent(applicationContext, BlockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("blocked_package", pkg)
                }
                startActivity(overlayIntent)
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
