package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.Term

/** Parses plain formulas ("H2O", "CuSO4", "Ca(OH)2") and writes them with subscripts. */
object ChemicalFormula {
    private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉"

    /** Atoms per element symbol in one formula unit. */
    fun count(formula: String): Map<String, Int> {
        var index = 0

        fun number(): Int {
            val start = index
            while (index < formula.length && formula[index].isDigit()) index++
            return if (index == start) 1 else formula.substring(start, index).toInt()
        }

        fun group(): Map<String, Int> {
            val out = LinkedHashMap<String, Int>()
            while (index < formula.length) {
                val c = formula[index]
                when {
                    c == '(' -> {
                        index++
                        val inner = group()
                        require(index < formula.length && formula[index] == ')') { "Unclosed ( in $formula" }
                        index++
                        val times = number()
                        inner.forEach { (k, v) -> out[k] = (out[k] ?: 0) + v * times }
                    }
                    c == ')' -> return out
                    c.isUpperCase() -> {
                        val start = index++
                        while (index < formula.length && formula[index].isLowerCase()) index++
                        val symbol = formula.substring(start, index)
                        out[symbol] = (out[symbol] ?: 0) + number()
                    }
                    else -> throw IllegalArgumentException("Unexpected '$c' in $formula")
                }
            }
            return out
        }

        return group()
    }

    /** "CuSO4" -> "CuSO₄". */
    fun display(formula: String): String = formula.map { if (it.isDigit()) SUBSCRIPTS[it - '0'] else it }.joinToString("")

    /** Atoms per element on one side of an equation. */
    fun side(terms: List<Term>): Map<String, Int> {
        val out = HashMap<String, Int>()
        terms.forEach { term -> count(term.formula).forEach { (k, v) -> out[k] = (out[k] ?: 0) + v * term.coefficient } }
        return out
    }

    fun termText(term: Term): String = (if (term.coefficient == 1) "" else term.coefficient.toString()) + display(term.formula)

    fun sideText(terms: List<Term>): String = terms.joinToString(" + ") { termText(it) }

    /** True when every element has the same atom count on both sides. */
    fun balanced(reaction: Reaction): Boolean = side(reaction.reactants) == side(reaction.products)

    /** Electrons given up by oxidised elements equal electrons taken by reduced ones. */
    fun electronsConserved(reaction: Reaction): Boolean {
        val lost = reaction.redox.filter { it.oxidised }.sumOf { it.electrons }
        val gained = reaction.redox.filter { !it.oxidised }.sumOf { it.electrons }
        return lost == gained && lost > 0
    }

    fun electronsMoved(reaction: Reaction): Int = reaction.redox.filter { it.oxidised }.sumOf { it.electrons }
}
