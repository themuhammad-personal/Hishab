package com.hishab.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hishab.app.data.settings.SettingsRepository
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
                val enabled = SettingsRepository(context).settings.firstOrNull()?.remindersEnabled == true
                ReminderScheduler(context).schedule(enabled)
            } finally { pending.finish() }
        }
    }
}
