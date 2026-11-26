package com.example.elementspace.p7c3a91e4.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/*
 * Shared reading rules for the three side cards (v1.3.0, headset: "右侧的文字很多太密了，也小"):
 * one kind of content per page, body text never below bodyLarge, section titles titleMedium,
 * 32 dp padding and 28 dp between sections.
 */

private val NoFocus = Modifier.focusProperties { canFocus = false }

/** Scrolling column of one card page. Scroll state is keyed by [key] so each page starts at the top. */
@Composable
fun CardPage(key: Any, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = androidx.compose.runtime.key(key) { rememberScrollState() }
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = 32.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        content = content,
    )
}

/**
 * A card with a fixed [header], a row of page tabs and the selected page below. The selected tab
 * is kept per [key] (element, reaction or molecule) and can be set from the debug bus (`tab <n>`).
 */
@Composable
fun PagedCard(
    key: Any,
    tabs: List<String>,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    page: @Composable ColumnScope.(Int) -> Unit,
) {
    var selected by rememberSaveable(key) { mutableIntStateOf(0) }
    DebugBus.Collect { command -> if (command.cmd == "tab") command.number?.let { selected = (it - 1).coerceIn(0, tabs.lastIndex) } }
    val current = selected.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            header()
            if (tabs.size > 1) CardTabs(tabs, current) { selected = it }
        }
        CardPage(key = key to current) { page(current) }
    }
}

@Composable
fun CardTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier =
                    Modifier.weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (on) PicoTheme.colorScheme.fillPrimary.copy(alpha = 0.9f) else Color.Transparent)
                        .then(NoFocus)
                        .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = PicoTheme.typography.titleSmall,
                    color = if (on) PicoTheme.colorScheme.labelPrimary else PicoTheme.colorScheme.labelSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun CardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
        content()
    }
}

@Composable
fun CardBody(text: String, color: Color = PicoTheme.colorScheme.labelPrimary) {
    Text(text, style = PicoTheme.typography.bodyLargeMultiline, color = color)
}

@Composable
fun CardSource(text: String) {
    Text(text, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelTertiary)
}

@Composable
fun CardTag(text: String, color: Color = PicoTheme.colorScheme.labelSecondary, background: Color = PicoTheme.colorScheme.fillTertiary) {
    Text(
        text,
        style = PicoTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(background).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
