package com.hishab.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hishab.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore(name = "hishab_settings")

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val bengaliDigits: Boolean = true,
    val remindersEnabled: Boolean = false,
    val cloudAiConsent: Boolean = false,
    val syncEnabled: Boolean = false,
    val initialized: Boolean = false,
    val lastBackupAt: Long? = null
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DIGITS = booleanPreferencesKey("bn_digits")
        val REMINDERS = booleanPreferencesKey("reminders")
        val CLOUD_AI = booleanPreferencesKey("cloud_ai_consent")
        val SYNC = booleanPreferencesKey("sync_enabled")
        val INITIALIZED = booleanPreferencesKey("initialized")
        val LAST_BACKUP = longPreferencesKey("last_backup")
    }
    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            bengaliDigits = p[Keys.DIGITS] ?: true,
            remindersEnabled = p[Keys.REMINDERS] ?: false,
            cloudAiConsent = p[Keys.CLOUD_AI] ?: false,
            syncEnabled = p[Keys.SYNC] ?: false,
            initialized = p[Keys.INITIALIZED] ?: false,
            lastBackupAt = p[Keys.LAST_BACKUP]
        )
    }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setBengaliDigits(value: Boolean) = context.dataStore.edit { it[Keys.DIGITS] = value }
    suspend fun setReminders(value: Boolean) = context.dataStore.edit { it[Keys.REMINDERS] = value }
    suspend fun setCloudAiConsent(value: Boolean) = context.dataStore.edit { it[Keys.CLOUD_AI] = value }
    suspend fun setSyncEnabled(value: Boolean) = context.dataStore.edit { it[Keys.SYNC] = value }
    suspend fun markInitialized() = context.dataStore.edit { it[Keys.INITIALIZED] = true }
    suspend fun markBackup(time: Long) = context.dataStore.edit { it[Keys.LAST_BACKUP] = time }
    suspend fun replaceAll(value: AppSettings) = context.dataStore.edit {
        it[Keys.THEME] = value.themeMode.name
        it[Keys.DIGITS] = value.bengaliDigits
        it[Keys.REMINDERS] = value.remindersEnabled
        it[Keys.CLOUD_AI] = value.cloudAiConsent
        it[Keys.SYNC] = value.syncEnabled
        it[Keys.INITIALIZED] = value.initialized
        value.lastBackupAt?.let { ts -> it[Keys.LAST_BACKUP] = ts } ?: it.remove(Keys.LAST_BACKUP)
    }
}
