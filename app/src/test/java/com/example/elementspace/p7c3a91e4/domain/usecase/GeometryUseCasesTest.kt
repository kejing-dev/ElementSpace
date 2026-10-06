package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.AllotropeStructure
import com.example.elementspace.p7c3a91e4.domain.model.Subshell
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildNucleusLayoutUseCaseTest {
    private val build = BuildNucleusLayoutUseCase()

    @Test
    fun `particle counts match the request`() {
        val carbon14 = build(protons = 6, neutrons = 8)
        assertEquals(6, carbon14.protons.size)
        assertEquals(8, carbon14.neutrons.size)
    }

    @Test
    fun `every nucleon stays inside the unit sphere`() {
        val oganesson = build(protons = 118, neutrons = 176)
        val all = oganesson.protons + oganesson.neutrons
        assertEquals(294, all.size)
        assertTrue(all.all { it.length() + oganesson.nucleonRadius <= 1.0001f })
    }

    @Test
    fun `nucleus is a compact ball rather than a chain or spiral`() {
        for ((protons, neutrons) in listOf(6 to 8, 27 to 32, 80 to 122, 118 to 176)) {
            val layout = build(protons, neutrons)
            val all = layout.protons + layout.neutrons
            // Roughly as wide along every axis: a chain or spiral would be thin in at least one.
            val spans = listOf(all.maxOf { it.x } - all.minOf { it.x }, all.maxOf { it.y } - all.minOf { it.y }, all.maxOf { it.z } - all.minOf { it.z })
            assertTrue("Z=$protons spans=$spans", spans.min() >= 0.7f * spans.max())
            // Neighbours touch but do not sink into one another.
            var closest = Float.MAX_VALUE
            for (i in all.indices) for (j in i + 1 until all.size) closest = minOf(closest, all[i].distanceTo(all[j]))
            assertTrue("Z=$protons closest=$closest r=${layout.nucleonRadius}", closest >= 1.7f * layout.nucleonRadius)
            // The centre is filled, not hollow.
            assertTrue("Z=$protons hollow", all.any { it.length() < 0.45f })
        }
    }

    @Test
    fun `protons and neutrons are mixed through the nucleus`() {
        val layout = build(protons = 27, neutrons = 32)
        fun centroid(points: List<com.example.elementspace.p7c3a91e4.domain.model.Vec3>) =
            points.fold(com.example.elementspace.p7c3a91e4.domain.model.Vec3.ZERO) { acc, p -> acc + p } * (1f / points.size)
        // If one kind were clumped on one side, the two centroids would be far apart.
        assertTrue(centroid(layout.protons).distanceTo(centroid(layout.neutrons)) < 0.25f)
    }

    @Test
    fun `hydrogen is a single proton and an empty request is empty`() {
        val hydrogen = build(protons = 1, neutrons = 0)
        assertEquals(1, hydrogen.protons.size)
        assertTrue(hydrogen.neutrons.isEmpty())
        assertTrue(build(0, 0).protons.isEmpty())
    }
}

class SampleOrbitalCloudUseCaseTest {
    private val sample = SampleOrbitalCloudUseCase()

    @Test
    fun `point count scales with electrons and stays inside the unit sphere`() {
        val cloud = sample(Subshell(n = 2, l = 1, electrons = 2), outermostShell = 2, pointsPerElectron = 50)
        assertEquals(100, cloud.points.size)
        assertTrue(cloud.points.all { it.length() <= 1.0001f })
    }

    @Test
    fun `sampling is deterministic`() {
        val a = sample(Subshell(3, 2, 5), outermostShell = 4)
        val b = sample(Subshell(3, 2, 5), outermostShell = 4)
        assertEquals(a.points, b.points)
    }

    @Test
    fun `a single p electron forms a dumbbell along one axis`() {
        // One electron occupies only the first p orbital (along x), so points hug the x axis.
        val cloud = sample(Subshell(n = 2, l = 1, electrons = 1), outermostShell = 2, pointsPerElectron = 400)
        val alongX = cloud.points.sumOf { abs(it.x).toDouble() }
        val alongY = cloud.points.sumOf { abs(it.y).toDouble() }
        assertTrue(alongX > alongY * 1.5)
    }

    @Test
    fun `inner shells sit closer to the nucleus than outer shells`() {
        val inner = sample(Subshell(1, 0, 2), outermostShell = 3).points.map { it.length() }.average()
        val outer = sample(Subshell(3, 0, 2), outermostShell = 3).points.map { it.length() }.average()
        assertTrue(inner < outer)
    }
}

class BuildAllotropeLatticeUseCaseTest {
    private val build = BuildAllotropeLatticeUseCase()

    @Test
    fun `diamond cell has 18 atoms and each interior carbon has four bonds`() {
        val diamond = build(AllotropeStructure.DIAMOND)
        assertEquals(18, diamond.atoms.size)
        assertEquals(16, diamond.bonds.size)
        for (interior in 14..17) {
            assertEquals(4, diamond.bonds.count { it.from == interior || it.to == interior })
        }
    }

    @Test
    fun `fullerene has 60 atoms 90 bonds and three bonds per atom`() {
        val c60 = build(AllotropeStructure.FULLERENE)
        assertEquals(60, c60.atoms.size)
        assertEquals(90, c60.bonds.size)
        for (i in c60.atoms.indices) assertEquals(3, c60.bonds.count { it.from == i || it.to == i })
    }

    @Test
    fun `every structure is centred and fits the unit sphere`() {
        for (structure in AllotropeStructure.entries) {
            val lattice = build(structure)
            assertTrue("$structure empty", lattice.atoms.isNotEmpty())
            assertEquals("$structure reach", 1f, lattice.atoms.maxOf { it.length() }, 1e-3f)
            assertTrue("$structure bonds", lattice.bonds.isNotEmpty())
        }
    }

    @Test
    fun `ozone is bent with two bonds and the body centred cube has eight`() {
        assertEquals(2, build(AllotropeStructure.OZONE).bonds.size)
        assertEquals(8, build(AllotropeStructure.BCC).bonds.size)
    }
}
