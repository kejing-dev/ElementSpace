package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.NucleusLayout
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Packs protons and neutrons into a compact, roughly spherical cluster, which is how a nucleus is
 * conventionally drawn: nucleons touching one another in a ball, the two kinds mixed throughout.
 *
 * Positions start spread through a ball and are then relaxed: overlapping nucleons push each other
 * apart while everything is pulled gently toward the centre. The result is deterministic, so the
 * same counts always give the same picture.
 */
class BuildNucleusLayoutUseCase {

    operator fun invoke(protons: Int, neutrons: Int): NucleusLayout {
        require(protons >= 0 && neutrons >= 0) { "particle counts must not be negative" }
        val total = protons + neutrons
        if (total == 0) return NucleusLayout(emptyList(), emptyList(), 0f)
        if (total == 1) {
            val single = listOf(Vec3.ZERO)
            return if (protons == 1) NucleusLayout(single, emptyList(), 1f)
            else NucleusLayout(emptyList(), single, 1f)
        }

        // Radius for equal spheres randomly packed in a ball of radius 1 (packing fraction ~0.64).
        val radius = 0.86f / cbrt(total.toFloat())
        val xs = FloatArray(total)
        val ys = FloatArray(total)
        val zs = FloatArray(total)
        seed(xs, ys, zs, reach = 1f - radius)
        relax(xs, ys, zs, radius)

        // Fit the finished cluster exactly inside the unit sphere.
        var extent = 0f
        for (i in 0 until total) extent = maxOf(extent, sqrt(xs[i] * xs[i] + ys[i] * ys[i] + zs[i] * zs[i]))
        val scale = 1f / (extent + radius)

        val protonList = ArrayList<Vec3>(protons)
        val neutronList = ArrayList<Vec3>(neutrons)
        var protonsPlaced = 0
        for (i in 0 until total) {
            val point = Vec3(xs[i] * scale, ys[i] * scale, zs[i] * scale)
            // Spread protons evenly through the sequence instead of placing them first.
            val protonsDue = ((i + 1).toLong() * protons / total).toInt()
            if (protonsDue > protonsPlaced) {
                protonList.add(point)
                protonsPlaced++
            } else {
                neutronList.add(point)
            }
        }
        return NucleusLayout(protonList, neutronList, radius * scale)
    }

    /**
     * Starting positions spread through the ball. Direction comes from a Fibonacci sphere and the
     * distance from the centre from an unrelated sequence; using the same sequence for both puts
     * every point on one spiral, which is not what a nucleus looks like.
     */
    private fun seed(xs: FloatArray, ys: FloatArray, zs: FloatArray, reach: Float) {
        val n = xs.size
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat()
        for (i in 0 until n) {
            val y = 1f - 2f * (i + 0.5f) / n
            val ring = sqrt((1f - y * y).coerceAtLeast(0f))
            val theta = goldenAngle * i
            val fraction = (0.5f + i * PLASTIC) % 1f
            val r = reach * cbrt(fraction)
            xs[i] = r * ring * cos(theta)
            ys[i] = r * y
            zs[i] = r * ring * sin(theta)
        }
    }

    private fun relax(xs: FloatArray, ys: FloatArray, zs: FloatArray, radius: Float) {
        repeat(ITERATIONS) {
            separate(xs, ys, zs, radius)
            for (i in xs.indices) {
                xs[i] *= GRAVITY
                ys[i] *= GRAVITY
                zs[i] *= GRAVITY
            }
        }
        // Gravity leaves neighbours slightly sunk into each other; finish by only pushing apart.
        repeat(SETTLE_ITERATIONS) { separate(xs, ys, zs, radius) }
    }

    private fun separate(xs: FloatArray, ys: FloatArray, zs: FloatArray, radius: Float) {
        val n = xs.size
        val minDistance = 2f * radius
        run {
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    var dx = xs[j] - xs[i]
                    var dy = ys[j] - ys[i]
                    var dz = zs[j] - zs[i]
                    var distance = sqrt(dx * dx + dy * dy + dz * dz)
                    if (distance >= minDistance) continue
                    if (distance < 1e-5f) {
                        // Coincident points: separate along a fixed axis.
                        dx = 1f
                        dy = 0f
                        dz = 0f
                        distance = 1f
                    }
                    val push = (minDistance - distance) / 2f / distance
                    xs[i] -= dx * push
                    ys[i] -= dy * push
                    zs[i] -= dz * push
                    xs[j] += dx * push
                    ys[j] += dy * push
                    zs[j] += dz * push
                }
            }
        }
    }

    private companion object {
        /** Reciprocal of the plastic number: a low-discrepancy step unrelated to the golden ratio. */
        const val PLASTIC = 0.7548777f
        const val ITERATIONS = 90
        const val SETTLE_ITERATIONS = 40
        const val GRAVITY = 0.985f
    }
}
