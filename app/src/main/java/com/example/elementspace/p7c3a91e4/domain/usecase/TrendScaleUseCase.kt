package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty

/** Normalised trend values used for pillar height and colour in the trend height map. */
data class TrendScale(
    val min: Double,
    val max: Double,
    /** Atomic number → 0..1. Elements without data are absent and stay flat. */
    val normalized: Map<Int, Float>,
)

class TrendScaleUseCase {

    operator fun invoke(elements: List<Element>, property: TrendProperty): TrendScale? {
        if (property == TrendProperty.CATEGORY) return null
        val values = elements.mapNotNull { e -> e.trendValue(property)?.let { e.atomicNumber to it } }
        if (values.isEmpty()) return null
        val min = values.minOf { it.second }
        val max = values.maxOf { it.second }
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        return TrendScale(
            min = min,
            max = max,
            normalized = values.associate { (z, v) -> z to ((v - min) / span).toFloat() },
        )
    }
}
