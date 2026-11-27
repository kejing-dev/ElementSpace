package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.16.0: every "element + element" reaction (what 碰一碰 can find) has an electron view, and each one adds up. */
class ElectronViewTest {
    private val elements = BundledElementRepository().elements().associateBy { it.symbol }
    private val repository = BundledReactionRepository.shared
    private val view = ElectronView(elements)

    private fun binary(): List<Reaction> =
        repository.all().filter { r -> r.reactants.size == 2 && r.reactants.all { ChemicalFormula.count(it.formula).size == 1 } && r.reactants.map { ChemicalFormula.count(it.formula).keys.single() }.distinct().size == 2 }

    @Test
    fun everyElementPairHasAnElectronView() {
        val pairs = binary()
        assertTrue(pairs.size >= 130)
        val missing = pairs.filter { repository.electronView(it.id) == null }.map { it.id }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun electronsAreConservedInEveryFrame() {
        binary().filter { it.kind != ReactionKind.ELECTRONS }.forEach { base ->
            val r = view.build(base)!!
            assertEquals(base.id, 4, r.steps.size)
            r.steps.forEachIndexed { i, step ->
                val f = step.frame
                val protons = f.particles.sumOf { elements.getValue(it.symbol).atomicNumber }
                val electrons = f.particles.sumOf { it.shells.sum() + it.hidden } + f.pairs.sumOf { it.count } + f.flights.sumOf { it.count }
                assertEquals("${base.id} step ${i + 1}", protons, electrons)
                assertTrue("${base.id} step ${i + 1} title", step.title.zh.isNotBlank() && step.title.en.isNotBlank())
                assertTrue("${base.id} step ${i + 1} why", step.why.zh.isNotBlank() && step.why.en.isNotBlank())
            }
            assertEquals(base.id, 0, r.steps[0].moved)
            assertTrue(base.id, r.steps[3].moved > 0)
        }
    }

    @Test
    fun atomsStayInsideThePictureAndDoNotOverlap() {
        binary().filter { it.kind != ReactionKind.ELECTRONS }.forEach { base ->
            view.build(base)!!.steps.forEachIndexed { i, step ->
                val f = step.frame
                val bonded = f.pairs.map { setOf(it.a, it.b) }.toSet()
                f.particles.forEach { p ->
                    val radius = ReactionLayout.particleRadius(p) * f.scale
                    assertTrue("${base.id} step ${i + 1} ${p.id} x", abs(p.position.x) + radius <= ElectronView.X_MAX + 0.002f)
                    assertTrue("${base.id} step ${i + 1} ${p.id} y", abs(p.position.y) + radius <= ElectronView.Y_MAX + 0.002f)
                }
                for (a in f.particles.indices) for (b in a + 1 until f.particles.size) {
                    val p = f.particles[a]
                    val q = f.particles[b]
                    val need = (ReactionLayout.particleRadius(p) + ReactionLayout.particleRadius(q)) * f.scale * if (setOf(p.id, q.id) in bonded) 0.8f else 0.98f
                    assertTrue("${base.id} step ${i + 1} ${p.id}/${q.id}", p.position.distanceTo(q.position) >= need - 0.002f)
                }
                assertTrue("${base.id} scale ${f.scale}", f.scale >= 0.3f)
            }
        }
    }

    @Test
    fun ionsCarryTheChargeTheirOxidationStateSays() {
        fun last(id: String) = view.build(repository.reaction(id)!!)!!.steps[3].frame
        val na2s = last(binary().first { it.products[0].formula == "Na2S" }.id)
        assertEquals(listOf("Na⁺", "Na⁺", "S²⁻"), na2s.particles.map { AtomLabelText.particle(it, elements.getValue(it.symbol).atomicNumber) })
        assertTrue(na2s.particles.all { it.tint == ChargeTint.POSITIVE || it.tint == ChargeTint.NEGATIVE })
        val fe3o4 = last(binary().first { it.products[0].formula == "Fe3O4" }.id)
        assertEquals(listOf("Fe³⁺", "Fe³⁺", "Fe²⁺"), fe3o4.particles.filter { it.symbol == "Fe" }.map { AtomLabelText.particle(it, 26) })
        val peroxide = last(binary().first { it.products[0].formula == "Na2O2" }.id)
        assertEquals(1, peroxide.pairs.size)
        assertEquals(listOf("O⁻", "O⁻"), peroxide.particles.filter { it.symbol == "O" }.map { AtomLabelText.particle(it, 8) })
    }

    @Test
    fun moleculesShareAndSaltsTransfer() {
        fun model(formula: String) = view.model(binary().first { it.products[0].formula == formula })
        listOf("HCl", "NH3", "CO2", "SO2", "P2O5", "AlCl3", "FeCl3", "XeF2", "SiO2").forEach { assertEquals(it, ElectronView.Model.SHARE, model(it)) }
        listOf("Na2S", "MgO", "Al2O3", "LiH", "Mg3N2", "CuO", "KO2").forEach { assertEquals(it, ElectronView.Model.TRANSFER, model(it)) }
        val co = view.build(binary().first { it.products[0].formula == "CO" })!!.steps[3].frame
        assertEquals(listOf(6, 6), co.pairs.map { it.count })
        val nh3 = view.build(binary().first { it.products[0].formula == "NH3" })!!.steps[3].frame
        assertEquals(6, nh3.pairs.size)
    }

    @Test
    fun theOtherViewIsReachableById() {
        val base = binary().first { it.products[0].formula == "Na2S" }
        val electrons = repository.reaction(base.id + ElectronView.SUFFIX)
        assertNotNull(electrons)
        assertEquals(ReactionKind.ELECTRONS, electrons!!.kind)
        assertTrue(electrons.sketch)
        assertEquals("na_cl2", repository.electronView("na_cl2")!!.id)
    }

    /** Not an assertion: writes every generated view to build/electron-views.txt for reading. */
    @Test
    fun writeReport() {
        val out = StringBuilder()
        binary().filter { it.kind != ReactionKind.ELECTRONS }.forEach { base ->
            val r = view.build(base) ?: return@forEach
            out.append("== ${base.id}  ${base.reactants.joinToString(" + ") { "${it.coefficient}${it.formula}" }} -> ${base.products[0].coefficient}${base.products[0].formula}  ${view.model(base)}  scale ${r.steps[0].frame.scale}  moved ${r.steps[3].moved}  redox ${r.redox}\n")
            r.steps.forEachIndexed { i, s ->
                out.append("  ${i + 1}. ${s.title.zh} | ${s.title.en}\n     ${s.why.zh}\n     ${s.why.en}\n")
                out.append("     " + s.frame.particles.joinToString(" ") { "${AtomLabelText.particle(it, elements.getValue(it.symbol).atomicNumber)}${it.shells}" + (if (it.vacancies > 0) "v${it.vacancies}" else "") } + "  pairs=" + s.frame.pairs.joinToString(",") { "${it.a}-${it.b}:${it.count}" } + "  flights=" + s.frame.flights.joinToString(",") { "${it.from}>${it.to}x${it.count}" } + "\n")
            }
        }
        File("build").mkdirs()
        File("build/electron-views.txt").writeText(out.toString())
    }
}
