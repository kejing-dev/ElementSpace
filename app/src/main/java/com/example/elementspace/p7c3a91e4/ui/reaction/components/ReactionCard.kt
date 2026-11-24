package com.example.elementspace.p7c3a91e4.ui.reaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionType
import com.example.elementspace.p7c3a91e4.domain.usecase.ChemicalFormula
import com.example.elementspace.p7c3a91e4.ui.reaction.ReactionEvent
import com.example.elementspace.p7c3a91e4.ui.reaction.ReactionUiState
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

fun ReactionType.label(language: AppLanguage): String = if (language == AppLanguage.ZH) zh else en

/** "2Na + Cl₂", condition, "2NaCl" as one line of text. */
@Composable
fun EquationLine(reaction: Reaction, language: AppLanguage, big: Boolean, modifier: Modifier = Modifier) {
    val main = if (big) PicoTheme.typography.headlineSmall else PicoTheme.typography.titleMedium
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ChemicalFormula.sideText(reaction.reactants), style = main, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            reaction.condition?.let {
                Text(it.get(language), style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
            }
            Text("⟶", style = main, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        }
        Text(ChemicalFormula.sideText(reaction.products), style = main, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeTags(reaction: Reaction, language: AppLanguage) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        reaction.types.forEach { type ->
            Text(
                text = type.label(language),
                style = PicoTheme.typography.labelMedium,
                color = PicoTheme.colorScheme.labelSecondary,
                maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Card beside the reaction Volume: equation, legend, steps, why, electron counters, facts, source. */
@Composable
fun ReactionCard(
    state: ReactionUiState,
    elements: Map<String, Element>,
    colors: Map<String, Color>,
    onEvent: (ReactionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reaction = state.reaction ?: return
    val language = state.language
    val step = reaction.steps[state.step]
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EquationLine(reaction, language, big = true)
            TypeTags(reaction, language)
        }
        Block(str(R.string.rx_legend)) { Legend(reaction, elements, colors, language) }
        Block(str(R.string.rx_steps)) {
            reaction.steps.forEachIndexed { index, s ->
                val current = index == state.step
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (current) PicoTheme.colorScheme.fillTertiary else Color.Transparent)
                            .focusProperties { canFocus = false }
                            .clickable { onEvent(ReactionEvent.StepSelected(index)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        (index + 1).toString(),
                        style = PicoTheme.typography.titleSmall,
                        color = if (current) ElementColors.ElectronTransferred else PicoTheme.colorScheme.labelTertiary,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        s.title.get(language),
                        style = PicoTheme.typography.bodyMedium,
                        color = if (current) PicoTheme.colorScheme.labelPrimary else PicoTheme.colorScheme.labelSecondary,
                        maxLines = 2,
                    )
                }
            }
        }
        Block(str(R.string.rx_why)) {
            Text(step.why.get(language), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
        }
        Block(str(R.string.rx_electrons)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Counter(step.moved, reaction.loseLabel.get(language), Modifier.weight(1f))
                Counter(step.moved, reaction.gainLabel.get(language), Modifier.weight(1f))
            }
        }
        Block(str(R.string.rx_phenomenon)) { Body(reaction.phenomenon.get(language)) }
        Block(str(R.string.rx_property)) { Body(reaction.property.get(language)) }
        Text(
            str(R.string.rx_source, reaction.source.get(language)),
            style = PicoTheme.typography.bodySmall,
            color = PicoTheme.colorScheme.labelTertiary,
        )
    }
}

@Composable
private fun Block(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PicoTheme.typography.titleSmall, color = PicoTheme.colorScheme.labelSecondary)
        content()
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary)
}

@Composable
private fun Counter(value: Int, label: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Text(label, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}

/** Which coloured nucleus is which element, plus the three marks used in the picture. */
@Composable
private fun Legend(reaction: Reaction, elements: Map<String, Element>, colors: Map<String, Color>, language: AppLanguage) {
    val symbols = (reaction.reactants + reaction.products).flatMap { ChemicalFormula.count(it.formula).keys }.distinct()
    val shown = symbols.filter { symbol -> reaction.steps.any { st -> st.frame.particles.any { it.symbol == symbol && it.cluster.isEmpty() } } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            shown.forEach { symbol ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(colors[symbol] ?: Color.Gray))
                    val name = elements[symbol]?.name(language)
                    Text(if (name != null && language == AppLanguage.ZH) "$symbol $name" else symbol, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(ElementColors.ElectronTransferred))
            Text(str(R.string.rx_legend_gold), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
            Box(Modifier.padding(start = 10.dp).size(14.dp).clip(CircleShape).background(ElementColors.ElectronTransferred.copy(alpha = 0.4f)))
            Text(str(R.string.rx_legend_vacancy), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
        }
        Text(str(R.string.rx_legend_ion), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}
