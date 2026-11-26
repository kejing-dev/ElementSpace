package com.example.elementspace.p7c3a91e4.ui.molecule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.data.repository.MoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.ReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoleculeUiState(
    val molecule: Molecule? = null,
    /** The molecule shown beside it, or null when not comparing. */
    val compare: Molecule? = null,
    /** The molecule the card offers to compare with. */
    val partner: Molecule? = null,
    /** Reactions this molecule takes part in that exist in the reaction library. */
    val reactions: List<Reaction> = emptyList(),
    val language: AppLanguage = AppLanguage.ZH,
    val mode: MoleculeMode = MoleculeMode.BALL_AND_STICK,
    val showAngles: Boolean = true,
    val showGroups: Boolean = true,
    /** Element symbols printed on the atoms (v1.3.0). */
    val showLabels: Boolean = true,
    /** Bumped to ask the 3D view to return to its starting orientation and scale. */
    val resetToken: Int = 0,
) {
    val comparing: Boolean
        get() = compare != null

    /** Same molecular formula: the comparison card says they are isomers. */
    val isomers: Boolean
        get() = compare != null && molecule?.formula == compare.formula
}

sealed interface MoleculeEvent {
    data class ModeSelected(val mode: MoleculeMode) : MoleculeEvent

    data object AnglesToggled : MoleculeEvent

    data object GroupsToggled : MoleculeEvent

    data object LabelsToggled : MoleculeEvent

    data object ResetView : MoleculeEvent

    /** Shows [id] beside the current molecule. */
    data class CompareOpened(val id: String) : MoleculeEvent

    data object CompareClosed : MoleculeEvent

    /** Shows the reaction in the reaction Volume; the screen opens the window. */
    data class ReactionOpened(val id: String) : MoleculeEvent
}

/** The single molecule Volume: shows whatever the session names, so picking another molecule swaps it in place. */
class MoleculeViewModel(
    private val molecules: MoleculeRepository,
    private val reactions: ReactionRepository,
    private val session: SessionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MoleculeUiState())
    val state: StateFlow<MoleculeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { session.language.collect { language -> _state.update { it.copy(language = language) } } }
        viewModelScope.launch {
            session.openMolecule.collect { view ->
                val molecule = view?.let { molecules.molecule(it.id) }
                _state.update { current ->
                    val changed = molecule?.id != current.molecule?.id
                    current.copy(
                        molecule = molecule,
                        compare = view?.compare?.let { molecules.molecule(it) },
                        partner = molecule?.compareWith?.let { molecules.molecule(it) },
                        reactions = molecule?.reactionIds.orEmpty().mapNotNull { reactions.reaction(it) },
                        // A new molecule starts face-on again; toggles carry over.
                        resetToken = if (changed) current.resetToken + 1 else current.resetToken,
                    )
                }
            }
        }
    }

    fun onEvent(event: MoleculeEvent) {
        when (event) {
            is MoleculeEvent.ModeSelected -> _state.update { it.copy(mode = event.mode) }
            MoleculeEvent.AnglesToggled -> _state.update { it.copy(showAngles = !it.showAngles) }
            MoleculeEvent.GroupsToggled -> _state.update { it.copy(showGroups = !it.showGroups) }
            MoleculeEvent.LabelsToggled -> _state.update { it.copy(showLabels = !it.showLabels) }
            MoleculeEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
            is MoleculeEvent.CompareOpened -> session.compareMolecule(event.id)
            MoleculeEvent.CompareClosed -> session.compareMolecule(null)
            is MoleculeEvent.ReactionOpened -> session.showReaction(event.id)
        }
    }
}

class MoleculeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MoleculeViewModel(BundledMoleculeRepository.shared, BundledReactionRepository.shared, InMemorySessionRepository.shared) as T
}
