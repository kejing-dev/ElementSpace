package com.example.elementspace.p7c3a91e4.ui.reaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.common.SideCard
import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.example.elementspace.p7c3a91e4.ui.reaction.components.ReactionCard
import com.example.elementspace.p7c3a91e4.ui.reaction.components.ReactionSceneSpec
import com.example.elementspace.p7c3a91e4.ui.reaction.components.ReactionSceneView
import com.example.elementspace.p7c3a91e4.ui.reaction.components.ReactionToolbarContent
import com.example.elementspace.p7c3a91e4.ui.theme.AppLocale
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Toolbar


/** The reaction Volume: 3D atoms in the middle, playback toolbar below, step card on the right. */
@Composable
fun ReactionScreen(modifier: Modifier = Modifier) {
    val viewModel: ReactionViewModel = viewModel(factory = ReactionViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elements = remember { BundledElementRepository().elements().associateBy { it.symbol } }
    val colors = remember(elements) { elements.mapValues { (symbol, element) -> ElementColors.model(symbol, element.category) } }

    DebugBus.Collect { command -> command.reactionEvent()?.let(viewModel::onEvent) }

    AppLocale(state.language) { ReactionContent(state, elements, colors, viewModel::onEvent, modifier) }
}

@Composable
internal fun ReactionContent(
    state: ReactionUiState,
    elements: Map<String, Element>,
    colors: Map<String, Color>,
    onEvent: (ReactionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val reaction = state.reaction
        val frame = state.frame
        if (reaction == null || frame == null) {
            Text(
                text = str(R.string.rx_empty),
                style = PicoTheme.typography.bodyLarge,
                color = PicoTheme.colorScheme.labelPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }
        ReactionSceneView(
            spec =
                ReactionSceneSpec(
                    reactionId = reaction.id,
                    step = state.step,
                    frame = frame,
                    nucleusColors = colors,
                    conditions = reaction.conditionsAt(state.step),
                    language = state.language,
                    atomicNumbers = elements.mapValues { it.value.atomicNumber },
                ),
            paused = !state.playing && frame.flights.isEmpty(),
            resetToken = state.resetToken,
            modifier = Modifier.fillMaxSize(),
        )
        Toolbar {
            ReactionToolbarContent(
                step = state.step,
                stepCount = state.stepCount,
                playing = state.playing,
                onEvent = onEvent,
                showingElectrons = if (state.otherView != null) state.showingElectrons else null,
            )
        }
        SideCard { ReactionCard(state = state, elements = elements, colors = colors, onEvent = onEvent) }
    }
}
