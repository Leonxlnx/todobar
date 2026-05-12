package dev.todobar.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Restarts the floating-bubble service after a reboot or app update, but only
 * when the user has already granted the overlay permission. Matches the
 * "Autostart on system login" behavior on desktop.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED
            && action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        if (!Settings.canDrawOverlays(context)) return
        BubbleService.start(context)
    }
}
