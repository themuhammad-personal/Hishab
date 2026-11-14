package com.hishab.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.HishabDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(WORK); return }
        val now = java.time.ZonedDateTime.now(ZoneId.systemDefault())
        var next = now.with(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = java.time.Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
    companion object { const val WORK = "hishab-due-reminders"; const val CHANNEL = "hishab_dues" }
}

class DueReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = HishabDatabase.create(applicationContext)
        return try {
            val transactions = db.transactions().observe().firstOrNull().orEmpty().filter { it.deletedAt == null && it.dueDirection != null }
            val payments = db.payments().observe().firstOrNull().orEmpty().groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amountPoisha } }
            val today = Dates.today()
            val due = transactions.filter { t ->
                val remaining = t.amountPoisha - (payments[t.id] ?: 0L)
                val d = Dates.parseDate(t.dueDate ?: t.date)
                remaining > 0 && d != null && !d.isAfter(today.plusDays(1))
            }
            if (due.isNotEmpty()) post(due.size, due.sumOf { it.amountPoisha - (payments[it.id] ?: 0L) })
            Result.success()
        } catch (_: Exception) { Result.retry() } finally { db.close() }
    }
    private fun post(count: Int, total: Long) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ReminderScheduler.CHANNEL, "বাকির রিমাইন্ডার", NotificationManager.IMPORTANCE_DEFAULT))
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("বাকি মনে করিয়ে দেওয়া")
            .setContentText("$countটি বাকি · মোট ${Money.formatPoisha(total)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("আজ/আগামীকালের মধ্যে পরিশোধযোগ্য বাকি আছে। হিসাব অ্যাপে বিস্তারিত দেখুন।"))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
