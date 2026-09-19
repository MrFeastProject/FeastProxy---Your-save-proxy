package MrFeastProject.FeastProxy.com

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object BackgroundUpdateManager {
    private const val TAG = "BackgroundUpdate"
    private const val CHANNEL_ID = "feastproxy_updates_channel"
    private const val NOTIFICATION_ID = 2001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null

    fun start(context: Context) {
        if (updateJob != null && updateJob?.isActive == true) return

        updateJob = scope.launch {
            createNotificationChannel(context)
            val settingsStore = SettingsStore(context)
            val currentVersion = "v${BuildConfig.VERSION_NAME.removePrefix("v")}"

            while (isActive) {
                try {
                    val isEnabled = settingsStore.autoUpdateBackgroundEnabled.first()
                    if (isEnabled) {
                        Log.i(TAG, "Фоновая проверка обновлений (текущая версия: $currentVersion)...")
                        val release = fetchLatestReleaseInfo(currentVersion)
                        if (release != null && isNewerVersion(currentVersion, release.versionTag)) {
                            Log.i(TAG, "Найдено обновление FeastProxy: ${release.versionTag}")
                            showUpdateNotification(context, release)
                        } else {
                            Log.i(TAG, "Версия FeastProxy актуальна ($currentVersion)")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Ошибка фоновой проверки обновления: ${e.message}")
                }

                // Проверка каждые 4 часа
                delay(4 * 60 * 60 * 1000L)
            }
        }
    }

    fun stop() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновления FeastProxy",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о выходе новых версий FeastProxy"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showUpdateNotification(context: Context, release: AppReleaseInfo) {
        val targetUrl = release.releaseUrl.takeIf { it.isNotBlank() }
            ?: "https://github.com/MrFeastProject/FeastProxy---Your-save-proxy/releases/latest"

        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Доступно обновление FeastProxy ${release.versionTag}")
            .setContentText("Нажмите, чтобы скачать новую версию приложения")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }
}
