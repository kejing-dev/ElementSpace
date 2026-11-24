package com.example.elementspace.p7c3a91e4.ui.reaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.SessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReactionUiState(
    val reaction: Reaction? = null,
    val language: AppLanguage = AppLanguage.ZH,
    val step: Int = 0,
    val playing: Boolean = true,
    /** Bumped to ask the 3D view to return to its starting orientation and scale. */
    val resetToken: Int = 0,
) {
    val stepCount: Int
        get() = reaction?.steps?.size ?: 0

    val frame: ReactionFrame?
        get() = reaction?.steps?.getOrNull(step)?.frame

    val lastStep: Boolean
        get() = step >= stepCount - 1
}

sealed interface ReactionEvent {
    data object PlayToggled : ReactionEvent

    data object Next : ReactionEvent

    data object Previous : ReactionEvent

    data object Replay : ReactionEvent

    data class StepSelected(val step: Int) : ReactionEvent

    data object ResetView : ReactionEvent
}

/**
 * The single reaction Volume. It shows whatever reaction the session names, so picking another
 * reaction on any info card swaps the content in place. While playing it advances one step every
 * few seconds and stops on the last step.
 */
class ReactionViewModel(
    private val reactions: ReactionRepository,
    sessionRepository: SessionRepository,
    private val stepMillis: (Reaction, Int) -> Long = ::defaultStepMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(ReactionUiState())
    val state: StateFlow<ReactionUiState> = _state.asStateFlow()
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.openReaction.collect { id ->
                val reaction = id?.let { reactions.reaction(it) }
                if (reaction?.id != _state.value.reaction?.id) {
                    _state.update { it.copy(reaction = reaction, step = 0, playing = true, resetToken = it.resetToken + 1) }
                    restartTicker()
                }
            }
        }
        viewModelScope.launch { sessionRepository.language.collect { language -> _state.update { it.copy(language = language) } } }
    }

    fun onEvent(event: ReactionEvent) {
        when (event) {
            ReactionEvent.PlayToggled ->
                _state.update {
                    when {
                        it.playing -> it.copy(playing = false)
                        it.lastStep -> it.copy(step = 0, playing = true)
                        else -> it.copy(playing = true)
                    }
                }
            ReactionEvent.Next -> _state.update { it.copy(step = (it.step + 1).coerceAtMost(it.stepCount - 1), playing = false) }
            ReactionEvent.Previous -> _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0), playing = false) }
            ReactionEvent.Replay -> _state.update { it.copy(step = 0, playing = true, resetToken = it.resetToken + 1) }
            is ReactionEvent.StepSelected ->
                _state.update { if (event.step in 0 until it.stepCount) it.copy(step = event.step, playing = false) else it }
            ReactionEvent.ResetView -> _state.update { it.copy(resetToken = it.resetToken + 1) }
        }
        restartTicker()
    }

    /** One timer for auto-advance; restarted on every change so a step always gets its full time. */
    private fun restartTicker() {
        ticker?.cancel()
        val current = _state.value
        val reaction = current.reaction ?: return
        if (!current.playing) return
        ticker =
            viewModelScope.launch {
                while (true) {
                    val s = _state.value
                    if (!s.playing || s.reaction?.id != reaction.id) break
                    delay(stepMillis(reaction, s.step))
                    val after = _state.value
                    if (!after.playing || after.reaction?.id != reaction.id) break
                    if (after.lastStep) {
                        _state.update { it.copy(playing = false) }
                        break
                    }
                    _state.update { it.copy(step = it.step + 1) }
                }
            }
    }

    companion object {
        /** Steps with a flying electron get time for two full flight loops. */
        fun defaultStepMillis(reaction: Reaction, step: Int): Long =
            if (reaction.steps.getOrNull(step)?.frame?.flights?.isNotEmpty() == true) 6_500L else 4_000L
    }
}

class ReactionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReactionViewModel(BundledReactionRepository.shared, InMemorySessionRepository.shared) as T
}
