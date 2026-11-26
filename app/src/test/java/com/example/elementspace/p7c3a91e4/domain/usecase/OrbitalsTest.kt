package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class OrbitalsTest {
    private val config = ElectronConfigurationUseCase()

    @Test
    fun `every orbital has n - l - 1 radial nodes`() {
        for (n in 1..7) for (l in 0 until minOf(n, 4)) {
            assertEquals("$n,$l", n - l - 1, Orbitals.radialNodes(n, l).size)
        }
    }

    @Test
    fun `most probable radii match the hydrogen atom (n squared for l = n - 1)`() {
        assertEquals(1f, Orbitals.mostProbableRadius(1, 0), 0.03f)
        assertEquals(4f, Orbitals.mostProbableRadius(2, 1), 0.05f)
        assertEquals(9f, Orbitals.mostProbableRadius(3, 2), 0.08f)
        // 2s: outer peak at (3 + √5) a₀ ≈ 5.24.
        assertEquals(5.236f, Orbitals.mostProbableRadius(2, 0), 0.06f)
    }

    @Test
    fun `2s node sits at 2 Bohr radii and 3s nodes at 1·90 and 7·10`() {
        assertEquals(2f, Orbitals.radialNodes(2, 0).single(), 0.02f)
        val n3 = Orbitals.radialNodes(3, 0)
        assertEquals(1.902f, n3[0], 0.03f)
        assertEquals(7.098f, n3[1], 0.03f)
    }

    @Test
    fun `l orbitals per subshell with l angular nodes each, and psi is zero on every one`() {
        for ((l, shapes) in Orbitals.shapes) {
            assertEquals(2 * l + 1, shapes.size)
            shapes.forEachIndexed { i, shape ->
                val planes = shape.angularNodes.filterIsInstance<NodalSurface.Plane>().size
                val cones = shape.angularNodes.filterIsInstance<NodalSurface.Cone>().size
                assertEquals("l=$l #$i", l, planes + 2 * cones)
                shape.angularNodes.forEach { node -> pointsOn(node).forEach { p -> assertEquals("l=$l #$i $node", 0f, shape.angular(p.x, p.y, p.z), 2e-4f) } }
            }
        }
    }

    private fun pointsOn(node: NodalSurface): List<Vec3> =
        when (node) {
            is NodalSurface.Plane -> {
                val n = node.normal
                val helper = if (abs(n.z) < 0.9f) Vec3(0f, 0f, 1f) else Vec3(1f, 0f, 0f)
                val u = cross(n, helper).let { it * (1f / it.length()) }
                val w = cross(n, u)
                (0 until 12).map { k -> val t = k * 0.52f; u * cos(t) + w * sin(t) }
            }
            is NodalSurface.Cone -> {
                val t = Math.toRadians(node.halfAngleDegrees.toDouble()).toFloat()
                (0 until 12).map { k -> val a = k * 0.52f; Vec3(sin(t) * cos(a), sin(t) * sin(a), cos(t)) }
            }
            is NodalSurface.Sphere -> emptyList()
        }

    private fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    @Test
    fun `hund fills singly first`() {
        assertArrayEquals(intArrayOf(1, 1, 0), Orbitals.hund(1, 2))
        assertArrayEquals(intArrayOf(2, 1, 1, 1, 1), Orbitals.hund(2, 6))
        assertArrayEquals(intArrayOf(1, 1, 1, 1, 1), Orbitals.hund(2, 5))
    }

    @Test
    fun `slater effective charges match the worked textbook values`() {
        fun z(atomic: Int, n: Int, l: Int) = Slater.zEff(atomic, config(atomic).subshells, n, l).zEff
        assertEquals(3.25f, z(6, 2, 1), 1e-3f)
        assertEquals(3.90f, z(7, 2, 1), 1e-3f)
        assertEquals(4.55f, z(8, 2, 1), 1e-3f)
        assertEquals(2.20f, z(11, 3, 0), 1e-3f)
        assertEquals(3.75f, z(26, 4, 0), 1e-3f)
        assertEquals(6.25f, z(26, 3, 2), 1e-3f)
        assertEquals(2.95f, z(24, 4, 0), 1e-3f)
        assertEquals(3.70f, z(29, 4, 0), 1e-3f)
        assertEquals(1.70f, z(2, 1, 0), 1e-3f)
    }

    @Test
    fun `sampled points fit the unit sphere and the 2pz lobes have opposite signs`() {
        val sample = SampleOrbitalUseCase()(OrbitalRef(2, 1, 2), count = 1500)
        assertEquals(1500, sample.positive.size + sample.negative.size)
        assertTrue((sample.positive + sample.negative).all { it.length() <= 1.001f })
        assertTrue(sample.positive.all { it.z >= -1e-4f })
        assertTrue(sample.negative.all { it.z <= 1e-4f })
        assertEquals(1500 / 2f, sample.positive.size.toFloat(), 120f)
    }

    @Test
    fun `fill explanations name exceptions, interleaving and half-filled p`() {
        fun why(z: Int) = OrbitalTexts.why(z, 0, config(z).subshells, config.aufbauOnly(z)).joinToString(" ") { it.zh }
        assertTrue(why(24), why(24).contains("例外") && why(24).contains("3d⁵ 4s¹") && why(24).contains("半充满"))
        assertTrue(why(29), why(29).contains("全充满"))
        assertTrue(why(26), why(26).contains("能级交错") && !why(26).contains("例外"))
        assertTrue(why(7), why(7).contains("2p³ 半充满"))
        assertTrue(why(51), !why(51).contains("半充满，较稳定"))
        assertTrue(why(6), why(6).contains("未成对电子 2 个"))
    }
}
