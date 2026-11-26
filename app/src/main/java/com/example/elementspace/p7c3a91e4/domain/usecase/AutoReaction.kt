package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.BondMark
import com.example.elementspace.p7c3a91e4.domain.model.ConditionVisual
import com.example.elementspace.p7c3a91e4.domain.model.FrameAtom
import com.example.elementspace.p7c3a91e4.domain.model.FrameBond
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
import com.example.elementspace.p7c3a91e4.domain.model.ReactionStep
import com.example.elementspace.p7c3a91e4.domain.model.ReactionType
import com.example.elementspace.p7c3a91e4.domain.model.SchoolStage
import com.example.elementspace.p7c3a91e4.domain.model.Term
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Builds a reaction from its equation alone (M5): every formula unit is drawn as a small
 * schematic cluster, and the four steps show the atoms of the reactants regrouping into the
 * products — reactants, old bonds break, atoms separate, products with new bonds. The clusters
 * show which atoms belong together and that atoms are conserved, not real geometry.
 */
object AutoReaction {
    /** What the data row says; [visuals] and [condition] come from the condition key. */
    class Spec(
        val id: String,
        val equation: String,
        val condition: L10n?,
        val visuals: List<ConditionVisual>,
        val types: List<ReactionType>,
        val stages: Set<SchoolStage>,
        val source: L10n,
        val phenomenon: L10n,
    )

    fun parseSide(text: String): List<Term> =
        text.split(" + ").map { raw ->
            val t = raw.trim()
            val digits = t.takeWhile { it.isDigit() }
            Term(if (digits.isEmpty()) 1 else digits.toInt(), t.drop(digits.length))
        }

    fun build(spec: Spec): Reaction {
        val reversible = " <=> " in spec.equation
        val (left, right) = spec.equation.split(if (reversible) " <=> " else " = ").map(::parseSide)
        val pictures = frames(left, right)
        val leftCount = countText(ChemicalFormula.side(left))
        val rightCount = countText(ChemicalFormula.side(right))
        val conditionZh = spec.condition?.let { "条件：${it.zh}。" } ?: ""
        val conditionEn = spec.condition?.let { " Condition: ${it.en}." } ?: ""
        val steps =
            listOf(
                ReactionStep(L10n("反应物", "Reactants"), L10n("左边：${leftCount.zh}。每个球是一个原子，标着元素符号。", "Left: ${leftCount.en}. Each ball is one atom, labelled with its symbol."), pictures[0], 0),
                ReactionStep(L10n("旧键断开", "Old bonds break"), L10n("反应物里的化学键断开（红色虚线）。$conditionZh", "The bonds in the reactants break (red dashes).$conditionEn"), pictures[1], 0),
                ReactionStep(L10n("原子重新组合", "Atoms regroup"), L10n("示意：原子分开，再按生成物的方式组合。", "Schematic: the atoms come apart and regroup as the products."), pictures[2], 0),
                ReactionStep(
                    L10n("生成物", "Products"),
                    L10n("右边：${rightCount.zh}。原子的种类和个数都没变，只是重新组合（金色是新键）。", "Right: ${rightCount.en}. Same kinds and numbers of atoms, only regrouped (gold = new bonds)."),
                    pictures[3],
                    0,
                ),
            )
        return Reaction(
            id = spec.id,
            reactants = left,
            products = right,
            condition = spec.condition,
            types = spec.types,
            stages = spec.stages,
            redox = emptyList(),
            loseLabel = L10n("", ""),
            gainLabel = L10n("", ""),
            steps = steps,
            property = L10n(spec.types.joinToString("") { DEFINITIONS.getValue(it).zh }, spec.types.joinToString(" ") { DEFINITIONS.getValue(it).en }),
            phenomenon = spec.phenomenon,
            source = spec.source,
            kind = ReactionKind.BONDS,
            reversible = reversible,
            visuals = spec.visuals,
            conditionSteps = setOf(1, 2),
            schematic = true,
        )
    }

    private val DEFINITIONS =
        mapOf(
            ReactionType.COMBINATION to L10n("化合反应：两种或两种以上物质生成另一种物质。", "Combination: two or more substances form one."),
            ReactionType.DECOMPOSITION to L10n("分解反应：一种物质生成两种或两种以上其他物质。", "Decomposition: one substance gives two or more."),
            ReactionType.DISPLACEMENT to L10n("置换反应：一种单质和一种化合物生成另一种单质和另一种化合物。", "Displacement: an element and a compound give another element and another compound."),
            ReactionType.DOUBLE_DISPLACEMENT to L10n("复分解反应：两种化合物互相交换成分，生成另外两种化合物。", "Double displacement: two compounds swap partners."),
            ReactionType.REDOX to L10n("氧化还原反应：反应前后有元素的化合价发生变化。", "Redox: some element's oxidation state changes."),
            ReactionType.IONIC_BOND to L10n("", ""),
            ReactionType.COVALENT_BOND to L10n("", ""),
            ReactionType.SUBSTITUTION to L10n("", ""),
            ReactionType.ADDITION to L10n("", ""),
            ReactionType.ESTERIFICATION to L10n("", ""),
        )

    private fun countText(counts: Map<String, Int>): L10n {
        val ordered = counts.entries.sortedBy { it.key }
        return L10n(ordered.joinToString("，") { "${it.key} ${it.value} 个" }, ordered.joinToString(", ") { "${it.key} ${it.value}" })
    }

    // ---- layout ----------------------------------------------------------------------------

    private class Atom(val symbol: String, val group: Int, var p: Vec3 = Vec3.ZERO)

    private class Unit(val atoms: List<Atom>, val bonds: List<Pair<Int, Int>>) {
        /** Room it needs, including step 3 where it spreads by APART. */
        val radius: Float get() = atoms.maxOf { it.p.length() } * APART + 0.6f
    }

    private val NONMETALS = setOf("H", "He", "B", "C", "N", "O", "F", "Ne", "Si", "P", "S", "Cl", "Ar", "As", "Se", "Br", "Kr", "Te", "I", "Xe", "At", "Rn")

    /** Atoms that usually sit in the middle of their formula (S in SO₄, Mn in MnO₄, Xe in XeF₄ …). */
    private val CENTRES = listOf("S", "N", "C", "P", "Si", "B", "Cl", "Br", "I", "Se", "As", "Sb", "Te", "Xe", "Kr", "Rn", "Cr", "Mn", "V", "Mo", "W", "Re", "Tc", "Os", "Ru", "Ge", "Sn", "Pb", "Al", "Be", "Au", "Pt")

    private const val BOND = 1.5f
    private const val BOND_H = 1.05f

    /** Step 3 spreads each reactant cluster by this factor about its own centre. */
    private const val APART = 1.3f

    /** Space between clusters, ångström; leaves room for step 3's spread. */
    private const val GAP = 0.8f

    /** Atoms of one formula unit in order, with an id per parenthesised group copy (0 = main). */
    private fun atoms(formula: String): List<Atom> {
        val out = ArrayList<Atom>()
        var group = 0
        var i = 0
        fun number(): Int {
            val start = i
            while (i < formula.length && formula[i].isDigit()) i++
            return if (i == start) 1 else formula.substring(start, i).toInt()
        }
        fun symbol(): String {
            val start = i++
            while (i < formula.length && formula[i].isLowerCase()) i++
            return formula.substring(start, i)
        }
        while (i < formula.length) {
            if (formula[i] == '(') {
                i++
                val inner = ArrayList<Pair<String, Int>>()
                while (formula[i] != ')') inner += symbol() to number()
                i++
                repeat(number()) {
                    group++
                    inner.forEach { (s, n) -> repeat(n) { out += Atom(s, group) } }
                }
            } else {
                val s = symbol()
                repeat(number()) { out += Atom(s, 0) }
            }
        }
        return out
    }

    private fun centreOf(list: List<Atom>): Atom? {
        val heavy = list.filter { it.symbol != "H" && it.symbol != "O" }
        val singles = heavy.groupBy { it.symbol }.filterValues { it.size == 1 }.keys
        val pick = CENTRES.firstOrNull { it in singles } ?: singles.firstOrNull { it in NONMETALS } ?: singles.firstOrNull()
        if (pick != null) return heavy.first { it.symbol == pick }
        val oxygens = list.filter { it.symbol == "O" }
        return if (heavy.isEmpty() && oxygens.size == 1) oxygens.first() else null
    }

    /** Lays one formula unit out around the origin (ångström, z = 0). */
    private fun unit(formula: String): Unit {
        val atoms = atoms(formula)
        val bonds = ArrayList<Pair<Int, Int>>()
        if (atoms.size == 1) return Unit(atoms, bonds)
        fun idx(a: Atom) = atoms.indexOf(a)

        fun place(list: List<Atom>, centre: Atom?, origin: Vec3, start: Float, spread: Float) {
            val hs = list.filter { it.symbol == "H" && it != centre }
            val metalsOnO = if (centre != null && list.any { it.symbol == "O" && it != centre }) list.filter { it != centre && it.symbol != "O" && it.symbol != "H" && it.symbol !in NONMETALS } else emptyList()
            val ring = list.filter { it != centre && it.symbol != "H" && it !in metalsOnO }
            val oxygensHere = list.any { it.symbol == "O" && it != centre }
            // With no oxygen to carry them, H sits on the centre, spaced with its other neighbours (NH₄Cl, CH₄).
            val hOnCentre = centre != null && !oxygensHere
            if (centre != null) {
                centre.p = origin
                val around = if (hOnCentre) ring + hs else ring
                around.forEachIndexed { k, a ->
                    // A full circle starts on the x axis (CO₂ lies flat); a half fan is centred on its direction.
                    val t = start + spread * (k + if (spread > 6f) 0f else 0.5f) / around.size
                    a.p = origin + Vec3(cos(t), sin(t), 0f) * (if (a.symbol == "H") BOND_H else BOND)
                    bonds += idx(centre) to idx(a)
                }
                if (hOnCentre) return
            } else if (ring.isNotEmpty()) {
                // No single centre (Fe₂O₃, N₂, P₂O₅ …): a ring, alternating elements where it can.
                val byElement = ring.groupBy { it.symbol }.values.map { ArrayDeque(it) }
                val ordered = ArrayList<Atom>()
                while (byElement.any { it.isNotEmpty() }) byElement.forEach { q -> q.removeFirstOrNull()?.let { ordered += it } }
                val n = ordered.size
                val r = if (n <= 2) BOND / 2f else (BOND * 0.87f / 2f) / sin((PI / n).toFloat())
                ordered.forEachIndexed { k, a ->
                    val t = start + 2f * PI.toFloat() * k / n
                    a.p = origin + Vec3(cos(t), sin(t), 0f) * r
                }
                if (n == 2) bonds += idx(ordered[0]) to idx(ordered[1])
                else if (n > 2) for (k in 0 until n) bonds += idx(ordered[k]) to idx(ordered[(k + 1) % n])
            }
            // Metals of a salt hang on an oxygen (Cu on SO₄), pointing away from the centre.
            val oxygens = list.filter { it.symbol == "O" && it != centre }
            val metalHosts = HashSet<Atom>()
            metalsOnO.forEachIndexed { k, m ->
                val host = oxygens[k % oxygens.size]
                metalHosts += host
                m.p = host.p + direction(host.p - origin) * (BOND * 1.1f)
                bonds += idx(host) to idx(m)
            }
            // H on an O if there is one, else on the centre, else on a ring atom; H₂ is a pair.
            // An O that already holds a metal is left alone (NaHCO₃: Na on one O, H on another).
            val hosts = oxygens.filter { it !in metalHosts }.ifEmpty { oxygens }.ifEmpty { listOfNotNull(centre).ifEmpty { ring } }
            if (hosts.isEmpty()) {
                hs.forEachIndexed { k, h -> h.p = origin + Vec3(if (k == 0) -BOND_H / 2 else BOND_H / 2, 0f, 0f) }
                if (hs.size == 2) bonds += idx(hs[0]) to idx(hs[1])
                return
            }
            val assigned = hs.mapIndexed { k, h -> h to hosts[k % hosts.size] }
            val totals = assigned.groupingBy { it.second }.eachCount()
            val seen = HashMap<Atom, Int>()
            assigned.forEach { (h, host) ->
                val k = seen[host] ?: 0
                seen[host] = k + 1
                val out = host.p - origin
                val t =
                    if (out.length() < 1e-3f) start + k * 2f * PI.toFloat() / totals.getValue(host)
                    else atan2(out.y, out.x) + (k - (totals.getValue(host) - 1) / 2f) * 0.9f
                h.p = host.p + Vec3(cos(t), sin(t), 0f) * BOND_H
                bonds += idx(host) to idx(h)
            }
        }

        val main = atoms.filter { it.group == 0 }
        val groups = atoms.filter { it.group > 0 }.groupBy { it.group }.values.toList()
        val centre = centreOf(main)
        place(main, centre, Vec3.ZERO, 0f, 2f * PI.toFloat())
        groups.forEachIndexed { k, g ->
            val t = (PI / 2).toFloat() + 2f * PI.toFloat() * (k + 0.5f) / groups.size
            val dir = Vec3(cos(t), sin(t), 0f)
            val anchor = (centre ?: main.minByOrNull { (it.p - dir * 10f).length() })
            val gc = centreOf(g) ?: g.first { it.symbol != "H" }
            val origin = (anchor?.p ?: Vec3.ZERO) + dir * (BOND * if (g.size > 2) 2.2f else 1.3f)
            place(g, gc, origin, t - PI.toFloat() / 2f, PI.toFloat())
            // Oxoanions (NO₃) bind through an oxygen; OH, NH₄ and CO through their centre.
            val oxo = g.count { it.symbol == "O" } >= 3
            val via = if (oxo) g.filter { it.symbol == "O" }.minBy { (it.p - (anchor?.p ?: Vec3.ZERO)).length() } else gc
            if (anchor != null) bonds += idx(anchor) to idx(via)
        }
        // Centre the unit.
        val mid = atoms.fold(Vec3.ZERO) { a, b -> a + b.p } * (1f / atoms.size)
        atoms.forEach { it.p = it.p - mid }
        return Unit(atoms, bonds)
    }

    private fun direction(v: Vec3): Vec3 = if (v.length() < 1e-4f) Vec3(0f, 1f, 0f) else v * (1f / v.length())

    private class Placed(val unit: Unit, val offset: Vec3)

    /** Rows of at most three units, packed by radius; reactants left of x = 0, products right. */
    private fun side(terms: List<Term>, sign: Float): List<Placed> {
        val units = terms.flatMap { t -> List(t.coefficient) { unit(t.formula) } }
        val rows = units.chunked(3)
        val heights = rows.map { r -> 2f * r.maxOf { it.radius } + GAP }
        val widths = rows.map { r -> r.sumOf { (2f * it.radius + GAP).toDouble() }.toFloat() - GAP }
        val w = widths.max()
        var y = heights.sum() / 2f
        val out = ArrayList<Placed>()
        rows.forEachIndexed { ri, r ->
            var x = if (sign < 0) -1.4f - w + (w - widths[ri]) / 2f else 1.4f + (w - widths[ri]) / 2f
            val cy = y - heights[ri] / 2f
            r.forEach { u ->
                out += Placed(u, Vec3(x + u.radius, cy, 0f))
                x += 2f * u.radius + GAP
            }
            y -= heights[ri]
        }
        return out
    }

    /** The four frames, in scene units, with atom ids kept from reactants to products. */
    fun frames(left: List<Term>, right: List<Term>): List<ReactionFrame> {
        val l = side(left, -1f)
        val r = side(right, 1f)
        val all = (l + r).flatMap { pl -> pl.unit.atoms.map { pl.offset + it.p } } + l.flatMap { pl -> pl.unit.atoms.map { pl.offset + it.p * APART } }
        val minX = all.minOf { it.x } - 0.7f
        val maxX = all.maxOf { it.x } + 0.7f
        val minY = all.minOf { it.y } - 0.7f
        val maxY = all.maxOf { it.y } + 0.7f
        val shift = Vec3((minX + maxX) / 2f, (minY + maxY) / 2f, 0f)
        // Same size as the hand-made organic reactions (0.13 per Å) unless the picture must shrink to fit.
        // Half-width 0.7: the side card starts at x = 0.8 of the Volume and would hide anything further right.
        val scale = min(0.13f, min(1.4f / (maxX - minX), 1.6f / (maxY - minY)))

        // Reactant atom ids: a<unit>_<atom>.
        data class Slot(val id: String, val symbol: String, val unit: Int, val start: Vec3, val centre: Vec3)
        val slots = l.flatMapIndexed { ui, pl -> pl.unit.atoms.mapIndexed { ai, a -> Slot("a${ui}_$ai", a.symbol, ui, pl.offset + a.p, pl.offset) } }
        val startBonds = l.flatMapIndexed { ui, pl -> pl.unit.bonds.map { (i, j) -> FrameBond("a${ui}_$i", "a${ui}_$j") } }

        // Each product atom takes a reactant atom of the same element, preferring a reactant unit it already drew from.
        val used = HashSet<String>()
        val end = HashMap<String, Vec3>()
        val endBonds = ArrayList<FrameBond>()
        r.forEach { pl ->
            val taken = HashSet<Int>()
            val ids = pl.unit.atoms.map { a ->
                val slot = slots.firstOrNull { it.id !in used && it.symbol == a.symbol && it.unit in taken } ?: slots.first { it.id !in used && it.symbol == a.symbol }
                used += slot.id
                taken += slot.unit
                end[slot.id] = pl.offset + a.p
                slot.id
            }
            pl.unit.bonds.forEach { (i, j) -> endBonds += FrameBond(ids[i], ids[j], mark = BondMark.NEW) }
        }

        fun frame(positions: (Slot) -> Vec3, bonds: List<FrameBond>) =
            ReactionFrame(atoms = slots.map { FrameAtom(it.id, it.symbol, (positions(it) - shift) * scale) }, bonds = bonds, atomScale = scale)

        return listOf(
            frame({ it.start }, startBonds),
            frame({ it.start }, startBonds.map { it.copy(mark = BondMark.BREAK) }),
            frame({ it.centre + (it.start - it.centre) * APART }, emptyList()),
            frame({ end.getValue(it.id) }, endBonds),
        )
    }
}
