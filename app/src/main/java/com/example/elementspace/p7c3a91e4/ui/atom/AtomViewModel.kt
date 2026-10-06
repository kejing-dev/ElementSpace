package com.example.elementspace.p7c3a91e4.ui.atom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.ElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildAllotropeLatticeUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildNucleusLayoutUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.ElectronConfigurationUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.SampleOrbitalCloudUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AtomViewModel(
    private val atomicNumber: Int,
    elementRepository: ElementRepository,
    private val sessionRepository: SessionRepository,
    private val electronConfiguration: ElectronConfigurationUseCase,
    private val buildNucleus: BuildNucleusLayoutUseCase,
    private val sampleCloud: SampleOrbitalCloudUseCase,
    private val buildLattice: BuildAllotropeLatticeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AtomUiState())
    val state: StateFlow<AtomUiState> = _state.asStateFlow()

    init {
        val element = elementRepository.element(atomicNumber)
        if (element != null) {
            val neutral = electronConfiguration(atomicNumber)
            val valence = neutral.valenceElectrons
            _state.value =
                derive(
                    AtomUiState(
                        element = element,
                        isotopes = elementRepository.isotopes(atomicNumber),
                        allotropes = elementRepository.allotropes(atomicNumber),
                        maxCharge = maxCharge(element.category, valence, atomicNumber),
                        minCharge = minCharge(element.category, valence, atomicNumber),
                    )
                )
        }
        viewModelScope.launch {
            sessionRepository.language.collect { language -> _state.update { it.copy(language = language) } }
        }
    }

    fun onEvent(event: AtomEvent) {
        when (event) {
            is AtomEvent.ViewSelected -> change { it.copy(view = event.view) }
            AtomEvent.AdvancedToggled -> toggleAdvanced()
            is AtomEvent.IsotopeSelected ->
                change { if (event.index in it.isotopes.indices) it.copy(isotopeIndex = event.index) else it }
            is AtomEvent.ChargeStepped ->
                change { it.copy(charge = (it.charge + event.delta).coerceIn(it.minCharge, it.maxCharge)) }
            is AtomEvent.AllotropeStepped ->
                change {
                    if (it.allotropes.isEmpty()) it
                    else it.copy(allotropeIndex = (it.allotropeIndex + event.delta).mod(it.allotropes.size))
                }
            is AtomEvent.AllotropeSelected ->
                change { if (event.index in it.allotropes.indices) it.copy(allotropeIndex = event.index) else it }
            is AtomEvent.SubshellToggled ->
                _state.update {
                    val hidden = it.hiddenSubshells
                    it.copy(hiddenSubshells = if (event.label in hidden) hidden - event.label else hidden + event.label)
                }
            AtomEvent.PauseToggled -> _state.update { it.copy(paused = !it.paused) }
            AtomEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
            AtomEvent.Touched -> sessionRepository.touchAtom(atomicNumber)
            AtomEvent.Closed -> sessionRepository.closeAtom(atomicNumber)
        }
    }

    private fun toggleAdvanced() {
        change { current ->
            if (current.advancedExpanded) {
                // Collapsing leaves the advanced views and returns to a neutral atom.
                current.copy(
                    advancedExpanded = false,
                    view = if (current.view.advanced) AtomViewMode.SHELL else current.view,
                    charge = 0,
                )
            } else {
                current.copy(advancedExpanded = true)
            }
        }
    }

    private fun change(block: (AtomUiState) -> AtomUiState) {
        sessionRepository.touchAtom(atomicNumber)
        _state.update { derive(block(it)) }
    }

    /** Rebuilds the configuration and 3D geometry that follow from the selections. */
    private fun derive(state: AtomUiState): AtomUiState {
        val element = state.element ?: return state
        val configuration = electronConfiguration(element.atomicNumber, state.charge)
        val outermost = configuration.shells.size.coerceAtLeast(1)
        return state.copy(
            configuration = configuration,
            nucleus = buildNucleus(state.protons, state.neutrons),
            clouds =
                if (state.view == AtomViewMode.CLOUD) {
                    val perElectron = (CLOUD_POINT_BUDGET / state.electrons.coerceAtLeast(1)).coerceIn(12, 90)
                    configuration.subshells.map { sampleCloud(it, outermost, perElectron) }
                } else {
                    emptyList()
                },
            lattice = if (state.view == AtomViewMode.ALLOTROPE) state.allotrope?.let { buildLattice(it.structure) } else null,
        )
    }

    /** Electrons an atom commonly gives up: its valence electrons, none for noble gases. */
    private fun maxCharge(category: ElementCategory, valence: Int, atomicNumber: Int): Int =
        when (category) {
            ElementCategory.NOBLE_GAS -> 0
            ElementCategory.TRANSITION_METAL, ElementCategory.LANTHANIDE, ElementCategory.ACTINIDE -> 3
            else -> minOf(valence, 4, atomicNumber)
        }

    /** Electrons an atom commonly gains to complete its outer shell. */
    private fun minCharge(category: ElementCategory, valence: Int, atomicNumber: Int): Int =
        when {
            category == ElementCategory.NOBLE_GAS -> 0
            atomicNumber == 1 -> -1
            category == ElementCategory.NONMETAL || category == ElementCategory.HALOGEN -> -(8 - valence).coerceIn(0, 4)
            else -> 0
        }

    private companion object {
        /** Upper bound on cloud points so heavy atoms stay within the frame budget. */
        const val CLOUD_POINT_BUDGET = 2400
    }
}
