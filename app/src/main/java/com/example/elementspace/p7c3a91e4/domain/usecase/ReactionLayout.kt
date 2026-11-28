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
        if (particle.cluster.isNotEmpty()) 0.12f else shellRadius((particle.shells.size - 1).coerceAtLeast(0) + particle.ring)

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
        val gap = PAIR_GAP * frame.scale
        val along = gap * 1.15f
        return if (pair.count <= 2) {
            listOf(centre + normal * gap, centre - normal * gap)
        } else if (pair.count >= 6) {
            // A triple bond: three pairs side by side along the bond.
            listOf(-2f, 0f, 2f).flatMap { k -> listOf(centre + normal * gap + dir * (along * k), centre - normal * gap + dir * (along * k)) }
        } else {
            listOf(
                centre + normal * gap + dir * along,
                centre - normal * gap + dir * along,
                centre + normal * gap - dir * along,
                centre - normal * gap - dir * along,
            )
        }
    }

    /** Half the distance between the two electrons of a pair, scene units at scale 1. */
    const val PAIR_GAP = 0.022f

    /** Direction (radians) from each bonded particle toward every partner it shares a pair with. */
    fun bondAngles(frame: ReactionFrame): Map<String, List<Float>> {
        val at = frame.particles.associate { it.id to it.position }
        val out = HashMap<String, MutableList<Float>>()
        frame.pairs.forEach { pair ->
            val a = at[pair.a] ?: return@forEach
            val b = at[pair.b] ?: return@forEach
            out.getOrPut(pair.a) { ArrayList() } += atan2(b.y - a.y, b.x - a.x)
            out.getOrPut(pair.b) { ArrayList() } += atan2(a.y - b.y, a.x - b.x)
        }
        return out
    }

    /**
     * Directions for [groups] lone pairs around an atom whose bonds point along [bonds] (v1.18.0):
     * spread evenly through the gaps between the bonds, the widest gap first, the way an
     * electron-dot formula is written. Without bonds they start at the top.
     */
    fun loneDirections(groups: Int, bonds: List<Float>): List<Float> {
        if (groups <= 0) return emptyList()
        val sorted = bonds.map { ((it % TWO_PI) + TWO_PI) % TWO_PI }.sorted().fold(ArrayList<Float>()) { acc, a -> if (acc.isEmpty() || a - acc.last() > 1e-3f) acc += a; acc }
        if (sorted.isEmpty()) return (0 until groups).map { PI.toFloat() / 2f + TWO_PI * it / groups }
        if (sorted.size == 1 && groups == 2) return listOf(sorted[0] + PI.toFloat() / 2f, sorted[0] - PI.toFloat() / 2f)
        val gaps = sorted.indices.map { i -> if (i == sorted.lastIndex) sorted[0] + TWO_PI - sorted[i] else sorted[i + 1] - sorted[i] }
        val counts = IntArray(sorted.size)
        repeat(groups) {
            val i = gaps.indices.maxBy { gaps[it] / (counts[it] + 1) }
            counts[i]++
        }
        return sorted.indices.flatMap { i -> (1..counts[i]).map { k -> sorted[i] + gaps[i] * k / (counts[i] + 1) } }
    }

    /**
     * Outer-ring electrons of a bonded atom, drawn as lone pairs that keep clear of its bonds, so
     * the shared pairs between two atoms are not crowded by ring electrons.
     */
    fun loneSlots(electrons: Int, radius: Float, bonds: List<Float>): List<Vec3> {
        val groups = (electrons + 1) / 2
        val half = PAIR_GAP / radius
        return loneDirections(groups, bonds).flatMapIndexed { g, a ->
            val single = g == groups - 1 && electrons % 2 == 1
            (if (single) listOf(a) else listOf(a - half, a + half)).map { Vec3(radius * cos(it), radius * sin(it), 0f) }
        }
    }

    /** Start (donor's outer ring, facing the acceptor) and end (acceptor's landing slot) of a flight. */
    fun flightEnds(frame: ReactionFrame, flight: ElectronFlight): Pair<Vec3, Vec3>? {
        val from = frame.particles.firstOrNull { it.id == flight.from } ?: return null
        val to = frame.particles.firstOrNull { it.id == flight.to } ?: return null
        val angle = atan2(to.position.y - from.position.y, to.position.x - from.position.x)
        val r = particleRadius(from) * frame.scale
        val start = from.position + Vec3(r * cos(angle), r * sin(angle), 0f)
        val outer = shellRadius((to.shells.size - 1).coerceAtLeast(0) + to.ring) * frame.scale
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
