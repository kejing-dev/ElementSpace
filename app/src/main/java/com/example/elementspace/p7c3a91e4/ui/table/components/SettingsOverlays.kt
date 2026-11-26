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
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.usecase.CollideResult
import com.example.elementspace.p7c3a91e4.ui.reaction.components.EquationLine
import com.example.elementspace.p7c3a91e4.ui.table.CollideOutcome
import com.example.elementspace.p7c3a91e4.ui.table.PeriodicTableEvent
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
            Triple(R.string.how_to_molecule, R.string.how_to_molecule_both, 0),
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

/**
 * Result of 碰一碰 (v1.6.0): the two elements, whether they react, why, and what to do next.
 * Everything is a single tap: ▶ opens the 3D reaction, the other buttons close or open an element.
 */
@Composable
fun CollideOverlay(outcome: CollideOutcome, language: AppLanguage, onEvent: (PeriodicTableEvent) -> Unit) {
    val close = { onEvent(PeriodicTableEvent.CollideDismissed) }
    Scrim(onDismiss = close) {
        Card(width = 760.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${outcome.a.symbol} ${outcome.a.name(language)}  +  ${outcome.b.symbol} ${outcome.b.name(language)}",
                    style = PicoTheme.typography.headlineSmall,
                    color = PicoTheme.colorScheme.labelPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                val (label, ink, chip) =
                    when (outcome.result) {
                        is CollideResult.Reacts -> Triple(R.string.collide_yes, Color(0xFF1F9D55), Color(0xFFE3F6EA))
                        is CollideResult.NoReaction -> Triple(R.string.collide_no, Color(0xFFD03A2F), Color(0xFFFDE8E6))
                        is CollideResult.NoExperiment -> Triple(R.string.collide_no_experiment, Color(0xFF5B6370), Color(0xFFECEEF1))
                        CollideResult.NoCommonReaction -> Triple(R.string.collide_unknown, Color(0xFF5B6370), Color(0xFFECEEF1))
                    }
                Text(
                    str(label),
                    style = PicoTheme.typography.titleMedium,
                    color = ink,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(chip).padding(horizontal = 14.dp, vertical = 4.dp),
                    maxLines = 1,
                )
            }
            when (val result = outcome.result) {
                is CollideResult.Reacts -> {
                    Text(str(R.string.collide_why), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
                    Text(result.why.get(language), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
                    result.reactions.take(3).forEach { reaction ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // gate:allow button-gap equation and its phenomenon line belong together; the only button is the trailing one
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                EquationLine(reaction, language, big = false)
                                Text(reaction.phenomenon.get(language), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 2)
                            }
                            Button(onClick = { onEvent(PeriodicTableEvent.CollideReactionOpened(reaction.id)) }, modifier = NoFocus.height(64.dp)) {
                                Icon(painterResource(R.drawable.ic_player_play), contentDescription = null, modifier = Modifier.size(24.dp))
                                Text(str(R.string.collide_watch), maxLines = 1)
                            }
                        }
                    }
                }
                is CollideResult.NoReaction -> {
                    Text(str(R.string.collide_why), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
                    Text(result.why.get(language), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
                }
                is CollideResult.NoExperiment -> Text(result.why.get(language), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
                CollideResult.NoCommonReaction -> {
                    Text(str(R.string.collide_unknown_line), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(outcome.a, outcome.b).forEach { element ->
                            Button(onClick = { onEvent(PeriodicTableEvent.CollideElementOpened(element.atomicNumber)) }, modifier = NoFocus.weight(1f).height(64.dp)) {
                                Text(str(R.string.collide_see, element.name(language)), maxLines = 1)
                            }
                        }
                    }
                }
            }
            Button(onClick = close, modifier = NoFocus.fillMaxWidth().padding(top = 8.dp).height(64.dp)) {
                Text(str(R.string.collide_again), maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}
