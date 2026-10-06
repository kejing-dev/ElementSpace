package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.OrbitalCloud
import com.example.elementspace.p7c3a91e4.domain.model.Subshell
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Samples points from an approximate electron probability density, one cloud per subshell.
 *
 * The angular part uses the real s / p / d / f orbital shapes (sphere, dumbbell, clover, …) and
 * only the orbitals that actually hold electrons under Hund's rule. The radial part is a single
 * peak that grows with the principal quantum number. This is a teaching approximation of
 * hydrogen-like orbitals, not a solution for the many-electron atom.
 */
class SampleOrbitalCloudUseCase {

    /**
     * @param outermostShell largest principal quantum number in the atom, used to scale radii so
     *   the outermost cloud fits a sphere of radius 1.
     * @param pointsPerElectron how many sample points represent one electron.
     */
    operator fun invoke(
        subshell: Subshell,
        outermostShell: Int,
        pointsPerElectron: Int = 60,
        seed: Int = 7,
    ): OrbitalCloud {
        val random = Random(seed * 31 + subshell.n * 7 + subshell.l)
        val orbitals = ANGULAR.getValue(subshell.l)
        val occupied = orbitals.take(minOf(subshell.electrons, orbitals.size))
        val total = subshell.electrons * pointsPerElectron
        val peak = 0.72f * (subshell.n.toFloat() / outermostShell).pow(1.35f)
        val spread = 0.2f * peak

        val points = ArrayList<Vec3>(total)
        var index = 0
        while (points.size < total) {
            val shape = occupied[index % occupied.size]
            index++
            val direction = sampleDirection(shape, random)
            val radius = (peak + spread * gaussian(random)).coerceIn(0.06f * peak, 1f)
            points.add(direction * radius)
        }
        return OrbitalCloud(subshell, points)
    }

    private fun sampleDirection(shape: AngularShape, random: Random): Vec3 {
        while (true) {
            val y = 2f * random.nextFloat() - 1f
            val ring = sqrt(1f - y * y)
            val theta = (2.0 * PI * random.nextFloat()).toFloat()
            val v = Vec3(ring * cos(theta), y, ring * sin(theta))
            val amplitude = shape.amplitude(v) / shape.peak
            if (random.nextFloat() <= amplitude * amplitude) return v
        }
    }

    private fun gaussian(random: Random): Float {
        var sum = 0f
        repeat(4) { sum += random.nextFloat() }
        return (sum - 2f) * 1.7f
    }

    /** Real spherical-harmonic shape (unnormalised) and its maximum absolute value. */
    private class AngularShape(val peak: Float, val amplitude: (Vec3) -> Float)

    private companion object {
        val ANGULAR: Map<Int, List<AngularShape>> =
            mapOf(
                0 to listOf(AngularShape(1f) { 1f }),
                1 to
                    listOf(
                        AngularShape(1f) { it.x },
                        AngularShape(1f) { it.y },
                        AngularShape(1f) { it.z },
                    ),
                2 to
                    listOf(
                        AngularShape(2f) { 3f * it.y * it.y - 1f },
                        AngularShape(0.5f) { it.x * it.y },
                        AngularShape(0.5f) { it.y * it.z },
                        AngularShape(0.5f) { it.x * it.z },
                        AngularShape(1f) { it.x * it.x - it.z * it.z },
                    ),
                3 to
                    listOf(
                        AngularShape(2f) { it.y * (5f * it.y * it.y - 3f) },
                        AngularShape(1.38f) { it.x * (5f * it.y * it.y - 1f) },
                        AngularShape(1.38f) { it.z * (5f * it.y * it.y - 1f) },
                        AngularShape(0.4f) { it.y * (it.x * it.x - it.z * it.z) },
                        AngularShape(0.2f) { it.x * it.y * it.z },
                        AngularShape(1f) { it.x * (it.x * it.x - 3f * it.z * it.z) },
                        AngularShape(1f) { it.z * (3f * it.x * it.x - it.z * it.z) },
                    ),
            )
    }
}
