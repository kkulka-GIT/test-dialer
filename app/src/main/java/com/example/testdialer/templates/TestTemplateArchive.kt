package com.example.testdialer.templates

import com.example.testdialer.domain.TestAction
import com.example.testdialer.data.DataVolume
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** A portable, parameter-only format. Decode is strict and has no persistence or execution side effects. */
object TestTemplateArchive {
    const val MAX_FILE_BYTES = 4_194_304
    private const val FORMAT = "test-dialer-templates"

    fun read(input: InputStream): List<TestTemplate> {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_FILE_BYTES) { "Plik przekracza 4 MB." }
            output.write(buffer, 0, count)
        }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString()
        return decode(text)
    }

    fun encode(items: List<TestTemplate>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("name", item.name).put("type", item.action.serviceType.name)
                .put("target", when (val action = item.action) {
                    is TestAction.Voice -> action.destination
                    is TestAction.Sms -> action.destination
                    is TestAction.Data -> action.target
                }).apply {
                    (item.action as? TestAction.Sms)?.message?.let { put("message", it) }
                    item.requestedBytes?.let { put("bytes", it) }
                })
        }
        return JSONObject().put("format", FORMAT).put("schemaVersion", 1).put("templates", array).toString(2)
    }

    fun decode(text: String): List<TestTemplate> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Plik przekracza 4 MB." }
        val root = JSONObject(text)
        require(root.opt("format") == FORMAT) { "To nie jest plik szablonów Test Dialer." }
        require(root.opt("schemaVersion") is Int && root.getInt("schemaVersion") == 1) { "Nieobsługiwana wersja pliku szablonów." }
        val items = root.getJSONArray("templates")
        require(items.length() <= TestTemplateStore.MAX_ITEMS) { "Plik zawiera za dużo szablonów." }
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            fun string(key: String): String = (item.get(key) as? String)
                ?: throw IllegalArgumentException("Pole $key musi być tekstem (szablon ${index + 1}).")
            val name = string("name").trim()
            require(name.isNotBlank() && name.length <= 80) { "Nazwa szablonu: od 1 do 80 znaków." }
            val target = string("target")
            val action = when (string("type")) {
                "VOICE" -> TestAction.Voice(target).also { require(target.length <= 500) }
                "SMS" -> TestAction.Sms(target, if (item.has("message")) string("message") else null)
                    .also { require(target.length <= 500 && (it.message?.length ?: 0) <= 10000) }
                "DATA" -> TestAction.Data(target).also { require(target.length <= 2000) }
                else -> throw IllegalArgumentException("Nieznany rodzaj testu (szablon ${index + 1}).")
            }
            val bytes = if (item.has("bytes")) {
                val value = item.get("bytes")
                require(value is Int || value is Long) { "Ilość danych musi być liczbą całkowitą." }
                (value as Number).toLong().also { require(action is TestAction.Data && it in 1..DataVolume.MAX_BYTES) { "Nieprawidłowa ilość danych." } }
            } else null
            TestTemplate(UUID.randomUUID().toString(), name, action, bytes)
        }
    }
}
