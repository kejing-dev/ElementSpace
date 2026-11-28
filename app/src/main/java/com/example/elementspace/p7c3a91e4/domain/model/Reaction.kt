package com.example.elementspace.p7c3a91e4.domain.model

/** Text in both app languages. Reaction content is data, so it carries its own translations. */
data class L10n(val zh: String, val en: String) {
    fun get(language: AppLanguage): String =
        when (language) {
            AppLanguage.ZH -> zh
            AppLanguage.EN -> en
        }
}

/**
 * One side of an equation: a coefficient and a formula such as "CuSO4" or "Ca(OH)2". [shown] is
 * an optional way to print it (e.g. "CH2=CH2"); [formula] is what gets counted.
 */
data class Term(val coefficient: Int, val formula: String, val shown: String? = null)

enum class ReactionType(val zh: String, val en: String) {
    COMBINATION("化合反应", "Combination"),
    DECOMPOSITION("分解反应", "Decomposition"),
    DISPLACEMENT("置换反应", "Displacement"),
    DOUBLE_DISPLACEMENT("复分解反应", "Double displacement"),
    REDOX("氧化还原", "Redox"),
    IONIC_BOND("离子键", "Ionic bond"),
    COVALENT_BOND("共价键", "Covalent bond"),
    SUBSTITUTION("取代反应", "Substitution"),
    ADDITION("加成反应", "Addition"),
    ESTERIFICATION("酯化反应", "Esterification"),
}

/** How the reaction Volume animates a reaction: electrons moving between Bohr models, or bonds in ball-and-stick molecules. */
enum class ReactionKind { ELECTRONS, BONDS }

/** School stage the reaction is taught in, used by the Junior / Senior filter. */
enum class SchoolStage { JUNIOR, SENIOR }

/**
 * Oxidation-state change of one element across the reaction. [atoms] is how many atoms of it
 * change in the balanced equation, so the electrons it moves are |to - from| x atoms.
 */
data class RedoxChange(val symbol: String, val from: Int, val to: Int, val atoms: Int) {
    val electrons: Int
        get() = kotlin.math.abs(to - from) * atoms

    val oxidised: Boolean
        get() = to > from
}

// ---- 3D frames ------------------------------------------------------------------------------

/** Tint of the faint sphere drawn around a charged particle so ions read at a glance. */
enum class ChargeTint { NONE, POSITIVE, NEGATIVE, PARTIAL_POSITIVE, PARTIAL_NEGATIVE }

/**
 * One atom or ion drawn as a Bohr model in the reaction Volume. [shells] are the electrons drawn
 * on each ring; electrons of a shared pair are not on the rings but in [ReactionFrame.pairs].
 * [gold] marks electrons that came from another atom (shell index -> count, drawn last on that
 * ring); [vacancies] are empty slots drawn on the outer ring.
 */
data class FrameParticle(
    val id: String,
    val symbol: String,
    val shells: List<Int>,
    val position: Vec3,
    val tint: ChargeTint = ChargeTint.NONE,
    val gold: Map<Int, Int> = emptyMap(),
    val vacancies: Int = 0,
    /** A grouped spectator ion such as SO4 2-, drawn as a small dim cluster without shells. */
    val cluster: List<String> = emptyList(),
    /** Inner electrons that are not drawn (v1.16.0 electron views draw the outer shells only). */
    val hidden: Int = 0,
    /** Text printed on the atom instead of the computed one, e.g. "O⁻" in a peroxide. */
    val label: String? = null,
    /** Ring the first drawn shell sits on: 1 when only the outer shell is drawn, so it keeps room for the symbol and lone pairs. */
    val ring: Int = 0,
)

/** A shared electron pair between two particles; [bias] 0.5 = centred, toward 1 = closer to [b]. */
data class SharedPair(val a: String, val b: String, val bias: Float = 0.5f, val count: Int = 2)

/** Electrons flying from the outer ring of [from] to the outer ring of [to] during a step. */
data class ElectronFlight(val from: String, val to: String, val count: Int = 1)

/** How a bond in a ball-and-stick frame is marked. */
enum class BondMark {
    NONE,
    /** About to break: drawn red and dashed. */
    BREAK,
    /** One bond of a double bond about to open (addition): one red dashed stick beside a normal one. */
    BREAK_ONE,
    /** Formed in this reaction: drawn gold. */
    NEW,
}

/** One atom of a ball-and-stick frame; [id] stays the same across steps so the atom glides. */
data class FrameAtom(val id: String, val symbol: String, val position: Vec3, val faded: Boolean = false)

data class FrameBond(val a: String, val b: String, val order: Int = 1, val mark: BondMark = BondMark.NONE)

data class ReactionFrame(
    val particles: List<FrameParticle> = emptyList(),
    val pairs: List<SharedPair> = emptyList(),
    val flights: List<ElectronFlight> = emptyList(),
    /** Ball-and-stick atoms and bonds ([ReactionKind.BONDS]); positions in scene units. */
    val atoms: List<FrameAtom> = emptyList(),
    val bonds: List<FrameBond> = emptyList(),
    /** Scene units per ångström for ball sizes; 0.13 for the hand-made reactions, less when an auto picture shrinks to fit. */
    val atomScale: Float = 0.13f,
    /** Ball radius factor: the schematic pictures draw larger balls so the symbols on them can be read. */
    val ballScale: Float = 1f,
    /** Size of the Bohr models: below 1 when a generated electron view has many atoms to fit. */
    val scale: Float = 1f,
    /** False when the frame is not something a textbook writes as a formula (a network solid drawn as one unit …). */
    val paper: Boolean = true,
)

/** What a reaction condition looks like in the Volume (v1.3.0). */
enum class ConditionKind(val icon: String) {
    /** A small glowing sun with rays on the molecule whose bond light breaks. */
    LIGHT("☀"),
    /** A burner flame under the molecules. */
    HEAT("🔥"),
    /** A small flame with sparks where the reactants meet. */
    IGNITE("🔥"),
    /** A label only: a catalyst is not a reactant and is not counted. */
    CATALYST("⚗"),
    /** A label only, for conditions that are not drawn: electric discharge, pressure, −196 ℃ … */
    NOTE("ℹ"),
}

data class ConditionVisual(val kind: ConditionKind, val label: L10n)

data class ReactionStep(
    val title: L10n,
    val why: L10n,
    val frame: ReactionFrame,
    /** Electrons moved so far, shown in the card counters. */
    val moved: Int,
)

data class Reaction(
    val id: String,
    val reactants: List<Term>,
    val products: List<Term>,
    /** Condition written over the arrow, e.g. 点燃 / ignite; null for none. */
    val condition: L10n?,
    val types: List<ReactionType>,
    val stages: Set<SchoolStage>,
    val redox: List<RedoxChange>,
    /** Labels of the two electron counters: who loses, who gains (or who the pair leans from / to). */
    val loseLabel: L10n,
    val gainLabel: L10n,
    val steps: List<ReactionStep>,
    val property: L10n,
    val phenomenon: L10n,
    val source: L10n,
    val kind: ReactionKind = ReactionKind.ELECTRONS,
    /** Drawn with ⇌ instead of ⟶. */
    val reversible: Boolean = false,
    /** The [condition] drawn in the Volume; shown on [conditionSteps] (0-based), the steps it acts in. */
    val visuals: List<ConditionVisual> = emptyList(),
    val conditionSteps: Set<Int> = setOf(1, 2),
    /** Built from the equation alone (M5): the 3D shows atoms regrouping, not real structure. */
    val schematic: Boolean = false,
    /** A generated electron view (v1.16.0): drawn from oxidation states and outer shells, not hand-made. */
    val sketch: Boolean = false,
) {
    fun conditionsAt(step: Int): List<ConditionVisual> = if (step in conditionSteps) visuals else emptyList()
}
