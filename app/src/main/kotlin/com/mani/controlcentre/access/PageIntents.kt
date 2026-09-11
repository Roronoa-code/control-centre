package com.mani.controlcentre.access

import android.content.Context
import android.content.Intent
import com.mani.controlcentre.core.Page
import com.mani.controlcentre.ui.PanelActivity

/** Builds and reads the only launch contract the panel accepts: an allowlisted page id. */
object PageIntents {
    const val EXTRA_PAGE = "com.mani.controlcentre.extra.PAGE"

    private val aliasPages = mapOf(
        "com.mani.controlcentre.access.EverydayEntry" to Page.EVERYDAY,
        "com.mani.controlcentre.access.ToolsEntry" to Page.TOOLS,
        "com.mani.controlcentre.access.DeviceEntry" to Page.DEVICE,
    )

    fun panel(context: Context, page: Page): Intent =
        Intent(context, PanelActivity::class.java)
            .putExtra(EXTRA_PAGE, page.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** Explicit extra first, then the alias that was launched, otherwise Everyday. Unknown values fall back to Everyday. */
    fun pageOf(intent: Intent?): Page {
        val extra = intent?.getStringExtra(EXTRA_PAGE)
        if (extra != null) return Page.parse(extra)
        return aliasPages[intent?.component?.className] ?: Page.EVERYDAY
    }
}
