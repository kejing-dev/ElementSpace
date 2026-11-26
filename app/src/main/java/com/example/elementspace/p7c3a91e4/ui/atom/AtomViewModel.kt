package com.example.elementspace.p7c3a91e4.ui.atom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.EverydayThings
import com.example.elementspace.p7c3a91e4.data.repository.MoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.ReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AtomAction
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildAllotropeLatticeUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildNucleusLayoutUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.ElectronConfigurationUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.SampleOrbitalCloudUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalRef
import com.example.elementspace.p7c3a91e4.domain.usecase.Orbitals
import com.example.elementspace.p7c3a91e4.domain.usecase.SampleOrbitalUseCase
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
    reactionRepository: ReactionRepository,
    moleculeRepository: MoleculeRepository = BundledMoleculeRepository.shared,
    private val sampleOrbital: SampleOrbitalUseCase = SampleOrbitalUseCase(),
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
                        reactions = reactionRepository.forSymbol(element.symbol),
                        molecules = moleculeRepository.forSymbol(element.symbol),
                        things = EverydayThings.forSymbol(element.symbol),
                        experiments = com.example.elementspace.p7c3a91e4.data.repository.Experiments.forSymbol(element.symbol),
                    )
                )
        }
        viewModelScope.launch {
            sessionRepository.language.collect { language -> _state.update { it.copy(language = language) } }
        }
        viewModelScope.launch { sessionRepository.level.collect { level -> _state.update { it.copy(level = level) } } }
        // Guided paths (v1.7.0) drive this atom with the same events the buttons send.
        viewModelScope.launch {
            sessionRepository.atomCommands.collect { command ->
                if (command.atomicNumber != atomicNumber) return@collect
                when (val action = command.action) {
                    is AtomAction.Charge -> onEvent(AtomEvent.ChargeStepped(action.delta))
                    is AtomAction.View -> show(action.view)
                    is AtomAction.Thing -> onEvent(AtomEvent.ThingSelected(action.id))
                    is AtomAction.Allotrope -> {
                        show(AtomViewMode.ALLOTROPE)
                        onEvent(AtomEvent.AllotropeSelected(action.index))
                    }
                }
            }
        }
    }

    fun onEvent(event: AtomEvent) {
        when (event) {
            is AtomEvent.ViewSelected -> change { it.copy(view = event.view, thing = null) }
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
            is AtomEvent.ReactionOpened -> {
                sessionRepository.touchAtom(atomicNumber)
                sessionRepository.showReaction(event.id)
            }
            is AtomEvent.ExperimentOpened -> sessionRepository.showExperiment(event.id)
            is AtomEvent.MoleculeOpened -> {
                sessionRepository.touchAtom(atomicNumber)
                sessionRepository.showMolecule(event.id)
            }
            is AtomEvent.ThingSelected -> {
                sessionRepository.touchAtom(atomicNumber)
                _state.update { s ->
                    val thing = event.id?.let { id -> s.things.firstOrNull { it.id == id } }
                    // A lamp starts switched on; a match starts unlit and a drink closed.
                    s.copy(thing = thing, thingActive = thing?.action?.switch == true)
                }
            }
            AtomEvent.ThingActionToggled -> _state.update { s -> if (s.thing?.action != null) s.copy(thingActive = !s.thingActive) else s }
            is AtomEvent.ThingAllotropeOpened -> {
                _state.update { it.copy(thing = null, thingActive = false) }
                show(AtomViewMode.ALLOTROPE)
                onEvent(AtomEvent.AllotropeSelected(event.index))
            }
            is AtomEvent.CloudModeSelected -> change { it.copy(singleOrbital = event.single) }
            is AtomEvent.OrbitalSelected -> change { it.copy(singleOrbital = true, orbital = event.ref) }
            is AtomEvent.OrbitalOptionToggled ->
                _state.update {
                    when (event.option) {
                        OrbitalOption.PHASE -> it.copy(showPhase = !it.showPhase)
                        OrbitalOption.NODES -> it.copy(showNodes = !it.showNodes)
                        OrbitalOption.AXES -> it.copy(showAxes = !it.showAxes)
                    }
                }
            AtomEvent.PauseToggled -> _state.update { it.copy(paused = !it.paused) }
            AtomEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
            AtomEvent.Touched -> sessionRepository.touchAtom(atomicNumber)
            AtomEvent.Closed -> sessionRepository.closeAtom(atomicNumber)
        }
    }

    private fun show(view: AtomViewMode) {
        if (view.advanced && !_state.value.advancedExpanded) onEvent(AtomEvent.AdvancedToggled)
        onEvent(AtomEvent.ViewSelected(view))
    }

    private fun toggleAdvanced() {
        change { current ->
            if (current.advancedExpanded) {
                // Collapsing leaves the advanced views; the charge stays (v1.6.0: making ions is a basic action).
                current.copy(
                    advancedExpanded = false,
                    view = if (current.view.advanced) AtomViewMode.SHELL else current.view,
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
        val cloud = state.view == AtomViewMode.CLOUD
        // Keep the chosen orbital while its subshell is occupied; otherwise the outermost one.
        val orbital =
            state.orbital?.takeIf { ref -> configuration.subshells.any { it.n == ref.n && it.l == ref.l } }
                ?: configuration.subshells.lastOrNull()?.let { OrbitalRef(it.n, it.l, 0) }
        val single = cloud && state.singleOrbital && orbital != null
        return state.copy(
            orbital = orbital,
            orbitalSample = if (single) state.orbitalSample?.takeIf { it.ref == orbital } ?: sampleOrbital(orbital!!) else null,
            configuration = configuration,
            nucleus = buildNucleus(state.protons, state.neutrons),
            clouds =
                if (cloud && !single) {
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
