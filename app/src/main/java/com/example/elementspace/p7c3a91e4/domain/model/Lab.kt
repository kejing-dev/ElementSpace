package com.example.elementspace.p7c3a91e4.domain.model

/** A piece of equipment on the bench: model assets/models/lab/<model>.glb, where it stands, how big. */
data class LabProp(
    val model: String,
    /** Position on the bench top, scene units: x right, z toward the viewer. */
    val x: Float,
    val z: Float,
    /** Height of the fitted model, scene units. */
    val height: Float,
    /** Drawn as clear glass (untextured Tripo glassware). */
    val glass: Boolean = false,
)

/** RGB 0..1 with alpha, kept in the domain so experiments stay plain data. */
data class LabColor(val r: Float, val g: Float, val b: Float, val a: Float = 1f)

/** What is visible in one stage of an experiment. Positions are relative to the bench top. */
sealed interface LabEffect {
    /** A flame: additive sprites rising from [at]. [size] 1 = an alcohol-lamp flame. */
    data class Flame(val at: Vec3, val color: LabColor, val size: Float = 1f, val light: Float = 1f) : LabEffect

    /** Sparks thrown out from [at] and pulled down by gravity. */
    data class Sparks(val at: Vec3, val color: LabColor, val rate: Float, val speed: Float) : LabEffect

    data class Smoke(val at: Vec3, val color: LabColor, val rate: Float) : LabEffect

    /** A steady bright glow (burning magnesium, thermite): a big sprite plus a point light. */
    data class Glow(val at: Vec3, val color: LabColor, val size: Float, val light: Float) : LabEffect

    /** Bubbles rising inside a liquid from around [at] up to [top]. */
    data class Bubbles(val at: Vec3, val top: Float, val rate: Float, val spread: Float) : LabEffect

    /** Flakes forming at [at] and sinking to [bottom] (a precipitate). */
    data class Flakes(val at: Vec3, val bottom: Float, val color: LabColor, val rate: Float, val spread: Float) : LabEffect

    /** A liquid in a container: a column of [radius] from [bottom] to [top], turning to [color] over [seconds]. */
    data class Liquid(val at: Vec3, val radius: Float, val height: Float, val color: LabColor, val seconds: Float = 0f) : LabEffect

    /** A small solid: a strip, pellet, wire or lump of the reactant or product. */
    data class Solid(
        val at: Vec3, val size: Vec3, val color: LabColor, val round: Boolean = false, val metal: Boolean = false, val glowing: Boolean = false,
        /** Dropped in on this step: it falls into place instead of just appearing. */
        val drops: Boolean = false,
    ) : LabEffect

    /** A pellet that darts about on a liquid surface (sodium on water). */
    data class Skater(val at: Vec3, val radius: Float, val size: Float, val color: LabColor) : LabEffect

    /** Crystals growing along a wire from [at] upward to [height] (a silver tree). */
    data class Crystals(val at: Vec3, val height: Float, val color: LabColor, val seconds: Float) : LabEffect
}

/** One tap: [button] is what you do, [observe] what the textbook says you then see. */
data class LabStep(val button: L10n, val observe: L10n, val effects: List<LabEffect>)

/**
 * Something to pick after the first step. Either a reagent that tints every flame (the salts of the
 * flame test), or, with [effects], what the bench shows instead of the step's own effects (which
 * metal goes into the acid, which solution meets the indicator). Choices apply to step 1 only.
 */
data class LabChoice(val label: L10n, val note: L10n, val flame: LabColor? = null, val effects: List<LabEffect>? = null)

data class Experiment(
    val id: String,
    val name: L10n,
    /** What is on the bench before anything is done. */
    val setup: L10n,
    val props: List<LabProp>,
    /** Shown before the first step. */
    val start: List<LabEffect>,
    val steps: List<LabStep>,
    val choices: List<LabChoice> = emptyList(),
    /** Heading over the choices; null = "换一种盐". */
    val choiceTitle: L10n? = null,
    /** Label of the small / medium / large control, or null when nothing is adjustable. */
    val adjust: L10n? = null,
    /** Reaction in the library for "看微观". */
    val reactionId: String? = null,
    val source: L10n,
)
