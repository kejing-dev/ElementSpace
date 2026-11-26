package com.example.elementspace.p7c3a91e4.ui.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.common.SideCard
import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeCard
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeMode
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeSceneSpec
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeSceneView
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeToolbarContent
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.example.elementspace.p7c3a91e4.ui.theme.AppLocale
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Toolbar
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator


/** The molecule Volume: 3D molecule(s) in the middle, view toggles below, facts on the right. */
@Composable
fun MoleculeScreen(modifier: Modifier = Modifier) {
    val viewModel: MoleculeViewModel = viewModel(factory = MoleculeViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = remember { BundledElementRepository().elements().associate { it.symbol to ElementColors.model(it.symbol, it.category) } }
    val navigator = LocalSpatialNavigator.current
    val onEvent: (MoleculeEvent) -> Unit = { event ->
        viewModel.onEvent(event)
        if (event is MoleculeEvent.ReactionOpened) {
            runCatching { navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG) }
        }
    }

    DebugBus.Collect { command ->
        when (command.cmd) {
            "mmode" -> viewModel.onEvent(MoleculeEvent.ModeSelected(if (command.arg == "fill") MoleculeMode.SPACE_FILLING else MoleculeMode.BALL_AND_STICK))
            "angles" -> viewModel.onEvent(MoleculeEvent.AnglesToggled)
            "groups" -> viewModel.onEvent(MoleculeEvent.GroupsToggled)
            "labels" -> viewModel.onEvent(MoleculeEvent.LabelsToggled)
        }
    }

    AppLocale(state.language) {
        Box(modifier = modifier.fillMaxSize()) {
            val molecule = state.molecule
            if (molecule == null) {
                Text(
                    text = str(R.string.mol_empty),
                    style = PicoTheme.typography.bodyLarge,
                    color = PicoTheme.colorScheme.labelPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                return@Box
            }
            MoleculeSceneView(
                spec =
                    MoleculeSceneSpec(
                        molecules = listOfNotNull(molecule, state.compare),
                        mode = state.mode,
                        showAngles = state.showAngles,
                        showGroups = state.showGroups,
                        showLabels = state.showLabels,
                        colors = colors,
                    ),
                resetToken = state.resetToken,
                modifier = Modifier.fillMaxSize(),
            )
            Toolbar { MoleculeToolbarContent(state, onEvent) }
            SideCard { MoleculeCard(state, onEvent) }
        }
    }
}
