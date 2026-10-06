package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.AllotropeStructure
import com.example.elementspace.p7c3a91e4.domain.model.Bond
import com.example.elementspace.p7c3a91e4.domain.model.Lattice
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds ball-and-stick geometry for an allotrope or crystal structure.
 *
 * Atom positions come from the ideal structure; bonds are every pair at the nearest-neighbour
 * distance. The result is centred and scaled to fit a sphere of radius 1.
 */
class BuildAllotropeLatticeUseCase {

    operator fun invoke(structure: AllotropeStructure): Lattice {
        val (atoms, bondLength) =
            when (structure) {
                AllotropeStructure.DIAMOND -> diamond() to sqrt(3f) / 4f
                AllotropeStructure.GRAPHITE -> graphite() to 1f
                AllotropeStructure.FULLERENE -> fullerene() to 2f
                AllotropeStructure.DIATOMIC -> listOf(Vec3(-0.5f, 0f, 0f), Vec3(0.5f, 0f, 0f)) to 1f
                AllotropeStructure.OZONE -> ozone() to 1f
                AllotropeStructure.S8_RING -> crownRing() to crownRing().let { it[0].distanceTo(it[1]) }
                AllotropeStructure.P4_TETRAHEDRON -> tetrahedron() to sqrt(8f)
                AllotropeStructure.BCC -> bcc() to sqrt(3f) / 2f
                AllotropeStructure.FCC -> fcc() to sqrt(2f) / 2f
                AllotropeStructure.HCP -> hcp() to 1f
            }
        return normalise(atoms, bonds(atoms, bondLength))
    }

    private fun bonds(atoms: List<Vec3>, length: Float): List<Bond> {
        val tolerance = length * 0.04f
        val result = ArrayList<Bond>()
        for (i in atoms.indices) {
            for (j in i + 1 until atoms.size) {
                if (abs(atoms[i].distanceTo(atoms[j]) - length) <= tolerance) result.add(Bond(i, j))
            }
        }
        return result
    }

    private fun normalise(atoms: List<Vec3>, bonds: List<Bond>): Lattice {
        val n = atoms.size.toFloat()
        val centroid =
            Vec3(atoms.sumOf { it.x.toDouble() }.toFloat() / n, atoms.sumOf { it.y.toDouble() }.toFloat() / n, atoms.sumOf { it.z.toDouble() }.toFloat() / n)
        val centred = atoms.map { it - centroid }
        val reach = centred.maxOf { it.length() }.takeIf { it > 0f } ?: 1f
        return Lattice(centred.map { it * (1f / reach) }, bonds)
    }

    /** Conventional cubic cell: 8 corners, 6 face centres and 4 interior atoms. */
    private fun diamond(): List<Vec3> {
        val atoms = ArrayList<Vec3>()
        for (x in 0..1) for (y in 0..1) for (z in 0..1) atoms.add(Vec3(x.toFloat(), y.toFloat(), z.toFloat()))
        atoms += faceCentres()
        atoms += listOf(Vec3(.25f, .25f, .25f), Vec3(.75f, .75f, .25f), Vec3(.75f, .25f, .75f), Vec3(.25f, .75f, .75f))
        return atoms
    }

    private fun faceCentres(): List<Vec3> =
        listOf(Vec3(.5f, .5f, 0f), Vec3(.5f, .5f, 1f), Vec3(.5f, 0f, .5f), Vec3(.5f, 1f, .5f), Vec3(0f, .5f, .5f), Vec3(1f, .5f, .5f))

    private fun bcc(): List<Vec3> {
        val atoms = ArrayList<Vec3>()
        for (x in 0..1) for (y in 0..1) for (z in 0..1) atoms.add(Vec3(x.toFloat(), y.toFloat(), z.toFloat()))
        atoms.add(Vec3(.5f, .5f, .5f))
        return atoms
    }

    private fun fcc(): List<Vec3> {
        val atoms = ArrayList<Vec3>()
        for (x in 0..1) for (y in 0..1) for (z in 0..1) atoms.add(Vec3(x.toFloat(), y.toFloat(), z.toFloat()))
        atoms += faceCentres()
        return atoms
    }

    /** Hexagonal prism: two centred hexagons with three atoms in the layer between them. */
    private fun hcp(): List<Vec3> {
        val atoms = ArrayList<Vec3>()
        val height = sqrt(8f / 3f)
        for (layer in 0..1) {
            val y = layer * height
            atoms.add(Vec3(0f, y, 0f))
            for (k in 0 until 6) {
                val a = (k * PI / 3).toFloat()
                atoms.add(Vec3(cos(a), y, sin(a)))
            }
        }
        for (k in 0 until 3) {
            val a = (PI / 6 + k * 2 * PI / 3).toFloat()
            val r = 1f / sqrt(3f)
            atoms.add(Vec3(r * cos(a), height / 2f, r * sin(a)))
        }
        return atoms
    }

    /** Two AB-stacked honeycomb sheets; in-plane bond length 1, layer spacing 2.36. */
    private fun graphite(): List<Vec3> {
        val atoms = ArrayList<Vec3>()
        val a1 = Vec3(sqrt(3f), 0f, 0f)
        val a2 = Vec3(sqrt(3f) / 2f, 0f, 1.5f)
        for (layer in 0..1) {
            val y = (layer - 0.5f) * 2.36f
            val shift = if (layer == 0) Vec3.ZERO else Vec3(0f, 0f, 1f)
            for (i in -3..3) for (j in -3..3) {
                val cell = a1 * i.toFloat() + a2 * j.toFloat() + shift
                for (basis in listOf(Vec3.ZERO, Vec3(0f, 0f, 1f))) {
                    val p = cell + basis
                    if (sqrt(p.x * p.x + p.z * p.z) <= 2.7f) atoms.add(Vec3(p.x, y, p.z))
                }
            }
        }
        return atoms
    }

    /** Truncated icosahedron: 60 vertices, every edge of length 2. */
    private fun fullerene(): List<Vec3> {
        val phi = (1f + sqrt(5f)) / 2f
        val seeds = listOf(Vec3(0f, 1f, 3f * phi), Vec3(1f, 2f + phi, 2f * phi), Vec3(phi, 2f, 2f * phi + 1f))
        val atoms = LinkedHashSet<Vec3>()
        for (s in seeds) {
            for (sx in signs(s.x)) for (sy in signs(s.y)) for (sz in signs(s.z)) {
                val v = Vec3(s.x * sx, s.y * sy, s.z * sz)
                atoms.add(v)
                atoms.add(Vec3(v.y, v.z, v.x))
                atoms.add(Vec3(v.z, v.x, v.y))
            }
        }
        return atoms.toList()
    }

    private fun signs(value: Float): List<Float> = if (value == 0f) listOf(1f) else listOf(1f, -1f)

    private fun ozone(): List<Vec3> {
        val half = (116.8 / 2.0 * PI / 180.0).toFloat()
        return listOf(Vec3(-sin(half), -cos(half), 0f), Vec3.ZERO, Vec3(sin(half), -cos(half), 0f))
    }

    private fun crownRing(): List<Vec3> =
        (0 until 8).map { k ->
            val a = (k * PI / 4).toFloat()
            Vec3(cos(a), if (k % 2 == 0) 0.25f else -0.25f, sin(a))
        }

    private fun tetrahedron(): List<Vec3> =
        listOf(Vec3(1f, 1f, 1f), Vec3(1f, -1f, -1f), Vec3(-1f, 1f, -1f), Vec3(-1f, -1f, 1f))
}
