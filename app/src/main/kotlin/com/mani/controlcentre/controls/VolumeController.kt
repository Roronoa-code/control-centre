package com.mani.controlcentre.controls

import android.content.Context
import android.media.AudioManager
import com.mani.controlcentre.diagnostics.GestureLog

/** Media (STREAM_MUSIC) volume through AudioManager. Call only from a visible activity; Android 17 blocks background changes. */
class VolumeController(context: Context) {
    private val audio: AudioManager = context.getSystemService(AudioManager::class.java)

    val max: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val min: Int get() = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)

    /** What the system reports right now. This, not the slider, is the displayed state. */
    fun read(): Int = audio.getStreamVolume(AudioManager.STREAM_MUSIC)

    /** Requests [level] and returns the level the system reports afterwards, or the failure. */
    fun apply(level: Int): Result<Int> = runCatching {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(min, max), 0)
        read()
    }.onSuccess { observed ->
        GestureLog.log("Volume requested=$level observed=$observed")
    }.onFailure { error ->
        GestureLog.log("Volume requested=$level FAILED ${error.javaClass.simpleName}: ${error.message}")
    }
}
