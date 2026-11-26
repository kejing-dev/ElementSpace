package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.data.repository.GuidePaths
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.ui.table.GuideProgress
import com.example.elementspace.p7c3a91e4.ui.table.PeriodicTableEvent
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/** 跟我学: pick one of the short paths. */
@Composable
fun GuideListOverlay(language: AppLanguage, onEvent: (PeriodicTableEvent) -> Unit) {
    val close = { onEvent(PeriodicTableEvent.GuideListVisible(false)) }
    Scrim(onDismiss = close) {
        Card(width = 720.dp) {
            Text(str(R.string.guide_title), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary)
            Text(str(R.string.guide_line), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary)
            GuidePaths.ALL.forEach { path ->
                Button(onClick = { onEvent(PeriodicTableEvent.GuideStarted(path.id)) }, modifier = NoFocus.fillMaxWidth().height(64.dp)) {
                    Text(path.title.get(language), maxLines = 1, modifier = Modifier.weight(1f))
                    Text(str(R.string.guide_steps, path.steps.size), maxLines = 1, color = PicoTheme.colorScheme.labelSecondary)
                }
            }
            Button(onClick = close, modifier = NoFocus.fillMaxWidth().height(64.dp)) {
                Text(str(R.string.close), maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * The path being followed, docked at the bottom of the table window so it stays in the same place
 * while atoms and reactions open around it: one sentence, one button, and a way out.
 */
@Composable
fun GuideBar(progress: GuideProgress, language: AppLanguage, onEvent: (PeriodicTableEvent) -> Unit) {
    val path = progress.path
    val step = path.steps.getOrNull(progress.step)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(ElementColors.OverlayCard).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1D2127)), contentAlignment = Alignment.Center) {
            Text(
                if (step == null) "✓" else "${progress.step + 1}/${path.steps.size}",
                style = PicoTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
            )
        }
        // gate:allow button-gap title and sentence of one step; the buttons are the trailing ones, 20 dp apart
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(path.title.get(language), style = PicoTheme.typography.titleSmall, color = Color(0xFF5B6370), maxLines = 1)
            Text((step?.text ?: path.done).get(language), style = PicoTheme.typography.titleMedium, color = Color(0xFF1D2127), maxLines = 3)
        }
        if (step != null) {
            Button(onClick = { onEvent(PeriodicTableEvent.GuideNext) }, modifier = NoFocus.height(64.dp)) {
                Icon(painterResource(R.drawable.ic_player_play), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(step.button.get(language), maxLines = 1)
            }
        } else {
            Button(onClick = { onEvent(PeriodicTableEvent.GuideListVisible(true)) }, modifier = NoFocus.height(64.dp)) { Text(str(R.string.guide_another), maxLines = 1) }
        }
        Button(onClick = { onEvent(PeriodicTableEvent.GuideExit) }, modifier = NoFocus.height(64.dp)) { Text(str(if (step == null) R.string.guide_finish else R.string.guide_exit), maxLines = 1) }
    }
}

/** First launch: three things to try, then straight into a path or the table. */
@Composable
fun FirstRunOverlay(onEvent: (PeriodicTableEvent) -> Unit) {
    val close = { onEvent(PeriodicTableEvent.FirstRunVisible(false)) }
    Scrim(onDismiss = close) {
        Card(width = 760.dp) {
            Text(str(R.string.first_title), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary)
            listOf(R.string.first_1, R.string.first_2, R.string.first_3).forEachIndexed { i, line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(PicoTheme.colorScheme.fillTertiary), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary)
                    }
                    Text(str(line), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1f))
                }
            }
            Text(str(R.string.first_how), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = close, modifier = NoFocus.weight(1f).height(64.dp)) {
                    Text(str(R.string.first_free), maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Button(
                    onClick = {
                        close()
                        onEvent(PeriodicTableEvent.GuideStarted("sodium_water"))
                    },
                    modifier = NoFocus.weight(1f).height(64.dp),
                ) {
                    Text(str(R.string.first_guided), maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
