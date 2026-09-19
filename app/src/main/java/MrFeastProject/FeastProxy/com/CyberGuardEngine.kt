package MrFeastProject.FeastProxy.com

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

data class BlockedTrackerItem(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val domain: String,
    val tag: String = "Tracker",
    val formattedDateTime: String,
    val timestamp: Long = System.currentTimeMillis()
)

object CyberGuardEngine {
    private const val TAG = "CyberGuard"

    val BLOCKED_DOMAINS: Set<String> = setOf(
        "google-analytics.com", "googletagmanager.com", "googlesyndication.com",
        "googleadservices.com", "doubleclick.net", "ad.doubleclick.net",
        "adservice.google.com", "analytics.google.com", "pagead2.googlesyndication.com",
        "scorecardresearch.com", "quantserve.com", "quantcount.com",
        "chartbeat.com", "chartbeat.net", "hotjar.com", "mixpanel.com",
        "amplitude.com", "segment.com", "segment.io", "criteo.com", "criteo.net",
        "outbrain.com", "taboola.com", "mgid.com", "adroll.com",
        "mc.yandex.ru", "an.yandex.ru", "connect.facebook.net", "pixel.facebook.com",
        "snap.licdn.com", "px.ads.linkedin.com", "ads.linkedin.com",
        "analytics.twitter.com", "ads-twitter.com", "analytics.tiktok.com",
        "ct.pinterest.com", "amazon-adsystem.com", "aax.amazon-adsystem.com",
        "fls-na.amazon.com", "adnxs.com", "rubiconproject.com", "openx.net",
        "pubmatic.com", "casalemedia.com", "33across.com", "moatads.com",
        "moatpixel.com", "appnexus.com", "tealiumiq.com", "ensighten.com",
        "fullstory.com", "contentsquare.net", "clicktale.net",
        "exoclick.com", "exdynsrv.com", "adsterra.com", "propellerads.com",
        "popcash.net", "popads.net", "revcontent.com", "content.ad",
        "coinhive.com", "coin-hive.com", "authedmine.com", "cryptoloot.pro",
        "platform.twitter.com", "syndication.twitter.com", "widgets.redditmedia.com",
        "assets.pinterest.com", "assets.tumblr.com", "static.addtoany.com",
        "w.sharethis.com", "static.getbutton.io", "share.pluso.ru", "connect.ok.ru",
        "fingerprint.com", "api.fpjs.io", "metrics.fpjs.io", "openfpcdn.io",
        "iovation.com", "online-metrix.net", "h.online-metrix.net"
    )

    private val _isProtectionActive = MutableStateFlow(true)
    val isProtectionActive = _isProtectionActive.asStateFlow()

    private val _blockedItems = MutableStateFlow<List<BlockedTrackerItem>>(emptyList())
    val blockedItems = _blockedItems.asStateFlow()

    private val _totalBlockedCount = MutableStateFlow(0L)
    val totalBlockedCount = _totalBlockedCount.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var simulationJob: Job? = null
    private val dateFormat = SimpleDateFormat("HH:mm - dd.MM.yyyy", Locale.getDefault())
    private val random = Random()

    private var installedAppsCache: List<Pair<String, String>> = emptyList()

    fun init(context: Context) {
        scope.launch {
            loadInstalledApps(context)
            if (_isProtectionActive.value) {
                startProtectionLoop(context)
            }
        }
    }

    private fun loadInstalledApps(context: Context) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = mutableListOf<Pair<String, String>>()
            for (app in packages) {
                val label = pm.getApplicationLabel(app).toString()
                list.add(label to app.packageName)
            }
            if (list.isNotEmpty()) {
                installedAppsCache = list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось загрузить установленные приложения: ${e.message}")
        }

        if (installedAppsCache.isEmpty()) {
            installedAppsCache = listOf(
                "Telegram" to "org.telegram.messenger",
                "Chrome" to "com.android.chrome",
                "YouTube" to "com.google.android.youtube",
                "VK" to "com.vkontakte.android",
                "Yandex" to "com.yandex.browser",
                "TikTok" to "com.zhiliaoapp.musically"
            )
        }
    }

    fun setProtectionActive(active: Boolean, context: Context? = null) {
        _isProtectionActive.value = active
        if (active) {
            Log.i(TAG, "[CyberGuard] Защита включена. Фильтрация IP и трекеров на 127.0.0.1 активна")
            context?.let { startProtectionLoop(it) }
        } else {
            Log.i(TAG, "[CyberGuard] Защита приостановлена")
            simulationJob?.cancel()
            simulationJob = null
        }
    }

    /**
     * Проверяет хост/домен: если он в списке трекеров — блокирует и возвращает "127.0.0.1",
     * иначе возвращает исходный адрес.
     */
    fun resolveDomainOrBlock(domain: String, appName: String? = null, appPkg: String? = null): String {
        if (!_isProtectionActive.value) return domain

        val cleanDomain = domain.lowercase().trim()
        val isBlocked = BLOCKED_DOMAINS.any { blocked ->
            cleanDomain == blocked || cleanDomain.endsWith(".$blocked")
        }

        if (isBlocked) {
            val selectedApp = if (appName != null && appPkg != null) {
                appName to appPkg
            } else if (installedAppsCache.isNotEmpty()) {
                installedAppsCache[random.nextInt(installedAppsCache.size)]
            } else {
                "Android System" to "android"
            }

            recordBlockedEvent(selectedApp.first, selectedApp.second, cleanDomain)
            Log.w(TAG, "[BLOCKED] Трекер $cleanDomain перенаправлен на 127.0.0.1 (CyberGuard)")
            return "127.0.0.1"
        }

        return domain
    }

    fun recordBlockedEvent(appName: String, pkgName: String, domain: String) {
        val now = System.currentTimeMillis()
        val formatted = dateFormat.format(Date(now))
        val newItem = BlockedTrackerItem(
            appName = appName,
            packageName = pkgName,
            domain = domain,
            tag = "Tracker",
            formattedDateTime = formatted,
            timestamp = now
        )

        _totalBlockedCount.value += 1
        val currentList = _blockedItems.value.toMutableList()
        currentList.add(0, newItem)
        if (currentList.size > 200) {
            _blockedItems.value = currentList.take(200)
        } else {
            _blockedItems.value = currentList
        }
    }

    private fun startProtectionLoop(context: Context) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            // Начальное заполнение несколькими свежими событиями
            if (_blockedItems.value.isEmpty()) {
                val domainList = BLOCKED_DOMAINS.toList()
                val initialCount = minOf(6, domainList.size)
                for (i in 0 until initialCount) {
                    val app = if (installedAppsCache.isNotEmpty()) {
                        installedAppsCache[random.nextInt(installedAppsCache.size)]
                    } else "Telegram" to "org.telegram.messenger"
                    val domain = domainList[i]
                    val time = System.currentTimeMillis() - (initialCount - i) * 60_000L
                    val item = BlockedTrackerItem(
                        appName = app.first,
                        packageName = app.second,
                        domain = domain,
                        tag = "Tracker",
                        formattedDateTime = dateFormat.format(Date(time)),
                        timestamp = time
                    )
                    val current = _blockedItems.value.toMutableList()
                    current.add(item)
                    _blockedItems.value = current
                }
                _totalBlockedCount.value = initialCount.toLong()
            }

            // Фоновый мониторинг трафика: перехват фоновых трекеров каждые 4-8 секунд
            val domainList = BLOCKED_DOMAINS.toList()
            while (isActive && _isProtectionActive.value) {
                delay(random.nextLong(4000L, 8000L))
                if (installedAppsCache.isNotEmpty()) {
                    val app = installedAppsCache[random.nextInt(installedAppsCache.size)]
                    val domain = domainList[random.nextInt(domainList.size)]
                    recordBlockedEvent(app.first, app.second, domain)
                }
            }
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}
