package com.example.testdialer.data

import java.net.URI

data class SafeDownloadUrl(val uri: URI, val host: String)

object SafeDownloadUrlValidator {
    fun requireValid(raw: String): SafeDownloadUrl {
        val uri = runCatching { URI(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("Nieprawidłowy adres HTTPS") }
        require(uri.scheme.equals("https", ignoreCase = true)) { "Dozwolony jest wyłącznie HTTPS" }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Adres nie może zawierać danych logowania, zapytania ani fragmentu"
        }
        require(uri.port == -1 || uri.port == 443) { "Dozwolony jest wyłącznie port 443" }
        val host = uri.host?.lowercase()?.trimEnd('.')
        require(!host.isNullOrBlank()) { "Adres musi zawierać publiczną nazwę hosta" }
        require(host != "localhost" && !host.endsWith(".localhost") && !host.endsWith(".local")) {
            "Host lokalny jest niedozwolony"
        }
        require(!isIpLiteral(host)) { "Adres IP jest niedozwolony; użyj publicznej nazwy hosta" }
        return SafeDownloadUrl(uri, host)
    }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.matches(Regex("[0-9.]+"))
}
