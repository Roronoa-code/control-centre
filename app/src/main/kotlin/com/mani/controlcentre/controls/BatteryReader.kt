package com.mani.controlcentre.controls

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** Battery facts from the platform. The temperature is the battery sensor, not room or CPU temperature. */
data class BatterySnapshot(val percent: Int?, val batteryTemperatureC: Float?, val charging: Boolean)

fun readBattery(context: Context): BatterySnapshot {
    val manager = context.getSystemService(BatteryManager::class.java)
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
    val tenthsC = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
    return BatterySnapshot(
        percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 },
        batteryTemperatureC = tenthsC.takeIf { it != Int.MIN_VALUE }?.let { it / 10f },
        charging = manager.isCharging,
    )
}
