package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.OrganicReactions
import com.example.elementspace.p7c3a91e4.domain.model.BondMark
import com.example.elementspace.p7c3a91e4.domain.model.FrameBond
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
import com.example.elementspace.p7c3a91e4.domain.model.Skeleton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Chemistry and geometry checks for the molecule library and the ball-and-stick reaction frames. */
class MoleculeLibraryTest {
    private val library = BundledMoleculeRepository().all()
    private val reactions = BundledReactionRepository().all()
    private val valence = mapOf("C" to 4f, "H" to 1f, "O" to 2f, "Cl" to 1f, "Br" to 1f)

    /** Textbook bond lengths (Å) by sorted element pair and order. */
    private val lengths =
        mapOf(
            "C-C/1" to 1.54f, "C-C/2" to 1.34f, "C-C/3" to 1.20f, "C-C/4" to 1.39f,
            "C-H/1" to 1.08f, "C-O/1" to 1.40f, "C-O/2" to 1.18f, "H-O/1" to 0.96f,
        )

    private fun valenceOf(m: Molecule, i: Int) = m.bonds.filter { it.a == i || it.b == i }.sumOf { it.valence.toDouble() }.toFloat()

    @Test
    fun `fourteen molecules with unique ids, both languages and a textbook source`() {
        assertEquals(14, library.size)
        assertEquals(library.size, library.map { it.id }.toSet().size)
        library.forEach { m ->
            listOf(m.name, m.geometry, m.property).forEach { assertTrue(m.id, it.zh.isNotBlank() && it.en.isNotBlank()) }
            assertTrue(m.id, m.source.zh.contains("人教版") && m.source.en.contains("PEP"))
        }
    }

    @Test
    fun `the molecular formula matches the atoms in the model`() {
        library.forEach { m ->
            assertEquals(m.id, ChemicalFormula.count(m.formula), m.atoms.groupingBy { it.symbol }.eachCount())
            assertEquals(m.id, ChemicalFormula.count(m.formula), ChemicalFormula.count(m.condensed.filter { it != '=' && it != '≡' }).takeIf { m.id != "cyclohexane" } ?: ChemicalFormula.count(m.formula))
        }
    }

    @Test
    fun `every atom has its textbook valence - C 4, H 1, O 2`() {
        library.forEach { m ->
            m.atoms.forEachIndexed { i, a -> assertEquals("${m.id} atom $i ${a.symbol}", valence.getValue(a.symbol), valenceOf(m, i), 0.01f) }
        }
    }

    @Test
    fun `bond lengths are within 0_1 angstrom of the textbook values`() {
        library.forEach { m ->
            m.bonds.forEach { b ->
                val key = listOf(m.atoms[b.a].symbol, m.atoms[b.b].symbol).sorted().joinToString("-") + "/${b.order}"
                val expected = lengths[key] ?: error("${m.id}: no reference length for $key")
                val actual = m.atoms[b.a].position.distanceTo(m.atoms[b.b].position)
                assertEquals("${m.id} $key", expected, actual, 0.12f)
            }
        }
    }

    @Test
    fun `labelled bond angles match the textbook`() {
        val expected = mapOf("methane" to 109.47f, "butane" to 109.47f, "benzene" to 120f, "ethylene" to 120f, "acetylene" to 180f, "co2" to 180f, "water" to 104.5f)
        expected.forEach { (id, degrees) ->
            val m = library.first { it.id == id }
            assertEquals(id, degrees, m.angles.single().degrees, 1.5f)
        }
    }

    @Test
    fun `atoms that are not bonded never overlap`() {
        library.forEach { m ->
            val bonded = m.bonds.map { setOf(it.a, it.b) }.toSet()
            for (i in m.atoms.indices) for (j in i + 1 until m.atoms.size) {
                if (setOf(i, j) in bonded) continue
                val d = m.atoms[i].position.distanceTo(m.atoms[j].position)
                assertTrue("${m.id} $i-$j ${"%.2f".format(d)}", d > 1.5f)
            }
        }
    }

    @Test
    fun `flat and linear molecules really are flat and linear`() {
        listOf("benzene", "ethylene").forEach { id -> library.first { it.id == id }.atoms.forEach { assertEquals(id, 0f, it.position.z, 0.01f) } }
        listOf("acetylene", "co2").forEach { id -> library.first { it.id == id }.atoms.forEach { assertEquals(id, 0f, abs(it.position.y) + abs(it.position.z), 0.01f) } }
        // Cyclohexane is not flat.
        assertTrue(library.first { it.id == "cyclohexane" }.atoms.any { abs(it.position.z) > 0.2f })
    }

    @Test
    fun `skeleton groups, comparisons and isomers`() {
        val byId = library.associateBy { it.id }
        assertEquals(Skeleton.BRANCHED, byId.getValue("isobutane").skeleton)
        assertEquals(setOf("cyclohexane", "benzene"), library.filter { it.skeleton == Skeleton.RING }.map { it.id }.toSet())
        library.forEach { m -> m.compareWith?.let { assertNotNull("${m.id} -> $it", byId[it]); assertNotNull(m.id, m.compareNote) } }
        assertEquals(byId.getValue("butane").formula, byId.getValue("isobutane").formula)
        assertEquals(byId.getValue("ethanol").formula, byId.getValue("ether").formula)
    }

    @Test
    fun `linked reactions exist in the reaction library`() {
        val ids = reactions.map { it.id }.toSet()
        library.forEach { m -> m.reactionIds.forEach { assertTrue("${m.id} -> $it", it in ids) } }
    }

    // ---- organic reaction frames ----------------------------------------------------------------

    private val organic = reactions.filter { it.kind == ReactionKind.BONDS }

    private fun counts(frame: ReactionFrame) = frame.atoms.groupingBy { it.symbol }.eachCount()

    private fun valences(frame: ReactionFrame, keep: (FrameBond) -> Boolean): Map<String, Int> {
        val v = HashMap<String, Int>()
        frame.bonds.filter(keep).forEach { b ->
            v[b.a] = (v[b.a] ?: 0) + b.order
            v[b.b] = (v[b.b] ?: 0) + b.order
        }
        return v
    }

    @Test
    fun `three organic reactions, balanced, four steps each`() {
        assertEquals(setOf("ch4_cl2", "c2h4_br2", "esterification"), organic.map { it.id }.toSet())
        organic.forEach { r ->
            assertTrue(r.id, ChemicalFormula.balanced(r))
            assertEquals(r.id, 4, r.steps.size)
        }
    }

    @Test
    fun `every step has the same atoms as both sides of the equation`() {
        organic.forEach { r ->
            val ids = r.steps.first().frame.atoms.map { it.id }
            r.steps.forEach { step ->
                assertEquals(r.id, ids, step.frame.atoms.map { it.id })
                assertEquals(r.id, ChemicalFormula.side(r.reactants), counts(step.frame))
                step.frame.bonds.forEach { b -> assertTrue("${r.id} ${b.a}-${b.b}", b.a in ids && b.b in ids) }
            }
        }
    }

    @Test
    fun `reactants and products have textbook valences, new bonds only in the products`() {
        organic.forEach { r ->
            val first = r.steps.first().frame
            val last = r.steps.last().frame
            listOf(first, last).forEach { frame ->
                val v = valences(frame) { true }
                frame.atoms.forEach { a -> assertEquals("${r.id} ${a.id}", valence.getValue(a.symbol).toInt(), v[a.id] ?: 0) }
            }
            assertTrue(r.id, first.bonds.none { it.mark == BondMark.NEW })
            assertTrue(r.id, r.steps[1].frame.bonds.any { it.mark == BondMark.BREAK || it.mark == BondMark.BREAK_ONE })
            assertTrue(r.id, last.bonds.any { it.mark == BondMark.NEW } && last.bonds.none { it.mark == BondMark.BREAK || it.mark == BondMark.BREAK_ONE })
        }
    }

    @Test
    fun `frame atoms never overlap and stay inside the Volume`() {
        organic.forEach { r ->
            r.steps.forEachIndexed { s, step ->
                val atoms = step.frame.atoms
                val bonded = step.frame.bonds.map { setOf(it.a, it.b) }.toSet()
                for (i in atoms.indices) for (j in i + 1 until atoms.size) {
                    if (setOf(atoms[i].id, atoms[j].id) in bonded) continue
                    val d = atoms[i].position.distanceTo(atoms[j].position) / OrganicReactions.SCALE
                    assertTrue("${r.id} step ${s + 1} ${atoms[i].id}-${atoms[j].id} ${"%.2f".format(d)} Å", d > 1.1f)
                }
                atoms.forEach { a -> assertTrue("${r.id} ${a.id} ${a.position}", a.position.length() < 0.85f) }
            }
        }
    }
}
