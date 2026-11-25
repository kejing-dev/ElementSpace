package com.example.elementspace.p7c3a91e4.ui.molecule.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.molecule.MoleculeEvent
import com.example.elementspace.p7c3a91e4.ui.molecule.MoleculeUiState
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton
import com.pico.spatial.ui.design.VerticalDivider

/** Buttons never take key focus: controller A / Enter must not press them (profile D60). */
private val NoFocus = Modifier.focusProperties { canFocus = false }

/** Under the molecule Volume: ball-and-stick / space-filling, angle and group marks, reset. */
@Composable
fun RowScope.MoleculeToolbarContent(state: MoleculeUiState, onEvent: (MoleculeEvent) -> Unit) {
    listOf(MoleculeMode.BALL_AND_STICK to R.string.mol_ball, MoleculeMode.SPACE_FILLING to R.string.mol_fill).forEach { (mode, label) ->
        ToggleButton(checked = state.mode == mode, onCheckedChange = { onEvent(MoleculeEvent.ModeSelected(mode)) }, modifier = NoFocus) {
            Text(str(label), maxLines = 1)
        }
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    // Marks are drawn on the sticks, so they only apply to the ball-and-stick model.
    val sticks = state.mode == MoleculeMode.BALL_AND_STICK
    ToggleButton(checked = state.showAngles && sticks, onCheckedChange = { onEvent(MoleculeEvent.AnglesToggled) }, enabled = sticks, modifier = NoFocus) {
        Text(str(R.string.mol_angles), maxLines = 1)
    }
    ToggleButton(checked = state.showGroups && sticks, onCheckedChange = { onEvent(MoleculeEvent.GroupsToggled) }, enabled = sticks, modifier = NoFocus) {
        Text(str(R.string.mol_groups), maxLines = 1)
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    IconButton(onClick = { onEvent(MoleculeEvent.ResetView) }, modifier = NoFocus) {
        Icon(painter = painterResource(R.drawable.ic_rotate), contentDescription = str(R.string.mol_reset))
    }
}
