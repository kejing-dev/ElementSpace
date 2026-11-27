package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.Reaction

/** What happens when two elements (as simple substances) are brought together (v1.6.0 "碰一碰"). */
sealed interface CollideResult {
    /** The library has their reaction(s); [why] says who gives and who takes electrons. */
    data class Reacts(val reactions: List<Reaction>, val why: L10n) : CollideResult

    /** They do not react, and we can say why. */
    data class NoReaction(val why: L10n) : CollideResult

    /** One of them only exists as a few atoms: no experiment to show. */
    data class NoExperiment(val why: L10n) : CollideResult

    /** No common direct reaction in the textbooks or the reference; the card links each element's own reactions. */
    data object NoCommonReaction : CollideResult
}

/**
 * Rules, in order: a library reaction between the two simple substances; elements with no
 * chemistry to show; noble gases; two metals; otherwise "no common direct reaction". It never
 * guesses a product: only reactions in the sourced library are shown as reactions.
 */
class CollideElementsUseCase(
    private val reactions: List<Reaction>,
    /** Elements that have no common reaction at all, with the reason (ElementReactions.NOTES). */
    private val notes: Map<String, L10n>,
    private val configuration: ElectronConfigurationUseCase = ElectronConfigurationUseCase(),
) {
    operator fun invoke(a: Element, b: Element): CollideResult {
        val pair = setOf(a.symbol, b.symbol)
        val hits =
            reactions.filter { r ->
                r.reactants.size == 2 && r.reactants.all { ChemicalFormula.count(it.formula).size == 1 } &&
                    r.reactants.flatMap { ChemicalFormula.count(it.formula).keys }.toSet() == pair
            }
        if (hits.isNotEmpty()) return CollideResult.Reacts(hits, whyReacts(a, b))

        // He, Ne and Ar are in the notes too, but their reason is the full shell (next rule), not scarcity.
        listOf(a, b).firstOrNull { it.symbol in notes && it.symbol !in setOf("He", "Ne", "Ar") }?.let { return CollideResult.NoExperiment(notes.getValue(it.symbol)) }

        listOf(a, b).firstOrNull { it.category == ElementCategory.NOBLE_GAS }?.let { gas ->
            val outer = configuration(gas.atomicNumber).valenceElectrons
            val heavy = gas.atomicNumber >= 36
            val zh = "${gas.nameZh}是稀有气体，最外层 $outer 个电子已经排满，既不容易失去电子，也不容易得到电子，所以不反应。" +
                if (heavy) "只有氟这种得电子能力最强的元素才能和它化合。" else ""
            val en = "${gas.nameEn} is a noble gas: its outer shell is full with $outer electrons, so it neither gives nor takes electrons easily and does not react." +
                if (heavy) " Only fluorine, the strongest electron taker, combines with it." else ""
            return CollideResult.NoReaction(L10n(zh, en))
        }

        NO_DIRECT[pair]?.let { return CollideResult.NoReaction(it) }
        if ("Ra" in pair) {
            return CollideResult.NoExperiment(
                L10n(
                    "镭有很强的放射性，全世界提炼出的金属镭也很少，一般不用它的单质做化合实验。它的化学性质和同族的钡相似。",
                    "Radium is intensely radioactive and very little of the metal has ever been made, so it is not used for experiments like this. Chemically it behaves like barium, the element above it.",
                )
            )
        }

        if (a.isMetal() && b.isMetal()) {
            return CollideResult.NoReaction(
                L10n(
                    "${a.nameZh}和${b.nameZh}都是金属，原子都倾向于失去电子，没有谁来接收，所以不发生化学反应。把它们熔在一起得到的是合金。",
                    "${a.nameEn} and ${b.nameEn} are both metals: both tend to give electrons away and neither takes them, so there is no chemical reaction. Melted together they form an alloy.",
                )
            )
        }
        return CollideResult.NoCommonReaction
    }

    private companion object {
        /** Well-known pairs that do not combine directly, with the textbook reason. */
        val NO_DIRECT: Map<Set<String>, L10n> =
            mapOf(
                setOf("Au", "O") to L10n("金很不活泼，即使在高温下也不和氧气反应，所以说「真金不怕火炼」。", "Gold is very unreactive: it does not react with oxygen even at high temperature."),
                setOf("Pt", "O") to L10n("铂很不活泼，在空气中加热也不被氧化，所以能做坩埚和电极。", "Platinum is very unreactive: heating in air does not oxidise it, which is why it is used for crucibles and electrodes."),
                setOf("Ag", "O") to L10n("银不活泼，在空气中加热也不和氧气化合（氧化银受热反而分解）。银器变黑是因为生成了硫化银。", "Silver is unreactive: it does not combine with oxygen even when heated in air (silver oxide decomposes on heating). Silverware tarnishes because silver sulfide forms."),
            )
    }

    private fun Element.isMetal(): Boolean =
        category in setOf(ElementCategory.ALKALI_METAL, ElementCategory.ALKALINE_EARTH, ElementCategory.TRANSITION_METAL, ElementCategory.POST_TRANSITION, ElementCategory.LANTHANIDE, ElementCategory.ACTINIDE)

    private fun Element.isMainGroupMetal(): Boolean = category in setOf(ElementCategory.ALKALI_METAL, ElementCategory.ALKALINE_EARTH, ElementCategory.POST_TRANSITION)

    private fun whyReacts(a: Element, b: Element): L10n {
        val noble = listOf(a, b).firstOrNull { it.category == ElementCategory.NOBLE_GAS }
        if (noble != null) {
            val other = if (noble == a) b else a
            return L10n(
                "稀有气体通常不反应。但${other.nameZh}得电子的能力最强，能把${noble.nameZh}原子的电子拉过来一些，所以它们能化合。",
                "Noble gases normally do not react, but ${other.nameEn} is the strongest electron taker and can pull electrons from ${noble.nameEn}, so they combine.",
            )
        }
        val metal = listOf(a, b).firstOrNull { it.isMetal() }
        if (metal != null) {
            val other = if (metal == a) b else a
            val given = if (metal.isMainGroupMetal()) configuration(metal.atomicNumber).valenceElectrons else null
            val metalZh = if (given != null) "${metal.nameZh}是金属，最外层只有 $given 个电子，容易失去" else "${metal.nameZh}是金属，容易失去电子"
            val metalEn = if (given != null) "${metal.nameEn} is a metal with only $given outer electron${if (given == 1) "" else "s"}, easily lost" else "${metal.nameEn} is a metal and loses electrons easily"
            val outer = configuration(other.atomicNumber).valenceElectrons
            val need = if (other.symbol == "H") 1 else 8 - outer
            val takerZh = if (other.category != ElementCategory.METALLOID && need in 1..4) "${other.nameZh}最外层 $outer 个电子，再得 $need 个就排满" else "${other.nameZh}得电子的能力比${metal.nameZh}强"
            val takerEn = if (other.category != ElementCategory.METALLOID && need in 1..4) "${other.nameEn} has $outer outer electron${if (outer == 1) "" else "s"} and needs $need more to fill its shell" else "${other.nameEn} attracts electrons more strongly than ${metal.nameEn}"
            return L10n(
                "$metalZh；$takerZh。反应时${metal.nameZh}失去电子（化合价升高），${other.nameZh}得到电子（化合价降低）。",
                "$metalEn; $takerEn. In the reaction ${metal.nameEn} loses electrons (oxidation state rises) and ${other.nameEn} gains them (it falls).",
            )
        }
        // Two non-metals (or a metalloid): they share electrons.
        val ea = a.electronegativity
        val eb = b.electronegativity
        val stronger = if (ea != null && eb != null && ea != eb) (if (ea > eb) a else b) else null
        return L10n(
            "${a.nameZh}和${b.nameZh}都不容易失去电子，谁也夺不走对方的，于是共用电子对（共价键）。" + (stronger?.let { "共用的电子对偏向得电子能力更强的${it.nameZh}。" } ?: ""),
            "Neither ${a.nameEn} nor ${b.nameEn} gives up electrons easily, so they share electron pairs (covalent bonds)." + (stronger?.let { " The shared pairs lie closer to ${it.nameEn}, the stronger electron taker." } ?: ""),
        )
    }
}

/** One line for the atom card when the atom has become an ion. */
object IonText {
    private val NOBLE = mapOf(2 to L10n("氦", "helium"), 10 to L10n("氖", "neon"), 18 to L10n("氩", "argon"), 36 to L10n("氪", "krypton"), 54 to L10n("氙", "xenon"), 86 to L10n("氡", "radon"))

    fun explain(element: Element, charge: Int, shells: List<Int>): L10n {
        val n = kotlin.math.abs(charge)
        val electrons = element.atomicNumber - charge
        val shellText = shells.filter { it > 0 }.joinToString("、")
        val like = NOBLE[electrons]
        val lostZh = if (charge > 0) "失去 $n 个电子，带 $n 个单位正电荷" else "得到 $n 个电子，带 $n 个单位负电荷"
        val lostEn = if (charge > 0) "lost $n electron${if (n == 1) "" else "s"}, so it carries $n positive charge${if (n == 1) "" else "s"}" else "gained $n electron${if (n == 1) "" else "s"}, so it carries $n negative charge${if (n == 1) "" else "s"}"
        return if (like != null && electrons > 0) {
            L10n(
                "${element.nameZh}原子$lostZh。现在电子排布是 $shellText，和${like.zh}原子一样最外层排满，所以这种离子很稳定。",
                "The ${element.nameEn} atom has $lostEn. Its electrons are now arranged ${shells.filter { it > 0 }.joinToString(", ")}, the same full outer shell as ${like.en}, which makes this ion stable.",
            )
        } else if (electrons == 0) {
            L10n("${element.nameZh}原子$lostZh，只剩一个原子核（质子）。", "The ${element.nameEn} atom has $lostEn; only the bare nucleus is left.")
        } else {
            L10n("${element.nameZh}原子$lostZh。电子排布变成 $shellText。", "The ${element.nameEn} atom has $lostEn. Its electrons are now arranged ${shells.filter { it > 0 }.joinToString(", ")}.")
        }
    }
}
