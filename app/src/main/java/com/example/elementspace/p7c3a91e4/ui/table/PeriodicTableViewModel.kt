package com.example.elementspace.p7c3a91e4.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.ElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementReactions
import com.example.elementspace.p7c3a91e4.data.repository.GuidePaths
import com.example.elementspace.p7c3a91e4.domain.model.AtomCommand
import com.example.elementspace.p7c3a91e4.domain.model.GuideAction
import com.example.elementspace.p7c3a91e4.domain.usecase.CollideElementsUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.FilterElementsUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScaleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeriodicTableViewModel(
    private val elementRepository: ElementRepository,
    private val sessionRepository: SessionRepository,
    private val filterElements: FilterElementsUseCase,
    private val trendScale: TrendScaleUseCase,
    private val collide: CollideElementsUseCase = CollideElementsUseCase(BundledReactionRepository.shared.all(), ElementReactions.NOTES),
) : ViewModel() {

    private val _state = MutableStateFlow(PeriodicTableUiState())
    val state: StateFlow<PeriodicTableUiState> = _state.asStateFlow()

    init {
        val elements = elementRepository.elements()
        _state.update {
            it.copy(
                elements = elements,
                matches = filterElements(elements, "", null),
                maxOpenAtoms = InMemorySessionRepository.MAX_OPEN_ATOMS,
            )
        }
        viewModelScope.launch {
            sessionRepository.language.collect { language -> _state.update { it.copy(language = language) } }
        }
        viewModelScope.launch { sessionRepository.level.collect { level -> _state.update { it.copy(level = level) } } }
        viewModelScope.launch {
            sessionRepository.openAtoms.collect { open -> _state.update { it.copy(openAtoms = open) } }
        }
    }

    fun onEvent(event: PeriodicTableEvent) {
        when (event) {
            is PeriodicTableEvent.QueryChanged -> refilter { it.copy(query = event.query) }
            is PeriodicTableEvent.CategoryToggled ->
                refilter { it.copy(category = if (it.category == event.category) null else event.category) }
            is PeriodicTableEvent.TrendSelected -> selectTrend(event.trend)
            is PeriodicTableEvent.ElementSelected -> if (_state.value.collideMode) pick(event.atomicNumber) else openAtom(event.atomicNumber)
            is PeriodicTableEvent.LevelSelected -> {
                sessionRepository.setLevel(event.level)
                _state.update { it.copy(languageMenuOpen = false) }
            }
            is PeriodicTableEvent.GuideListVisible -> _state.update { it.copy(guideListOpen = event.visible, languageMenuOpen = false) }
            is PeriodicTableEvent.GuideStarted ->
                GuidePaths.path(event.id)?.let { path ->
                    _state.update { it.copy(guideListOpen = false, guide = GuideProgress(path), collideMode = false, collidePicks = emptyList(), collideOutcome = null) }
                }
            PeriodicTableEvent.GuideNext -> guideNext()
            PeriodicTableEvent.GuideExit -> _state.update { it.copy(guide = null) }
            is PeriodicTableEvent.FirstRunVisible -> _state.update { it.copy(firstRunOpen = event.visible) }
            PeriodicTableEvent.AuxWindowHandled -> _state.update { it.copy(auxWindow = null) }
            PeriodicTableEvent.CollideToggled -> _state.update { it.copy(collideMode = !it.collideMode, collidePicks = emptyList(), collideOutcome = null, languageMenuOpen = false) }
            PeriodicTableEvent.CollideDismissed -> _state.update { it.copy(collidePicks = emptyList(), collideOutcome = null) }
            is PeriodicTableEvent.CollideReactionOpened -> sessionRepository.showReaction(event.id)
            is PeriodicTableEvent.CollideElementOpened -> {
                _state.update { it.copy(collideMode = false, collidePicks = emptyList(), collideOutcome = null) }
                openAtom(event.atomicNumber)
            }
            is PeriodicTableEvent.LanguageMenuVisible ->
                _state.update { it.copy(languageMenuOpen = event.visible) }
            is PeriodicTableEvent.LanguageSelected -> {
                sessionRepository.setLanguage(event.language)
                _state.update { it.copy(languageMenuOpen = false) }
            }
            is PeriodicTableEvent.HowToVisible ->
                _state.update { it.copy(howToOpen = event.visible, exitConfirmOpen = false, languageMenuOpen = false) }
            is PeriodicTableEvent.ExitConfirmVisible ->
                _state.update { it.copy(exitConfirmOpen = event.visible, howToOpen = false, languageMenuOpen = false) }
            PeriodicTableEvent.BackPressed ->
                _state.update {
                    when {
                        it.firstRunOpen -> it.copy(firstRunOpen = false)
                        it.guideListOpen -> it.copy(guideListOpen = false)
                        it.collideOutcome != null -> it.copy(collidePicks = emptyList(), collideOutcome = null)
                        it.exitConfirmOpen -> it.copy(exitConfirmOpen = false)
                        it.howToOpen -> it.copy(howToOpen = false)
                        it.languageMenuOpen -> it.copy(languageMenuOpen = false)
                        else -> it
                    }
                }
            PeriodicTableEvent.WindowRequestHandled -> _state.update { it.copy(windowRequest = null) }
            PeriodicTableEvent.ReplacedNoticeShown -> _state.update { it.copy(replacedNotice = null) }
        }
    }

    /** Runs the current step's action, then shows the next step (or the closing line). */
    private fun guideNext() {
        val progress = _state.value.guide ?: return
        val step = progress.path.steps.getOrNull(progress.step) ?: return
        // A result card left over from an earlier step would cover what this step opens.
        _state.update { it.copy(collideOutcome = null, collidePicks = emptyList(), collideMode = false) }
        when (val action = step.action) {
            is GuideAction.OpenAtom -> openAtom(action.atomicNumber)
            is GuideAction.Atom -> sessionRepository.sendAtomCommand(AtomCommand(action.atomicNumber, action.action))
            is GuideAction.Reaction -> {
                sessionRepository.showReaction(action.id)
                _state.update { it.copy(auxWindow = AuxWindow.REACTION) }
            }
            is GuideAction.Molecule -> {
                sessionRepository.showMolecule(action.id)
                _state.update { it.copy(auxWindow = AuxWindow.MOLECULE) }
            }
            is GuideAction.Collide -> {
                val a = elementRepository.element(action.a)
                val b = elementRepository.element(action.b)
                if (a != null && b != null) {
                    _state.update { it.copy(collideMode = true, collidePicks = listOf(a.atomicNumber, b.atomicNumber), collideOutcome = CollideOutcome(a, b, collide(a, b))) }
                }
            }
        }
        _state.update { it.copy(guide = progress.copy(step = progress.step + 1)) }
    }

    /** First tap picks, tapping it again unpicks, the second element shows the result. */
    private fun pick(atomicNumber: Int) {
        _state.update { current ->
            val picks = current.collidePicks
            when {
                atomicNumber in picks -> current.copy(collidePicks = picks - atomicNumber)
                picks.isEmpty() -> current.copy(collidePicks = listOf(atomicNumber))
                else -> {
                    val a = elementRepository.element(picks.first())
                    val b = elementRepository.element(atomicNumber)
                    if (a == null || b == null) current
                    else current.copy(collidePicks = listOf(a.atomicNumber, b.atomicNumber), collideOutcome = CollideOutcome(a, b, collide(a, b)))
                }
            }
        }
    }

    private fun refilter(change: (PeriodicTableUiState) -> PeriodicTableUiState) {
        _state.update { current ->
            val next = change(current)
            next.copy(matches = filterElements(next.elements, next.query, next.category))
        }
    }

    private fun selectTrend(trend: TrendProperty) {
        _state.update { it.copy(trend = trend, trendScale = trendScale(it.elements, trend)) }
    }

    private fun openAtom(atomicNumber: Int) {
        if (elementRepository.element(atomicNumber) == null) return
        val result = sessionRepository.openAtom(atomicNumber)
        _state.update { current ->
            current.copy(
                windowRequest = AtomWindowRequest(open = atomicNumber, close = result.replaced),
                replacedNotice = result.replaced?.let(elementRepository::element) ?: current.replacedNotice,
            )
        }
    }
}
