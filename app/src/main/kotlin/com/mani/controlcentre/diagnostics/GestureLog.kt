package com.mani.controlcentre.diagnostics

import android.os.SystemClock
import android.util.Log

/** Bounded in-process trace of gesture, launch and control events. Never stores clipboard, note or notification text. */
object GestureLog {
    private const val TAG = "CC.Gesture"
    private const val LIMIT = 300
    private val entries = ArrayDeque<String>()

    @Synchronized
    fun log(message: String) {
        Log.i(TAG, message)
        entries.addLast("${SystemClock.uptimeMillis()} $message")
        while (entries.size > LIMIT) entries.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<String> = entries.toList()
}
