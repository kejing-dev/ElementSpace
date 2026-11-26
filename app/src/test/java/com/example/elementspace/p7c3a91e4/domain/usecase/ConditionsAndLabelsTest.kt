package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ConditionKind
import com.example.elementspace.p7c3a91e4.ui.common.SceneLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** v1.3.0: conditions drawn in the reaction Volume and the symbols printed on atoms. */
class ConditionsAndLabelsTest {
    private val library = BundledReactionRepository().all()
    private val z = BundledElementRepository().elements().associate { it.symbol to it.atomicNumber }

    @Test
    fun `every written condition is drawn, matching the textbook words`() {
        val expected =
            mapOf(
                "na_cl2" to listOf(ConditionKind.IGNITE),
                "h2_o2" to listOf(ConditionKind.IGNITE),
                "ch4_cl2" to listOf(ConditionKind.LIGHT),
                "esterification" to listOf(ConditionKind.HEAT, ConditionKind.CATALYST),
                "fe_cuso4" to emptyList(),
                "c2h4_br2" to emptyList(),
            )
        library.forEach { r ->
            if (!r.schematic) assertEquals(r.id, expected[r.id], r.visuals.map { it.kind })
            assertEquals("${r.id}: condition text and visuals go together", r.condition != null, r.visuals.isNotEmpty())
        }
    }

    @Test
    fun `conditions are off for the reactants and the products and on while bonds change`() {
        library.filter { it.visuals.isNotEmpty() }.forEach { r ->
            assertTrue(r.id, r.conditionsAt(0).isEmpty())
            assertTrue(r.id, r.conditionsAt(r.steps.lastIndex).isEmpty())
            assertTrue(r.id, (1 until r.steps.lastIndex).all { r.conditionsAt(it) == r.visuals })
        }
    }

    @Test
    fun `ions carry their charge, everything else the bare symbol`() {
        val seen = HashSet<String>()
        library.forEach { r -> r.steps.forEach { s -> s.frame.particles.forEach { seen += AtomLabelText.particle(it, z[it.symbol]) } } }
        assertTrue(seen.toString(), seen.containsAll(listOf("Na", "Cl", "Na⁺", "Cl⁻", "Fe", "Fe²⁺", "Cu²⁺", "Cu", "SO₄²⁻", "H", "O")))
        // Partial charges (polar bonds) never print a charge.
        library.forEach { r ->
            r.steps.forEach { s ->
                s.frame.particles.filter { it.tint == ChargeTint.PARTIAL_POSITIVE || it.tint == ChargeTint.PARTIAL_NEGATIVE }.forEach {
                    assertEquals(it.symbol, AtomLabelText.particle(it, z[it.symbol]))
                }
            }
        }
    }

    @Test
    fun `labels are pushed towards the viewer whatever the rotation`() {
        // A point pushed by towardsViewer, then rotated by the parent's Rx(pitch) * Ry(yaw), lands on +z.
        listOf(0f to 0f, 0.6f to 0.35f, -2.1f to -0.9f, 3f to 1.2f).forEach { (yaw, pitch) ->
            val v = SceneLabels.towardsViewer(yaw, pitch, 1f)
            // Ry(yaw)
            val x1 = v.x * kotlin.math.cos(yaw) + v.z * kotlin.math.sin(yaw)
            val z1 = -v.x * kotlin.math.sin(yaw) + v.z * kotlin.math.cos(yaw)
            // Rx(pitch)
            val y2 = v.y * kotlin.math.cos(pitch) - z1 * kotlin.math.sin(pitch)
            val z2 = v.y * kotlin.math.sin(pitch) + z1 * kotlin.math.cos(pitch)
            assertTrue("x $yaw $pitch", abs(x1) < 1e-4f)
            assertTrue("y $yaw $pitch", abs(y2) < 1e-4f)
            assertTrue("z $yaw $pitch", abs(z2 - 1f) < 1e-4f)
        }
    }
}
