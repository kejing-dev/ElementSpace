package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.BondMark
import com.example.elementspace.p7c3a91e4.domain.model.ConditionKind
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
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder

/**
 * Organic reactions shown as bonds breaking and forming in ball-and-stick molecules
 * ([ReactionKind.BONDS]). Every step uses the same atom ids, so atoms glide from the reactants to
 * the products. Positions are built in ångström and scaled by [SCALE] into scene units.
 */
object OrganicReactions {
    /** Scene units per ångström; also used by the renderer for atom and stick sizes. */
    const val SCALE = 0.13f

    private val S_ORG_1 = L10n("人教版高中化学 必修第二册 第七章第一节「认识有机化合物」", "PEP High-school Chemistry Book 2, Ch. 7 §1 (Organic compounds)")
    private val S_ORG_2 = L10n("人教版高中化学 必修第二册 第七章第二节「乙烯与有机高分子材料」", "PEP High-school Chemistry Book 2, Ch. 7 §2 (Ethylene and polymers)")
    private val S_ORG_3 = L10n("人教版高中化学 必修第二册 第七章第三节「乙醇与乙酸」", "PEP High-school Chemistry Book 2, Ch. 7 §3 (Ethanol and acetic acid)")
    private val NONE = L10n("", "")

    /** Atom symbols by id plus helpers to write frames as id -> position maps. */
    private class Cast {
        val symbols = LinkedHashMap<String, String>()
        val bonds = ArrayList<FrameBond>()

        /** Registers [b]'s atoms as "$prefix$index" and returns their positions (Å) moved by [offset]. */
        fun add(prefix: String, b: MoleculeBuilder, offset: Vec3, transform: (Vec3) -> Vec3 = { it }): Map<String, Vec3> {
            b.atoms.forEachIndexed { i, a -> symbols["$prefix$i"] = a.symbol }
            b.bonds.forEach { bonds += FrameBond("$prefix${it.a}", "$prefix${it.b}", it.order) }
            return b.atoms.mapIndexed { i, a -> "$prefix$i" to transform(a.position) + offset }.toMap()
        }

        fun frame(positions: Map<String, Vec3>, bonds: List<FrameBond>, faded: Set<String> = emptySet()): ReactionFrame {
            require(positions.keys == symbols.keys) { "frame must place every atom: ${symbols.keys - positions.keys}" }
            return ReactionFrame(
                atoms = symbols.map { (id, symbol) -> FrameAtom(id, symbol, positions.getValue(id) * SCALE, id in faded) },
                bonds = bonds,
            )
        }
    }

    private fun List<FrameBond>.mark(a: String, b: String, mark: BondMark, order: Int? = null): List<FrameBond> =
        map { if ((it.a == a && it.b == b) || (it.a == b && it.b == a)) it.copy(mark = mark, order = order ?: it.order) else it }

    private fun List<FrameBond>.without(a: String, b: String): List<FrameBond> =
        filterNot { (it.a == a && it.b == b) || (it.a == b && it.b == a) }

    private fun Map<String, Vec3>.shift(ids: Collection<String>, by: Vec3): Map<String, Vec3> = mapValues { (id, p) -> if (id in ids) p + by else p }

    private fun step(zh: String, en: String, whyZh: String, whyEn: String, frame: ReactionFrame) =
        ReactionStep(L10n(zh, en), L10n(whyZh, whyEn), frame, moved = 0)

    // ---- CH4 + Cl2 -> CH3Cl + HCl --------------------------------------------------------------

    private fun chlorination(): Reaction {
        val cast = Cast()
        val m = cast.add("m", BundledMoleculeRepository.build("methane"), Vec3(-1.8f, 0f, 0f)) // m1 is the H pointing at the Cl2
        val cl = mapOf("x0" to Vec3(1.0f, 0f, 0f), "x1" to Vec3(1.0f + MoleculeBuilder.CL_CL, 0f, 0f))
        cast.symbols["x0"] = "Cl"
        cast.symbols["x1"] = "Cl"
        val start = m + cl
        val bonds = cast.bonds + FrameBond("x0", "x1")
        val marked = bonds.mark("m0", "m1", BondMark.BREAK).mark("x0", "x1", BondMark.BREAK)
        val open = bonds.without("m0", "m1").without("x0", "x1")
        val apart = start + mapOf("m1" to Vec3(0.2f, -1.3f, 0.3f), "x0" to Vec3(0.5f, 0.7f, 0f), "x1" to Vec3(3.4f, -0.4f, 0f))
        val c = start.getValue("m0")
        val done =
            start +
                mapOf(
                    "x0" to c + Vec3(MoleculeBuilder.C_CL, 0f, 0f),
                    "m1" to Vec3(1.6f, -1.8f, 0f),
                    "x1" to Vec3(1.6f + MoleculeBuilder.H_CL, -1.8f, 0f),
                )
        val formed = open + FrameBond("m0", "x0", mark = BondMark.NEW) + FrameBond("m1", "x1", mark = BondMark.NEW)
        return Reaction(
            id = "ch4_cl2",
            reactants = listOf(Term(1, "CH4"), Term(1, "Cl2")),
            products = listOf(Term(1, "CH3Cl"), Term(1, "HCl")),
            condition = L10n("光照", "light"),
            visuals = listOf(ConditionVisual(ConditionKind.LIGHT, L10n("光照", "light"))),
            types = listOf(ReactionType.SUBSTITUTION),
            stages = setOf(SchoolStage.SENIOR),
            redox = emptyList(),
            loseLabel = NONE,
            gainLabel = NONE,
            steps =
                listOf(
                    step("甲烷和氯气混合", "Methane meets chlorine",
                        "甲烷是正四面体，C 在中心连 4 个 H；氯气是两个 Cl 共用一对电子。暗处它们不反应，要光照。",
                        "Methane is a tetrahedron with C bonded to four H; chlorine is two Cl sharing one electron pair. In the dark nothing happens: it needs light.",
                        cast.frame(start, bonds)),
                    step("光照：标出要断的键", "Light: the bonds that will break",
                        "光照下 Cl—Cl 键断开；甲烷的一个 C—H 键也会断开（红虚线）。",
                        "Light breaks the Cl–Cl bond; one C–H bond of methane breaks too (red dashes).",
                        cast.frame(start, marked)),
                    step("断键：H 和 Cl 离开", "Bonds break",
                        "断开后，一个 Cl 去接 C 空出来的位置，被替下来的 H 和另一个 Cl 走到一起。",
                        "One Cl moves into the place the H left on carbon; the freed H meets the other Cl.",
                        cast.frame(apart, open)),
                    step("成键：一氯甲烷和氯化氢", "New bonds: chloromethane and HCl",
                        "Cl 替代了一个 H，这就是取代反应。左边 C 1、H 4、Cl 2，右边也是 C 1、H 4、Cl 2。",
                        "Cl has taken the place of one H: a substitution. Left: C 1, H 4, Cl 2; right: the same.",
                        cast.frame(done, formed)),
                ),
            property = L10n(
                "取代反应：有机物分子里的某些原子或原子团被其他原子或原子团所替代。生成的 CH₃Cl 还会继续被取代，依次生成 CH₂Cl₂、CHCl₃、CCl₄。",
                "Substitution: an atom or group in an organic molecule is replaced by another. CH₃Cl reacts further to CH₂Cl₂, CHCl₃ and CCl₄.",
            ),
            phenomenon = L10n(
                "试管内气体颜色逐渐变浅，内壁出现油状液滴，试管内液面上升。",
                "The gas colour fades, oily drops form on the tube wall and the liquid level in the tube rises.",
            ),
            source = S_ORG_1,
            kind = ReactionKind.BONDS,
        )
    }

    // ---- CH2=CH2 + Br2 -> CH2BrCH2Br ---------------------------------------------------------

    private fun bromination(): Reaction {
        val cast = Cast()
        // Ethylene faces the viewer (C=C along x, H up and down); Br2 comes from above.
        val e = cast.add("c", BundledMoleculeRepository.build("ethylene"), Vec3(0f, -0.9f, 0f))
        cast.symbols["b0"] = "Br"
        cast.symbols["b1"] = "Br"
        val half = MoleculeBuilder.BR_BR / 2f
        val start = e + mapOf("b0" to Vec3(-half, 2.4f, 0f), "b1" to Vec3(half, 2.4f, 0f))
        val bonds = cast.bonds + FrameBond("b0", "b1")
        val marked = bonds.mark("c0", "c1", BondMark.BREAK_ONE).mark("b0", "b1", BondMark.BREAK)
        val open = bonds.without("b0", "b1").mark("c0", "c1", BondMark.NONE, order = 1)
        val apart = start + mapOf("b0" to Vec3(-1.1f, 2.0f, 0f), "b1" to Vec3(1.1f, 2.0f, 0f))
        // Product: each carbon tetrahedral again (outward x), Br up, its two H swing down to the front and back.
        val y0 = -0.9f
        fun around(sign: Float, t: Double) = Vec3(sign * 0.3333f, 0.9428f * kotlin.math.cos(t).toFloat(), 0.9428f * kotlin.math.sin(t).toFloat())
        val c0 = Vec3(-0.77f, y0, 0f)
        val c1 = Vec3(0.77f, y0, 0f)
        val t1 = 2 * Math.PI / 3
        val t2 = 4 * Math.PI / 3
        val h = MoleculeBuilder.C_H
        // ethylene H order: c2 = c0 upper, c3 = c0 lower, c4 = c1 upper, c5 = c1 lower
        val done =
            mapOf(
                "c0" to c0, "c1" to c1,
                "c2" to c0 + around(-1f, t1) * h,
                "c3" to c0 + around(-1f, t2) * h,
                "c4" to c1 + around(1f, t1) * h,
                "c5" to c1 + around(1f, t2) * h,
                "b0" to c0 + around(-1f, 0.0) * MoleculeBuilder.C_BR,
                "b1" to c1 + around(1f, 0.0) * MoleculeBuilder.C_BR,
            )
        val formed = open + FrameBond("c0", "b0", mark = BondMark.NEW) + FrameBond("c1", "b1", mark = BondMark.NEW)
        return Reaction(
            id = "c2h4_br2",
            reactants = listOf(Term(1, "C2H4", "CH2=CH2"), Term(1, "Br2")),
            products = listOf(Term(1, "CH2BrCH2Br")),
            condition = null,
            types = listOf(ReactionType.ADDITION),
            stages = setOf(SchoolStage.SENIOR),
            redox = emptyList(),
            loseLabel = NONE,
            gainLabel = NONE,
            steps =
                listOf(
                    step("乙烯遇到溴", "Ethylene meets bromine",
                        "乙烯的两个 C 之间是双键（两根棍），6 个原子在同一平面。溴分子 Br₂ 从上方靠近。",
                        "The two carbons of ethylene share a double bond (two sticks); all six atoms lie in one plane. A Br₂ molecule approaches from above.",
                        cast.frame(start, bonds)),
                    step("标出要断的键", "The bonds that will break",
                        "双键里有一个键比较容易断开（红虚线）；Br—Br 键也断开。",
                        "One of the two bonds in C=C opens easily (red dashes); the Br–Br bond breaks too.",
                        cast.frame(start, marked)),
                    step("双键变单键", "The double bond opens",
                        "C=C 变成 C—C，两个 C 各空出一个价键；两个 Br 分开。",
                        "C=C becomes C–C and each carbon has one free bond; the two Br separate.",
                        cast.frame(apart, open)),
                    step("成键：1,2-二溴乙烷", "New bonds: 1,2-dibromoethane",
                        "两个 Br 分别连到双键两端的 C 上，没有原子被替下来，这就是加成反应。每个 C 又变回四面体。",
                        "Each Br joins one carbon of the former double bond and nothing is replaced: an addition. Each carbon is tetrahedral again.",
                        cast.frame(done, formed)),
                ),
            property = L10n(
                "加成反应：有机物分子中双键（或三键）两端的碳原子与其他原子或原子团直接结合生成新的化合物。可用来鉴别乙烯和甲烷。",
                "Addition: the carbons at a double (or triple) bond join directly to other atoms or groups. It tells ethylene apart from methane.",
            ),
            phenomenon = L10n("溴的四氯化碳溶液（红棕色）褪色。", "The red-brown bromine solution turns colourless."),
            source = S_ORG_2,
            kind = ReactionKind.BONDS,
        )
    }

    // ---- CH3COOH + CH3CH2OH <=> CH3COOCH2CH3 + H2O ---------------------------------------------

    private fun esterification(): Reaction {
        val cast = Cast()
        // acetic acid: a0 CH3 C, a1 carboxyl C, a2 O(H), a3 =O, a4 H on O, a5..a7 H
        val acid = cast.add("a", BundledMoleculeRepository.build("acetic"), Vec3(-3.1f, 0f, 0f))
        // ethanol mirrored so its OH faces the acid: e0 CH3 C, e1 CH2 C, e2 O, e3 H on O, e4..e6, e7..e8 H
        val alcohol = cast.add("e", BundledMoleculeRepository.build("ethanol"), Vec3(3.4f, 0f, 0f)) { Vec3(-it.x, it.y, it.z) }
        val start = acid + alcohol
        val bonds = cast.bonds.toList()
        val marked = bonds.mark("a1", "a2", BondMark.BREAK).mark("e2", "e3", BondMark.BREAK)
        // Water from the acid's OH and the alcohol's H.
        val w = BundledMoleculeRepository.build("water")
        val waterAt = Vec3(0f, -2.3f, 0f)
        val water = mapOf("a2" to w.p(0) + waterAt, "a4" to w.p(1) + waterAt, "e3" to w.p(2) + waterAt)
        val acyl = listOf("a0", "a1", "a3", "a5", "a6", "a7")
        val ethoxy = listOf("e0", "e1", "e2", "e4", "e5", "e6", "e7", "e8")
        val apart = start.shift(acyl, Vec3(0.5f, 0.4f, 0f)).shift(ethoxy, Vec3(-0.5f, 0.4f, 0f)) + water
        val open = bonds.without("a1", "a2").without("e2", "e3") + FrameBond("a2", "e3", mark = BondMark.NEW)
        // Ester atoms in builder order -> the ids they came from.
        val es = BundledMoleculeRepository.build("ester")
        val map = listOf("a0", "a1", "e2", "a3", "e1", "e0", "a5", "a6", "a7", "e7", "e8", "e4", "e5", "e6")
        val esterAt = Vec3(0f, 0.7f, 0f)
        val done = map.mapIndexed { i, id -> id to es.p(i) + esterAt }.toMap() + water
        val formed = open + FrameBond("a1", "e2", mark = BondMark.NEW)
        return Reaction(
            id = "esterification",
            reactants = listOf(Term(1, "CH3COOH"), Term(1, "CH3CH2OH")),
            products = listOf(Term(1, "CH3COOCH2CH3"), Term(1, "H2O")),
            condition = L10n("浓硫酸、△", "conc. H₂SO₄, heat"),
            visuals =
                listOf(
                    ConditionVisual(ConditionKind.HEAT, L10n("加热", "heat")),
                    ConditionVisual(ConditionKind.CATALYST, L10n("浓硫酸 · 催化剂", "conc. H₂SO₄ · catalyst")),
                ),
            types = listOf(ReactionType.ESTERIFICATION, ReactionType.SUBSTITUTION),
            stages = setOf(SchoolStage.SENIOR),
            redox = emptyList(),
            loseLabel = NONE,
            gainLabel = NONE,
            steps =
                listOf(
                    step("乙酸和乙醇", "Acetic acid and ethanol",
                        "乙酸的官能团是羧基 —COOH，乙醇的官能团是羟基 —OH。浓硫酸作催化剂，加热。",
                        "Acetic acid carries a carboxyl group –COOH, ethanol a hydroxyl group –OH. Concentrated sulfuric acid catalyses; the mixture is heated.",
                        cast.frame(start, bonds)),
                    step("酸脱羟基、醇脱氢", "The acid loses OH, the alcohol loses H",
                        "断开的是乙酸羧基里的 C—O 键和乙醇羟基里的 O—H 键（红虚线）。这是用同位素示踪法证明的。",
                        "The bonds that break are the C–O bond in the acid's carboxyl group and the O–H bond in the alcohol (red dashes), as isotope labelling shows.",
                        cast.frame(start, marked)),
                    step("—OH 和 H 结合成水", "OH and H become water",
                        "乙酸脱下的 —OH 和乙醇脱下的 H 结合成 H₂O（金色是新形成的 O—H 键）。",
                        "The OH from the acid and the H from the alcohol join into H₂O (gold: the new O–H bond).",
                        cast.frame(apart, open)),
                    step("成键：乙酸乙酯", "New bond: ethyl acetate",
                        "乙酸剩下的部分和乙醇剩下的部分通过 C—O 键连起来，生成乙酸乙酯。左边 C 4、H 10、O 3，右边也一样。反应可逆。",
                        "What is left of each joins through a new C–O bond: ethyl acetate. Left: C 4, H 10, O 3; right: the same. The reaction is reversible.",
                        cast.frame(done, formed)),
                ),
            property = L10n(
                "酯化反应：酸和醇反应生成酯和水，属于取代反应。反应可逆；浓硫酸作催化剂和吸水剂。",
                "Esterification: an acid and an alcohol give an ester and water, a kind of substitution. It is reversible; concentrated sulfuric acid is catalyst and drying agent.",
            ),
            phenomenon = L10n(
                "饱和碳酸钠溶液的液面上有透明的油状液体生成，能闻到香味。",
                "A clear oily layer forms on the saturated sodium carbonate solution, with a fruity smell.",
            ),
            source = S_ORG_3,
            kind = ReactionKind.BONDS,
            reversible = true,
        )
    }

    val ALL: List<Reaction> by lazy { listOf(chlorination(), bromination(), esterification()) }
}
