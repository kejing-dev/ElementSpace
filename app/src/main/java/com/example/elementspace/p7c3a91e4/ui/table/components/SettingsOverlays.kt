package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

private val NoFocus = Modifier.focusProperties { canFocus = false }

/** Dims the table and swallows taps so only the card on top can be used. */
@Composable
private fun Scrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        // The card itself eats taps so they do not reach the scrim.
        Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) { content() }
    }
}

@Composable
private fun Card(width: androidx.compose.ui.unit.Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier.width(width)
                .clip(RoundedCornerShape(32.dp))
                // Opaque so the table tiles behind never show through the text (emulator check v1.1.0).
                .background(ElementColors.OverlayCard)
                .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** "How to use": one row per action, hand and controller side by side (profile B19, D19). */
@Composable
fun HowToOverlay(onClose: () -> Unit) {
    val rows =
        listOf(
            Triple(R.string.how_to_open, R.string.how_to_open_hand, R.string.how_to_open_ctrl),
            Triple(R.string.how_to_rotate, R.string.how_to_rotate_hand, R.string.how_to_rotate_ctrl),
            Triple(R.string.how_to_scale, R.string.how_to_scale_hand, R.string.how_to_scale_ctrl),
            Triple(R.string.how_to_reaction, R.string.how_to_reaction_both, 0),
            Triple(R.string.how_to_play, R.string.how_to_play_both, 0),
            Triple(R.string.how_to_close, R.string.how_to_close_hand, R.string.how_to_close_ctrl),
        )
    Scrim(onDismiss = onClose) {
        Card(width = 860.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(str(R.string.how_to_title), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = NoFocus) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = str(R.string.close))
                }
            }
            HowToRow(str(R.string.how_to_col_action), str(R.string.how_to_col_hand), str(R.string.how_to_col_controller), header = true)
            rows.forEach { (action, hand, ctrl) ->
                HowToRow(str(action), str(hand), if (ctrl == 0) null else str(ctrl), header = false)
            }
            Text(str(R.string.how_to_hint), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary)
        }
    }
}

@Composable
private fun HowToRow(action: String, hand: String, controller: String?, header: Boolean) {
    val style = if (header) PicoTheme.typography.titleSmall else PicoTheme.typography.bodyLarge
    val color = if (header) PicoTheme.colorScheme.labelSecondary else PicoTheme.colorScheme.labelPrimary
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(action, style = if (header) style else PicoTheme.typography.titleSmall, color = color, modifier = Modifier.width(220.dp), maxLines = 2)
        if (controller == null) {
            Text(hand, style = style, color = color, modifier = Modifier.weight(2f), maxLines = 2)
        } else {
            Text(hand, style = style, color = color, modifier = Modifier.weight(1f), maxLines = 2)
            Text(controller, style = style, color = color, modifier = Modifier.weight(1f), maxLines = 2)
        }
    }
}

/** Exit with a second confirmation (profile D26). */
@Composable
fun ExitConfirmOverlay(onCancel: () -> Unit, onExit: () -> Unit) {
    Scrim(onDismiss = onCancel) {
        Card(width = 520.dp) {
            Icon(
                painterResource(R.drawable.ic_exit),
                contentDescription = null,
                modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally),
                tint = PicoTheme.colorScheme.labelPrimary,
            )
            Text(
                str(R.string.exit_confirm_title),
                style = PicoTheme.typography.headlineSmall,
                color = PicoTheme.colorScheme.labelPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                str(R.string.exit_confirm_line),
                style = PicoTheme.typography.bodyLarge,
                color = PicoTheme.colorScheme.labelSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onCancel, modifier = NoFocus.weight(1f).height(64.dp)) {
                    Text(str(R.string.cancel), maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Button(onClick = onExit, modifier = NoFocus.weight(1f).height(64.dp)) {
                    Icon(painterResource(R.drawable.ic_exit), contentDescription = null, modifier = Modifier.size(24.dp), tint = PicoTheme.colorScheme.alert)
                    Text(str(R.string.exit_yes), maxLines = 1, color = PicoTheme.colorScheme.alert)
                }
            }
        }
    }
}
