package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.BondAngle
import com.example.elementspace.p7c3a91e4.domain.model.MolAtom
import com.example.elementspace.p7c3a91e4.domain.model.MolBond
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds ball-and-stick geometry from textbook bond lengths (Å) and angles: tetrahedral carbon
 * (109°28′), trigonal C=C / C=O (≈120°), linear C≡C. Positions are good enough to read the shape,
 * not a computed equilibrium structure.
 */
class MoleculeBuilder {
    val atoms = ArrayList<MolAtom>()
    val bonds = ArrayList<MolBond>()
    val angles = ArrayList<BondAngle>()

    fun atom(symbol: String, position: Vec3, group: Boolean = false): Int {
        atoms += MolAtom(symbol, position, group)
        return atoms.lastIndex
    }

    fun bond(a: Int, b: Int, order: Int = 1) {
        bonds += MolBond(a, b, order)
    }

    fun angle(at: Int, a: Int, b: Int, label: String) {
        angles += BondAngle(at, a, b, label, degrees(at, a, b))
    }

    fun p(i: Int) = atoms[i].position

    /** Adds an atom bonded to [from] at [length] along [direction] (normalised here). */
    fun branch(from: Int, symbol: String, direction: Vec3, length: Float, order: Int = 1, group: Boolean = false): Int {
        val i = atom(symbol, p(from) + norm(direction) * length, group)
        bond(from, i, order)
        return i
    }

    /** Three H on [atom] pointing away from [parent], tetrahedral; [phase] turns them about the bond. */
    fun capH3(atom: Int, parent: Int, phase: Float = 0f, group: Boolean = false) {
        tetraAround(atom, parent, phase).forEach { branch(atom, "H", it, C_H, group = group) }
    }

    /** The three free tetrahedral directions of [atom] whose fourth bond goes to [parent]. */
    fun tetraAround(atom: Int, parent: Int, phase: Float = 0f): List<Vec3> {
        val axis = norm(p(atom) - p(parent))
        val (u, v) = basis(axis)
        return (0 until 3).map { k ->
            val t = phase + k * 2f * PI.toFloat() / 3f
            axis * AXIAL + (u * cos(t) + v * sin(t)) * RADIAL
        }
    }

    /** Two H on a CH2 whose other bonds go to [n1] and [n2]. */
    fun ch2(atom: Int, n1: Int, n2: Int) {
        val a = norm(p(n1) - p(atom))
        val b = norm(p(n2) - p(atom))
        val w = norm(a + b) * -1f
        val n = norm(cross(a, b))
        for (s in listOf(1f, -1f)) branch(atom, "H", w * 0.577f + n * (0.816f * s), C_H)
    }

    /** The in-plane direction opposite the two bonds at [atom] (for C=O on an sp2 carbon). */
    fun outward(atom: Int, n1: Int, n2: Int): Vec3 = norm(norm(p(n1) - p(atom)) + norm(p(n2) - p(atom))) * -1f

    /** Re-centres the molecule on its centroid. */
    fun centred(): MoleculeBuilder {
        val c = atoms.fold(Vec3.ZERO) { s, a -> s + a.position } * (1f / atoms.size)
        atoms.replaceAll { it.copy(position = it.position - c) }
        return this
    }

    fun degrees(at: Int, a: Int, b: Int): Float {
        val u = norm(p(a) - p(at))
        val v = norm(p(b) - p(at))
        return (acos((u.x * v.x + u.y * v.y + u.z * v.z).coerceIn(-1f, 1f)) * 180f / PI.toFloat())
    }

    companion object {
        const val C_C = 1.54f
        const val C_DOUBLE = 1.34f
        const val C_TRIPLE = 1.20f
        const val C_AROMATIC = 1.39f
        const val C_H = 1.09f
        const val C_O = 1.43f
        const val C_DOUBLE_O = 1.21f
        const val O_H = 0.96f
        const val C_CL = 1.77f
        const val C_BR = 1.94f
        const val CL_CL = 1.99f
        const val BR_BR = 2.28f
        const val H_CL = 1.27f

        // cos and sin of 70.53° (= 180° − 109.47°): the free bonds of a tetrahedral atom.
        private const val AXIAL = 0.3333f
        private const val RADIAL = 0.9428f

        /** Four tetrahedral directions, the first along +x. */
        val TETRAHEDRAL: List<Vec3> =
            listOf(Vec3(1f, 0f, 0f)) +
                (0 until 3).map { k ->
                    val t = k * 2f * PI.toFloat() / 3f
                    Vec3(-AXIAL, RADIAL * cos(t), RADIAL * sin(t))
                }

        /** Zig-zag chain in the XY plane with tetrahedral angles; bond length [b]. */
        fun zigzag(n: Int, b: Float = C_C): List<Vec3> {
            val half = 54.735f * PI.toFloat() / 180f
            val h = b * sin(half)
            val v = b * cos(half)
            return (0 until n).map { i -> Vec3(i * h, if (i % 2 == 1) v else 0f, 0f) }
        }

        fun norm(a: Vec3): Vec3 = a * (1f / a.length())

        fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

        fun basis(axis: Vec3): Pair<Vec3, Vec3> {
            val t = if (abs(axis.z) < 0.9f) Vec3(0f, 0f, 1f) else Vec3(1f, 0f, 0f)
            val u = norm(cross(axis, t))
            return u to cross(axis, u)
        }

        fun dot(a: Vec3, b: Vec3) = a.x * b.x + a.y * b.y + a.z * b.z

        fun lengthOf(a: Vec3) = sqrt(dot(a, a))
    }
}
