package com.example.elementspace.p7c3a91e4.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory

/**
 * App-owned colours taken from the accepted Figma design. They tint content only; every text,
 * divider and control state colour still comes from the native SpatialUI colour scheme.
 */
object ElementColors {
    // Category spectrum: hue follows the column position, warm on the left to violet on the right.
    val CategoryAlkaliMetal = Color(0xFFD9483B) // design-style: fixed-figma-color Category/01
    val CategoryAlkalineEarth = Color(0xFFC2621A) // design-style: fixed-figma-color Category/02
    val CategoryTransitionMetal = Color(0xFF9A7B14) // design-style: fixed-figma-color Category/03
    val CategoryPostTransition = Color(0xFF4F8A27) // design-style: fixed-figma-color Category/04
    val CategoryMetalloid = Color(0xFF1A8770) // design-style: fixed-figma-color Category/05
    val CategoryNonmetal = Color(0xFF1B7CAB) // design-style: fixed-figma-color Category/06
    val CategoryHalogen = Color(0xFF3D6FD9) // design-style: fixed-figma-color Category/07
    val CategoryNobleGas = Color(0xFF7A5BD6) // design-style: fixed-figma-color Category/08
    val CategoryLanthanide = Color(0xFFC24A8F) // design-style: fixed-figma-color Category/09
    val CategoryActinide = Color(0xFF9C4BB5) // design-style: fixed-figma-color Category/10

    val ParticleProton = Color(0xFFFF6B5E) // design-style: fixed-figma-color Particle/proton
    val ParticleNeutron = Color(0xFFA9B8C8) // design-style: fixed-figma-color Particle/neutron
    val ParticleElectron = Color(0xFF5AD4FF) // design-style: fixed-figma-color Particle/electron

    val TrendLow = Color(0xFF2F5FA8) // design-style: fixed-figma-color Trend ramp low
    val TrendMid = Color(0xFF8A4FB5) // design-style: fixed-figma-color Trend ramp mid
    val TrendHigh = Color(0xFFD9483B) // design-style: fixed-figma-color Trend ramp high

    val OrbitalS = Color(0xFFFFD166) // design-style: fixed-figma-color Orbital s
    val OrbitalP = Color(0xFFC9A4FF) // design-style: fixed-figma-color Orbital p
    val OrbitalD = Color(0xFF7BE495) // design-style: fixed-figma-color Orbital d
    val OrbitalF = Color(0xFFFF9F7F) // design-style: fixed-figma-color Orbital f

    val LatticeAtom = Color(0xFF737986) // design-style: fixed-figma-color Lattice atom
    val LatticeBond = Color(0xFFE8ECF0) // design-style: fixed-figma-color Lattice bond

    // Reaction Volume. Gold marks an electron that moved from one atom to another (sample M1).
    val ElectronTransferred = Color(0xFFFFB020) // design-style: fixed-sample-color Reaction/transferred electron
    val IonPositive = Color(0xFFFF6B5E) // design-style: fixed-sample-color Reaction/cation halo
    val IonNegative = Color(0xFF5A8CFF) // design-style: fixed-sample-color Reaction/anion halo
    val OverlayCard = Color(0xFFF3F4F6) // design-style: fixed-sample-color Settings card, light like the system glass
    val ClusterArm = Color(0xFFE5483B) // design-style: fixed-sample-color Reaction/oxygen in a spectator ion (model O)

    fun clusterCentre(symbol: String): Color = MODEL[symbol] ?: LatticeAtom // design-style: fixed-sample-color Reaction/sulfur

    /**
     * Nucleus colours in the reaction Volume: the ball-and-stick colours of school models (H white,
     * O red, Cl green, …). Category colours could not be used there: H and O are both non-metals and
     * came out the same blue (emulator check v1.1.0).
     */
    private val MODEL = mapOf(
        "H" to Color(0xFFF2F4F7), // design-style: fixed-sample-color model/H
        "O" to Color(0xFFE5483B), // design-style: fixed-sample-color model/O
        "Na" to Color(0xFF9B6BE0), // design-style: fixed-sample-color model/Na
        "Cl" to Color(0xFF39C26B), // design-style: fixed-sample-color model/Cl
        "Fe" to Color(0xFF9AA6B8), // design-style: fixed-sample-color model/Fe
        "Cu" to Color(0xFFD9803F), // design-style: fixed-sample-color model/Cu
        "S" to Color(0xFFE8CF4A), // design-style: fixed-sample-color model/S
        "C" to Color(0xFF4A4F58), // design-style: fixed-sample-color model/C
        "N" to Color(0xFF4F7BEA), // design-style: fixed-sample-color model/N
        "Mg" to Color(0xFF6FCF6A), // design-style: fixed-sample-color model/Mg
    )

    fun model(symbol: String, category: ElementCategory): Color = MODEL[symbol] ?: category(category)

    fun category(category: ElementCategory): Color =
        when (category) {
            ElementCategory.ALKALI_METAL -> CategoryAlkaliMetal
            ElementCategory.ALKALINE_EARTH -> CategoryAlkalineEarth
            ElementCategory.TRANSITION_METAL -> CategoryTransitionMetal
            ElementCategory.POST_TRANSITION -> CategoryPostTransition
            ElementCategory.METALLOID -> CategoryMetalloid
            ElementCategory.NONMETAL -> CategoryNonmetal
            ElementCategory.HALOGEN -> CategoryHalogen
            ElementCategory.NOBLE_GAS -> CategoryNobleGas
            ElementCategory.LANTHANIDE -> CategoryLanthanide
            ElementCategory.ACTINIDE -> CategoryActinide
        }

    fun orbital(l: Int): Color =
        when (l) {
            0 -> OrbitalS
            1 -> OrbitalP
            2 -> OrbitalD
            else -> OrbitalF
        }

    /** Colour for a normalised trend value in 0..1. */
    fun trend(t: Float): Color {
        val x = t.coerceIn(0f, 1f)
        return if (x < 0.5f) lerp(TrendLow, TrendMid, x * 2f) else lerp(TrendMid, TrendHigh, (x - 0.5f) * 2f)
    }
}
