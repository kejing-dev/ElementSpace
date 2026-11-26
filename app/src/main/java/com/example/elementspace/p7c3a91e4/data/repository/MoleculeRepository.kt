package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.MolBond
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.model.Skeleton
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_AROMATIC
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_C
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_DOUBLE
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_DOUBLE_O
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_H
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_O
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.C_TRIPLE
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.O_H
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.TETRAHEDRAL
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.norm
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder.Companion.zigzag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface MoleculeRepository {
    fun all(): List<Molecule>

    fun molecule(id: String): Molecule?

    /** Molecules that contain [symbol], in library order. */
    fun forSymbol(symbol: String): List<Molecule>
}

/**
 * The bundled molecule library: C, H and O compounds from school chemistry, each with its
 * textbook source (人教版 / PEP edition). Geometry comes from [MoleculeBuilder] so the tests can
 * check every valence, bond length and angle.
 */
class BundledMoleculeRepository : MoleculeRepository {
    override fun all(): List<Molecule> = LIBRARY

    override fun molecule(id: String): Molecule? = LIBRARY.firstOrNull { it.id == id }

    override fun forSymbol(symbol: String): List<Molecule> = LIBRARY.filter { symbol in it.symbols }

    companion object {
        val shared: MoleculeRepository by lazy { BundledMoleculeRepository() }

        private val S_JUNIOR_FUEL = L10n("人教版九年级上册 第七单元「燃料及其利用」课题2", "PEP Chemistry Grade 9 Vol. 1, Unit 7 Topic 2 (Fuels)")
        private val S_JUNIOR_CARBON = L10n("人教版九年级上册 第六单元「碳和碳的氧化物」", "PEP Chemistry Grade 9 Vol. 1, Unit 6 (Carbon and its oxides)")
        private val S_JUNIOR_WATER = L10n("人教版九年级上册 第四单元 课题3「水的组成」", "PEP Chemistry Grade 9 Vol. 1, Unit 4 Topic 3 (Composition of water)")
        private val S_ORG_1 = L10n("人教版高中化学 必修第二册 第七章第一节「认识有机化合物」", "PEP High-school Chemistry Book 2, Ch. 7 §1 (Organic compounds)")
        private val S_ORG_2 = L10n("人教版高中化学 必修第二册 第七章第二节「乙烯与有机高分子材料」", "PEP High-school Chemistry Book 2, Ch. 7 §2 (Ethylene and polymers)")
        private val S_ORG_3 = L10n("人教版高中化学 必修第二册 第七章第三节「乙醇与乙酸」", "PEP High-school Chemistry Book 2, Ch. 7 §3 (Ethanol and acetic acid)")
        private val S_STRUCT = L10n("人教版高中化学 选择性必修3 第一章第一节「有机化合物的结构特点」", "PEP Organic Chemistry, Ch. 1 §1 (Structure of organic compounds)")
        private val S_ALKYNE = L10n("人教版高中化学 选择性必修3 第二章第二节「烯烃 炔烃」", "PEP Organic Chemistry, Ch. 2 §2 (Alkenes and alkynes)")
        private val S_AROMATIC = L10n("人教版高中化学 选择性必修3 第二章第三节「芳香烃」", "PEP Organic Chemistry, Ch. 2 §3 (Aromatic hydrocarbons)")
        private val S_SHAPE = L10n("人教版高中化学 选择性必修2 第二章第二节「分子的空间结构」", "PEP Structure of Matter, Ch. 2 §2 (Molecular shapes)")

        private fun both(a: L10n, b: L10n) = L10n("${a.zh}；${b.zh}", "${a.en}; ${b.en}")

        private val Z = zigzag(3)
        private val D1 = norm(Z[1] - Z[0])
        private val D2 = norm(Z[2] - Z[1])

        private fun methane(): MoleculeBuilder =
            MoleculeBuilder().apply {
                val c = atom("C", Vec3.ZERO)
                TETRAHEDRAL.forEach { branch(c, "H", it, C_H) }
                angle(c, 1, 2, "109°28′")
            }

        private fun ethane() =
            MoleculeBuilder().apply {
                val a = atom("C", Vec3.ZERO)
                val b = branch(a, "C", Vec3(1f, 0f, 0f), C_C)
                capH3(a, b)
                capH3(b, a, phase = (PI / 3).toFloat())
            }.centred()

        private fun butane() =
            MoleculeBuilder().apply {
                val c0 = atom("C", Vec3.ZERO)
                val c1 = branch(c0, "C", D1, C_C)
                val c2 = branch(c1, "C", D2, C_C)
                val c3 = branch(c2, "C", D1, C_C)
                capH3(c0, c1, phase = 0.5f)
                ch2(c1, c0, c2)
                ch2(c2, c1, c3)
                capH3(c3, c2, phase = 0.5f)
                angle(c1, c0, c2, "≈109°28′")
            }.centred()

        private fun isobutane() =
            MoleculeBuilder().apply {
                val c = atom("C", Vec3.ZERO)
                branch(c, "H", TETRAHEDRAL[0], C_H)
                TETRAHEDRAL.drop(1).forEach { d ->
                    val m = branch(c, "C", d, C_C)
                    capH3(m, c, phase = 0.5f)
                }
            }.centred()

        private fun cyclohexane() =
            MoleculeBuilder().apply {
                // Chair: alternate carbons sit 0.25 Å in front of / behind the ring plane.
                val c = (0 until 6).map { k ->
                    val t = k * PI.toFloat() / 3f
                    atom("C", Vec3(1.45f * cos(t), 1.45f * sin(t), if (k % 2 == 1) 0.25f else -0.25f))
                }
                for (k in 0 until 6) bond(c[k], c[(k + 1) % 6])
                for (k in 0 until 6) ch2(c[k], c[(k + 5) % 6], c[(k + 1) % 6])
            }

        private fun benzene(): Pair<MoleculeBuilder, List<Int>> {
            val b = MoleculeBuilder()
            val c = (0 until 6).map { k ->
                val t = k * PI.toFloat() / 3f
                b.atom("C", Vec3(C_AROMATIC * cos(t), C_AROMATIC * sin(t), 0f), group = true)
            }
            for (k in 0 until 6) b.bond(c[k], c[(k + 1) % 6], MolBond.AROMATIC)
            for (k in 0 until 6) {
                val t = k * PI.toFloat() / 3f
                b.branch(c[k], "H", Vec3(cos(t), sin(t), 0f), C_H)
            }
            b.angle(c[0], c[1], c[5], "120°")
            return b to c
        }

        private fun ethylene() =
            MoleculeBuilder().apply {
                val a = atom("C", Vec3(-C_DOUBLE / 2, 0f, 0f), group = true)
                val b = atom("C", Vec3(C_DOUBLE / 2, 0f, 0f), group = true)
                bond(a, b, 2)
                val h = branch(a, "H", Vec3(-0.5f, 0.866f, 0f), C_H)
                branch(a, "H", Vec3(-0.5f, -0.866f, 0f), C_H)
                branch(b, "H", Vec3(0.5f, 0.866f, 0f), C_H)
                branch(b, "H", Vec3(0.5f, -0.866f, 0f), C_H)
                angle(a, b, h, "≈120°")
            }

        private fun acetylene() =
            MoleculeBuilder().apply {
                val a = atom("C", Vec3(-C_TRIPLE / 2, 0f, 0f), group = true)
                val b = atom("C", Vec3(C_TRIPLE / 2, 0f, 0f), group = true)
                bond(a, b, 3)
                val h = branch(a, "H", Vec3(-1f, 0f, 0f), 1.06f)
                branch(b, "H", Vec3(1f, 0f, 0f), 1.06f)
                angle(a, b, h, "180°")
            }

        private fun ethanol() =
            MoleculeBuilder().apply {
                val c0 = atom("C", Vec3.ZERO)
                val c1 = branch(c0, "C", D1, C_C)
                val o = branch(c1, "O", D2, C_O, group = true)
                branch(o, "H", D1, O_H, group = true)
                capH3(c0, c1, phase = 0.5f)
                ch2(c1, c0, o)
            }.centred()

        private fun ether() =
            MoleculeBuilder().apply {
                val a = atom("C", Vec3.ZERO, group = true)
                val o = branch(a, "O", D1, C_O, group = true)
                val b = branch(o, "C", D2, C_O)
                atoms[b] = atoms[b].copy(group = true)
                capH3(a, o, phase = 0.5f)
                capH3(b, o, phase = 0.5f)
            }.centred()

        private fun acetic() =
            MoleculeBuilder().apply {
                val c0 = atom("C", Vec3.ZERO)
                val c1 = branch(c0, "C", D1, 1.52f, group = true)
                val oh = branch(c1, "O", D2, 1.36f, group = true)
                branch(c1, "O", outward(c1, c0, oh), C_DOUBLE_O, order = 2, group = true)
                branch(oh, "H", D1, O_H, group = true)
                capH3(c0, c1, phase = 0.5f)
            }.centred()

        private fun ester() =
            MoleculeBuilder().apply {
                val c0 = atom("C", Vec3.ZERO)
                val c1 = branch(c0, "C", D1, 1.52f, group = true)
                val o = branch(c1, "O", D2, 1.36f, group = true)
                branch(c1, "O", outward(c1, c0, o), C_DOUBLE_O, order = 2, group = true)
                val c3 = branch(o, "C", D1, C_O)
                val c4 = branch(c3, "C", D2, C_C)
                capH3(c0, c1, phase = 0.5f)
                ch2(c3, o, c4)
                capH3(c4, c3, phase = 0.5f)
            }.centred()

        private fun co2() =
            MoleculeBuilder().apply {
                val c = atom("C", Vec3.ZERO)
                val a = branch(c, "O", Vec3(-1f, 0f, 0f), 1.16f, order = 2)
                branch(c, "O", Vec3(1f, 0f, 0f), 1.16f, order = 2)
                angle(c, a, 2, "180°")
            }

        private fun water() =
            MoleculeBuilder().apply {
                val o = atom("O", Vec3.ZERO)
                val half = 104.5f / 2f * PI.toFloat() / 180f
                val a = branch(o, "H", Vec3(-sin(half), -cos(half), 0f), O_H)
                val b = branch(o, "H", Vec3(sin(half), -cos(half), 0f), O_H)
                angle(o, a, b, "104.5°")
            }.centred()

        private fun make(
            id: String,
            name: L10n,
            formula: String,
            condensed: String,
            structural: String?,
            skeleton: Skeleton,
            builder: MoleculeBuilder,
            geometry: L10n,
            group: L10n?,
            property: L10n,
            source: L10n,
            compareWith: String? = null,
            compareNote: L10n? = null,
            reactionIds: List<String> = emptyList(),
            ring: List<Int> = emptyList(),
        ) = Molecule(
            id = id, name = name, formula = formula, condensed = condensed, structural = structural, skeleton = skeleton,
            atoms = builder.atoms.toList(), bonds = builder.bonds.toList(), angles = builder.angles.ifEmpty { carbonAngles(builder) }, aromaticRing = ring,
            geometry = geometry, group = group, property = property, compareWith = compareWith, compareNote = compareNote,
            reactionIds = reactionIds, source = source,
        )

        /**
         * When a molecule has no hand-picked angle, mark one angle per kind of carbon it has, with the
         * textbook value for that carbon: 4 bonds → 109°28′ (sp³), 3 → 120° (sp²), 2 → 180° (sp).
         * Source: 人教版 选择性必修2 第二章第二节「分子的空间结构」/ 选择性必修3 第一章第一节.
         */
        internal fun carbonAngles(b: MoleculeBuilder): List<com.example.elementspace.p7c3a91e4.domain.model.BondAngle> {
            val ideal = mapOf(4 to (109.47f to "≈109°28′"), 3 to (120f to "≈120°"), 2 to (180f to "180°"))
            val neighbours = b.atoms.indices.associateWith { i -> b.bonds.mapNotNull { if (it.a == i) it.b else if (it.b == i) it.a else null } }
            val seen = HashSet<Int>()
            val out = ArrayList<com.example.elementspace.p7c3a91e4.domain.model.BondAngle>()
            for (c in b.atoms.indices) {
                if (b.atoms[c].symbol != "C") continue
                val n = neighbours.getValue(c)
                val (target, label) = ideal[n.size] ?: continue
                if (!seen.add(n.size)) continue
                // Prefer the pair closest to the ideal, heavy atoms first so the arc is easy to see.
                val pairs = n.flatMapIndexed { i, x -> n.drop(i + 1).map { y -> x to y } }
                val best = pairs.minBy { (x, y) ->
                    val heavy = listOf(x, y).count { b.atoms[it].symbol != "H" }
                    kotlin.math.abs(b.degrees(c, x, y) - target) - heavy * 0.5f
                }
                out += com.example.elementspace.p7c3a91e4.domain.model.BondAngle(c, best.first, best.second, label, b.degrees(c, best.first, best.second))
            }
            return out
        }

        private val BENZENE = benzene()

        val LIBRARY: List<Molecule> =
            listOf(
                make(
                    "methane", L10n("甲烷", "Methane"), "CH4", "CH4",
                    "    H\n    |\nH — C — H\n    |\n    H",
                    Skeleton.CHAIN, methane(),
                    L10n("正四面体：C 在中心，4 个 H 在四个顶点。4 个 C—H 键完全相同，键角 109°28′。", "Regular tetrahedron: C in the centre, the four H at the corners. All four C–H bonds are the same; bond angle 109°28′."),
                    null,
                    L10n("天然气、沼气的主要成分；可燃，完全燃烧生成 CO₂ 和 H₂O；光照下能和 Cl₂ 发生取代反应。", "Main part of natural gas and biogas; burns completely to CO₂ and H₂O; in light it undergoes substitution with Cl₂."),
                    both(S_JUNIOR_FUEL, S_ORG_1),
                    reactionIds = listOf("ch4_cl2"),
                ),
                make(
                    "ethane", L10n("乙烷", "Ethane"), "C2H6", "CH3CH3",
                    "    H   H\n    |   |\nH — C — C — H\n    |   |\n    H   H",
                    Skeleton.CHAIN, ethane(),
                    L10n("两个 C 各连 3 个 H，每个 C 周围都是四面体。C—C 单键可以绕键轴转动。", "Each C carries three H and sits in a tetrahedron. The C–C single bond can turn about its axis."),
                    null,
                    L10n("烷烃：碳原子之间都是单键，其余价键都连 H，叫饱和烃。", "An alkane: only single bonds between carbons, every other bond goes to H (saturated hydrocarbon)."),
                    S_ORG_1,
                    compareWith = "ethylene",
                    compareNote = L10n("C—C 单键 · 饱和", "C–C single bond · saturated"),
                ),
                make(
                    "butane", L10n("正丁烷", "Butane"), "C4H10", "CH3CH2CH2CH3", null,
                    Skeleton.CHAIN, butane(),
                    L10n("4 个 C 连成一条链。链不是直线，是锯齿形，C—C—C 键角接近 109°28′。", "Four carbons in one chain. The chain is a zig-zag, not a straight line; the C–C–C angle is close to 109°28′."),
                    null,
                    L10n("沸点 −0.5 ℃。和异丁烷分子式相同、结构不同，互为同分异构体。", "Boils at −0.5 °C. Same formula as isobutane, different structure: they are isomers."),
                    S_ORG_1,
                    compareWith = "isobutane",
                    compareNote = L10n("直链 · 沸点 −0.5 ℃", "Straight chain · bp −0.5 °C"),
                ),
                make(
                    "isobutane", L10n("异丁烷", "Isobutane"), "C4H10", "CH3CH(CH3)CH3", null,
                    Skeleton.BRANCHED, isobutane(),
                    L10n("中间一个 C 连 3 个 CH₃ 和 1 个 H：主链 3 个 C，带一个支链。", "The middle C carries three CH₃ and one H: a three-carbon main chain with one branch."),
                    null,
                    L10n("沸点 −11.7 ℃，比正丁烷低：支链越多，分子越接近球形，沸点越低。", "Boils at −11.7 °C, lower than butane: more branching makes the molecule rounder and lowers the boiling point."),
                    S_ORG_1,
                    compareWith = "butane",
                    compareNote = L10n("带支链 · 沸点 −11.7 ℃", "Branched · bp −11.7 °C"),
                ),
                make(
                    "ethylene", L10n("乙烯", "Ethylene"), "C2H4", "CH2=CH2",
                    "H       H\n  \\   /\n   C=C\n  /   \\\nH       H",
                    Skeleton.CHAIN, ethylene(),
                    L10n("平面形：6 个原子在同一平面，键角约 120°。", "Flat: all six atoms lie in one plane, bond angles about 120°."),
                    L10n("碳碳双键 C=C", "C=C double bond"),
                    L10n("能使溴的四氯化碳溶液、酸性高锰酸钾溶液褪色；能发生加成反应、聚合成聚乙烯；可作果实催熟剂。", "Decolourises bromine solution and acidic KMnO₄; undergoes addition and polymerises to polyethylene; ripens fruit."),
                    S_ORG_2,
                    compareWith = "acetylene",
                    compareNote = L10n("C=C 双键 · 平面形", "C=C double bond · flat"),
                    reactionIds = listOf("c2h4_br2"),
                ),
                make(
                    "acetylene", L10n("乙炔", "Acetylene"), "C2H2", "CH≡CH", "H — C ≡ C — H",
                    Skeleton.CHAIN, acetylene(),
                    L10n("直线形：4 个原子在一条直线上，键角 180°。", "Linear: all four atoms on one line, bond angle 180°."),
                    L10n("碳碳三键 C≡C", "C≡C triple bond"),
                    L10n("能发生加成反应；在氧气中燃烧火焰温度很高，用于焊接和切割金属。", "Undergoes addition; its flame in oxygen is very hot and is used to weld and cut metal."),
                    S_ALKYNE,
                    compareWith = "ethylene",
                    compareNote = L10n("C≡C 三键 · 直线形", "C≡C triple bond · linear"),
                ),
                make(
                    "cyclohexane", L10n("环己烷", "Cyclohexane"), "C6H12", "(CH2)6", null,
                    Skeleton.RING, cyclohexane(),
                    L10n("6 个 C 首尾相连成环，C—C 都是单键。6 个 C 不在同一平面上（每个 C 仍是四面体）。", "Six carbons joined head to tail in a ring, all single bonds. The carbons are not in one plane (each is still tetrahedral)."),
                    null,
                    L10n("饱和的环状烃，性质和烷烃相似。", "A saturated ring hydrocarbon; behaves much like the alkanes."),
                    S_STRUCT,
                    compareWith = "benzene",
                    compareNote = L10n("全是单键 · 不共面", "Single bonds only · not flat"),
                ),
                make(
                    "benzene", L10n("苯", "Benzene"), "C6H6", "C6H6", null,
                    Skeleton.RING, BENZENE.first,
                    L10n("平面正六边形，12 个原子在同一平面，键角都是 120°。6 个碳碳键完全相同，是介于单键和双键之间的独特的键。", "A flat regular hexagon: all 12 atoms in one plane, every angle 120°. The six C–C bonds are identical, a special bond between single and double."),
                    L10n("苯环", "Benzene ring"),
                    L10n("无色、有特殊气味的液体，不溶于水；不能使酸性高锰酸钾溶液褪色。", "Colourless liquid with a distinctive smell, insoluble in water; does not decolourise acidic KMnO₄."),
                    S_AROMATIC,
                    compareWith = "cyclohexane",
                    compareNote = L10n("平面 · 6 个键相同", "Flat · six equal bonds"),
                    ring = BENZENE.second,
                ),
                make(
                    "ethanol", L10n("乙醇", "Ethanol"), "C2H6O", "CH3CH2OH",
                    "    H   H\n    |   |\nH — C — C — O — H\n    |   |\n    H   H",
                    Skeleton.CHAIN, ethanol(),
                    L10n("可以看成乙烷分子里的一个 H 被羟基（—OH）取代。", "Like ethane with one H replaced by a hydroxyl group (–OH)."),
                    L10n("羟基 —OH", "Hydroxyl –OH"),
                    L10n("俗称酒精；能和钠反应放出 H₂；在 Cu 或 Ag 催化、加热下被氧化成乙醛；可燃，是一种燃料。", "Alcohol; reacts with sodium giving H₂; oxidised to acetaldehyde over hot Cu or Ag; burns and is used as fuel."),
                    both(S_JUNIOR_FUEL, S_ORG_3),
                    compareWith = "ether",
                    compareNote = L10n("有 —OH · 能和钠反应", "Has –OH · reacts with sodium"),
                    reactionIds = listOf("esterification"),
                ),
                make(
                    "ether", L10n("二甲醚", "Dimethyl ether"), "C2H6O", "CH3OCH3",
                    "    H       H\n    |       |\nH — C — O — C — H\n    |       |\n    H       H",
                    Skeleton.CHAIN, ether(),
                    L10n("O 连两个 CH₃，没有 O—H 键。", "The O joins two CH₃ groups; there is no O–H bond."),
                    L10n("醚键 C—O—C", "Ether link C–O–C"),
                    L10n("和乙醇分子式相同、结构不同，互为同分异构体。常温是气体（沸点 −24.9 ℃），不和钠反应。", "Same formula as ethanol, different structure: they are isomers. A gas at room temperature (bp −24.9 °C); does not react with sodium."),
                    S_STRUCT,
                    compareWith = "ethanol",
                    compareNote = L10n("C—O—C · 不和钠反应", "C–O–C · no reaction with sodium"),
                ),
                make(
                    "acetic", L10n("乙酸", "Acetic acid"), "C2H4O2", "CH3COOH",
                    "    H   O\n    |   ‖\nH — C — C — O — H\n    |\n    H",
                    Skeleton.CHAIN, acetic(),
                    L10n("羧基里的 C 连一个 =O 和一个 —OH，它们和这个 C 在同一平面。", "The carboxyl C carries one =O and one –OH, all in one plane with it."),
                    L10n("羧基 —COOH", "Carboxyl –COOH"),
                    L10n("俗称醋酸，食醋的主要成分；弱酸，酸性比碳酸强；和乙醇发生酯化反应。", "The acid in vinegar; a weak acid, stronger than carbonic acid; forms an ester with ethanol."),
                    S_ORG_3,
                    reactionIds = listOf("esterification"),
                ),
                make(
                    "ester", L10n("乙酸乙酯", "Ethyl acetate"), "C4H8O2", "CH3COOCH2CH3", null,
                    Skeleton.CHAIN, ester(),
                    L10n("乙酸的 —OH 换成了乙醇去掉 H 后剩下的 —OCH₂CH₃。", "Acetic acid with its –OH replaced by –OCH₂CH₃, what is left of ethanol after losing H."),
                    L10n("酯基 —COO—", "Ester group –COO–"),
                    L10n("无色、有香味的液体，难溶于水，密度比水小。", "Colourless, fragrant liquid; barely soluble in water and less dense than water."),
                    S_ORG_3,
                    reactionIds = listOf("esterification"),
                ),
                make(
                    "co2", L10n("二氧化碳", "Carbon dioxide"), "CO2", "O=C=O", "O = C = O",
                    Skeleton.INORGANIC, co2(),
                    L10n("直线形，键角 180°；两个 C=O 双键。", "Linear, bond angle 180°; two C=O double bonds."),
                    null,
                    L10n("不燃烧、不支持燃烧，密度比空气大；能使澄清石灰水变浑浊；含碳燃料完全燃烧的产物。", "Does not burn or support burning, denser than air; turns limewater cloudy; formed when carbon fuels burn completely."),
                    both(S_JUNIOR_CARBON, S_SHAPE),
                ),
                make(
                    "water", L10n("水", "Water"), "H2O", "H2O", "H — O — H",
                    Skeleton.INORGANIC, water(),
                    L10n("V 形（角形），键角 104.5°。", "Bent (V-shaped), bond angle 104.5°."),
                    null,
                    L10n("由氢、氧两种元素组成；含氢燃料燃烧的产物。", "Made of hydrogen and oxygen; formed when hydrogen-containing fuels burn."),
                    both(S_JUNIOR_WATER, S_SHAPE),
                    reactionIds = listOf("h2_o2"),
                ),
            )

        /** Building blocks reused by the organic reaction frames (fresh copies each call). */
        fun build(id: String): MoleculeBuilder =
            when (id) {
                "methane" -> methane()
                "ethylene" -> ethylene()
                "ethanol" -> ethanol()
                "acetic" -> acetic()
                "ester" -> ester()
                "water" -> water()
                else -> error("no builder for $id")
            }

    }
}
