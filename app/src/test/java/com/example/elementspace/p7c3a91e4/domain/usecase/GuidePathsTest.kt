package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementReactions
import com.example.elementspace.p7c3a91e4.data.repository.GuidePaths
import com.example.elementspace.p7c3a91e4.domain.model.AtomAction
import com.example.elementspace.p7c3a91e4.domain.model.GuideAction
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidePathsTest {
    private val elements = BundledElementRepository()
    private val reactions = BundledReactionRepository()
    private val molecules = BundledMoleculeRepository()

    @Test
    fun `every step points at something that exists and has both languages`() {
        assertTrue(GuidePaths.ALL.size >= 5)
        GuidePaths.ALL.forEach { path ->
            assertTrue(path.id, path.steps.size in 3..5)
            assertTrue(path.done.zh.isNotBlank() && path.done.en.isNotBlank())
            path.steps.forEach { step ->
                assertTrue(step.text.zh.isNotBlank() && step.text.en.isNotBlank() && step.button.zh.isNotBlank() && step.button.en.isNotBlank())
                when (val a = step.action) {
                    is GuideAction.OpenAtom -> assertNotNull(path.id, elements.element(a.atomicNumber))
                    is GuideAction.Atom -> {
                        assertNotNull(elements.element(a.atomicNumber))
                        (a.action as? AtomAction.Allotrope)?.let { assertTrue(path.id, it.index in elements.allotropes(a.atomicNumber).indices) }
                    }
                    is GuideAction.Reaction -> assertNotNull("${path.id} ${a.id}", reactions.reaction(a.id))
                    is GuideAction.Molecule -> assertNotNull("${path.id} ${a.id}", molecules.molecule(a.id))
                    is GuideAction.Lab -> assertNotNull("${path.id} ${a.id}", com.example.elementspace.p7c3a91e4.data.repository.Experiments.experiment(a.id))
                    is GuideAction.Collide -> {
                        assertNotNull(elements.element(a.a))
                        assertNotNull(elements.element(a.b))
                    }
                }
            }
        }
        // The sodium path relies on na_3 being sodium + water.
        assertTrue(reactions.reaction("na_3")!!.reactants.any { it.formula == "H2O" })
    }

    @Test
    fun `every element has a one-line summary in both languages`() {
        elements.elements().forEach { e ->
            val line = ElementSummary.of(e, ElementReactions.NOTES[e.symbol])
            assertTrue(e.symbol, line.zh.length in 8..90 && line.en.length in 15..200)
        }
    }
}
