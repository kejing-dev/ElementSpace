package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ElectronFlight
import com.example.elementspace.p7c3a91e4.domain.model.FrameParticle
import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.ReactionStep
import com.example.elementspace.p7c3a91e4.domain.model.ReactionType
import com.example.elementspace.p7c3a91e4.domain.model.RedoxChange
import com.example.elementspace.p7c3a91e4.domain.model.SchoolStage
import com.example.elementspace.p7c3a91e4.domain.model.SharedPair
import com.example.elementspace.p7c3a91e4.domain.model.Term
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.domain.usecase.ChemicalFormula

/** Common school reactions, listed on the info card of every element they contain. */
interface ReactionRepository {
    fun all(): List<Reaction>

    fun reaction(id: String): Reaction?

    /** Reactions whose equation contains [symbol], in library order. */
    fun forSymbol(symbol: String): List<Reaction>
}

/**
 * Bundled reaction library. Every entry follows the PEP (人教版) textbooks — junior: 九年级《化学》
 * (2012 edition); senior: 《化学》必修第一册 (2019 edition) — and names its chapter in [Reaction.source].
 * ReactionLibraryTest checks each one is balanced and conserves electrons.
 *
 * Frame coordinates are scene units (the whole picture fits radius 1), x to the right, y up, z
 * toward the viewer. Particle ids stay the same across the steps of a reaction so the 3D view can
 * move each atom from where it was.
 */
class BundledReactionRepository : ReactionRepository {
    override fun all(): List<Reaction> = LIBRARY

    override fun reaction(id: String): Reaction? = LIBRARY.firstOrNull { it.id == id }

    override fun forSymbol(symbol: String): List<Reaction> =
        LIBRARY.filter { reaction ->
            (reaction.reactants + reaction.products).any { symbol in ChemicalFormula.count(it.formula) }
        }

    companion object {
        val shared: ReactionRepository by lazy { BundledReactionRepository() }

        private fun v(x: Float, y: Float) = Vec3(x, y, 0f)

        private val IGNITE = L10n("点燃", "ignite")

        // ---- 2Na + Cl2 = 2NaCl ------------------------------------------------------------
        private val NA_CL2 =
            Reaction(
                id = "na_cl2",
                reactants = listOf(Term(2, "Na"), Term(1, "Cl2")),
                products = listOf(Term(2, "NaCl")),
                condition = IGNITE,
                types = listOf(ReactionType.COMBINATION, ReactionType.REDOX, ReactionType.IONIC_BOND),
                stages = setOf(SchoolStage.SENIOR),
                redox = listOf(RedoxChange("Na", 0, 1, 2), RedoxChange("Cl", 0, -1, 2)),
                loseLabel = L10n("Na 失去 e⁻", "Na loses e⁻"),
                gainLabel = L10n("Cl 得到 e⁻", "Cl gains e⁻"),
                steps =
                    listOf(
                        ReactionStep(
                            title = L10n("反应物：2Na + Cl₂", "Reactants: 2Na + Cl₂"),
                            why =
                                L10n(
                                    "Na 最外层只有 1 个电子，容易失去；Cl 最外层 7 个，差 1 个就满 8 个。Cl₂ 里两个 Cl 共用一对电子。",
                                    "Na has 1 outer electron and gives it up easily; Cl has 7 and needs 1 more to reach 8. In Cl₂ the two Cl atoms share a pair.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Na1", "Na", listOf(2, 8, 1), v(-0.62f, 0.34f)),
                                            FrameParticle("Na2", "Na", listOf(2, 8, 1), v(-0.62f, -0.34f)),
                                            FrameParticle("Cl1", "Cl", listOf(2, 8, 6), v(0.2f, 0f)),
                                            FrameParticle("Cl2", "Cl", listOf(2, 8, 6), v(0.6f, 0f)),
                                        ),
                                    pairs = listOf(SharedPair("Cl1", "Cl2")),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("点燃，Cl₂ 拆成 2 个 Cl", "Ignite: Cl₂ splits into 2 Cl"),
                            why =
                                L10n(
                                    "分子里的两个 Cl 先分开，才能各自接一个电子。金色虚圈是还差的那个空位。",
                                    "The two Cl atoms separate first so each can take one electron. The gold ring marks the missing slot.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Na1", "Na", listOf(2, 8, 1), v(-0.62f, 0.34f)),
                                            FrameParticle("Na2", "Na", listOf(2, 8, 1), v(-0.62f, -0.34f)),
                                            FrameParticle("Cl1", "Cl", listOf(2, 8, 7), v(0.42f, 0.34f), vacancies = 1),
                                            FrameParticle("Cl2", "Cl", listOf(2, 8, 7), v(0.42f, -0.34f), vacancies = 1),
                                        ),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("每个 Na 失 1 e⁻ 给 Cl", "Each Na gives 1 e⁻ to a Cl"),
                            why =
                                L10n(
                                    "失电子 → 化合价升高（0 → +1），Na 被氧化；得电子 → 化合价降低（0 → −1），Cl 被还原。失去的 2 e⁻ = 得到的 2 e⁻。",
                                    "Losing electrons raises the oxidation state (0 → +1): Na is oxidised. Gaining lowers it (0 → −1): Cl is reduced. 2 e⁻ lost = 2 e⁻ gained.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Na1", "Na", listOf(2, 8, 0), v(-0.62f, 0.34f)),
                                            FrameParticle("Na2", "Na", listOf(2, 8, 0), v(-0.62f, -0.34f)),
                                            FrameParticle("Cl1", "Cl", listOf(2, 8, 7), v(0.42f, 0.34f), vacancies = 1),
                                            FrameParticle("Cl2", "Cl", listOf(2, 8, 7), v(0.42f, -0.34f), vacancies = 1),
                                        ),
                                    flights = listOf(ElectronFlight("Na1", "Cl1"), ElectronFlight("Na2", "Cl2")),
                                ),
                            moved = 2,
                        ),
                        ReactionStep(
                            title = L10n("Na⁺ 和 Cl⁻ 吸引，生成 NaCl", "Na⁺ and Cl⁻ attract: NaCl forms"),
                            why =
                                L10n(
                                    "Na⁺ 和 Cl⁻ 都成了 8 电子稳定结构。正负离子靠静电吸引结合，这就是离子键。",
                                    "Na⁺ and Cl⁻ both have a full outer shell of 8. Opposite ions held together by attraction: an ionic bond.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Na1", "Na", listOf(2, 8), v(-0.28f, 0.34f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("Na2", "Na", listOf(2, 8), v(-0.28f, -0.34f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("Cl1", "Cl", listOf(2, 8, 8), v(0.2f, 0.34f), tint = ChargeTint.NEGATIVE, gold = mapOf(2 to 1)),
                                            FrameParticle("Cl2", "Cl", listOf(2, 8, 8), v(0.2f, -0.34f), tint = ChargeTint.NEGATIVE, gold = mapOf(2 to 1)),
                                        ),
                                ),
                            moved = 2,
                        ),
                    ),
                property =
                    L10n(
                        "钠很活泼，有强还原性；氯气是强氧化剂。生成的 NaCl 就是食盐的主要成分。",
                        "Sodium is very reactive (a strong reducing agent); chlorine is a strong oxidising agent. NaCl is table salt.",
                    ),
                phenomenon =
                    L10n(
                        "钠在氯气中剧烈燃烧，发出黄色火焰，生成大量白烟（NaCl 小颗粒）。",
                        "Sodium burns fiercely in chlorine with a yellow flame, giving thick white smoke (fine NaCl).",
                    ),
                source =
                    L10n(
                        "人教版高中《化学》必修第一册 第二章第二节「氯及其化合物」、第四章第三节「化学键」",
                        "PEP Chemistry, Senior Compulsory Book 1, Ch. 2 §2 (Chlorine) and Ch. 4 §3 (Chemical bonds)",
                    ),
            )

        // ---- 2H2 + O2 = 2H2O --------------------------------------------------------------
        private fun waterFrame(partial: Boolean): ReactionFrame {
            val o = if (partial) ChargeTint.PARTIAL_NEGATIVE else ChargeTint.NONE
            val h = if (partial) ChargeTint.PARTIAL_POSITIVE else ChargeTint.NONE
            return ReactionFrame(
                particles =
                    listOf(
                        FrameParticle("O1", "O", listOf(2, 4), v(-0.4f, 0.16f), tint = o),
                        FrameParticle("H1", "H", listOf(0), v(-0.62f, -0.07f), tint = h),
                        FrameParticle("H2", "H", listOf(0), v(-0.18f, -0.07f), tint = h),
                        FrameParticle("O2", "O", listOf(2, 4), v(0.4f, 0.16f), tint = o),
                        FrameParticle("H3", "H", listOf(0), v(0.18f, -0.07f), tint = h),
                        FrameParticle("H4", "H", listOf(0), v(0.62f, -0.07f), tint = h),
                    ),
                pairs =
                    listOf(
                        SharedPair("H1", "O1", bias = 0.62f),
                        SharedPair("H2", "O1", bias = 0.62f),
                        SharedPair("H3", "O2", bias = 0.62f),
                        SharedPair("H4", "O2", bias = 0.62f),
                    ),
            )
        }

        private val H2_O2 =
            Reaction(
                id = "h2_o2",
                reactants = listOf(Term(2, "H2"), Term(1, "O2")),
                products = listOf(Term(2, "H2O")),
                condition = IGNITE,
                types = listOf(ReactionType.COMBINATION, ReactionType.REDOX, ReactionType.COVALENT_BOND),
                stages = setOf(SchoolStage.JUNIOR, SchoolStage.SENIOR),
                redox = listOf(RedoxChange("H", 0, 1, 4), RedoxChange("O", 0, -2, 2)),
                loseLabel = L10n("H 偏离 e⁻", "e⁻ shift from H"),
                gainLabel = L10n("O 偏向 e⁻", "e⁻ shift to O"),
                steps =
                    listOf(
                        ReactionStep(
                            title = L10n("反应物：2H₂ + O₂", "Reactants: 2H₂ + O₂"),
                            why =
                                L10n(
                                    "H₂ 里两个 H 共用 1 对电子；O₂ 里两个 O 共用 2 对电子（双键）。",
                                    "In H₂ the two H atoms share 1 pair; in O₂ the two O atoms share 2 pairs (a double bond).",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("H1", "H", listOf(0), v(-0.72f, 0.4f)),
                                            FrameParticle("H2", "H", listOf(0), v(-0.5f, 0.4f)),
                                            FrameParticle("H3", "H", listOf(0), v(-0.72f, -0.4f)),
                                            FrameParticle("H4", "H", listOf(0), v(-0.5f, -0.4f)),
                                            FrameParticle("O1", "O", listOf(2, 4), v(0.26f, 0f)),
                                            FrameParticle("O2", "O", listOf(2, 4), v(0.58f, 0f)),
                                        ),
                                    pairs =
                                        listOf(
                                            SharedPair("H1", "H2"),
                                            SharedPair("H3", "H4"),
                                            SharedPair("O1", "O2", count = 4),
                                        ),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("点燃，分子拆成原子", "Ignite: molecules split into atoms"),
                            why =
                                L10n(
                                    "H 最外层 1 个电子，差 1 个到 2；O 最外层 6 个，差 2 个到 8。金色虚圈是空位。",
                                    "H has 1 outer electron and needs 1 more for 2; O has 6 and needs 2 more for 8. Gold rings mark the slots.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("H1", "H", listOf(1), v(-0.75f, 0.42f), vacancies = 1),
                                            FrameParticle("H2", "H", listOf(1), v(-0.4f, 0.42f), vacancies = 1),
                                            FrameParticle("H3", "H", listOf(1), v(-0.75f, -0.42f), vacancies = 1),
                                            FrameParticle("H4", "H", listOf(1), v(-0.4f, -0.42f), vacancies = 1),
                                            FrameParticle("O1", "O", listOf(2, 6), v(0.4f, 0.36f), vacancies = 2),
                                            FrameParticle("O2", "O", listOf(2, 6), v(0.4f, -0.36f), vacancies = 2),
                                        ),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("H 和 O 共用电子对", "H and O share electron pairs"),
                            why =
                                L10n(
                                    "不是整颗电子飞走，而是和 O 共用一对电子；O 吸电子能力强，电子对偏向 O。按化合价算 H 0 → +1、O 0 → −2，共转移 4 e⁻。",
                                    "No electron leaves outright: each H shares a pair with O, and the pair leans toward O, which pulls harder. By oxidation state H 0 → +1, O 0 → −2: 4 e⁻ in all.",
                                ),
                            frame = waterFrame(partial = false),
                            moved = 4,
                        ),
                        ReactionStep(
                            title = L10n("生成 2 个 H₂O", "2 H₂O form"),
                            why =
                                L10n(
                                    "电子对偏向 O，所以 O 一端略带负电（δ−，蓝），H 一端略带正电（δ+，红）。这是共价键，不是离子键。",
                                    "With the pairs leaning to O, the O end is slightly negative (δ−, blue) and the H ends slightly positive (δ+, red). A covalent bond, not an ionic one.",
                                ),
                            frame = waterFrame(partial = true),
                            moved = 4,
                        ),
                    ),
                property =
                    L10n(
                        "氢气有可燃性，燃烧只生成水，是清洁燃料；点燃前必须验纯，混有空气点燃会爆炸。",
                        "Hydrogen burns to water only, a clean fuel. Test its purity before lighting: mixed with air it explodes.",
                    ),
                phenomenon =
                    L10n(
                        "纯净的氢气安静燃烧，产生淡蓝色火焰；罩在火焰上方的干冷烧杯内壁出现水雾。",
                        "Pure hydrogen burns quietly with a pale blue flame; a cold dry beaker held above fogs with water.",
                    ),
                source =
                    L10n(
                        "人教版九年级上册 第四单元「自然界的水」课题 3「水的组成」；高中必修第一册 第四章第三节「化学键」",
                        "PEP Chemistry Grade 9 Vol. 1, Unit 4 (Composition of water); Senior Compulsory Book 1, Ch. 4 §3",
                    ),
            )

        // ---- Fe + CuSO4 = FeSO4 + Cu -------------------------------------------------------
        private val SULFATE = listOf("S", "O", "O", "O", "O")

        private val FE_CUSO4 =
            Reaction(
                id = "fe_cuso4",
                reactants = listOf(Term(1, "Fe"), Term(1, "CuSO4")),
                products = listOf(Term(1, "FeSO4"), Term(1, "Cu")),
                condition = null,
                types = listOf(ReactionType.DISPLACEMENT, ReactionType.REDOX),
                stages = setOf(SchoolStage.JUNIOR, SchoolStage.SENIOR),
                redox = listOf(RedoxChange("Fe", 0, 2, 1), RedoxChange("Cu", 2, 0, 1)),
                loseLabel = L10n("Fe 失去 e⁻", "Fe loses e⁻"),
                gainLabel = L10n("Cu²⁺ 得到 e⁻", "Cu²⁺ gains e⁻"),
                steps =
                    listOf(
                        ReactionStep(
                            title = L10n("反应物：Fe + 溶液里的 Cu²⁺、SO₄²⁻", "Reactants: Fe + Cu²⁺ and SO₄²⁻ in solution"),
                            why =
                                L10n(
                                    "CuSO₄ 溶在水里是 Cu²⁺ 和 SO₄²⁻ 两种离子。Fe 最外层有 2 个电子。",
                                    "In water CuSO₄ is two ions, Cu²⁺ and SO₄²⁻. Fe has 2 outer electrons.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Fe", "Fe", listOf(2, 8, 14, 2), v(-0.6f, 0f)),
                                            FrameParticle("Cu", "Cu", listOf(2, 8, 17), v(0.38f, 0.22f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("SO4", "S", emptyList(), v(0.5f, -0.46f), tint = ChargeTint.NEGATIVE, cluster = SULFATE),
                                        ),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("Fe 靠近 Cu²⁺", "Fe meets Cu²⁺"),
                            why =
                                L10n(
                                    "金属活动性 Fe > Cu：Fe 比 Cu 更容易失去电子。",
                                    "In the activity series Fe is above Cu: Fe gives up electrons more easily.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Fe", "Fe", listOf(2, 8, 14, 2), v(-0.16f, 0.1f)),
                                            FrameParticle("Cu", "Cu", listOf(2, 8, 17), v(0.38f, 0.22f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("SO4", "S", emptyList(), v(0.5f, -0.46f), tint = ChargeTint.NEGATIVE, cluster = SULFATE),
                                        ),
                                ),
                            moved = 0,
                        ),
                        ReactionStep(
                            title = L10n("Fe 失 2 e⁻ 给 Cu²⁺", "Fe gives 2 e⁻ to Cu²⁺"),
                            why =
                                L10n(
                                    "Fe 的 2 个最外层电子转给 Cu²⁺：Fe 0 → +2 被氧化，Cu +2 → 0 被还原。SO₄²⁻ 不参与，是旁观离子。",
                                    "Fe's 2 outer electrons pass to Cu²⁺: Fe 0 → +2 (oxidised), Cu +2 → 0 (reduced). SO₄²⁻ takes no part: a spectator ion.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Fe", "Fe", listOf(2, 8, 14, 0), v(-0.16f, 0.1f)),
                                            FrameParticle("Cu", "Cu", listOf(2, 8, 17), v(0.38f, 0.22f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("SO4", "S", emptyList(), v(0.5f, -0.46f), tint = ChargeTint.NEGATIVE, cluster = SULFATE),
                                        ),
                                    flights = listOf(ElectronFlight("Fe", "Cu", count = 2)),
                                ),
                            moved = 2,
                        ),
                        ReactionStep(
                            title = L10n("生成 Fe²⁺ 和 Cu，SO₄²⁻ 不变", "Fe²⁺ and Cu form; SO₄²⁻ is unchanged"),
                            why =
                                L10n(
                                    "Fe 变成 Fe²⁺ 进入溶液，和 SO₄²⁻ 组成 FeSO₄；Cu²⁺ 变成 Cu 原子，附着在铁表面。",
                                    "Fe becomes Fe²⁺ in solution, pairing with SO₄²⁻ as FeSO₄; Cu²⁺ becomes Cu atoms that coat the iron.",
                                ),
                            frame =
                                ReactionFrame(
                                    particles =
                                        listOf(
                                            FrameParticle("Fe", "Fe", listOf(2, 8, 14), v(-0.16f, 0.1f), tint = ChargeTint.POSITIVE),
                                            FrameParticle("Cu", "Cu", listOf(2, 8, 18, 1), v(0.38f, 0.22f), gold = mapOf(2 to 1, 3 to 1)),
                                            FrameParticle("SO4", "S", emptyList(), v(0.5f, -0.46f), tint = ChargeTint.NEGATIVE, cluster = SULFATE),
                                        ),
                                ),
                            moved = 2,
                        ),
                    ),
                property =
                    L10n(
                        "铁比铜活泼，能把铜从它的盐溶液里置换出来；古代「湿法炼铜」就是这个反应。",
                        "Iron is more active than copper and displaces it from its salt solution — the old 'wet' way of making copper.",
                    ),
                phenomenon =
                    L10n(
                        "铁表面析出红色的铜，溶液由蓝色逐渐变成浅绿色。",
                        "Red copper coats the iron; the solution slowly turns from blue to pale green.",
                    ),
                source =
                    L10n(
                        "人教版九年级下册 第八单元 课题 2「金属的化学性质」；高中必修第一册 第一章第三节「氧化还原反应」",
                        "PEP Chemistry Grade 9 Vol. 2, Unit 8 §2 (Properties of metals); Senior Compulsory Book 1, Ch. 1 §3 (Redox)",
                    ),
            )

        val LIBRARY: List<Reaction> = listOf(NA_CL2, H2_O2, FE_CUSO4)
    }
}
