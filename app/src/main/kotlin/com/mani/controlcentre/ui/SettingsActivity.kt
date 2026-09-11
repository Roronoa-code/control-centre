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
import com.mani.controlcentre.access.PanelAccessibilityService
import com.mani.controlcentre.data.Prefs
import com.mani.controlcentre.diagnostics.GestureLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Setup, the experiment switch and a live trace. Nothing here opens a panel. */
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
private fun SettingsScreen(prefs: Prefs, serviceEnabled: Boolean, onOpenAccessibilitySettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val companionEnabled by prefs.companionEnabled.collectAsStateWithLifecycle(initialValue = true)
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
                "Companion-page experiment. Open Samsung's Quick Panel as usual; a small handle appears at the right edge. Swipe it left for a blank test page, swipe right to return.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (serviceEnabled) "Accessibility service: enabled" else "Accessibility service: not enabled",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onOpenAccessibilitySettings) { Text("Open accessibility settings") }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Companion experiment", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = companionEnabled, onCheckedChange = { scope.launch { prefs.setCompanionEnabled(it) } })
            }
            Text("Recent trace", style = MaterialTheme.typography.titleMedium)
            lines.takeLast(30).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
