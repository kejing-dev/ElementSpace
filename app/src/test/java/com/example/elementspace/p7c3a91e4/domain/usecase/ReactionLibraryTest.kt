package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Content checks for every bundled reaction: chemistry first, then the 3D frames. */
class ReactionLibraryTest {
    private val library = BundledReactionRepository().all()
    private val z = BundledElementRepository().elements().associate { it.symbol to it.atomicNumber }

    private fun electrons(frame: ReactionFrame): Int =
        frame.particles.sumOf { it.shells.sum() } + frame.pairs.sumOf { it.count } + frame.flights.sumOf { it.count }

    @Test
    fun `formulas parse with groups and print with subscripts`() {
        assertEquals(mapOf("Cu" to 1, "S" to 1, "O" to 4), ChemicalFormula.count("CuSO4"))
        assertEquals(mapOf("Ca" to 1, "O" to 2, "H" to 2), ChemicalFormula.count("Ca(OH)2"))
        assertEquals("CuSO₄", ChemicalFormula.display("CuSO4"))
        assertEquals("2Na + Cl₂", ChemicalFormula.sideText(listOf(Term(2, "Na"), Term(1, "Cl2"))))
    }

    @Test
    fun `an unbalanced equation is caught`() {
        val wrong = library.first().copy(products = listOf(Term(1, "NaCl")))
        assertFalse(ChemicalFormula.balanced(wrong))
    }

    @Test
    fun `every reaction is balanced atom for atom`() {
        library.forEach { assertTrue(it.id, ChemicalFormula.balanced(it)) }
    }

    @Test
    fun `electrons lost equal electrons gained`() {
        library.forEach { assertTrue(it.id, ChemicalFormula.electronsConserved(it)) }
    }

    @Test
    fun `redox atom counts match the balanced equation`() {
        library.forEach { reaction ->
            val reactantAtoms = ChemicalFormula.side(reaction.reactants)
            reaction.redox.forEach { change -> assertEquals("${reaction.id} ${change.symbol}", reactantAtoms[change.symbol], change.atoms) }
        }
    }

    @Test
    fun `every reaction has steps, both languages and a textbook source`() {
        library.forEach { reaction ->
            assertTrue(reaction.id, reaction.steps.size in 3..6)
            assertTrue(reaction.id, reaction.source.zh.contains("人教版") && reaction.source.en.contains("PEP"))
            reaction.steps.forEach { step ->
                assertTrue(step.title.zh.isNotBlank() && step.title.en.isNotBlank())
                assertTrue(step.why.zh.isNotBlank() && step.why.en.isNotBlank())
            }
            assertEquals(reaction.id, ChemicalFormula.electronsMoved(reaction), reaction.steps.last().moved)
        }
    }

    @Test
    fun `no electron appears or disappears between steps`() {
        library.forEach { reaction ->
            val counts = reaction.steps.map { electrons(it.frame) }
            assertTrue("${reaction.id} $counts", counts.distinct().size == 1)
        }
    }

    @Test
    fun `neutral atoms in the first frame carry exactly Z electrons`() {
        library.forEach { reaction ->
            val first = reaction.steps.first().frame
            first.particles
                .filter { it.cluster.isEmpty() && it.tint == ChargeTint.NONE && first.pairs.none { p -> p.a == it.id || p.b == it.id } }
                .forEach { assertEquals("${reaction.id} ${it.id}", z[it.symbol], it.shells.sum()) }
        }
    }

    @Test
    fun `ions in the last frame have the textbook electron counts`() {
        val last = library.associate { it.id to it.steps.last().frame.particles.associateBy { p -> p.id } }
        assertEquals(10, last["na_cl2"]!!["Na1"]!!.shells.sum()) // Na+ 2,8
        assertEquals(18, last["na_cl2"]!!["Cl1"]!!.shells.sum()) // Cl- 2,8,8
        assertEquals(24, last["fe_cuso4"]!!["Fe"]!!.shells.sum()) // Fe2+
        assertEquals(29, last["fe_cuso4"]!!["Cu"]!!.shells.sum()) // Cu
    }

    @Test
    fun `particles keep their ids across steps and flights and pairs point at real particles`() {
        library.forEach { reaction ->
            val ids = reaction.steps.first().frame.particles.map { it.id }.toSet()
            reaction.steps.forEach { step ->
                val frame = step.frame
                assertEquals(reaction.id, ids, frame.particles.map { it.id }.toSet())
                frame.pairs.forEach { assertTrue(it.a in ids && it.b in ids) }
                frame.flights.forEach { assertTrue(ReactionLayout.flightEnds(frame, it) != null) }
            }
        }
    }

    @Test
    fun `atoms in a frame never overlap except bonded neighbours`() {
        library.forEach { reaction ->
            reaction.steps.forEachIndexed { index, step ->
                val f = step.frame
                for (i in f.particles.indices) for (j in i + 1 until f.particles.size) {
                    val a = f.particles[i]
                    val b = f.particles[j]
                    val bonded = f.pairs.any { (it.a == a.id && it.b == b.id) || (it.a == b.id && it.b == a.id) }
                    if (bonded) continue
                    val gap = a.position.distanceTo(b.position) - ReactionLayout.particleRadius(a) - ReactionLayout.particleRadius(b)
                    assertTrue("${reaction.id} step ${index + 1} ${a.id}/${b.id} gap $gap", gap > 0.02f)
                }
            }
        }
    }

    @Test
    fun `everything stays inside the Volume`() {
        library.forEach { reaction ->
            reaction.steps.forEach { step ->
                step.frame.particles.forEach { p ->
                    val reach = kotlin.math.abs(p.position.x) + ReactionLayout.particleRadius(p)
                    assertTrue("${reaction.id} ${p.id} $reach", reach < 0.95f && kotlin.math.abs(p.position.y) + ReactionLayout.particleRadius(p) < 0.95f)
                }
            }
        }
    }
}
