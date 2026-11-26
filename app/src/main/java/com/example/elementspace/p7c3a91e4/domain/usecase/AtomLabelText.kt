package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.FrameParticle

/** The text printed on an atom in the reaction Volume (v1.3.0): its symbol, with the charge once it is an ion. */
object AtomLabelText {
    private const val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private const val SUBSCRIPT_DIGITS = "₀₁₂₃₄₅₆₇₈₉"

    /** "Na⁺", "O²⁻". */
    fun ion(symbol: String, value: Int): String = symbol + charge(value)

    fun charge(value: Int): String {
        if (value == 0) return ""
        val n = kotlin.math.abs(value)
        val digits = if (n == 1) "" else n.toString().map { SUPERSCRIPT_DIGITS[it - '0'] }.joinToString("")
        return digits + if (value > 0) "⁺" else "⁻"
    }

    /**
     * A full ion (tint POSITIVE / NEGATIVE) gets protons minus drawn electrons as its charge; atoms
     * with shared pairs or partial charges keep the bare symbol (their pair electrons are not on the
     * rings, so the count would be wrong). A grouped ion such as sulfate prints its formula.
     */
    fun particle(p: FrameParticle, atomicNumber: Int?): String {
        if (p.cluster.isNotEmpty()) return cluster(p)
        val full = p.tint == ChargeTint.POSITIVE || p.tint == ChargeTint.NEGATIVE
        if (!full || atomicNumber == null) return p.symbol
        return p.symbol + charge(atomicNumber - p.shells.sum())
    }

    private fun cluster(p: FrameParticle): String {
        val centre = p.cluster.first()
        val arms = p.cluster.drop(1).groupingBy { it }.eachCount()
        val formula = centre + arms.entries.joinToString("") { (s, n) -> s + if (n == 1) "" else n.toString().map { SUBSCRIPT_DIGITS[it - '0'] }.joinToString("") }
        // Only sulfate is drawn as a cluster; its charge is fixed by the textbook formula.
        val charge = if (formula == "SO₄") -2 else 0
        return formula + charge(charge)
    }
}
