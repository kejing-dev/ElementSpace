package com.example.elementspace.p7c3a91e4.domain.model

/** How the carbon skeleton is laid out; the info card groups molecules by this. */
enum class Skeleton(val zh: String, val en: String) {
    CHAIN("链状 · 直链", "Chain · straight"),
    BRANCHED("链状 · 带支链", "Chain · branched"),
    RING("环状", "Ring"),
    INORGANIC("无机小分子", "Small inorganic"),
}

/** One atom of a molecule, position in ångström. [group] marks the functional group / feature. */
data class MolAtom(val symbol: String, val position: Vec3, val group: Boolean = false)

/** [order] 1, 2, 3; [AROMATIC] for the six equal bonds of a benzene ring. */
data class MolBond(val a: Int, val b: Int, val order: Int = 1) {
    /** Bonding electrons this bond adds to the valence of each end (aromatic counts 1.5). */
    val valence: Float
        get() = if (order == AROMATIC) 1.5f else order.toFloat()

    companion object {
        const val AROMATIC = 4
    }
}

/** A bond angle drawn as an arc at atom [at] between the bonds to [a] and [b]. */
data class BondAngle(val at: Int, val a: Int, val b: Int, val label: String, val degrees: Float)

data class Molecule(
    val id: String,
    val name: L10n,
    /** Molecular formula as written, e.g. "C2H6O"; checked against the atoms in tests. */
    val formula: String,
    /** Condensed structural formula, e.g. "CH3CH2OH" (digits become subscripts on screen). */
    val condensed: String,
    /** Plain-text structural formula drawn in a monospaced block, or null when too large. */
    val structural: String?,
    val skeleton: Skeleton,
    val atoms: List<MolAtom>,
    val bonds: List<MolBond>,
    val angles: List<BondAngle> = emptyList(),
    /** Ring atoms of an aromatic ring, drawn with a dashed inner circle. */
    val aromaticRing: List<Int> = emptyList(),
    val geometry: L10n,
    /** Functional group / feature name, or null when there is none to highlight. */
    val group: L10n?,
    val property: L10n,
    /** Id of the molecule it is compared with, and the one-line difference shown under each. */
    val compareWith: String? = null,
    val compareNote: L10n? = null,
    /** Reactions in the reaction library that this molecule takes part in. */
    val reactionIds: List<String> = emptyList(),
    val source: L10n,
) {
    val symbols: Set<String>
        get() = atoms.map { it.symbol }.toSet()
}
