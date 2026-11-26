package com.example.elementspace.p7c3a91e4.ui.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.Experiments
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Experiment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LabUiState(
    val language: AppLanguage = AppLanguage.ZH,
    val experiment: Experiment? = null,
    /** 0 = set up; k = step k has been done. */
    val stage: Int = 0,
    /** Index into the experiment's choices (the salt in the flame), or null. */
    val choice: Int? = null,
    /** 0 small, 1 medium, 2 large. */
    val power: Int = 1,
    val resetToken: Int = 0,
) {
    val finished: Boolean
        get() = experiment != null && stage >= experiment.steps.size

    val powerFactor: Float
        get() = POWER[power.coerceIn(0, 2)]

    companion object {
        val POWER = floatArrayOf(0.6f, 1f, 1.5f)
    }
}

sealed interface LabEvent {
    data class ExperimentSelected(val id: String) : LabEvent

    /** The one button: do the next step. */
    data object Next : LabEvent

    data object Restart : LabEvent

    data class ChoiceSelected(val index: Int) : LabEvent

    data class PowerSelected(val level: Int) : LabEvent

    /** "看微观": the screen opens the reaction Volume on this experiment's reaction. */
    data object MicroOpened : LabEvent

    data object ResetView : LabEvent
}

/** The single lab Volume: shows the experiment the session names. Every action is one tap. */
class LabViewModel(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(LabUiState())
    val state: StateFlow<LabUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { session.language.collect { language -> _state.update { it.copy(language = language) } } }
        viewModelScope.launch {
            session.openExperiment.collect { id ->
                val experiment = id?.let { Experiments.experiment(it) }
                _state.update { current ->
                    if (experiment?.id == current.experiment?.id) current
                    else current.copy(experiment = experiment, stage = 0, choice = null, power = 1, resetToken = current.resetToken + 1)
                }
            }
        }
    }

    fun onEvent(event: LabEvent) {
        when (event) {
            is LabEvent.ExperimentSelected -> session.showExperiment(event.id)
            LabEvent.Next -> _state.update { s -> if (s.experiment != null && !s.finished) s.copy(stage = s.stage + 1) else s }
            LabEvent.Restart -> _state.update { it.copy(stage = 0, choice = null) }
            is LabEvent.ChoiceSelected -> _state.update { s -> if (s.experiment != null && event.index in s.experiment.choices.indices) s.copy(choice = event.index) else s }
            is LabEvent.PowerSelected -> _state.update { it.copy(power = event.level.coerceIn(0, 2)) }
            LabEvent.MicroOpened -> _state.value.experiment?.reactionId?.let { session.showReaction(it) }
            LabEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
        }
    }
}

class LabViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = LabViewModel(InMemorySessionRepository.shared) as T
}
