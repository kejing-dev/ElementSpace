package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.ElectronFlight
import com.example.elementspace.p7c3a91e4.domain.model.FrameParticle
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.SharedPair
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry of the flat Bohr models in the reaction Volume, kept pure so it can be unit-tested:
 * ring radii, where each electron, vacancy and transferred electron sits, shared-pair positions
 * and the arc an electron flies along.
 */
object ReactionLayout {
    /** Seconds of one flight loop: fly, rest at the acceptor, start again. */
    const val FLIGHT_CYCLE = 3.0f
    const val FLIGHT_TIME = 1.6f

    private const val TWO_PI = (2.0 * PI).toFloat()

    fun shellRadius(shell: Int): Float = 0.06f + 0.052f * (shell + 1)

    fun particleRadius(particle: FrameParticle): Float =
        if (particle.cluster.isNotEmpty()) 0.12f else shellRadius((particle.shells.size - 1).coerceAtLeast(0))

    fun ring(radius: Float, count: Int): List<Vec3> =
        (0 until count).map { i ->
            val a = TWO_PI * i / count
            Vec3(radius * cos(a), radius * sin(a), 0f)
        }

    enum class SlotKind { ELECTRON, GOLD, VACANCY }

    data class Slot(val kind: SlotKind, val position: Vec3)

    /**
     * Slots on one ring. Slot 0 points left (toward the donors, which stand on the left in every
     * frame), so vacancies and transferred (gold) electrons sit where a flying electron lands.
     */
    fun slots(electrons: Int, vacancies: Int, gold: Int, radius: Float): List<Slot> {
        val total = electrons + vacancies
        if (total == 0) return emptyList()
        return (0 until total).map { k ->
            val a = PI.toFloat() + TWO_PI * k / total
            val p = Vec3(radius * cos(a), radius * sin(a), 0f)
            val kind =
                when {
                    k < vacancies -> SlotKind.VACANCY
                    k - vacancies < gold -> SlotKind.GOLD
                    else -> SlotKind.ELECTRON
                }
            Slot(kind, p)
        }
    }

    /** Positions of the electrons of [pair], between the two particles' outer rings. */
    fun pairElectrons(frame: ReactionFrame, pair: SharedPair): List<Vec3> {
        val a = frame.particles.firstOrNull { it.id == pair.a } ?: return emptyList()
        val b = frame.particles.firstOrNull { it.id == pair.b } ?: return emptyList()
        val d = b.position - a.position
        val length = d.length().coerceAtLeast(1e-4f)
        val dir = d * (1f / length)
        val centre = a.position + d * pair.bias
        val normal = Vec3(-dir.y, dir.x, 0f)
        val gap = 0.019f * frame.scale
        return if (pair.count <= 2) {
            listOf(centre + normal * gap, centre - normal * gap)
        } else if (pair.count >= 6) {
            // A triple bond: three pairs side by side along the bond.
            listOf(-2f, 0f, 2f).flatMap { k -> listOf(centre + normal * gap + dir * (gap * k), centre - normal * gap + dir * (gap * k)) }
        } else {
            listOf(
                centre + normal * gap + dir * gap,
                centre - normal * gap + dir * gap,
                centre + normal * gap - dir * gap,
                centre - normal * gap - dir * gap,
            )
        }
    }

    /** Start (donor's outer ring, facing the acceptor) and end (acceptor's landing slot) of a flight. */
    fun flightEnds(frame: ReactionFrame, flight: ElectronFlight): Pair<Vec3, Vec3>? {
        val from = frame.particles.firstOrNull { it.id == flight.from } ?: return null
        val to = frame.particles.firstOrNull { it.id == flight.to } ?: return null
        val angle = atan2(to.position.y - from.position.y, to.position.x - from.position.x)
        val r = particleRadius(from) * frame.scale
        val start = from.position + Vec3(r * cos(angle), r * sin(angle), 0f)
        val outer = shellRadius((to.shells.size - 1).coerceAtLeast(0)) * frame.scale
        // Landing slot 0 of the acceptor's outer ring (see [slots]).
        val end = to.position + Vec3(-outer, 0f, 0f)
        return start to end
    }

    /** Where a flying electron is [t] seconds into its loop: rests at the start, arcs over, rests at the end. */
    fun flightPoint(from: Vec3, to: Vec3, lift: Float, t: Float): Vec3 {
        val u = ease(((t - 0.3f) / FLIGHT_TIME).coerceIn(0f, 1f))
        val straight = from + (to - from) * u
        val arc = 4f * u * (1f - u) * lift
        return straight + Vec3(0f, arc, arc * 0.5f)
    }

    fun ease(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    /** Four directions of a tetrahedron, for the O atoms around S in SO4 2-. */
    fun tetrahedron(r: Float): List<Vec3> {
        val k = r / sqrt(3f)
        return listOf(Vec3(k, k, k), Vec3(-k, -k, k), Vec3(-k, k, -k), Vec3(k, -k, -k))
    }
}
