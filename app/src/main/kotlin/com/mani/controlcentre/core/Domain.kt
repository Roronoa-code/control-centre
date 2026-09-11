package com.mani.controlcentre.core

import java.net.IDN
import kotlin.math.abs
import kotlin.math.hypot

enum class Page(val id: String, val title: String) {
    EVERYDAY("everyday", "Everyday"), TOOLS("tools", "Tools"), DEVICE("device", "Device");
    companion object { fun parse(id: String?) = entries.find { it.id == id } ?: EVERYDAY }
}
enum class Route { SIDE, TOP, EXTERNAL }
data class TriggerConfig(
    val enabled: Boolean = false,
    val route: Route = Route.SIDE,
    val left: Boolean = false,
    val position: Float = 0.45f,
    val widthDp: Float = 16f,
    val heightDp: Float = 96f,
    val topWidthDp: Float = 80f,
    val showHandle: Boolean = true,
    val excluded: Set<String> = emptySet(),
)
data class GestureSample(val page: Page, val progress: Float, val recognised: Boolean)

/** Coordinates are density-independent displacement from the original pointer. */
class GestureRouter(private val route: Route, private val left: Boolean = false, private val slop: Float = 10f) {
    var sample = GestureSample(Page.EVERYDAY, 0f, false)
        private set
    private var cancelled = false
    fun move(dx: Float, dy: Float): GestureSample {
        if (cancelled || !dx.isFinite() || !dy.isFinite() || route == Route.EXTERNAL) return sample
        val travel = if (route == Route.TOP) dy else if (left) dx else -dx
        val page = when {
            route == Route.TOP && dx <= -144f -> Page.DEVICE
            route == Route.TOP && dx <= -64f -> Page.TOOLS
            route == Route.TOP -> Page.EVERYDAY
            dy < -abs(travel) * 0.55f -> Page.TOOLS
            dy > abs(travel) * 0.55f -> Page.DEVICE
            else -> Page.EVERYDAY
        }
        val threshold = if (route == Route.TOP) 52f else 40f
        sample = GestureSample(page, (travel / threshold).coerceIn(0f, 1f), travel > slop && hypot(dx, dy) > slop)
        return sample
    }
    fun finish(): Page? = sample.page.takeIf { !cancelled && sample.recognised && sample.progress >= 1f }
    fun cancel() { cancelled = true; sample = GestureSample(Page.EVERYDAY, 0f, false) }
}

enum class ControlId(val label: String, val defaultPage: Page, val defaultSpan: Int = 1) {
    WIFI("Wi-Fi", Page.EVERYDAY), BLUETOOTH("Bluetooth", Page.EVERYDAY),
    SOUND("Sound", Page.EVERYDAY), DND("Do not disturb", Page.EVERYDAY),
    TORCH("Torch", Page.EVERYDAY), ROTATION("Auto-rotate", Page.EVERYDAY),
    BRIGHTNESS("Brightness", Page.EVERYDAY, 2), VOLUME("Media volume", Page.EVERYDAY, 2),
    MEDIA("Now playing", Page.EVERYDAY, 2),
    COUNTER("Counter", Page.TOOLS, 2), CAFFEINE("Caffeine", Page.TOOLS), CLIPBOARD("Clear clipboard", Page.TOOLS),
    COIN("Coin flip", Page.TOOLS), DICE("Dice", Page.TOOLS), BREATHE("Breathe", Page.TOOLS, 2), NOTES("Quick note", Page.TOOLS, 2),
    BATTERY("Battery", Page.DEVICE, 2), STORAGE("Internal storage", Page.DEVICE), RAM("Memory", Page.DEVICE),
    LUX("Light meter", Page.DEVICE), LEVEL("Level", Page.DEVICE), DNS("Private DNS", Page.DEVICE, 2),
    SCREENSHOT("Screenshot", Page.DEVICE), LOCK("Lock screen", Page.DEVICE),
}
data class Placement(val id: ControlId, val page: Page = id.defaultPage, val span: Int = id.defaultSpan, val hidden: Boolean = false)
object LayoutCodec {
    fun defaults() = ControlId.entries.map { Placement(it) }
    fun encode(items: List<Placement>) = items.joinToString(";") { "${it.id.name},${it.page.id},${it.span.coerceIn(1, 2)},${it.hidden}" }
    fun decode(raw: String?): List<Placement> {
        if (raw.isNullOrBlank()) return defaults()
        val seen = mutableSetOf<ControlId>()
        val result = raw.split(';').mapNotNull { item ->
            val fields = item.split(',')
            val id = ControlId.entries.find { it.name == fields.firstOrNull() } ?: return@mapNotNull null
            if (!seen.add(id)) return@mapNotNull null
            Placement(id, Page.parse(fields.getOrNull(1)), fields.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 2) ?: id.defaultSpan,
                fields.getOrNull(3)?.toBooleanStrictOrNull() ?: false)
        }
        return result + defaults().filter { it.id !in seen }
    }
}
object CounterMath {
    fun increment(value: Long) = if (value == Long.MAX_VALUE) value else value + 1
    fun decrement(value: Long) = (value - 1).coerceAtLeast(0)
}

enum class RestoreDecision { RESTORE, KEEP_USER_CHANGE, NOTHING }
fun restoreDecision(original: Int?, applied: Int?, current: Int): RestoreDecision = when {
    original == null || applied == null -> RestoreDecision.NOTHING
    current == applied -> RestoreDecision.RESTORE
    else -> RestoreDecision.KEEP_USER_CHANGE
}

data class BreathPhase(val name: String, val progress: Float, val cycle: Long)
fun breathPhase(elapsedMs: Long): BreathPhase {
    val safe = elapsedMs.coerceAtLeast(0)
    val phase = (safe % 12_000L).toFloat()
    return when {
        phase < 4_000f -> BreathPhase("Breathe in", phase / 4_000f, safe / 12_000)
        phase < 6_000f -> BreathPhase("Hold", 1f, safe / 12_000)
        else -> BreathPhase("Breathe out", 1f - (phase - 6_000f) / 6_000f, safe / 12_000)
    }
}

/** A hostname only: no URLs, IP literals, whitespace, commands, or empty labels. */
fun dnsHostname(input: String): String? {
    val value = input.trim().removeSuffix(".")
    if (value.isEmpty() || value.any { it.isWhitespace() } || value.contains(Regex("[/:;|&$`'\"\\\\]"))) return null
    val ascii = runCatching { IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).lowercase() }.getOrNull() ?: return null
    if (ascii.length > 253 || ascii.all { it.isDigit() || it == '.' }) return null
    val labels = ascii.split('.')
    if (labels.size < 2 || labels.any { it.length !in 1..63 || !it.first().isLetterOrDigit() || !it.last().isLetterOrDigit() || it.any { c -> !c.isLetterOrDigit() && c != '-' } }) return null
    return ascii
}
