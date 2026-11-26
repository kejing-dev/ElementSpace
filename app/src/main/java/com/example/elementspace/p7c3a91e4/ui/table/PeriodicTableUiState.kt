package com.example.elementspace.p7c3a91e4.ui.table

import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.GuidePath
import com.example.elementspace.p7c3a91e4.domain.model.LearningLevel
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.domain.usecase.CollideResult
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScale

/** A window operation the screen must perform once, then acknowledge. */
data class AtomWindowRequest(
    val open: Int,
    /** Volume to close first because the limit was reached, or null. */
    val close: Int?,
)

enum class AuxWindow { REACTION, MOLECULE, LAB }

/** [step] is the next step to do; equal to the number of steps when the path is finished. */
data class GuideProgress(val path: GuidePath, val step: Int = 0) {
    val finished: Boolean
        get() = step >= path.steps.size
}

/** Two picked elements and what happens when they meet. */
data class CollideOutcome(val a: Element, val b: Element, val result: CollideResult)

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
    /** The settings menu under the title bar's Settings button (language, how-to, exit). */
    val languageMenuOpen: Boolean = false,
    /** Full-window overlays opened from settings; BACK closes the topmost. */
    val howToOpen: Boolean = false,
    val exitConfirmOpen: Boolean = false,
    val windowRequest: AtomWindowRequest? = null,
    /** Element whose Volume was just replaced, shown once as a snackbar. */
    val replacedNotice: Element? = null,
    /** 碰一碰 (v1.6.0): tapping tiles picks two elements instead of opening an atom. */
    val collideMode: Boolean = false,
    val collidePicks: List<Int> = emptyList(),
    val collideOutcome: CollideOutcome? = null,
    val level: LearningLevel = LearningLevel.BASIC,
    /** 跟我学 (v1.7.0): the list of paths, and the path being followed. */
    val guideListOpen: Boolean = false,
    val guide: GuideProgress? = null,
    /** First-launch card: three things to try. */
    val firstRunOpen: Boolean = false,
    /** A reaction or molecule Volume the screen must open once, then acknowledge. */
    val auxWindow: AuxWindow? = null,
) {
    val filtering: Boolean
        get() = query.isNotBlank() || category != null

    val overlayOpen: Boolean
        get() = howToOpen || exitConfirmOpen || collideOutcome != null || guideListOpen || firstRunOpen

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

    data class HowToVisible(val visible: Boolean) : PeriodicTableEvent

    data class ExitConfirmVisible(val visible: Boolean) : PeriodicTableEvent

    /** BACK / controller B: closes the topmost overlay or menu; never leaves the app. */
    data object BackPressed : PeriodicTableEvent

    data class LevelSelected(val level: LearningLevel) : PeriodicTableEvent

    data class GuideListVisible(val visible: Boolean) : PeriodicTableEvent

    data class GuideStarted(val id: String) : PeriodicTableEvent

    /** The step's single button: do what it says and move to the next step. */
    data object GuideNext : PeriodicTableEvent

    data object GuideExit : PeriodicTableEvent

    data class FirstRunVisible(val visible: Boolean) : PeriodicTableEvent

    /** Opens the lab bench on [id], or on the experiment last shown (the first one when none). */
    data class LabOpened(val id: String? = null) : PeriodicTableEvent

    data object AuxWindowHandled : PeriodicTableEvent

    data object CollideToggled : PeriodicTableEvent

    /** Closes the result card; the mode stays on for another try. */
    data object CollideDismissed : PeriodicTableEvent

    /** ▶ on the result card; the screen opens the reaction Volume. */
    data class CollideReactionOpened(val id: String) : PeriodicTableEvent

    /** "See this element" on the result card: leaves the mode and opens its atom. */
    data class CollideElementOpened(val atomicNumber: Int) : PeriodicTableEvent

    data object WindowRequestHandled : PeriodicTableEvent

    data object ReplacedNoticeShown : PeriodicTableEvent
}
