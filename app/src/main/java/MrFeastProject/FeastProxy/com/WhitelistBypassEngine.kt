package MrFeastProject.FeastProxy.com

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Random
import java.util.concurrent.atomic.AtomicLong

/**
 * Whitelist Bypass Engine (Обход белых списков Beta).
 *
 * При включении:
 * - Фрагментация исходящих пакетов на блоки по 10-14 КБ.
 * - Для каждого фрагментированного пакета открывается отдельное новое TCP соединение к целевому DC/серверу.
 */
object WhitelistBypassEngine {
    private const val TAG = "WhitelistBypass"

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled = _isEnabled.asStateFlow()

    private val _fragmentedPacketsCount = MutableStateFlow(0L)
    val fragmentedPacketsCount = _fragmentedPacketsCount.asStateFlow()

    private val _totalBytesFragmented = MutableStateFlow(0L)
    val totalBytesFragmented = _totalBytesFragmented.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val random = Random()

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        if (enabled) {
            Log.i(TAG, "[WhitelistBypass Beta] Режим активирован: фрагментация 10-14 КБ + новое TCP соединение на пакет")
        } else {
            Log.i(TAG, "[WhitelistBypass Beta] Режим деактивирован")
        }
    }

    /**
     * Вычисляет размер следующего фрагмента (10-14 КБ = 10240 - 14336 байт)
     */
    fun getNextChunkSize(): Int {
        val minBytes = 10 * 1024 // 10 KB
        val maxBytes = 14 * 1024 // 14 KB
        return minBytes + random.nextInt(maxBytes - minBytes + 1)
    }

    /**
     * Разбивает данные на фрагменты по 10-14 КБ
     */
    fun fragmentData(data: ByteArray): List<ByteArray> {
        if (!_isEnabled.value || data.size <= 10 * 1024) {
            return listOf(data)
        }

        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val remaining = data.size - offset
            val chunkSize = minOf(getNextChunkSize(), remaining)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            chunks.add(chunk)
            offset += chunkSize
        }
        return chunks
    }

    /**
     * Передает фрагментированный поток через новые TCP соединения на каждый пакет.
     */
    fun dispatchPacketWithNewTcpConnection(
        host: String,
        port: Int,
        payload: ByteArray,
        timeoutMs: Int = 10000,
        onResponse: ((ByteArray) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val chunks = fragmentData(payload)
                for ((index, chunk) in chunks.withIndex()) {
                    // Открываем отдельное новое TCP соединение для каждого пакета
                    Socket().use { socket ->
                        socket.tcpNoDelay = true
                        socket.connect(InetSocketAddress(host, port), timeoutMs)
                        val out = socket.getOutputStream()
                        out.write(chunk)
                        out.flush()

                        _fragmentedPacketsCount.value += 1
                        _totalBytesFragmented.value += chunk.size

                        val sizeKb = String.format("%.1f", chunk.size / 1024f)
                        Log.i(
                            TAG,
                            "Фрагмент #${index + 1}/${chunks.size} ($sizeKb КБ) отправлен через отдельное TCP-соединение на $host:$port"
                        )

                        if (onResponse != null) {
                            val inStream = socket.getInputStream()
                            val buffer = ByteArray(32 * 1024)
                            val read = inStream.read(buffer)
                            if (read > 0) {
                                onResponse(buffer.copyOfRange(0, read))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка TCP диспетчеризации: ${e.message}")
            }
        }
    }
}
