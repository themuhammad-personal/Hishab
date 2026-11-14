package com.hishab.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hishab.app.HishabApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as HishabApplication).container
                val enabled = container.settings.settings.firstOrNull()?.remindersEnabled == true
                container.reminders.schedule(enabled)
            } finally { pending.finish() }
        }
    }
}
