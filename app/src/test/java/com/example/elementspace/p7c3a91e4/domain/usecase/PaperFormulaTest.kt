package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.18.0: the electron-dot and structural formulas written on the card agree with the 3D frame. */
class PaperFormulaTest {
    private val elements = BundledElementRepository().elements().associateBy { it.symbol }
    private val numbers = elements.mapValues { it.value.atomicNumber }
    private val repository = BundledReactionRepository.shared

    private fun views(): List<Reaction> = repository.all().mapNotNull { repository.electronView(it.id) }.filter { it.sketch }

    private fun text(d: PaperFormula.Diagram): String {
        val atoms = d.atoms.withIndex().joinToString(" ") { (i, a) -> (if (d.times[i] > 1) "${d.times[i]}×" else "") + AtomLabelText.ion(a.symbol, a.charge) + "(" + a.lone.joinToString("") { if (it.dots == 2) ":" else "." } + ")" }
        val bonds = d.bonds.joinToString(" ") { "${d.atoms[it.a].symbol}${it.a}${"-=≡".getOrElse(it.pairs - 1) { '?' }}${d.atoms[it.b].symbol}${it.b}" }
        val groups = d.groups.joinToString(" ") { g -> "[" + g.atoms.joinToString("") { d.atoms[it].symbol } + "]" + AtomLabelText.charge(g.charge) }
        return "$atoms | $bonds | $groups"
    }

    @Test
    fun everyGeneratedFrameCanBeWrittenAndIsNeutral() {
        val out = StringBuilder()
        val all = views()
        assertTrue(all.size >= 250)
        all.forEach { r ->
            out.append("== ${r.id}\n")
            r.steps.forEachIndexed { i, step ->
                val d = PaperFormula.of(step.frame, numbers)
                // Reactants (atoms, X₂) can always be written; some products are left out on purpose.
                if (i < 2) assertNotNull("${r.id} step ${i + 1}", d)
                if (d == null) { out.append("  ${i + 1}. -\n"); return@forEachIndexed }
                // Electrons in flight belong to no atom yet, so the atoms are short of exactly those.
                val charge = d.atoms.indices.sumOf { d.atoms[it].charge * d.times[it] } + d.groups.sumOf { it.charge * d.times[it.atoms.first()] }
                assertEquals("${r.id} step ${i + 1} charge", step.frame.flights.sumOf { it.count }, charge)
                d.atoms.forEach { a -> assertTrue("${r.id} step ${i + 1} ${a.symbol} dots", a.lone.sumOf { it.dots } <= 8) }
                // A cation is written without dots; an anion has a full outer shell.
                d.atoms.filter { it.charge > 0 }.forEach { a -> assertTrue("${r.id} ${a.symbol}", a.lone.isEmpty()) }
                out.append("  ${i + 1}. ${text(d)}\n")
            }
        }
        File("build/paper-formulas.txt").writeText(out.toString())
    }

    @Test
    fun textbookExamples() {
        fun last(id: String) = PaperFormula.of(repository.electronView(id)!!.steps.last().frame, numbers)!!
        // H:Cl with three lone pairs on Cl.
        val hcl = last("h_1")
        assertEquals(listOf(1), hcl.bonds.map { it.pairs }.distinct())
        assertEquals(6, hcl.atoms.first { it.symbol == "Cl" }.lone.sumOf { it.dots })
        assertEquals(0, hcl.atoms.first { it.symbol == "H" }.lone.size)
        // S::C::S, two lone pairs on each S, none on C.
        val cs2 = last("s_7")
        assertEquals(listOf(2, 2), cs2.bonds.map { it.pairs })
        assertEquals(3, cs2.atoms.size)
        assertEquals(listOf(4, 4), cs2.atoms.filter { it.symbol == "S" }.map { a -> a.lone.sumOf { it.dots } })
        assertEquals(0, cs2.atoms.first { it.symbol == "C" }.lone.size)
        // Na⁺ [O:O]²⁻ Na⁺.
        val peroxide = last("na_2")
        assertEquals(listOf(-2), peroxide.groups.map { it.charge })
        assertEquals(listOf(1), peroxide.atoms.filter { it.symbol == "Na" }.map { it.charge })
        assertEquals(listOf(2), peroxide.multipliers.map { it.count })
        // 2HCl is written once, with a 2 in front.
        assertEquals(2, hcl.atoms.size)
        assertEquals(listOf(2), hcl.multipliers.map { it.count })
    }

    @Test
    fun handMadeElectronReactionsDoNotBreak() {
        val out = StringBuilder()
        repository.all().filter { it.kind == ReactionKind.ELECTRONS }.forEach { r ->
            out.append("== ${r.id}\n")
            r.steps.forEachIndexed { i, step -> out.append("  ${i + 1}. ${PaperFormula.of(step.frame, numbers)?.let(::text) ?: "-"}\n") }
        }
        File("build/paper-formulas-handmade.txt").writeText(out.toString())
    }
}
