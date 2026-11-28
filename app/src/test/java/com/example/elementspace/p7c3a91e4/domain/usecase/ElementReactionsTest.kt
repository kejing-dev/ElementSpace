package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementReactions
import com.example.elementspace.p7c3a91e4.ui.molecule.components.BallStick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ElementReactionsTest {
    private val repo = BundledReactionRepository()

    @Test
    fun `every element has a reaction or says why it has none`() {
        BundledElementRepository().elements().forEach { e ->
            val has = repo.forSymbol(e.symbol).isNotEmpty()
            val note = ElementReactions.NOTES[e.symbol]
            assertTrue("${e.symbol}: reactions or a note", has || note != null)
            assertTrue("${e.symbol}: not both", !(has && note != null))
        }
    }

    @Test
    fun `ids are unique across the library`() {
        val ids = repo.all().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `auto frames keep every atom and match the equation`() {
        ElementReactions.ALL.forEach { r ->
            val counts = ChemicalFormula.side(r.reactants)
            r.steps.forEach { step ->
                val drawn = step.frame.atoms.groupingBy { it.symbol }.eachCount()
                assertEquals(r.id, counts, drawn)
            }
            val ids = r.steps.map { s -> s.frame.atoms.map { it.id }.toSet() }.distinct()
            assertEquals(r.id, 1, ids.size)
        }
    }

    @Test
    fun `auto pictures fit the Volume and atoms do not overlap`() {
        ElementReactions.ALL.forEach { r ->
            r.steps.forEachIndexed { k, step ->
                val f = step.frame
                f.atoms.forEach { a ->
                    val reach = BallStick.ball(a.symbol) * f.atomScale * f.ballScale
                    assertTrue("${r.id} ${a.id}", abs(a.position.x) + reach < 0.75f && abs(a.position.y) + reach < 0.95f)
                }
                for (i in f.atoms.indices) for (j in i + 1 until f.atoms.size) {
                    val a = f.atoms[i]
                    val b = f.atoms[j]
                    val gap = a.position.distanceTo(b.position) - (BallStick.ball(a.symbol) + BallStick.ball(b.symbol)) * f.atomScale * f.ballScale
                    assertTrue("${r.id} step ${k + 1} ${a.symbol}${a.id}/${b.symbol}${b.id} gap $gap", gap > -1e-4f)
                }
            }
        }
    }
}
