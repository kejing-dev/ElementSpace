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
