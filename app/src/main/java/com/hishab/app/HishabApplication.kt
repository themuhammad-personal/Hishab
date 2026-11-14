package com.hishab.app

import android.app.Application
import com.hishab.app.data.local.HishabDatabase
import com.hishab.app.data.repository.BackupManager
import com.hishab.app.data.repository.LedgerRepository
import com.hishab.app.data.settings.SettingsRepository
import com.hishab.app.notifications.ReminderScheduler

class HishabApplication : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val database = HishabDatabase.create(app)
    val settings = SettingsRepository(app)
    val ledger = LedgerRepository(database, settings)
    val backup = BackupManager(database, settings)
    val reminders = ReminderScheduler(app)
}
