package com.kanso.focus.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.kanso.focus.data.FocusSessionManager

class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusSessionManager.init(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        FocusSessionManager.init(applicationContext)
        if (!FocusSessionManager.isSessionActive.value) return

        val pkg = event?.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: return

        if (pkg == applicationContext.packageName) return

        if (FocusSessionManager.restrictedPackages.value.contains(pkg)) {
            // Cierra la ventana restringida enviando al inicio y lanza la pantalla de bloqueo Kanso
            performGlobalAction(GLOBAL_ACTION_HOME)
            FocusSessionManager.triggerBlockOverlay(applicationContext, pkg)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
