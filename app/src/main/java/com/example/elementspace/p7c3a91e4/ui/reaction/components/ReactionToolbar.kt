package com.example.elementspace.p7c3a91e4.ui.reaction.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.reaction.ReactionEvent
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.VerticalDivider

/** Buttons never take key focus: controller A / Enter must not press them (profile D60). */
private val NoFocus = Modifier.focusProperties { canFocus = false }

/** Playback under the reaction Volume: previous, play / pause, next, replay, then "2 / 4". */
@Composable
fun RowScope.ReactionToolbarContent(
    step: Int,
    stepCount: Int,
    playing: Boolean,
    onEvent: (ReactionEvent) -> Unit,
    /** null: this reaction has one view only; true: the electron view is on screen. */
    showingElectrons: Boolean? = null,
) {
    if (showingElectrons != null) {
        Button(onClick = { onEvent(ReactionEvent.ViewToggled) }, modifier = NoFocus) {
            Text(str(if (showingElectrons) R.string.rx_view_atoms else R.string.rx_view_electrons), maxLines = 1)
        }
        VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    }
    Button(onClick = { onEvent(ReactionEvent.Previous) }, enabled = step > 0, modifier = NoFocus) {
        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(str(R.string.step_previous), maxLines = 1)
    }
    IconButton(onClick = { onEvent(ReactionEvent.PlayToggled) }, modifier = NoFocus) {
        Icon(
            painter = painterResource(if (playing) R.drawable.ic_player_pause else R.drawable.ic_player_play),
            contentDescription = str(if (playing) R.string.pause else R.string.play),
        )
    }
    Button(onClick = { onEvent(ReactionEvent.Next) }, enabled = step < stepCount - 1, modifier = NoFocus) {
        Text(str(R.string.step_next), maxLines = 1)
        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(20.dp))
    }
    IconButton(onClick = { onEvent(ReactionEvent.Replay) }, modifier = NoFocus) {
        Icon(painter = painterResource(R.drawable.ic_rotate), contentDescription = str(R.string.replay))
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    Text(
        text = str(R.string.step_of, step + 1, stepCount),
        style = PicoTheme.typography.titleSmall,
        color = PicoTheme.colorScheme.labelPrimary,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
