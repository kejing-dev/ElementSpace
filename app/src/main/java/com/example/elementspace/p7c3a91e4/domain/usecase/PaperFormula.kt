package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI

/**
 * How a frame of Bohr models is written on paper (v1.18.0): the electron-dot formula (电子式) and,
 * when atoms share pairs, the structural formula (结构式). The 3D picture is crowded where two
 * atoms share electrons; the textbook notation says the same thing with dots and lines.
 *
 * Rules, as in school textbooks: a free atom gets its outer electrons one per side first, then in
 * pairs; a bonded atom or an anion gets lone pairs; a cation is its symbol and charge without
 * dots; an anion stands in square brackets with its charge.
 */
object PaperFormula {
    /** Dots written beside a symbol in the direction [angle] (radians, y up): one or a pair. */
    data class Lone(val angle: Float, val dots: Int)

    /** [charge] is printed on the symbol itself: only a cation that is not part of a bracketed group. */
    data class Atom(val symbol: String, val position: Vec3, val lone: List<Lone>, val charge: Int)

    /** [pairs] shared electron pairs between atoms [a] and [b] (indices into [Diagram.atoms]). */
    data class Bond(val a: Int, val b: Int, val pairs: Int)

    /** Atoms inside one pair of square brackets, with the charge written after them. */
    data class Group(val atoms: List<Int>, val charge: Int)

    /** "2×" written in front of a species that occurs several times; [x] is its left edge, on the row's centre line. */
    data class Multiplier(val count: Int, val x: Float)

    /**
     * Each different species once, side by side in a row, with a multiplier in front when the frame
     * holds several of it. Positions are in scene units; [em] is how much of them one symbol height takes.
     */
    data class Diagram(val atoms: List<Atom>, val bonds: List<Bond>, val groups: List<Group>, val multipliers: List<Multiplier> = emptyList(), val em: Float = 1f, /** How many of its species the frame holds, per atom. */ val times: List<Int> = emptyList())

    /** Bonded atoms stand this many symbol heights apart: room for a triple bond's dots. */
    private const val SPACING = 2.7f

    private val SIDES = listOf(PI.toFloat() / 2f, 0f, -PI.toFloat() / 2f, PI.toFloat())

    /** Null when the frame has nothing to write this way: no particles, or a grouped ion such as sulfate. */
    fun of(frame: ReactionFrame, atomicNumbers: Map<String, Int>): Diagram? {
        val ps = frame.particles
        if (!frame.paper || ps.isEmpty() || ps.any { it.cluster.isNotEmpty() }) return null
        val index = ps.withIndex().associate { it.value.id to it.index }
        val merged = LinkedHashMap<Pair<Int, Int>, Int>()
        frame.pairs.forEach { pair ->
            val a = index[pair.a] ?: return null
            val b = index[pair.b] ?: return null
            val key = if (a < b) a to b else b to a
            merged[key] = (merged[key] ?: 0) + pair.count
        }
        val bonds = merged.map { (k, electrons) -> Bond(k.first, k.second, (electrons + 1) / 2) }
        val angles = ReactionLayout.bondAngles(frame)
        val shared = IntArray(ps.size)
        bonds.forEach { shared[it.a] += it.pairs; shared[it.b] += it.pairs }

        val charges =
            ps.mapIndexed { i, p ->
                val z = atomicNumbers[p.symbol] ?: return null
                val full = p.tint == ChargeTint.POSITIVE || p.tint == ChargeTint.NEGATIVE
                if (shared[i] > 0 && !full) 0 else z - p.hidden - p.shells.sum() - shared[i]
            }

        // Connected atoms are one species: its charge is the sum, and a negative one goes in brackets.
        val group = IntArray(ps.size) { it }
        fun root(i: Int): Int = if (group[i] == i) i else root(group[i]).also { group[i] = it }
        bonds.forEach { group[root(it.a)] = root(it.b) }
        val members = ps.indices.groupBy { root(it) }.values
        val groups = members.mapNotNull { m -> m.sumOf { charges[it] }.takeIf { it < 0 || (it != 0 && m.size > 1) }?.let { Group(m, it) } }
        val bracketed = groups.flatMap { it.atoms }.toSet()

        val atoms =
            ps.mapIndexed { i, p ->
                val outer = p.shells.lastOrNull() ?: 0
                val bonded = shared[i] > 0
                val lone =
                    when {
                        charges[i] > 0 && !bonded -> emptyList()
                        outer > 8 -> emptyList()
                        bonded -> {
                            val n = (outer + 1) / 2
                            ReactionLayout.loneDirections(n, angles[p.id].orEmpty()).mapIndexed { g, a -> Lone(a, if (g == n - 1 && outer % 2 == 1) 1 else 2) }
                        }
                        // An anion's outer shell is full: all pairs ([H:]⁻ has one, written on the right).
                        charges[i] < 0 -> (if (outer <= 2) listOf(0f) else SIDES.take(outer / 2)).map { Lone(it, 2) }
                        else -> SIDES.mapIndexedNotNull { k, a -> (if (outer > 4) (if (k < outer - 4) 2 else 1) else if (k < outer) 1 else 0).takeIf { it > 0 }?.let { Lone(a, it) } }
                    }
                Atom(p.symbol, p.position, lone, if (i in bracketed) 0 else charges[i])
            }
        // More than eight electrons around an atom (PCl₅, SF₆, the S=O way of drawing SO₂) is not written at school.
        val crowded = atoms.indices.any { i -> shared[i] > 0 && atoms[i].lone.sumOf { it.dots } + 2 * shared[i] > if (atoms[i].symbol == "H") 2 else 8 }
        return if (crowded) null else row(Diagram(atoms, bonds, groups), members.toList())
    }

    /** Keeps one of each species (same atoms, dots, bonds and charge) and lays them out left to right. */
    private fun row(d: Diagram, species: List<List<Int>>): Diagram {
        fun signature(m: List<Int>): String {
            val atoms = m.map { i -> d.atoms[i].let { a -> "${a.symbol}${a.charge}:${a.lone.sumOf { it.dots }}" } }.sorted()
            val bonds = d.bonds.filter { it.a in m }.map { b -> listOf(d.atoms[b.a].symbol, d.atoms[b.b].symbol).sorted().joinToString("") + b.pairs }.sorted()
            return "$atoms$bonds${d.groups.firstOrNull { it.atoms.toSet() == m.toSet() }?.charge}"
        }
        val ordered = species.sortedBy { it.min() }
        val kinds = ordered.groupBy(::signature).values.map { it.first() to it.size }
        val bonded = d.bonds.map { d.atoms[it.a].position.distanceTo(d.atoms[it.b].position) }
        val em = (bonded.minOrNull() ?: SPACING) / SPACING
        val keep = ArrayList<Int>()
        val times = ArrayList<Int>()
        val at = HashMap<Int, Vec3>()
        val multipliers = ArrayList<Multiplier>()
        var x = 0f
        kinds.forEach { (m, count) ->
            if (count > 1) { multipliers += Multiplier(count, x); x += 1.7f * em }
            val xs = m.map { d.atoms[it].position.x }
            val ys = m.map { d.atoms[it].position.y }
            val midY = (ys.max() + ys.min()) / 2f
            // 1.4 em around the outermost symbols: their dots, and the brackets of an anion.
            val left = x + 1.4f * em
            m.forEach { i -> at[i] = Vec3(left + d.atoms[i].position.x - xs.min(), d.atoms[i].position.y - midY, 0f) }
            keep += m
            m.forEach { _ -> times += count }
            x = left + (xs.max() - xs.min()) + 1.4f * em + 0.9f * em
        }
        val index = keep.withIndex().associate { it.value to it.index }
        return Diagram(
            atoms = keep.map { d.atoms[it].copy(position = at.getValue(it)) },
            bonds = d.bonds.filter { it.a in index }.map { Bond(index.getValue(it.a), index.getValue(it.b), it.pairs) },
            groups = d.groups.filter { g -> g.atoms.all { it in index } }.map { Group(it.atoms.map(index::getValue), it.charge) },
            multipliers = multipliers,
            em = em,
            times = times,
        )
    }
}
