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
import com.example.elementspace.p7c3a91e4.ui.common.CardBody
import com.example.elementspace.p7c3a91e4.ui.common.CardSection
import com.example.elementspace.p7c3a91e4.ui.common.CardSource
import com.example.elementspace.p7c3a91e4.ui.common.CardTag
import com.example.elementspace.p7c3a91e4.ui.common.PagedCard
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.ReactionKind
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
    val main = if (big) PicoTheme.typography.headlineMedium else PicoTheme.typography.titleMedium
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ChemicalFormula.sideText(reaction.reactants), style = main, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            reaction.condition?.let {
                val icon = reaction.visuals.firstOrNull()?.kind?.icon
                Text(
                    if (icon != null) "$icon ${it.get(language)}" else it.get(language),
                    style = if (big) PicoTheme.typography.labelLarge else PicoTheme.typography.labelMedium,
                    color = ElementColors.ConditionInk,
                    maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ElementColors.ConditionChip).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Text(if (reaction.reversible) "⇌" else "⟶", style = main, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
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
                style = PicoTheme.typography.labelLarge,
                color = PicoTheme.colorScheme.labelSecondary,
                maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Card beside the reaction Volume: equation on top, then "this step" and "what and why" pages. */
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
    PagedCard(
        key = reaction.id,
        tabs = listOf(str(R.string.tab_this_step), str(R.string.tab_about)),
        modifier = modifier,
        header = {
            EquationLine(reaction, language, big = true)
            TypeTags(reaction, language)
        },
    ) { page ->
        if (page == 0) ThisStep(state, reaction, onEvent) else About(reaction, elements, colors, language)
    }
}

@Composable
private fun ColumnScope.ThisStep(state: ReactionUiState, reaction: Reaction, onEvent: (ReactionEvent) -> Unit) {
    val language = state.language
    val step = reaction.steps[state.step]
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            reaction.steps.indices.forEach { index ->
                val current = index == state.step
                Box(
                    modifier =
                        Modifier.size(56.dp)
                            .clip(CircleShape)
                            .background(if (current) PicoTheme.colorScheme.labelPrimary else PicoTheme.colorScheme.fillTertiary)
                            .focusProperties { canFocus = false }
                            .clickable { onEvent(ReactionEvent.StepSelected(index)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (index + 1).toString(),
                        style = PicoTheme.typography.titleMedium,
                        color = if (current) ElementColors.OverlayCard else PicoTheme.colorScheme.labelSecondary,
                    )
                }
            }
        }
        Text(step.title.get(language), style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary)
        CardBody(step.why.get(language))
    }
    reaction.conditionsAt(state.step).takeIf { it.isNotEmpty() }?.let { shown ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEach { CardTag("${it.kind.icon} ${it.label.get(language)}", color = ElementColors.ConditionInk, background = ElementColors.ConditionChip) }
        }
    }
    if (reaction.kind == ReactionKind.BONDS) {
        // Bond reactions move no electrons between atoms; count atoms on each side instead.
        CardSection(str(R.string.rx_atoms)) {
            val left = ChemicalFormula.side(reaction.reactants)
            val right = ChemicalFormula.side(reaction.products)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                left.keys.forEach { symbol -> Counter("${left[symbol]} / ${right[symbol] ?: 0}", symbol, Modifier.weight(1f)) }
            }
        }
    } else {
        CardSection(str(R.string.rx_electrons)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Counter(step.moved.toString(), reaction.loseLabel.get(language), Modifier.weight(1f))
                Counter(step.moved.toString(), reaction.gainLabel.get(language), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.About(reaction: Reaction, elements: Map<String, Element>, colors: Map<String, Color>, language: AppLanguage) {
    CardSection(str(R.string.rx_phenomenon)) { CardBody(reaction.phenomenon.get(language)) }
    CardSection(str(R.string.rx_property)) { CardBody(reaction.property.get(language)) }
    if (reaction.visuals.isNotEmpty()) {
        CardSection(str(R.string.rx_condition)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reaction.visuals.forEach { CardTag("${it.kind.icon} ${it.label.get(language)}", color = ElementColors.ConditionInk, background = ElementColors.ConditionChip) }
            }
        }
    }
    CardSection(str(R.string.rx_legend)) { Legend(reaction, elements, colors, language) }
    if (reaction.schematic) CardBody(str(R.string.rx_schematic), PicoTheme.colorScheme.labelSecondary)
    if (reaction.sketch) CardBody(str(R.string.rx_sketch), PicoTheme.colorScheme.labelSecondary)
    CardSource(str(R.string.rx_source, reaction.source.get(language)))
}

@Composable
private fun Counter(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = PicoTheme.typography.headlineMedium, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Text(label, style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
/** Which coloured nucleus is which element, plus the three marks used in the picture. */
@Composable
private fun Legend(reaction: Reaction, elements: Map<String, Element>, colors: Map<String, Color>, language: AppLanguage) {
    val symbols = (reaction.reactants + reaction.products).flatMap { ChemicalFormula.count(it.formula).keys }.distinct()
    val shown =
        if (reaction.kind == ReactionKind.BONDS) symbols
        else symbols.filter { symbol -> reaction.steps.any { st -> st.frame.particles.any { it.symbol == symbol && it.cluster.isEmpty() } } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEach { symbol ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(colors[symbol] ?: Color.Gray))
                    val name = elements[symbol]?.name(language)
                    Text(if (name != null && language == AppLanguage.ZH) "$symbol $name" else symbol, style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
                }
            }
        }
        if (reaction.kind == ReactionKind.BONDS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(width = 22.dp, height = 4.dp).background(ElementColors.BondBreak))
                Text(str(R.string.rx_legend_break), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(width = 22.dp, height = 5.dp).background(ElementColors.ElectronTransferred))
                Text(str(R.string.rx_legend_new), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(ElementColors.ElectronTransferred))
            Text(str(R.string.rx_legend_gold), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
            Box(Modifier.padding(start = 10.dp).size(14.dp).clip(CircleShape).background(ElementColors.ElectronTransferred.copy(alpha = 0.4f)))
            Text(str(R.string.rx_legend_vacancy), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
        }
        Text(str(R.string.rx_legend_ion), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}
