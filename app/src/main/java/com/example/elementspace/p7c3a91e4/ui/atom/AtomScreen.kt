package com.example.elementspace.p7c3a91e4.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.usecase.AtomLabelText
import com.example.elementspace.p7c3a91e4.domain.usecase.NodalSurface
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalRef
import com.example.elementspace.p7c3a91e4.domain.usecase.Orbitals
import com.example.elementspace.p7c3a91e4.ui.atom.components.OrbitalView
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import androidx.compose.runtime.remember
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

    // QA screenshots: see DebugBus / AGENTS.md.
    DebugBus.Collect { command ->
        when (command.cmd) {
            "aview" -> AtomViewMode.entries.firstOrNull { it.name.equals(command.arg, ignoreCase = true) }?.let { view ->
                if (view.advanced && !viewModel.state.value.advancedExpanded) viewModel.onEvent(AtomEvent.AdvancedToggled)
                viewModel.onEvent(AtomEvent.ViewSelected(view))
            }
            "thing" -> viewModel.onEvent(AtomEvent.ThingSelected(command.arg?.takeIf { it != "none" }))
            "charge" -> command.number?.let { viewModel.onEvent(AtomEvent.ChargeStepped(it)) }
            "cloud" -> viewModel.onEvent(AtomEvent.CloudModeSelected(command.arg != "whole"))
            // "orbital 3d:0" = n, l letter, index into the real orbitals of that subshell.
            "orbital" -> command.arg?.let { arg ->
                val n = arg.first().digitToIntOrNull()
                val l = "spdf".indexOf(arg.getOrNull(1) ?: ' ')
                val index = arg.substringAfter(':', "0").toIntOrNull() ?: 0
                if (n != null && l >= 0) viewModel.onEvent(AtomEvent.OrbitalSelected(OrbitalRef(n, l, index.coerceIn(0, 2 * l))))
            }
            "phase" -> viewModel.onEvent(AtomEvent.OrbitalOptionToggled(OrbitalOption.PHASE))
            "nodes" -> viewModel.onEvent(AtomEvent.OrbitalOptionToggled(OrbitalOption.NODES))
            "axes" -> viewModel.onEvent(AtomEvent.OrbitalOptionToggled(OrbitalOption.AXES))
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

        val sample = state.orbitalSample
        val nodes =
            remember(sample?.ref) {
                sample?.ref?.let { ref -> Orbitals.orbital(ref).angularNodes + Orbitals.radialNodes(ref.n, ref.l).map { NodalSurface.Sphere(it) } }.orEmpty()
            }
        val orbitalView = sample?.let { OrbitalView(it, nodes, Orbitals.name(it.ref), state.showPhase, state.showNodes, state.showAxes) }

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
                    orbital = orbitalView,
                    thing = state.thing?.id,
                    thingFloats = state.thing?.floats == true,
                    thingGlass = state.thing?.glass == true,
                    charge = state.charge,
                    ionLabel = if (state.charge != 0) AtomLabelText.ion(state.element.symbol, state.charge) else null,
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
                canRemoveElectron = state.charge < state.maxCharge,
                canAddElectron = state.charge > state.minCharge,
                onCharge = { onEvent(AtomEvent.ChargeStepped(it)) },
                singleOrbital = state.singleOrbital,
                onCloudMode = { onEvent(AtomEvent.CloudModeSelected(it)) },
                onViewSelected = { onEvent(AtomEvent.ViewSelected(it)) },
                onAdvancedToggle = { onEvent(AtomEvent.AdvancedToggled) },
                onPauseToggle = { onEvent(AtomEvent.PauseToggled) },
                onReset = { onEvent(AtomEvent.ResetView) },
            )
        }
        SideCard { AtomInfoPanel(state = state, onEvent = onEvent) }
    }
}
