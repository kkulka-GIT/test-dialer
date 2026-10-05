package com.example.testdialer.active

import android.content.Context
import com.example.testdialer.data.DataVolume
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedScenarioPlan(val id: String, val name: String, val scenario: LocalScenario)

/** Parameter-only plan storage. Loading a plan creates fresh step ids and never executes a service. */
class ScenarioPlanStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun list(): List<SavedScenarioPlan> = runCatching {
        val array = JSONArray(preferences.getString("items", "[]"))
        (0 until array.length()).mapNotNull { index -> runCatching { decode(array.getJSONObject(index)) }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(name: String, source: LocalScenario): Boolean = synchronized(preferences) {
        val items = list()
        require(items.size < MAX_PLANS) { "Zapisano już $MAX_PLANS planów. Usuń niepotrzebny plan." }
        val saved = SavedScenarioPlan(UUID.randomUUID().toString(), name.trim(), source)
        validate(saved)
        write(items + saved)
    }

    fun delete(id: String): Boolean = synchronized(preferences) { write(list().filterNot { it.id == id }) }

    fun instantiate(id: String): LocalScenario {
        val saved = requireNotNull(list().firstOrNull { it.id == id }) { "Plan jest niedostępny." }
        val amounts = mutableMapOf<StepId, Long?>()
        val steps = saved.scenario.steps.mapIndexed { index, old ->
            val fresh = StepId("plan-${UUID.randomUUID()}")
            if (saved.scenario.dataAmounts.containsKey(old.id)) amounts[fresh] = saved.scenario.dataAmounts[old.id]
            old.copy(id = fresh, order = index)
        }
        return LocalScenario("saved-plan-${UUID.randomUUID()}", saved.name, steps, amounts)
    }

    private fun write(items: List<SavedScenarioPlan>): Boolean {
        val array = JSONArray()
        items.forEach { plan ->
            val steps = JSONArray()
            plan.scenario.steps.forEach { step ->
                steps.put(JSONObject().put("title", step.title).put("instruction", step.instruction)
                    .put("type", step.action.serviceType.name).put("target", when (val action = step.action) {
                        is TestAction.Voice -> action.destination
                        is TestAction.Sms -> action.destination
                        is TestAction.Data -> action.target
                    }).apply {
                        (step.action as? TestAction.Sms)?.message?.let { put("message", it) }
                        if (plan.scenario.dataAmounts.containsKey(step.id)) {
                            put("requiresBytes", true)
                            plan.scenario.dataAmounts[step.id]?.let { put("bytes", it) }
                        }
                    })
            }
            array.put(JSONObject().put("id", plan.id).put("name", plan.name).put("steps", steps))
        }
        return preferences.edit().putString("items", array.toString()).commit()
    }

    private fun decode(item: JSONObject): SavedScenarioPlan {
        val id = item.getString("id")
        val name = item.getString("name")
        val encoded = item.getJSONArray("steps")
        val amounts = mutableMapOf<StepId, Long?>()
        val steps = (0 until encoded.length()).map { index ->
            val raw = encoded.getJSONObject(index)
            val stepId = StepId("stored-$index")
            val target = raw.getString("target")
            val action = when (raw.getString("type")) {
                "VOICE" -> TestAction.Voice(target)
                "SMS" -> TestAction.Sms(target, if (raw.has("message")) raw.getString("message") else null)
                "DATA" -> TestAction.Data(target)
                else -> error("Nieznany rodzaj kroku")
            }
            if (raw.optBoolean("requiresBytes")) amounts[stepId] = if (raw.has("bytes")) raw.getLong("bytes") else null
            ScenarioStepDefinition(stepId, index, raw.getString("title"), raw.getString("instruction"), action)
        }
        return SavedScenarioPlan(id, name, LocalScenario("stored-$id", name, steps, amounts)).also(::validate)
    }

    private fun validate(plan: SavedScenarioPlan) {
        require(plan.id.isNotBlank() && plan.name.isNotBlank() && plan.name.length <= 80) { "Nazwa planu: od 1 do 80 znaków." }
        require(plan.scenario.steps.isNotEmpty() && plan.scenario.steps.size <= MAX_STEPS) { "Plan musi mieć od 1 do $MAX_STEPS kroków." }
        plan.scenario.steps.forEach { step ->
            require(step.title.length <= 120 && step.instruction.length <= 2_000)
            when (val action = step.action) {
                is TestAction.Voice -> require(action.destination.length <= 500)
                is TestAction.Sms -> require(action.destination.length <= 500 && (action.message?.length ?: 0) <= 10_000)
                is TestAction.Data -> require(action.target.length <= 2_000 && (plan.scenario.dataAmounts[step.id] == null || plan.scenario.dataAmounts[step.id] in 1..DataVolume.MAX_BYTES))
            }
        }
    }

    companion object {
        const val PREFERENCES = "scenario-plans-v1"
        const val MAX_PLANS = 20
        const val MAX_STEPS = 50
    }
}
