package com.mani.controlcentre.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mani.controlcentre.controls.BatterySnapshot
import com.mani.controlcentre.core.ControlId
import com.mani.controlcentre.core.CounterMath
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.data.Prefs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Volume as the system reports it, plus the single action the tile may perform. */
class VolumeUi(val max: Int, val observed: Int, val apply: (Int) -> Unit)

@Composable
fun PanelScreen(page: Page, volume: VolumeUi, prefs: Prefs, battery: () -> BatterySnapshot, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = page.ordinal) { Page.entries.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(page) { pager.scrollToPage(page.ordinal) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.74f)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Page.entries.forEach { entry ->
                    val selected = pager.currentPage == entry.ordinal
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(entry.ordinal) } }) {
                        Text(
                            entry.title,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                PageContent(Page.entries[index], volume, prefs, battery)
            }
        }
    }
}

@Composable
private fun PageContent(page: Page, volume: VolumeUi, prefs: Prefs, battery: () -> BatterySnapshot) {
    val controls = remember(page) { ControlId.entries.filter { it.defaultPage == page } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(controls, key = { it.name }, span = { GridItemSpan(it.defaultSpan) }) { control ->
            when (control) {
                ControlId.VOLUME -> VolumeTile(volume)
                ControlId.COUNTER -> CounterTile(prefs)
                ControlId.BATTERY -> BatteryTile(battery)
                else -> PlaceholderTile(control.label)
            }
        }
    }
}

@Composable
private fun Tile(title: String, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Restricted or unimplemented control. Visibly inert; never pretends to toggle anything. */
@Composable
private fun PlaceholderTile(title: String) = Tile(title, enabled = false) {
    Text("Not in this build", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun VolumeTile(volume: VolumeUi) = Tile("Media volume") {
    var dragging by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(volume.observed.toFloat()) }
    var lastRequested by remember { mutableIntStateOf(volume.observed) }
    LaunchedEffect(volume.observed, dragging) { if (!dragging) position = volume.observed.toFloat() }
    Slider(
        value = position,
        onValueChange = { value ->
            dragging = true
            position = value
            val level = value.roundToInt()
            if (level != lastRequested) {
                lastRequested = level
                volume.apply(level)
            }
        },
        onValueChangeFinished = {
            dragging = false
            volume.apply(position.roundToInt())
        },
        valueRange = 0f..volume.max.toFloat(),
        steps = (volume.max - 1).coerceAtLeast(0),
    )
    Text(
        "System reports ${volume.observed} / ${volume.max}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CounterTile(prefs: Prefs) = Tile("Counter") {
    val scope = rememberCoroutineScope()
    val count by prefs.counter.collectAsStateWithLifecycle(initialValue = 0L)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$count", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = { scope.launch { prefs.setCounter(CounterMath.decrement(count)) } }) { Text("−") }
        TextButton(onClick = { scope.launch { prefs.setCounter(CounterMath.increment(count)) } }) { Text("+") }
        TextButton(onClick = { scope.launch { prefs.setCounter(0L) } }) { Text("Reset") }
    }
}

@Composable
private fun BatteryTile(read: () -> BatterySnapshot) = Tile("Battery") {
    val snapshot = remember { read() }
    Text(snapshot.percent?.let { "$it%" } ?: "Level unavailable", style = MaterialTheme.typography.headlineMedium)
    val temperature = snapshot.batteryTemperatureC?.let { "Battery temperature ${(it * 10).roundToInt() / 10.0} °C" }
        ?: "Battery temperature unavailable"
    Text(
        if (snapshot.charging) "$temperature, charging" else temperature,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
