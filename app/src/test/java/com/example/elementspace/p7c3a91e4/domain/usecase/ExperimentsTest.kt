package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.Experiments
import com.example.elementspace.p7c3a91e4.domain.model.LabEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExperimentsTest {
    private val models = File("src/main/assets/models/lab")

    @Test
    fun `twelve experiments, each with steps, both languages, a source and existing equipment`() {
        val index = File(models, "index.json").readText()
        assertTrue(File(models, "bench.glb").length() > 1000)
        assertEquals(19, Experiments.ALL.size)
        assertEquals(19, Experiments.ALL.map { it.id }.toSet().size)
        Experiments.ALL.forEach { e ->
            assertTrue(e.id, e.steps.size in 1..4)
            assertTrue(e.id, e.name.zh.isNotBlank() && e.name.en.isNotBlank() && e.setup.zh.isNotBlank() && e.setup.en.isNotBlank())
            assertTrue(e.id, e.source.zh.contains("人教版") && e.source.en.contains("PEP"))
            e.steps.forEach { s -> assertTrue(e.id, s.button.zh.isNotBlank() && s.button.en.isNotBlank() && s.observe.zh.length > 6 && s.observe.en.length > 12) }
            e.props.forEach { p ->
                assertTrue("${e.id} ${p.model}.glb", File(models, "${p.model}.glb").length() > 1000)
                assertTrue("${e.id} ${p.model} in index.json", index.contains("\"${p.model}\""))
            }
            e.choices.forEach { c -> assertTrue(e.id, c.label.zh.isNotBlank() && c.note.zh.isNotBlank() && c.note.en.isNotBlank()) }
        }
    }

    @Test
    fun `the atom-level view of an experiment is the same reaction`() {
        val reactions = BundledReactionRepository()
        val expected =
            mapOf(
                "mg_burn" to "2Mg + O₂", "fe_o2" to "3Fe + 2O₂", "na_water" to "2Na + 2H₂O", "cu_agno3" to "Cu + 2AgNO₃",
                "cuso4_naoh" to "CuSO₄ + 2NaOH", "h2o2" to "2H₂O₂", "thermite" to "2Al + Fe₂O₃",
                "fe_cuso4" to "Fe + CuSO₄", "zn_h2so4" to "Zn + H₂SO₄", "caco3_hcl" to "CaCO₃ + 2HCl", "limewater_co2" to "Ca(OH)₂ + CO₂",
                "s_burn" to "S + O₂", "cu_hno3" to "Cu + 4HNO₃", "metal_acid" to "Fe + 2HCl",
            )
        Experiments.ALL.forEach { e ->
            val id = e.reactionId ?: return@forEach
            val reaction = reactions.reaction(id)
            assertNotNull("${e.id} -> $id", reaction)
            assertEquals(e.id, expected[e.id], ChemicalFormula.sideText(reaction!!.reactants))
        }
        assertEquals(expected.keys, Experiments.ALL.mapNotNull { e -> e.id.takeIf { e.reactionId != null } }.toSet())
    }

    @Test
    fun `everything stays on the bench and at most two lights burn at once`() {
        Experiments.ALL.forEach { e ->
            (listOf(e.start) + e.steps.map { it.effects }).forEach { effects ->
                val lights = effects.count { it is LabEffect.Flame && it.light > 0f } + effects.count { it is LabEffect.Glow }
                assertTrue("${e.id}: $lights lights", lights <= 2)
                effects.filterIsInstance<LabEffect.Solid>().forEach { s -> assertTrue(e.id, kotlin.math.abs(s.at.x) < 0.6f && s.at.y in 0f..0.8f) }
            }
        }
    }

    @Test
    fun `the flame test shows the textbook colours`() {
        val flame = Experiments.experiment("flame_test")!!
        val notes = flame.choices.joinToString(" ") { it.note.zh }
        listOf("钠：黄色", "钾：紫色", "锂：紫红色", "钙：砖红色", "锶：洋红色", "钡：黄绿色", "铜：绿色").forEach { assertTrue(it, notes.contains(it)) }
    }

    @Test
    fun `every experiment is offered on at least one real element card`() {
        val symbols = com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository().elements().map { it.symbol }.toSet()
        assertEquals(Experiments.ALL.map { it.id }.toSet(), Experiments.ELEMENTS.keys)
        Experiments.ELEMENTS.forEach { (id, list) -> assertTrue(id, list.isNotEmpty() && symbols.containsAll(list)) }
        assertTrue(Experiments.forSymbol("Fe").map { it.id }.containsAll(listOf("fe_o2", "thermite", "fe_cuso4")))
        assertEquals("fe_o2", Experiments.forReaction("fe_1")?.id)
    }

    @Test
    fun `a choice either tints the flame or shows its own effects, and the metals bubble in order of activity`() {
        Experiments.ALL.forEach { e -> e.choices.forEach { c -> assertTrue(e.id, (c.flame != null) != (c.effects != null)) } }
        val rates = Experiments.experiment("metal_acid")!!.choices.map { c -> c.effects!!.filterIsInstance<LabEffect.Bubbles>().sumOf { it.rate.toDouble() } }
        // Mg > Zn > Fe > Cu (none)
        assertTrue(rates[0] > rates[1] && rates[1] > rates[2] && rates[2] > 0.0 && rates[3] == 0.0)
    }
}
