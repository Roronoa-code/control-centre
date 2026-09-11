package com.mani.controlcentre.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mani.controlcentre.access.PageIntents
import com.mani.controlcentre.access.PanelAccessibilityService
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.data.Prefs
import com.mani.controlcentre.data.TriggerSettings
import com.mani.controlcentre.diagnostics.GestureLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Setup, trigger toggles and a live gesture trace. Never sits between the gesture and the panel. */
class SettingsActivity : ComponentActivity() {
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        setContent {
            ControlCentreTheme {
                SettingsScreen(
                    prefs = prefs,
                    serviceEnabled = serviceEnabled,
                    onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onOpenPage = { startActivity(PageIntents.panel(this, it)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = PanelAccessibilityService.isEnabled(this)
    }
}

@Composable
private fun SettingsScreen(
    prefs: Prefs,
    serviceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPage: (Page) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by prefs.triggers.collectAsStateWithLifecycle(initialValue = TriggerSettings())
    val lines by produceState(initialValue = GestureLog.snapshot()) {
        while (true) {
            value = GestureLog.snapshot()
            delay(1000)
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Control Centre", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Feasibility build. This screen is configuration only; the panel opens from the gesture, never from here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                if (serviceEnabled) "Gesture service: enabled" else "Gesture service: not enabled",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Enable \"Control Centre\" under Settings > Accessibility > Installed apps. Nothing else is required for the gesture test.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenAccessibilitySettings) { Text("Open accessibility settings") }

            SettingSwitch("Top-right pull (experimental)", settings.topEnabled) { scope.launch { prefs.setTopEnabled(it) } }
            SettingSwitch("Right-edge handle", settings.sideEnabled) { scope.launch { prefs.setSideEnabled(it) } }
            SettingSwitch("Left-handed (mirror to the left edge)", settings.leftHanded) { scope.launch { prefs.setLeftHanded(it) } }
            SettingSwitch("Show trigger areas", settings.showHandles) { scope.launch { prefs.setShowHandles(it) } }
            SettingSwitch("Pause gestures", settings.paused) { scope.launch { prefs.setPaused(it) } }

            Text("Open a page directly (test only)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Page.entries.forEach { page -> OutlinedButton(onClick = { onOpenPage(page) }) { Text(page.title) } }
            }

            Text("Recent gesture log", style = MaterialTheme.typography.titleMedium)
            lines.takeLast(25).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
