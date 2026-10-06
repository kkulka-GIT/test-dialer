package com.example.testdialer.active

import com.example.testdialer.data.DataVolume
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** Portable parameter-only plan archive. Decoding has no persistence or execution side effects. */
object ScenarioPlanArchive {
    const val MAX_FILE_BYTES = 4_194_304
    private const val FORMAT = "test-dialer-scenario-plans"

    fun read(input: InputStream): List<SavedScenarioPlan> {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_FILE_BYTES) { "Plik przekracza 4 MB." }
            output.write(buffer, 0, count)
        }
        return decode(Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString())
    }

    fun encode(items: List<SavedScenarioPlan>): String {
        require(items.size <= ScenarioPlanStore.MAX_PLANS)
        val plans = JSONArray()
        items.forEach { saved ->
            val steps = JSONArray()
            saved.scenario.steps.forEach { step ->
                steps.put(JSONObject().put("title", step.title).put("instruction", step.instruction)
                    .put("type", step.action.serviceType.name).put("target", when (val action = step.action) {
                        is TestAction.Voice -> action.destination
                        is TestAction.Sms -> action.destination
                        is TestAction.Data -> action.target
                    }).apply {
                        (step.action as? TestAction.Sms)?.message?.let { put("message", it) }
                        if (saved.scenario.dataAmounts.containsKey(step.id)) {
                            put("requiresBytes", true); saved.scenario.dataAmounts[step.id]?.let { put("bytes", it) }
                        }
                    })
            }
            plans.put(JSONObject().put("name", saved.name).put("steps", steps))
        }
        return JSONObject().put("format", FORMAT).put("schemaVersion", 1).put("plans", plans).toString(2)
            .also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Kopia planów przekracza 4 MB." } }
    }

    fun decode(text: String): List<SavedScenarioPlan> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Plik przekracza 4 MB." }
        val root = JSONObject(text)
        require(root.opt("format") == FORMAT) { "To nie jest plik planów Test Dialer." }
        require(root.opt("schemaVersion") is Int && root.getInt("schemaVersion") == 1) { "Nieobsługiwana wersja pliku planów." }
        val plans = root.getJSONArray("plans")
        require(plans.length() <= ScenarioPlanStore.MAX_PLANS) { "Plik zawiera za dużo planów." }
        return (0 until plans.length()).map { planIndex ->
            val rawPlan = plans.getJSONObject(planIndex)
            val name = strictString(rawPlan, "name").trim()
            require(name.isNotBlank() && name.length <= 80) { "Nazwa planu: od 1 do 80 znaków." }
            val rawSteps = rawPlan.getJSONArray("steps")
            require(rawSteps.length() in 1..ScenarioPlanStore.MAX_STEPS) { "Plan musi mieć od 1 do ${ScenarioPlanStore.MAX_STEPS} kroków." }
            val amounts = mutableMapOf<StepId, Long?>()
            val steps = (0 until rawSteps.length()).map { stepIndex ->
                val raw = rawSteps.getJSONObject(stepIndex); val id = StepId("archive-$planIndex-$stepIndex")
                val title = strictString(raw, "title"); val instruction = strictString(raw, "instruction"); val target = strictString(raw, "target")
                require(title.length <= 120 && instruction.length <= 2_000)
                val action = when (strictString(raw, "type")) {
                    "VOICE" -> TestAction.Voice(target).also { require(target.length <= 500) }
                    "SMS" -> TestAction.Sms(target, if (raw.has("message")) strictString(raw, "message") else null)
                        .also { require(target.length <= 500 && (it.message?.length ?: 0) <= 10_000) }
                    "DATA" -> TestAction.Data(target).also { require(target.length <= 2_000) }
                    else -> error("Nieznany rodzaj kroku w planie ${planIndex + 1}.")
                }
                if (raw.optBoolean("requiresBytes")) {
                    require(action is TestAction.Data) { "Ilość danych przypisano do innego rodzaju testu." }
                    amounts[id] = if (raw.has("bytes")) raw.get("bytes").let { value ->
                        require(value is Int || value is Long) { "Ilość danych musi być liczbą całkowitą." }
                        (value as Number).toLong().also { require(it in 1..DataVolume.MAX_BYTES) { "Nieprawidłowa ilość danych." } }
                    } else null
                }
                ScenarioStepDefinition(id, stepIndex, title, instruction, action)
            }
            val id = UUID.randomUUID().toString()
            SavedScenarioPlan(id, name, LocalScenario("archive-$id", name, steps, amounts))
        }
    }

    private fun strictString(source: JSONObject, key: String): String =
        (source.get(key) as? String) ?: throw IllegalArgumentException("Pole $key musi być tekstem.")
}
