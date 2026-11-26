package com.example.elementspace.p7c3a91e4.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.common.SideCard
import com.example.elementspace.p7c3a91e4.ui.atom.components.AtomInfoPanel
import com.example.elementspace.p7c3a91e4.ui.atom.components.AtomSceneSpec
import com.example.elementspace.p7c3a91e4.ui.atom.components.AtomSceneView
import com.example.elementspace.p7c3a91e4.ui.atom.components.AtomToolbarContent
import com.example.elementspace.p7c3a91e4.ui.navigation.DemoMode
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.example.elementspace.p7c3a91e4.ui.theme.AppLocale
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Toolbar


/** One atom Volume: a rotatable 3D model, a toolbar below it and an info card beside it. */
@Composable
fun AtomScreen(atomicNumber: Int, modifier: Modifier = Modifier) {
    val viewModel: AtomViewModel =
        viewModel(key = "atom_$atomicNumber", factory = AtomViewModelFactory(atomicNumber))
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Closing the Volume frees its slot in the three-window limit.
    DisposableEffect(viewModel) { onDispose { viewModel.onEvent(AtomEvent.Closed) } }

    // Demo recording only; returns at once unless the emulator setting names this atom.
    val context = LocalContext.current
    LaunchedEffect(state.allotropes.isNotEmpty()) {
        if (state.element != null) {
            DemoMode.runAtomScript(context, atomicNumber, state.allotropes.isNotEmpty(), viewModel::onEvent)
        }
    }

    // Picking a reaction shows it in the single reaction Volume, opening the window if needed.
    val navigator = LocalSpatialNavigator.current
    val onEvent: (AtomEvent) -> Unit = { event ->
        viewModel.onEvent(event)
        if (event is AtomEvent.ReactionOpened) {
            runCatching { navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG) }
        }
        if (event is AtomEvent.MoleculeOpened) {
            runCatching { navigator.openWindowContainer(WindowIds.MOLECULE_VOLUME, tag = WindowIds.MOLECULE_TAG) }
        }
    }

    AppLocale(state.language) { AtomContent(state = state, onEvent = onEvent, modifier = modifier) }
}

@Composable
internal fun AtomContent(
    state: AtomUiState,
    onEvent: (AtomEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.element == null) {
            Text(
                text = str(R.string.element_missing),
                style = PicoTheme.typography.bodyLarge,
                color = PicoTheme.colorScheme.labelPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        // The Volume itself holds only the model; every label lives in the window fittings below
        // so nothing sits between the viewer and the atom.
        AtomSceneView(
            spec =
                AtomSceneSpec(
                    view = state.view,
                    shells = state.configuration?.shells.orEmpty(),
                    nucleus = state.nucleus,
                    clouds = state.clouds,
                    hiddenSubshells = state.hiddenSubshells,
                    lattice = state.lattice,
                ),
            paused = state.paused,
            resetToken = state.resetToken,
            onInteract = { onEvent(AtomEvent.Touched) },
            modifier = Modifier.fillMaxSize(),
        )

        // Window-level fittings: positioned by the system around the Volume.
        Toolbar {
            AtomToolbarContent(
                view = state.view,
                advancedExpanded = state.advancedExpanded,
                allotropeAvailable = state.allotropes.isNotEmpty(),
                paused = state.paused,
                onViewSelected = { onEvent(AtomEvent.ViewSelected(it)) },
                onAdvancedToggle = { onEvent(AtomEvent.AdvancedToggled) },
                onPauseToggle = { onEvent(AtomEvent.PauseToggled) },
                onReset = { onEvent(AtomEvent.ResetView) },
            )
        }
        SideCard { AtomInfoPanel(state = state, onEvent = onEvent) }
    }
}
