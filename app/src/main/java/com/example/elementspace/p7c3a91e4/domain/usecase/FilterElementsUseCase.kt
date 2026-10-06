package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory

/**
 * Decides which tiles stay highlighted for a search query and a category filter.
 *
 * A query matches the Chinese name, the English name, the symbol or the atomic number, so "铁",
 * "iron", "Fe" and "26" all find the same tile.
 */
class FilterElementsUseCase {

    /** Returns the atomic numbers that match; all of them when nothing is being filtered. */
    operator fun invoke(
        elements: List<Element>,
        query: String,
        category: ElementCategory?,
    ): Set<Int> {
        val needle = query.trim()
        val inCategory = elements.filter { category == null || it.category == category }
        if (needle.isEmpty()) return inCategory.mapTo(HashSet()) { it.atomicNumber }

        needle.toIntOrNull()?.let { number ->
            return inCategory.filter { it.atomicNumber == number }.mapTo(HashSet()) { it.atomicNumber }
        }
        // A query that is exactly a symbol means that element, not every name starting with it:
        // "Fe" is iron, not also fermium.
        val bySymbol = inCategory.filter { it.symbol.equals(needle, ignoreCase = true) }
        val matched = bySymbol.ifEmpty { inCategory.filter { matchesName(it, needle) } }
        return matched.mapTo(HashSet()) { it.atomicNumber }
    }

    private fun matchesName(element: Element, needle: String): Boolean =
        element.nameZh.contains(needle) ||
            element.nameEn.startsWith(needle, ignoreCase = true) ||
            element.symbol.startsWith(needle, ignoreCase = true)
}
