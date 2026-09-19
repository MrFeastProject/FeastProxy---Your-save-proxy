package MrFeastProject.FeastProxy.com

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class BroadbandLine(
    val id: String,
    val name: String,
    val shortName: String,
    val dcTag: String,
    val flag: String,
    val description: String,
    val primaryIp: String,
    val secondaryIp: String = "",
    val port: Int = 443,
    val dc1: String = "",
    val dc2: String = "",
    val dc3: String = "",
    val dc4: String = "",
    val dc5: String = ""
)

object MrFeastBroadbandManager {

    val LINES = listOf(
        BroadbandLine(
            id = "france_1",
            name = "MrFeastProject France #1 (DC 2 и DC 4)",
            shortName = "France #1",
            dcTag = "DC 2 & 4",
            flag = "🇫🇷",
            description = "Европейский высокоскоростной шлюз. Идеально для большинства пользователей и медиа.",
            primaryIp = "149.154.167.220",
            secondaryIp = "149.154.167.91",
            dc2 = "149.154.167.220",
            dc4 = "149.154.167.91"
        ),
        BroadbandLine(
            id = "german_2",
            name = "MrFeastProject German #2 (DC 5)",
            shortName = "German #2",
            dcTag = "DC 5",
            flag = "🇩🇪",
            description = "Центрально-европейский сервер (Франкфурт) с оптимизированным прямым маршрутом.",
            primaryIp = "91.108.56.130",
            dc5 = "91.108.56.130"
        ),
        BroadbandLine(
            id = "private_gib",
            name = "MrFeastProject Private 1 GIB server (DC 1 и DC 3)",
            shortName = "Private 1 GIB",
            dcTag = "DC 1 & 3",
            flag = "⚡",
            description = "Выделенный гигабитный сервер с приоритетной маршрутизацией через скоростную магистраль.",
            primaryIp = "149.154.175.50",
            secondaryIp = "149.154.175.100",
            dc1 = "149.154.175.50",
            dc3 = "149.154.175.100"
        )
    )

    suspend fun pingHost(host: String, port: Int = 443, timeoutMs: Int = 2200): Long? = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext null
        try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val elapsed = System.currentTimeMillis() - start
            maxOf(1L, elapsed)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun measureAllLines(): Map<String, Long?> = coroutineScope {
        LINES.map { line ->
            async {
                val ping = pingHost(line.primaryIp, line.port)
                line.id to ping
            }
        }.awaitAll().toMap()
    }

    fun findFastestLine(latencies: Map<String, Long?>): BroadbandLine {
        val validPings = latencies.filterValues { it != null && it > 0 }
        if (validPings.isEmpty()) return LINES.first()
        val fastestId = validPings.minByOrNull { it.value ?: Long.MAX_VALUE }?.key
        return LINES.firstOrNull { it.id == fastestId } ?: LINES.first()
    }

    suspend fun applyLineToSettings(
        line: BroadbandLine,
        settingsStore: SettingsStore
    ) {
        settingsStore.saveBroadbandLineConfig(
            lineId = line.id,
            dc1 = line.dc1,
            dc2 = line.dc2,
            dc3 = line.dc3,
            dc4 = line.dc4,
            dc5 = line.dc5
        )
    }
}
