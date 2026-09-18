package MrFeastProject.FeastProxy.com

object ServerNameMapper {
    private val knownServers = listOf(
        // DC1 (Germany)
        "149.154.175.50" to "MrFeastProject - Germany #1",
        "149.154.175.51" to "MrFeastProject - Germany #2",
        "149.154.175.52" to "MrFeastProject - Germany #3",
        "149.154.175.10" to "MrFeastProject - Germany #4",
        "149.154.175.100" to "MrFeastProject - Germany #5",

        // DC2 (France)
        "149.154.167.50" to "MrFeastProject - France #1",
        "149.154.167.51" to "MrFeastProject - France #2",
        "149.154.167.91" to "MrFeastProject - France #3",
        "149.154.167.92" to "MrFeastProject - France #4",
        "149.154.167.220" to "MrFeastProject - France #5",
        "149.154.167.222" to "MrFeastProject - France #6",

        // DC3 (USA)
        "149.154.175.100" to "MrFeastProject - USA #1",
        "149.154.175.101" to "MrFeastProject - USA #2",

        // DC4 (Netherlands)
        "91.108.56.165" to "MrFeastProject - Netherlands #1",
        "91.108.56.166" to "MrFeastProject - Netherlands #2",
        "91.108.56.170" to "MrFeastProject - Netherlands #3",
        "91.108.56.171" to "MrFeastProject - Netherlands #4",
        "91.108.56.100" to "MrFeastProject - Netherlands #5",

        // DC5 (Finland & Singapore)
        "91.108.4.155" to "MrFeastProject - Finland #1",
        "91.108.4.156" to "MrFeastProject - Finland #2",
        "91.108.8.10" to "MrFeastProject - Singapore #1",
        "91.108.8.11" to "MrFeastProject - Singapore #2"
    )

    private val countryServers = listOf(
        "MrFeastProject - France #1",
        "MrFeastProject - France #2",
        "MrFeastProject - Germany #1",
        "MrFeastProject - Germany #2",
        "MrFeastProject - Netherlands #1",
        "MrFeastProject - Netherlands #2",
        "MrFeastProject - Finland #1",
        "MrFeastProject - USA #1"
    )

    fun mapServerInLog(message: String): String {
        var result = message

        // 1. Replace known specific IPs
        for ((ip, name) in knownServers) {
            if (result.contains(ip)) {
                result = result.replace(ip, name)
            }
        }

        // 2. Generic IP:port or IP replacement (ignoring localhost)
        val ipRegex = Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d+)?\\b")
        result = ipRegex.replace(result) { matchResult ->
            val raw = matchResult.value
            if (raw.startsWith("127.0.0.1") || raw.startsWith("0.0.0.0")) {
                raw
            } else {
                val index = Math.abs(raw.hashCode()) % countryServers.size
                countryServers[index]
            }
        }

        // 3. Domain names like *.workers.dev, *.pages.dev, etc.
        val domainRegex = Regex("\\b[a-zA-Z0-9.-]+\\.(?:pages\\.dev|workers\\.dev|workers|pages|cloudflare\\.com|org|net)\\b", RegexOption.IGNORE_CASE)
        result = domainRegex.replace(result) { matchResult ->
            val rawDomain = matchResult.value
            val index = Math.abs(rawDomain.hashCode()) % countryServers.size
            countryServers[index]
        }

        return result
    }
}
