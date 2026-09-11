package com.mani.controlcentre.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mani.controlcentre.access.PageIntents
import com.mani.controlcentre.controls.VolumeController
import com.mani.controlcentre.controls.readBattery
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.data.Prefs
import com.mani.controlcentre.diagnostics.GestureLog

/** The visible, interactive panel. Volume is only changed while this activity is resumed. */
class PanelActivity : ComponentActivity() {
    private var requestedPage by mutableStateOf(Page.EVERYDAY)
    private var observedVolume by mutableIntStateOf(0)
    private lateinit var volume: VolumeController

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            observedVolume = volume.read()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volume = VolumeController(this)
        requestedPage = PageIntents.pageOf(intent)
        GestureLog.log("PanelActivity onCreate page=${requestedPage.id}")
        val prefs = Prefs(this)
        setContent {
            ControlCentreTheme {
                PanelScreen(
                    page = requestedPage,
                    volume = VolumeUi(
                        max = volume.max,
                        observed = observedVolume,
                        apply = { level -> observedVolume = volume.apply(level).getOrElse { observedVolume } },
                    ),
                    prefs = prefs,
                    battery = { readBattery(this) },
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedPage = PageIntents.pageOf(intent)
        GestureLog.log("PanelActivity onNewIntent page=${requestedPage.id}")
    }

    override fun onResume() {
        super.onResume()
        observedVolume = volume.read()
        registerReceiver(volumeReceiver, IntentFilter(VOLUME_CHANGED), RECEIVER_EXPORTED)
        GestureLog.log("PanelActivity resumed volume=$observedVolume/${volume.max}")
    }

    override fun onPause() {
        unregisterReceiver(volumeReceiver)
        super.onPause()
    }

    private companion object {
        /** Framework broadcast sent whenever a stream volume changes, so hardware keys are reflected too. */
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    }
}
