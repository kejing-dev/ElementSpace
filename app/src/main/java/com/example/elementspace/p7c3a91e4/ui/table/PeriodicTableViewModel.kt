package com.example.elementspace.p7c3a91e4.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.ElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
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
            is PeriodicTableEvent.ElementSelected -> openAtom(event.atomicNumber)
            is PeriodicTableEvent.LanguageMenuVisible ->
                _state.update { it.copy(languageMenuOpen = event.visible) }
            is PeriodicTableEvent.LanguageSelected -> {
                sessionRepository.setLanguage(event.language)
                _state.update { it.copy(languageMenuOpen = false) }
            }
            PeriodicTableEvent.WindowRequestHandled -> _state.update { it.copy(windowRequest = null) }
            PeriodicTableEvent.ReplacedNoticeShown -> _state.update { it.copy(replacedNotice = null) }
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
