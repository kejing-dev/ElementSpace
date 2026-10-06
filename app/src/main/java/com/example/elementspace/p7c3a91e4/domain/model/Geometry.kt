package com.example.elementspace.p7c3a91e4.domain.model

import kotlin.math.sqrt

/** Plain 3D vector so geometry stays independent of the rendering SDK and unit-testable. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun distanceTo(o: Vec3): Float = (this - o).length()

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

/** Nucleon positions in units where the whole nucleus fits a sphere of radius 1. */
data class NucleusLayout(
    val protons: List<Vec3>,
    val neutrons: List<Vec3>,
    /** Radius of one nucleon in the same units. */
    val nucleonRadius: Float,
)

/** Sampled probability cloud of one subshell, in units where the outermost cloud fits radius 1. */
data class OrbitalCloud(val subshell: Subshell, val points: List<Vec3>)

data class Bond(val from: Int, val to: Int)

/** Ball-and-stick structure normalised to fit a sphere of radius 1 around its centroid. */
data class Lattice(val atoms: List<Vec3>, val bonds: List<Bond>)
