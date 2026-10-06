package com.example.elementspace.p7c3a91e4.ui.table

import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScale

/** A window operation the screen must perform once, then acknowledge. */
data class AtomWindowRequest(
    val open: Int,
    /** Volume to close first because the limit was reached, or null. */
    val close: Int?,
)

data class PeriodicTableUiState(
    val elements: List<Element> = emptyList(),
    val language: AppLanguage = AppLanguage.ZH,
    val query: String = "",
    val category: ElementCategory? = null,
    /** Atomic numbers that stay fully visible; the rest are dimmed while filtering. */
    val matches: Set<Int> = emptySet(),
    val trend: TrendProperty = TrendProperty.CATEGORY,
    val trendScale: TrendScale? = null,
    val openAtoms: List<Int> = emptyList(),
    val maxOpenAtoms: Int = 3,
    val languageMenuOpen: Boolean = false,
    val windowRequest: AtomWindowRequest? = null,
    /** Element whose Volume was just replaced, shown once as a snackbar. */
    val replacedNotice: Element? = null,
) {
    val filtering: Boolean
        get() = query.isNotBlank() || category != null

    val noMatch: Boolean
        get() = filtering && matches.isEmpty()
}

sealed interface PeriodicTableEvent {
    data class QueryChanged(val query: String) : PeriodicTableEvent

    data class CategoryToggled(val category: ElementCategory) : PeriodicTableEvent

    data class TrendSelected(val trend: TrendProperty) : PeriodicTableEvent

    data class ElementSelected(val atomicNumber: Int) : PeriodicTableEvent

    data class LanguageMenuVisible(val visible: Boolean) : PeriodicTableEvent

    data class LanguageSelected(val language: AppLanguage) : PeriodicTableEvent

    data object WindowRequestHandled : PeriodicTableEvent

    data object ReplacedNoticeShown : PeriodicTableEvent
}
