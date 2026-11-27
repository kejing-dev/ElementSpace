package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ElectronFlight
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.FrameParticle
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
import com.example.elementspace.p7c3a91e4.domain.model.ReactionStep
import com.example.elementspace.p7c3a91e4.domain.model.ReactionType
import com.example.elementspace.p7c3a91e4.domain.model.RedoxChange
import com.example.elementspace.p7c3a91e4.domain.model.SharedPair
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The electron view of a reaction between two elements (v1.16.0): the same four-step Bohr-model
 * picture as the hand-made Na + Cl₂, generated for every "element + element → one compound"
 * reaction in the library, so 碰一碰 can always show who loses and who gains electrons.
 *
 * Two pictures. TRANSFER (metal + non-metal): electrons fly from the metal to the non-metal and
 * the ions attract. SHARE (two non-metals, and the metal halides / oxides that are molecules):
 * atoms share electron pairs, which then lean toward the stronger electron taker.
 *
 * Electron counts come from oxidation states: the taker fills its outer shell (H 1, halogens 1,
 * O / S 2, N / P 3, C / Si 4) and the donors supply exactly that many. Only the outer shells are
 * drawn. The reaction itself (equation, phenomenon, source) is the library's; nothing is invented.
 */
class ElectronView(private val elements: Map<String, Element>, private val configuration: ElectronConfigurationUseCase = ElectronConfigurationUseCase()) {
    enum class Model { TRANSFER, SHARE }

    private enum class Oxygen { OXIDE, PEROXIDE, SUPEROXIDE }

    /** One element's part: how many atoms are drawn and how they come (single atoms or X₂). */
    private class Side(val element: Element, val atoms: Int, val diatomic: Boolean, val neutral: List<Int>) {
        val symbol: String get() = element.symbol
        val outer: Int get() = neutral.last()
    }

    fun model(reaction: Reaction): Model? = plan(reaction)?.model

    fun build(reaction: Reaction): Reaction? {
        val plan = plan(reaction) ?: return null
        val frames = SCALES.firstNotNullOfOrNull { s -> plan.frames(s).takeIf { fs -> fs.all { fits(it) } } } ?: plan.frames(SCALES.last())
        val steps = plan.texts(reaction).mapIndexed { i, (title, why) -> ReactionStep(title, why, frames[i], if (i >= 2) plan.moved else 0) }
        val bond = if (plan.model == Model.TRANSFER) ReactionType.IONIC_BOND else ReactionType.COVALENT_BOND
        return reaction.copy(
            id = reaction.id + SUFFIX,
            types = (reaction.types + ReactionType.REDOX + bond).distinct(),
            redox = plan.redox(reaction),
            loseLabel = if (plan.model == Model.TRANSFER) L10n("${plan.d.symbol} 失去 e⁻", "${plan.d.symbol} loses e⁻") else L10n("${plan.d.symbol} 偏离 e⁻", "e⁻ shift from ${plan.d.symbol}"),
            gainLabel = if (plan.model == Model.TRANSFER) L10n("${plan.t.symbol} 得到 e⁻", "${plan.t.symbol} gains e⁻") else L10n("${plan.t.symbol} 偏向 e⁻", "e⁻ shift to ${plan.t.symbol}"),
            steps = steps,
            kind = ReactionKind.ELECTRONS,
            conditionSteps = setOf(1),
            schematic = false,
            sketch = true,
        )
    }

    private fun fits(frame: ReactionFrame): Boolean {
        val ps = frame.particles
        val inside = ps.all { p -> val r = ReactionLayout.particleRadius(p) * frame.scale; abs(p.position.x) + r <= X_MAX + 1e-3f && abs(p.position.y) + r <= Y_MAX + 1e-3f }
        if (!inside) return false
        val bonded = frame.pairs.map { setOf(it.a, it.b) }.toSet()
        for (i in ps.indices) for (j in i + 1 until ps.size) {
            val need = (ReactionLayout.particleRadius(ps[i]) + ReactionLayout.particleRadius(ps[j])) * frame.scale
            val slack = if (setOf(ps[i].id, ps[j].id) in bonded) 0.80f else 0.98f
            if (ps[i].position.distanceTo(ps[j].position) < need * slack) return false
        }
        return true
    }

    // ---- what happens ------------------------------------------------------------------------

    private fun plan(reaction: Reaction): Plan? {
        if (reaction.kind == ReactionKind.ELECTRONS || reaction.reactants.size != 2 || reaction.products.size != 1) return null
        val parts = reaction.reactants.map { term -> ChemicalFormula.count(term.formula).entries.singleOrNull() ?: return null }
        val product = ChemicalFormula.count(reaction.products[0].formula)
        if (product.keys != parts.map { it.key }.toSet() || product.size != 2) return null
        val a = elements[parts[0].key] ?: return null
        val b = elements[parts[1].key] ?: return null
        val donor =
            when {
                a.isMetal() && b.isMetal() -> return null
                a.isMetal() || a.category == ElementCategory.NOBLE_GAS -> a
                b.isMetal() || b.category == ElementCategory.NOBLE_GAS -> b
                else -> if ((a.electronegativity ?: 0.0) <= (b.electronegativity ?: 0.0)) a else b
            }
        val taker = if (donor == a) b else a
        val need = NEED[taker.symbol] ?: return null
        val formula = reaction.products[0].formula
        var x = product.getValue(donor.symbol)
        var y = product.getValue(taker.symbol)
        val share = !donor.isMetal() || formula in MOLECULAR
        val oxygen =
            when {
                taker.symbol != "O" || donor.category != ElementCategory.ALKALI_METAL -> Oxygen.OXIDE
                y == 2 * x -> Oxygen.SUPEROXIDE
                y == x -> Oxygen.PEROXIDE
                else -> Oxygen.OXIDE
            }
        val cage = formula in CAGES
        val coefficient = reaction.products[0].coefficient
        val whole = coefficient == 1 || coefficient * (x + y) <= MAX_ATOMS
        val units = if (whole) coefficient else 1
        val electrons =
            when (oxygen) {
                Oxygen.OXIDE -> y * need
                Oxygen.PEROXIDE -> y
                Oxygen.SUPEROXIDE -> y / 2
            }
        val charges = (0 until x).map { i -> electrons / x + if (i < electrons % x) 1 else 0 }
        if (charges.any { it !in 1..8 }) return null
        if (share && (charges.distinct().size != 1 || (minOf(x, y) > 2 && !cage) || oxygen != Oxygen.OXIDE)) return null
        val diatomic = reaction.reactants.associate { term -> ChemicalFormula.count(term.formula).entries.single().let { it.key to (it.value == 2) } }
        return Plan(
            model = if (share) Model.SHARE else Model.TRANSFER,
            d = Side(donor, x * units, diatomic.getValue(donor.symbol), configuration(donor.atomicNumber).shells),
            t = Side(taker, y * units, diatomic.getValue(taker.symbol), configuration(taker.atomicNumber).shells),
            x = x, y = y, units = units, need = need, charges = charges, oxygen = oxygen, formula = formula, whole = whole, cage = cage, coefficient = coefficient,
        )
    }

    private fun Element.isMetal(): Boolean =
        category in setOf(ElementCategory.ALKALI_METAL, ElementCategory.ALKALINE_EARTH, ElementCategory.TRANSITION_METAL, ElementCategory.POST_TRANSITION, ElementCategory.LANTHANIDE, ElementCategory.ACTINIDE)

    private inner class Plan(
        val model: Model,
        val d: Side,
        val t: Side,
        /** Donor and taker atoms in one formula unit, and how many units are drawn. */
        val x: Int,
        val y: Int,
        val units: Int,
        val need: Int,
        /** Electrons each donor atom of one unit gives (they differ in mixed-valence oxides such as Fe3O4). */
        val charges: List<Int>,
        val oxygen: Oxygen,
        val formula: String,
        /** The picture has exactly the atoms of the balanced equation. */
        val whole: Boolean,
        /** As4O6 / Sb4O6: four atoms joined through six bridging O, single bonds only. */
        val cage: Boolean,
        val coefficient: Int,
    ) {
        /** Electrons moved in the whole balanced equation (the picture may show one unit of it). */
        val equationMoved: Int get() = charges.sum() * coefficient
        val moved: Int get() = charges.sum() * units

        private fun charge(donorIndex: Int) = charges[donorIndex % x]

        /** Bonds donor → taker inside one unit (SHARE): the electron pairs each pair of atoms shares. */
        private val bonds: List<Triple<Int, Int, Int>> by lazy {
            when {
                // Three corners and the middle: every pair of the four atoms is bridged by one O.
                cage -> listOf(0 to 1, 1 to 2, 2 to 0, 0 to 3, 1 to 3, 2 to 3).flatMapIndexed { o, (a, b) -> listOf(Triple(a, o, 1), Triple(b, o, 1)) }
                formula == "CO" -> listOf(Triple(0, 0, 3))
                x == 1 && y == 1 && charges[0] > 3 -> listOf(Triple(0, 0, 1))
                else -> {
                    val out = ArrayList<Triple<Int, Int, Int>>()
                    var di = 0
                    var left = charges[0]
                    for (ti in 0 until y) {
                        var want = need
                        while (want > 0 && di < x) {
                            val k = minOf(want, left)
                            if (k > 0) out.add(Triple(di, ti, k))
                            want -= k
                            left -= k
                            if (left == 0) { di++; left = charges.getOrElse(di) { 0 } }
                        }
                    }
                    out
                }
            }
        }

        /** A network solid drawn as one unit with a single pair (SiC): each atom really has four neighbours. */
        private val network: Boolean get() = model == Model.SHARE && formula != "CO" && x == 1 && y == 1 && charges[0] > 3

        // ---- shells --------------------------------------------------------------------------

        private fun padded(shells: List<Int>, size: Int) = shells + List((size - shells.size).coerceAtLeast(0)) { 0 }

        private fun donorIon(q: Int): List<Int> = padded(configuration(d.element.atomicNumber, q).shells, d.neutral.size)

        /** First shell that is drawn for the donor: the outer two, or more when an inner shell loses electrons. */
        private val dFirst: Int by lazy {
            val changed = charges.distinct().minOf { q -> val ion = donorIon(if (network) 1 else q); d.neutral.indices.first { d.neutral[it] != ion[it] } }
            minOf(changed, d.neutral.lastIndex - 1).coerceAtLeast(0)
        }
        private val tFirst: Int get() = (t.neutral.lastIndex - 1).coerceAtLeast(0)

        private fun donor(i: Int, shells: List<Int>, at: Vec3, tint: ChargeTint = ChargeTint.NONE, vacancies: Int = 0) =
            FrameParticle("${d.symbol}${i + 1}", d.symbol, shells.drop(dFirst), at, tint, vacancies = vacancies, hidden = d.neutral.take(dFirst).sum())

        private fun taker(i: Int, outer: Int, at: Vec3, tint: ChargeTint = ChargeTint.NONE, vacancies: Int = 0, gold: Int = 0, label: String? = null): FrameParticle {
            val shells = t.neutral.dropLast(1) + outer
            return FrameParticle(
                "${t.symbol}${i + 1}", t.symbol, shells.drop(tFirst), at, tint,
                gold = if (gold > 0) mapOf(shells.lastIndex - tFirst to gold) else emptyMap(), vacancies = vacancies, hidden = t.neutral.take(tFirst).sum(), label = label,
            )
        }

        private fun radius(side: Side, first: Int) = ReactionLayout.shellRadius(side.neutral.size - first - 1)

        // ---- frames --------------------------------------------------------------------------

        fun frames(s: Float): List<ReactionFrame> = if (model == Model.TRANSFER) transfer(s) else share(s)

        /** Pairs of an X₂ molecule: electrons each atom puts in, and the pair count drawn. */
        private fun bondOf(symbol: String): Int = when (symbol) { "N" -> 3; "O" -> 2; else -> 1 }

        private fun reactants(s: Float, dAt: List<Vec3>, tAt: List<Vec3>): ReactionFrame {
            val dk = if (d.diatomic) bondOf(d.symbol) else 0
            val tk = if (t.diatomic) bondOf(t.symbol) else 0
            val dPaired = if (d.diatomic) d.atoms / 2 * 2 else 0
            val tPaired = if (t.diatomic) t.atoms / 2 * 2 else 0
            val ds = dAt.indices.map { i -> donor(i, if (i < dPaired) d.neutral.dropLast(1) + (d.outer - dk) else d.neutral, dAt[i]) }
            val ts = tAt.indices.map { i -> taker(i, if (i < tPaired) t.outer - tk else t.outer, tAt[i]) }
            val pairs =
                (0 until dPaired / 2).map { SharedPair(ds[2 * it].id, ds[2 * it + 1].id, count = 2 * dk) } +
                    (0 until tPaired / 2).map { SharedPair(ts[2 * it].id, ts[2 * it + 1].id, count = 2 * tk) }
            return ReactionFrame(particles = ds + ts, pairs = pairs, scale = s)
        }

        private fun transfer(s: Float): List<ReactionFrame> {
            val rd = radius(d, dFirst) * s
            val rt = radius(t, tFirst) * s
            val keep = oxygen != Oxygen.OXIDE
            val dGrid = grid(d.atoms, rd, s, -1, d.diatomic)
            val tGrid = grid(t.atoms, rt, s, 1, t.diatomic)
            val tPairs = if (t.diatomic) t.atoms / 2 * 2 else 0
            val tClose = closed(tGrid, tPairs, rt)
            val dPairs = if (d.diatomic) d.atoms / 2 * 2 else 0
            val first = reactants(s, closed(dGrid, dPairs, rd), tClose)
            val tAt = if (keep) tClose else tGrid

            // What each taker atom takes: 2 for an oxide; in a peroxide 1 each, in a superoxide 1 per O₂.
            val takes = (0 until t.atoms).map { i -> when (oxygen) { Oxygen.OXIDE -> need; Oxygen.PEROXIDE -> 1; Oxygen.SUPEROXIDE -> if (i % 2 == 0) 1 else 0 } }
            val open = if (keep) t.outer - 1 else t.outer
            val keptPairs = if (keep) (0 until t.atoms / 2).map { SharedPair("${t.symbol}${2 * it + 1}", "${t.symbol}${2 * it + 2}", count = 2) } else emptyList()
            val apart = (0 until d.atoms).map { donor(it, d.neutral, dGrid[it]) } + (0 until t.atoms).map { taker(it, open, tAt[it], vacancies = takes[it]) }
            val second = ReactionFrame(particles = apart, pairs = keptPairs, scale = s)

            val flights = ArrayList<ElectronFlight>()
            val wants = takes.toMutableList()
            for (di in 0 until d.atoms) {
                var have = charge(di)
                // Prefer the taker in the same row, so the flights do not all cross.
                val order = wants.indices.sortedBy { abs(tAt[it].y - dGrid[di].y) }
                for (ti in order) {
                    val k = minOf(have, wants[ti])
                    if (k > 0) { flights.add(ElectronFlight("${d.symbol}${di + 1}", "${t.symbol}${ti + 1}", k)); have -= k; wants[ti] -= k }
                }
            }
            val giving = (0 until d.atoms).map { donor(it, donorIon(charge(it)), dGrid[it]) } + (0 until t.atoms).map { taker(it, open, tAt[it], vacancies = takes[it]) }
            val third = ReactionFrame(particles = giving, pairs = keptPairs, flights = flights, scale = s)

            // Ions drawn together: both groups slide toward the middle until they almost touch.
            val ionShells = (0 until d.atoms).map { donorIon(charge(it)).dropLastWhile { e -> e == 0 } }
            val gapNow = (tAt.minOf { it.x } - rt) - (dGrid.maxOf { it.x } + ionShells.maxOf { ReactionLayout.shellRadius((it.size - dFirst - 1).coerceAtLeast(0)) } * s)
            val shift = Vec3(((gapNow - 0.03f * s) / 2f).coerceAtLeast(0f), 0f, 0f)
            val label = when (oxygen) { Oxygen.PEROXIDE -> "O⁻"; Oxygen.SUPEROXIDE -> "O"; Oxygen.OXIDE -> null }
            val ions =
                (0 until d.atoms).map { donor(it, ionShells[it], dGrid[it] + shift, ChargeTint.POSITIVE) } +
                    (0 until t.atoms).map { taker(it, open + takes[it], tAt[it] - shift, ChargeTint.NEGATIVE, gold = takes[it], label = label) }
            return listOf(first, second, third, ReactionFrame(particles = ions, pairs = keptPairs, scale = s))
        }

        private fun share(s: Float): List<ReactionFrame> {
            val rd = radius(d, dFirst) * s
            val rt = radius(t, tFirst) * s
            val dGrid = grid(d.atoms, rd, s, -1, d.diatomic)
            val tGrid = grid(t.atoms, rt, s, 1, t.diatomic)
            val first = reactants(s, closed(dGrid, if (d.diatomic) d.atoms / 2 * 2 else 0, rd), closed(tGrid, if (t.diatomic) t.atoms / 2 * 2 else 0, rt))
            val dVacant = if (d.symbol == "H") 1 else 0
            val second =
                ReactionFrame(
                    particles = (0 until d.atoms).map { donor(it, d.neutral, dGrid[it], vacancies = dVacant) } + (0 until t.atoms).map { taker(it, t.outer, tGrid[it], vacancies = need) },
                    scale = s,
                )

            val (dAt, tAt) = molecules(rd, rt)
            val given = if (network) 1 else charges[0]
            // Electrons the taker puts into its bonds: one per pair, except CO where O gives four of the six.
            val takerGives = if (formula == "CO") 4 else if (network) 1 else need
            val donorGives = if (formula == "CO") 2 else given
            fun product(lean: Boolean): ReactionFrame {
                val ds = (0 until d.atoms).map { donor(it, donorIon(donorGives), dAt[it], if (lean) ChargeTint.PARTIAL_POSITIVE else ChargeTint.NONE) }
                val ts = (0 until t.atoms).map { taker(it, t.outer - takerGives, tAt[it], if (lean) ChargeTint.PARTIAL_NEGATIVE else ChargeTint.NONE) }
                val pairs =
                    (0 until units).flatMap { u ->
                        bonds.map { (di, ti, k) -> SharedPair(ds[u * x + di].id, ts[u * y + ti].id, bias = if (lean) 0.62f else 0.5f, count = 2 * k) }
                    }
                return ReactionFrame(particles = ds + ts, pairs = pairs, scale = s)
            }
            return listOf(first, second, product(false), product(true))
        }

        /** Atoms of one side in rows, centred on its half of the Volume; [side] −1 = left, +1 = right. */
        private fun grid(count: Int, r: Float, s: Float, side: Int, paired: Boolean): List<Vec3> {
            val cell = 2f * r + 0.05f * s
            val rowsMax = max(1, ((2f * Y_MAX) / cell).toInt())
            var cols = if (paired) 2 else 1
            while (ceil(count / cols.toFloat()).toInt() > rowsMax) cols += if (paired) 2 else 1
            val rows = ceil(count / cols.toFloat()).toInt()
            val centre = side * (SIDE_GAP + SIDE_WIDTH / 2f)
            return (0 until count).map { i ->
                val row = i / cols
                val col = i % cols
                Vec3(centre + (col - (cols - 1) / 2f) * cell, ((rows - 1) / 2f - row) * cell, 0f)
            }
        }

        /** The first [paired] atoms pulled together two by two, so each X₂ shows its shared pairs between the rings. */
        private fun closed(at: List<Vec3>, paired: Int, r: Float): List<Vec3> =
            at.mapIndexed { i, p ->
                if (i >= paired) return@mapIndexed p
                val mate = at[if (i % 2 == 0) i + 1 else i - 1]
                val mid = (p + mate) * 0.5f
                val half = r * BOND
                Vec3(mid.x + if (p.x < mate.x) -half else half, p.y, 0f)
            }

        /** Product molecules stacked in the middle: where every donor and taker atom ends up. */
        private fun molecules(rd: Float, rt: Float): Pair<List<Vec3>, List<Vec3>> {
            // The element with fewer atoms sits in the middle of the molecule.
            val donorCentre = x <= y
            val centres = if (donorCentre) x else y
            val arms = if (donorCentre) y else x
            val rc = if (donorCentre) rd else rt
            val ra = if (donorCentre) rt else rd
            // Arm j belongs to which centres.
            val owners = (0 until arms).map { j -> bonds.filter { (di, ti, _) -> (if (donorCentre) ti else di) == j }.map { (di, ti, _) -> if (donorCentre) di else ti }.distinct() }
            val centreAt: List<Vec3>
            val armAt = arrayOfNulls<Vec3>(arms)
            if (cage) {
                val reach = (rc + ra) * BOND
                centreAt = listOf(90f, 210f, 330f).map { polar(2f * reach, it) } + Vec3.ZERO
                // O on a spoke sits halfway; O on an outer edge is pushed outward so it is not mistaken for a neighbour of the middle atom.
                for (j in 0 until arms) armAt[j] = (centreAt[owners[j][0]] + centreAt[owners[j][1]]) * (if (3 in owners[j]) 0.5f else 0.85f)
            } else if (centres == 1) {
                centreAt = listOf(Vec3.ZERO)
                val reach = max((rc + ra) * BOND, if (arms > 2) ra * 1.04f / sin(PI.toFloat() / arms) else 0f)
                val start = when (arms) { 1 -> 0f; 2 -> 180f; 3 -> 90f; 4 -> 45f; 5 -> 90f; else -> 0f }
                for (j in 0 until arms) armAt[j] = polar(reach, start + 360f * j / arms)
            } else {
                val reach = (rc + ra) * BOND
                centreAt = listOf(Vec3(-reach, 0f, 0f), Vec3(reach, 0f, 0f))
                for (c in 0..1) {
                    val own = (0 until arms).filter { owners[it] == listOf(c) }
                    val step = if (own.size <= 2) 110f else 80f
                    own.forEachIndexed { k, j -> armAt[j] = centreAt[c] + polar(reach, (if (c == 0) 180f else 0f) + (k - (own.size - 1) / 2f) * step) }
                }
                (0 until arms).filter { owners[it].size == 2 }.forEach { armAt[it] = Vec3.ZERO }
            }
            val one = centreAt + armAt.map { it ?: Vec3.ZERO }
            val top = one.maxOf { it.y } + max(rc, ra)
            val bottom = one.minOf { it.y } - max(rc, ra)
            val right = one.maxOf { it.x } + max(rc, ra)
            val left = one.minOf { it.x } - max(rc, ra)
            val pitch = (top - bottom) + 0.04f
            val stride = (right - left) + 0.04f
            // Several molecules stand side by side when that fits, else one above the other.
            val sideBySide = units * stride - 0.04f <= 2f * X_MAX
            val dAt = ArrayList<Vec3>()
            val tAt = ArrayList<Vec3>()
            for (u in 0 until units) {
                val k = (units - 1) / 2f - u
                val lift = if (sideBySide) Vec3(-k * stride - (left + right) / 2f, -(top + bottom) / 2f, 0f) else Vec3(-(left + right) / 2f, k * pitch - (top + bottom) / 2f, 0f)
                val cs = centreAt.map { it + lift }
                val arm = armAt.map { (it ?: Vec3.ZERO) + lift }
                if (donorCentre) { dAt += cs; tAt += arm } else { tAt += cs; dAt += arm }
            }
            return dAt to tAt
        }

        private fun polar(r: Float, degrees: Float): Vec3 {
            val a = degrees * PI.toFloat() / 180f
            return Vec3(r * cos(a), r * sin(a), 0f)
        }

        // ---- words ---------------------------------------------------------------------------

        fun redox(reaction: Reaction): List<RedoxChange> {
            if (charges.distinct().size != 1) return emptyList()
            val dTerm = reaction.reactants.first { d.symbol in ChemicalFormula.count(it.formula) }
            val tTerm = reaction.reactants.first { t.symbol in ChemicalFormula.count(it.formula) }
            val dAtoms = dTerm.coefficient * ChemicalFormula.count(dTerm.formula).getValue(d.symbol)
            val tAtoms = tTerm.coefficient * ChemicalFormula.count(tTerm.formula).getValue(t.symbol)
            val total = charges[0] * dAtoms
            if (total % tAtoms != 0) return emptyList()
            return listOf(RedoxChange(d.symbol, 0, charges[0], dAtoms), RedoxChange(t.symbol, 0, -(total / tAtoms), tAtoms))
        }

        private fun full(symbol: String) = if (symbol == "H") 2 else 8

        private fun mol(side: Side) = if (side.diatomic) "${side.symbol}₂" else side.symbol

        private fun moleculeLine(side: Side): L10n? {
            if (!side.diatomic) return null
            val k = bondOf(side.symbol)
            val zh = "${side.symbol}₂ 分子里两个 ${side.symbol} 共用 $k 对电子" + when (k) { 2 -> "（双键）。"; 3 -> "（三键）。"; else -> "。" }
            val en = " In ${side.symbol}₂ the two ${side.symbol} atoms share $k pair${if (k == 1) "" else "s"}" + when (k) { 2 -> " (a double bond)."; 3 -> " (a triple bond)."; else -> "." }
            return L10n(zh, en)
        }

        private fun pictureLine(): L10n {
            val hidden = dFirst > 0 || tFirst > 0
            val shellsZh = if (hidden) "为了看清，只画最外面的电子层。" else ""
            val shellsEn = if (hidden) " Only the outer shells are drawn." else ""
            if (whole) return L10n(shellsZh, shellsEn)
            val odd = t.diatomic && t.atoms % 2 == 1
            return L10n(
                "这里只画一份 ${pretty(formula)}：${d.atoms} 个 ${d.symbol} 和 ${t.atoms} 个 ${t.symbol}" + (if (odd) "（单独的那个 ${t.symbol} 来自另一个 ${t.symbol}₂ 分子）" else "") + "。" + shellsZh,
                " The picture shows one ${pretty(formula)} unit: ${d.atoms} ${d.symbol} and ${t.atoms} ${t.symbol}" + (if (odd) " (the single ${t.symbol} comes from another ${t.symbol}₂ molecule)" else "") + "." + shellsEn,
            )
        }

        private fun oxidation(values: List<Int>, sign: String) = values.distinct().sorted().joinToString("、") { "$sign$it" }

        private fun oxidationEn(values: List<Int>, sign: String) = values.distinct().sorted().joinToString(" / ") { "$sign$it" }

        /** English name for a symbol that would read as a word ("I has 7 …"); the symbol otherwise. */
        private fun en(side: Side, start: Boolean = false): String =
            if (side.symbol in WORDS) side.element.nameEn.let { if (start) it.replaceFirstChar { c -> c.uppercase() } else it.lowercase() } else side.symbol

        private fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

        /** "点燃，" in the title for the short conditions; longer ones go into the explanation. */
        private fun conditionTitle(reaction: Reaction): L10n =
            reaction.condition?.takeIf { it.zh.length <= 2 }?.let { L10n("${it.zh}，", it.en.replaceFirstChar { c -> c.uppercase() } + ": ") } ?: L10n("", "")

        private fun conditionNote(reaction: Reaction): L10n =
            reaction.condition?.takeIf { it.zh.length > 2 }?.let { L10n("条件：${it.zh}。", " Condition: ${it.en}.") } ?: L10n("", "")

        /** How deep the donor reaches for the electrons it gives: 0 outer shell only, 1 the next shell in, 2 deeper. */
        private fun depth(): Int {
            val ion = donorIon(if (network) 1 else charges.max())
            return d.neutral.lastIndex - d.neutral.indices.first { d.neutral[it] != ion[it] }
        }

        private fun equationLine(reaction: Reaction, verbZh: String, verbEn: String): L10n {
            if (whole) return L10n("", "")
            val lhs = reaction.reactants.joinToString(" + ") { (if (it.coefficient == 1) "" else it.coefficient.toString()) + pretty(it.formula) }
            return L10n("图中这一份是 $moved e⁻；按方程式 $lhs 一共$verbZh $equationMoved e⁻。", " That is $moved e⁻ for the unit drawn; for the equation $lhs it is $equationMoved e⁻ $verbEn.")
        }

        fun texts(reaction: Reaction): List<Pair<L10n, L10n>> {
            val ds = d.symbol
            val ts = t.symbol
            val dE = en(d)
            val tE = en(t)
            val dStart = en(d, start = true)
            val tStart = en(t, start = true)
            val lhs = reaction.reactants.joinToString(" + ") { (if (it.coefficient == 1) "" else it.coefficient.toString()) + pretty(it.formula) }
            val cond = conditionTitle(reaction)
            val note = conditionNote(reaction)
            val picture = pictureLine()
            val molecules = listOfNotNull(moleculeLine(d), moleculeLine(t))
            val moleculesZh = molecules.joinToString("") { it.zh }
            val moleculesEn = molecules.joinToString("") { it.en }
            val title0 = L10n("反应物：$lhs", "Reactants: $lhs")
            val takerFull = full(ts)
            val splitNames = listOfNotNull(mol(d).takeIf { d.diatomic }, mol(t).takeIf { t.diatomic })
            val deep = depth()
            val innerZh = if (deep >= 2) "更里面电子层" else "里面一层"
            val innerEn = if (deep >= 2) "a deeper shell" else "the next shell in"

            if (model == Model.TRANSFER) {
                val active = d.element.category in setOf(ElementCategory.ALKALI_METAL, ElementCategory.ALKALINE_EARTH) || ds == "Al"
                val most = charges.max()
                val donorZh: String
                val donorEn: String
                when {
                    most > d.outer -> {
                        donorZh = "$ds 是金属，最外层有 ${d.outer} 个电子，反应时失去；它还能再失去${innerZh}的电子。"
                        donorEn = "$dStart is a metal with ${plural(d.outer, "outer electron")}, which it loses in the reaction; it can also lose some from $innerEn. "
                    }
                    most < d.outer -> {
                        donorZh = "$ds 是金属，最外层有 ${d.outer} 个电子，这里失去其中 $most 个。"
                        donorEn = "$dStart is a metal with ${d.outer} outer electrons and gives up $most of them here. "
                    }
                    active -> {
                        donorZh = "$ds 是金属，最外层只有 ${d.outer} 个电子，容易失去。"
                        donorEn = "$dStart is a metal with only ${plural(d.outer, "outer electron")}, easily lost. "
                    }
                    else -> {
                        donorZh = "$ds 是金属，最外层有 ${d.outer} 个电子，反应时失去。"
                        donorEn = "$dStart is a metal with ${plural(d.outer, "outer electron")}, which it loses in the reaction. "
                    }
                }
                val takerZh =
                    when (oxygen) {
                        Oxygen.OXIDE -> "$ts 最外层 ${t.outer} 个，再得 $need 个就排满 $takerFull 个。"
                        else -> "O 最外层 6 个，容易得电子。"
                    }
                val takerEn =
                    when (oxygen) {
                        Oxygen.OXIDE -> "$tStart has ${t.outer} and needs $need more to reach $takerFull."
                        else -> "O has 6 and takes electrons readily."
                    }
                val why0 = L10n(donorZh + takerZh + moleculesZh + picture.zh, donorEn + takerEn + moleculesEn + picture.en)

                val step1 =
                    when {
                        oxygen == Oxygen.PEROXIDE ->
                            L10n("${cond.zh}O₂ 的双键打开一根", "${cond.en}one of the two O=O bonds breaks") to
                                L10n(
                                    "两个 O 之间还留着一对共用电子，所以它们不分开。每个 O 还差 1 个电子（金色虚圈是空位）。" + note.zh,
                                    "The two O atoms keep one shared pair, so they stay together. Each O is 1 electron short (gold rings mark the slots).${note.en}",
                                )
                        oxygen == Oxygen.SUPEROXIDE ->
                            L10n("${cond.zh}O₂ 准备接 1 个电子", "${cond.en}O₂ gets ready to take 1 electron") to
                                L10n(
                                    "O₂ 的双键打开一根，两个 O 仍然连在一起。超氧化物里，整个 O₂ 只接 1 个电子（金色虚圈是空位）。" + note.zh,
                                    "One of the two O=O bonds breaks, and the two O atoms stay joined. In a superoxide the whole O₂ takes just 1 electron (the gold ring marks the slot).${note.en}",
                                )
                        t.diatomic ->
                            L10n("${cond.zh}${ts}₂ 拆成 $ts 原子", "${cond.en}${ts}₂ splits into $ts atoms") to
                                L10n(
                                    "分子里的两个 $ts 先分开，才能各自接电子。金色虚圈是空位：每个 $ts 还差 $need 个电子。" + note.zh,
                                    "The two $ts atoms separate first so each can take electrons. Gold rings mark the slots: each $tE atom is ${plural(need, "electron")} short.${note.en}",
                                )
                        else ->
                            L10n("${cond.zh}$ts 原子准备接电子", "${cond.en}$tE gets ready to take electrons") to
                                L10n(
                                    "金色虚圈是空位：每个 $ts 原子还差 $need 个电子。" + note.zh,
                                    "Gold rings mark the slots: each $tE atom is ${plural(need, "electron")} short.${note.en}",
                                )
                    }

                val up = oxidation(charges, "+")
                val upEn = oxidationEn(charges, "+")
                val down = when (oxygen) { Oxygen.OXIDE -> "−$need"; Oxygen.PEROXIDE -> "−1"; Oxygen.SUPEROXIDE -> "−½" }
                val target = if (oxygen == Oxygen.OXIDE) ts else "O₂"
                val targetEn = if (oxygen == Oxygen.OXIDE) tE else "O₂"
                val each =
                    if (charges.distinct().size == 1) L10n("每个 $ds 失 ${charges[0]} e⁻ 给 $target", "Each $dE atom gives ${charges[0]} e⁻ to $targetEn")
                    else L10n("$ds 一共失去 $moved e⁻ 给 $target", "$dStart gives $moved e⁻ to $targetEn in all")
                val equation = equationLine(reaction, "转移", "transferred")
                val why2 =
                    L10n(
                        "失电子 → 化合价升高（0 → $up），$ds 被氧化；得电子 → 化合价降低（0 → $down），$ts 被还原。失去的 $moved e⁻ = 得到的 $moved e⁻。" +
                            (if (charges.distinct().size > 1) "这种氧化物里的 $ds 有两种化合价。" else "") + equation.zh,
                        "Losing electrons raises the oxidation state (0 → $upEn): $dE is oxidised. Gaining lowers it (0 → $down): $tE is reduced. $moved e⁻ lost = $moved e⁻ gained." +
                            (if (charges.distinct().size > 1) " The $dE atoms in this oxide have two different oxidation states." else "") + equation.en,
                    )

                val dIons = charges.distinct().sorted().joinToString("、") { AtomLabelText.ion(ds, it) }
                val tIon = when (oxygen) { Oxygen.OXIDE -> AtomLabelText.ion(ts, -need); Oxygen.PEROXIDE -> "O₂²⁻"; Oxygen.SUPEROXIDE -> "O₂⁻" }
                val partly = !(d.element.category in setOf(ElementCategory.ALKALI_METAL, ElementCategory.ALKALINE_EARTH, ElementCategory.LANTHANIDE, ElementCategory.ACTINIDE)) || ds == "Be" || ts in setOf("Si", "C")
                val caveatZh = if (partly) "实际上这种化合物里的键带有共价成分，电子没有完全转移；化合价按完全转移来记。" else ""
                val caveatEn = if (partly) " In reality these bonds are partly covalent, so the transfer is not complete; oxidation states are counted as if it were." else ""
                val fullZh =
                    when (oxygen) {
                        Oxygen.OXIDE -> "$tIon 最外层排满了 $takerFull 个电子。"
                        Oxygen.PEROXIDE -> "O₂²⁻ 叫过氧根：两个 O 靠一对共用电子连在一起，各带 1 个负电荷。"
                        Oxygen.SUPEROXIDE -> "O₂⁻ 叫超氧根：两个 O 连在一起，一共只带 1 个负电荷。"
                    }
                val fullEn =
                    when (oxygen) {
                        Oxygen.OXIDE -> "$tIon now has a full outer shell of $takerFull."
                        Oxygen.PEROXIDE -> "O₂²⁻ is the peroxide ion: two O atoms joined by one shared pair, one negative charge each."
                        Oxygen.SUPEROXIDE -> "O₂⁻ is the superoxide ion: two O atoms joined together carrying one negative charge between them."
                    }
                val title3 = L10n("$dIons 和 $tIon 吸引，生成 ${pretty(formula)}", "${dIons.replace("、", " / ")} and $tIon attract: ${pretty(formula)} forms")
                val why3 = L10n(fullZh + "正负离子靠静电吸引结合，这就是离子键。" + caveatZh, "$fullEn Oppositely charged ions are held together by electrostatic attraction; this is an ionic bond.$caveatEn")
                return listOf(title0 to why0, step1, each to why2, title3 to why3)
            }

            // SHARE
            val given = charges[0]
            val deeperZh = if (!network && given > d.outer) "（还会拿出${innerZh}的电子来共用）" else ""
            val deeperEn = if (!network && given > d.outer) " (it also shares electrons from $innerEn)" else ""
            val why0 =
                L10n(
                    "$ds 最外层 ${d.outer} 个电子$deeperZh；$ts 最外层 ${t.outer} 个，再得 $need 个就排满 $takerFull 个。" + moleculesZh + picture.zh,
                    "$dStart has ${plural(d.outer, "outer electron")}$deeperEn; $tE has ${t.outer} and needs $need more to reach $takerFull.$moleculesEn${picture.en}",
                )
            val slotsZh = if (ds == "H") "金色虚圈是空位：每个 H 还差 1 个电子，每个 $ts 还差 $need 个。" else "金色虚圈是空位：每个 $ts 还差 $need 个电子。"
            val slotsEn = if (ds == "H") "Gold rings mark the slots: each H is 1 electron short and each $tE atom ${plural(need, "electron")} short." else "Gold rings mark the slots: each $tE atom is ${plural(need, "electron")} short."
            val step1 =
                if (splitNames.isNotEmpty()) {
                    L10n("${cond.zh}${splitNames.joinToString("、")} 拆成原子", "${cond.en}${splitNames.joinToString(" and ")} split${if (splitNames.size == 1) "s" else ""} into atoms") to
                        L10n("旧的共用电子对断开，原子才能重新搭配。$slotsZh${note.zh}", "The old shared pairs break so the atoms can pair up anew. $slotsEn${note.en}")
                } else {
                    L10n("${cond.zh}$ds 和 $ts 原子靠近", "${cond.en}$dE and $tE atoms come together") to L10n(slotsZh + note.zh, slotsEn + note.en)
                }
            val orders = bonds.map { it.third }.distinct().sorted()
            val orderZh = if (orders.size == 1) "每个 $ds—$ts 之间共用 ${orders[0]} 对。" else "有的 $ds—$ts 之间共用 ${orders.first()} 对，有的共用 ${orders.last()} 对。"
            val orderEn = if (orders.size == 1) " Each $ds–$ts bond is ${plural(orders[0], "shared pair")}." else " Some $ds–$ts bonds are ${plural(orders.first(), "shared pair")}, others ${orders.last()}."
            val special =
                when {
                    formula == "CO" -> L10n("三对里有一对是 O 单独拿出来的（配位键）。", " One of the three pairs comes from O alone (a dative bond).")
                    formula == "NO" -> L10n("NO 里的 N 还剩 1 个单电子，没有排满 8 个，所以 NO 很活泼。", " The N in NO is left with one unpaired electron and does not reach 8, which is why NO is so reactive.")
                    formula == "WC" -> L10n("图里只画了一对：碳化钨是很硬的晶体，每个 W 周围有 6 个 C，键里还带有金属键的成分。", " Only one pair is drawn: tungsten carbide is a very hard crystal in which every W has 6 C neighbours, and its bonding is partly metallic.")
                    network -> L10n("图里只画了一对：实际晶体里每个原子和周围 4 个原子各共用一对，连成一片。", " Only one pair is drawn: in the real crystal every atom shares a pair with each of 4 neighbours, forming a network.")
                    formula in NETWORK ->
                        L10n(
                            "图里画成 ${orders.last()} 对只是为了数电子：实际晶体里每个 $ds 和周围 ${if (y >= x) given else need} 个 $ts 各共用 1 对，原子连成一片。",
                            " ${orders.last()} pairs are drawn only to count the electrons: in the real crystal every $dE atom shares 1 pair with each of ${if (y >= x) given else need} $tE atoms, forming a network.",
                        )
                    cage -> L10n("${pretty(formula)} 是笼状分子：4 个 $ds 两两之间都隔着一个 O。", " ${pretty(formula)} is a cage: every two of the 4 $dE atoms are bridged by one O.")
                    d.element.category == ElementCategory.NOBLE_GAS -> L10n("$ds 的最外层本来已经排满，只有 F 这类得电子能力极强的元素才能和它共用电子。", " $dStart already has a full outer shell; only an extremely strong electron taker such as F can make it share.")
                    d.isMetalSide() -> L10n("$ds 虽然是金属，但在这种化合物里和 $ts 是共用电子，而不是完全给出。", " $dStart is a metal, but in this compound it shares electrons with $tE instead of giving them away completely.")
                    else -> L10n("", "")
                }
            val why2 =
                L10n(
                    "电子没有整个飞到另一个原子上，而是 $ds 和 $ts 各拿出电子，组成共用电子对（共价键）。$orderZh" + special.zh,
                    "No electron moves across completely: $dE and $tE each put in electrons to make shared pairs (covalent bonds).$orderEn${special.en}",
                )
            val total = given * d.atoms
            val equation = equationLine(reaction, "偏移", "shifted")
            val why3 =
                L10n(
                    "$ts 吸引电子的能力更强，共用电子对偏向 $ts。按化合价算 $ds 0 → +$given、$ts 0 → −$need，共偏移 $total e⁻。" + equation.zh +
                        "所以这也是氧化还原反应：$ds 被氧化，$ts 被还原。",
                    "$tStart attracts electrons more strongly, so the shared pairs lean toward $tE. By oxidation state $dE 0 → +$given, $tE 0 → −$need: $total e⁻ shifted in all.${equation.en} " +
                        "So this is a redox reaction too: $dE is oxidised and $tE is reduced.",
                )
            return listOf(
                title0 to why0,
                step1,
                L10n("$ds 和 $ts 共用电子对，生成 ${pretty(formula)}", "$dStart and $tE share electron pairs: ${pretty(formula)} forms") to why2,
                L10n("电子对偏向 $ts", "The pairs lean toward $tE") to why3,
            )
        }

        private fun Side.isMetalSide() = element.isMetal()
    }

    companion object {
        /** Id of the electron view of reaction "x" is "x~e". */
        const val SUFFIX = "~e"

        /** Outer edge of the picture, in scene units (the hand-made frames stay inside the same box). */
        const val X_MAX = 0.84f
        const val Y_MAX = 0.56f
        private const val SIDE_GAP = 0.12f
        private const val SIDE_WIDTH = 0.66f
        private const val MAX_ATOMS = 10

        /** Centre distance of two bonded atoms, in units of their ring radii: rings overlap a little so the pair sits between them. */
        private const val BOND = 0.93f
        private val SCALES = listOf(1f, 0.9f, 0.8f, 0.7f, 0.62f, 0.55f, 0.48f, 0.42f, 0.36f, 0.3f, 0.25f)

        /** Electrons a taker atom needs to fill its outer shell. */
        private val NEED =
            mapOf(
                "H" to 1, "F" to 1, "Cl" to 1, "Br" to 1, "I" to 1, "At" to 1,
                "O" to 2, "S" to 2, "Se" to 2, "Te" to 2,
                "N" to 3, "P" to 3, "As" to 3,
                "C" to 4, "Si" to 4, "Ge" to 4,
            )

        /** Metal compounds that are molecules held by covalent bonds, not ionic solids. */
        val MOLECULAR =
            setOf(
                "AlCl3", "AlBr3", "AlI3", "FeCl3", "FeBr3", "TiCl4", "SnCl4", "NbCl5", "AuCl3", "HgCl2", "BiCl3", "UF6", "OsO4", "Tc2O7", "Re2O7", "WC",
                "BeF2", "BeCl2", "BeBr2", "BeI2", "VCl4", "ZrCl4", "HfCl4", "MoCl5", "WCl6", "TaCl5", "GaCl3", "SnBr4", "SnI4", "HgBr2", "HgI2", "PtCl2", "PtF6", "AuF3", "AlN", "ReCl5", "OsCl4",
            )

        /** Covalent network solids, drawn as their simplest unit. */
        private val NETWORK = setOf("SiO2", "B2O3", "GeO2", "TeO2", "BN", "AlN")

        private val CAGES = setOf("As4O6", "Sb4O6")

        /** Symbols that read as English words at the start of a sentence; the English text uses the element name instead. */
        private val WORDS = setOf("I", "In", "As", "Am", "At", "Be", "He", "No")

        private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉"

        fun pretty(formula: String): String = formula.map { if (it.isDigit()) SUBSCRIPTS[it - '0'] else it }.joinToString("")
    }
}
