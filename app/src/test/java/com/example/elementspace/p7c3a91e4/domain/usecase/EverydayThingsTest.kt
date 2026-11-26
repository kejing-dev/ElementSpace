package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.EverydayThings
import com.example.elementspace.p7c3a91e4.domain.model.ThingInside
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EverydayThingsTest {
    private val elements = BundledElementRepository()
    private val models = File("src/main/assets/models/things")

    @Test
    fun `every thing has a model, a size entry, both languages and real elements`() {
        val index = File(models, "index.json").readText()
        assertTrue(EverydayThings.ALL.size >= 14)
        assertEquals(EverydayThings.ALL.size, EverydayThings.ALL.map { it.id }.toSet().size)
        EverydayThings.ALL.forEach { thing ->
            assertTrue("${thing.id}.glb", File(models, "${thing.id}.glb").length() > 1000)
            assertTrue("${thing.id} in index.json", index.contains("\"${thing.id}\""))
            assertTrue(thing.name.zh.isNotBlank() && thing.name.en.isNotBlank() && thing.line.zh.isNotBlank() && thing.line.en.isNotBlank())
            thing.symbols.forEach { symbol -> assertTrue("${thing.id} $symbol", elements.elements().any { it.symbol == symbol }) }
            assertEquals(thing.id, thing.inside == null, thing.insideLabel == null)
        }
    }

    @Test
    fun `what is inside a thing exists in the app`() {
        val reactions = BundledReactionRepository()
        val molecules = BundledMoleculeRepository()
        EverydayThings.ALL.forEach { thing ->
            when (val inside = thing.inside) {
                is ThingInside.Molecule -> assertNotNull(thing.id, molecules.molecule(inside.id))
                is ThingInside.Reaction -> assertNotNull(thing.id, reactions.reaction(inside.id))
                is ThingInside.Allotrope -> {
                    assertTrue(thing.id, inside.index in elements.allotropes(inside.atomicNumber).indices)
                    // The lattice opens in this element's own Volume, so the thing must be on its card.
                    assertTrue(thing.id, elements.element(inside.atomicNumber)!!.symbol in thing.symbols)
                }
                null -> Unit
            }
        }
    }
}
