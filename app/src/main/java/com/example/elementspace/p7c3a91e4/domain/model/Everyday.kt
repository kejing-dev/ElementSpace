package com.example.elementspace.p7c3a91e4.domain.model

/** What "看里面" opens for an everyday thing: something the app can already show in 3D. */
sealed interface ThingInside {
    data class Molecule(val id: String) : ThingInside

    /** An allotrope of the element whose card the thing is on. */
    data class Allotrope(val atomicNumber: Int, val index: Int) : ThingInside

    data class Reaction(val id: String) : ThingInside
}

/**
 * An everyday object that contains an element (v1.8.0 "生活里的它"). [id] is also the model file:
 * assets/models/things/<id>.glb. [inside] and its button text are null when there is nothing more
 * to open than the atom itself.
 */
data class EverydayThing(
    val id: String,
    val symbols: List<String>,
    val name: L10n,
    val line: L10n,
    val inside: ThingInside? = null,
    val insideLabel: L10n? = null,
    /** Balloons float: the model bobs gently instead of standing still. */
    val floats: Boolean = false,
    /** The model is glassware: drawn with a clear glass material instead of its own (untextured) one. */
    val glass: Boolean = false,
)
