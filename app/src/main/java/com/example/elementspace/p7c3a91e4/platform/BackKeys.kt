package com.example.elementspace.p7c3a91e4.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * BACK and controller B (profile D60): never exit or send the app to the background. Each press
 * is published on [presses]; the table window closes its topmost overlay, and with nothing open
 * the press does nothing. Windows themselves close with the system × under them.
 */
object BackKeys {
    private const val KEYCODE_BUTTON_B = 97
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val presses: SharedFlow<Unit> = flow.asSharedFlow()
    private var lastDownTime = -1L

    fun isBack(code: Int) = code == KeyEvent.KEYCODE_BACK || code == KEYCODE_BUTTON_B

    /** Consumes BACK / B; fires once on key-up (a second layer seeing the same press is ignored). */
    fun route(code: Int, action: Int, downTime: Long, canceled: Boolean): Boolean {
        if (!isBack(code)) return false
        if (action == KeyEvent.ACTION_UP && !canceled && downTime != lastDownTime) {
            lastDownTime = downTime
            flow.tryEmit(Unit)
        }
        return true
    }

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(a: Activity, b: Bundle?) {
                    wrap(a)
                    (a as? ComponentActivity)?.onBackPressedDispatcher?.addCallback(
                        a,
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                flow.tryEmit(Unit)
                            }
                        },
                    )
                }

                override fun onActivityResumed(a: Activity) = wrap(a)

                override fun onActivityStarted(a: Activity) {}

                override fun onActivityPaused(a: Activity) {}

                override fun onActivityStopped(a: Activity) {}

                override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}

                override fun onActivityDestroyed(a: Activity) {}
            }
        )
    }

    private fun wrap(a: Activity) {
        val w = a.window ?: return
        val cb = w.callback ?: return
        if (cb is Wrapped) return
        w.callback = Wrapped(cb)
    }

    private class Wrapped(private val d: Window.Callback) : Window.Callback by d {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean =
            route(event.keyCode, event.action, event.downTime, event.isCanceled) || d.dispatchKeyEvent(event)
    }
}
