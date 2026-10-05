package com.example.testdialer.templates

import android.content.Context
import com.example.testdialer.domain.TestAction
import com.example.testdialer.data.DataVolume
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TestTemplate(val id: String, val name: String, val action: TestAction, val requestedBytes: Long? = null)

/** Parameters only: loading a template never executes a telecom service. */
class TestTemplateStore(context: Context) {
    private val preferences = context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE)

    fun list(): List<TestTemplate> = runCatching {
        val entries = JSONArray(preferences.getString("items", "[]"))
        (0 until entries.length()).mapNotNull { i -> runCatching {
            val item = entries.getJSONObject(i)
            val action = when (item.getString("type")) {
                "VOICE" -> TestAction.Voice(item.getString("target"))
                "SMS" -> TestAction.Sms(item.getString("target"), item.optString("message"))
                "DATA" -> TestAction.Data(item.getString("target"))
                else -> error("Unknown template type")
            }
            TestTemplate(item.getString("id"), item.getString("name"), action,
                if (item.has("bytes")) item.getLong("bytes") else null).also(::validate)
        }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(name: String, action: TestAction, requestedBytes: Long? = null): Boolean = synchronized(preferences) {
        val template = TestTemplate(UUID.randomUUID().toString(), name.trim(), action, requestedBytes)
        validate(template)
        val items = list()
        require(items.size < MAX_ITEMS) { "Zapisano już $MAX_ITEMS szablonów. Usuń niepotrzebny szablon." }
        write(items + template)
    }

    fun delete(id: String): Boolean = synchronized(preferences) { write(list().filterNot { it.id == id }) }

    private fun write(items: List<TestTemplate>): Boolean {
        val array = JSONArray()
        items.forEach { item ->
            val target = when (val action = item.action) {
                is TestAction.Voice -> action.destination
                is TestAction.Sms -> action.destination
                is TestAction.Data -> action.target
            }
            array.put(JSONObject().put("id", item.id).put("name", item.name)
                .put("type", item.action.serviceType.name).put("target", target).apply {
                    (item.action as? TestAction.Sms)?.message?.let { put("message", it) }
                    item.requestedBytes?.let { put("bytes", it) }
                })
        }
        return preferences.edit().putString("items", array.toString()).commit()
    }

    private fun validate(template: TestTemplate) {
        require(template.id.isNotBlank() && template.name.isNotBlank() && template.name.length <= 80) { "Nazwa szablonu: od 1 do 80 znaków." }
        require(template.requestedBytes == null || (template.action is TestAction.Data && template.requestedBytes in 1..DataVolume.MAX_BYTES))
        when (val action = template.action) {
            is TestAction.Voice -> require(action.destination.length <= 500)
            is TestAction.Sms -> require(action.destination.length <= 500 && (action.message?.length ?: 0) <= 10000)
            is TestAction.Data -> require(action.target.length <= 2000)
        }
    }
    companion object { const val MAX_ITEMS = 50 }
}
