package com.example.elementspace.p7c3a91e4.ui.molecule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.usecase.ChemicalFormula
import com.example.elementspace.p7c3a91e4.ui.molecule.MoleculeEvent
import com.example.elementspace.p7c3a91e4.ui.molecule.MoleculeUiState
import com.example.elementspace.p7c3a91e4.ui.reaction.components.EquationLine
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

private val NoFocus = Modifier.focusProperties { canFocus = false }

/** "C₂H₆O · CH₃CH₂OH" */
fun Molecule.formulaLine(): String =
    if (formula == condensed) ChemicalFormula.display(formula) else "${ChemicalFormula.display(formula)} · ${ChemicalFormula.display(condensed)}"

/** Card beside the molecule Volume: one molecule, or the comparison table when two are shown. */
@Composable
fun MoleculeCard(state: MoleculeUiState, onEvent: (MoleculeEvent) -> Unit, modifier: Modifier = Modifier) {
    val molecule = state.molecule ?: return
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val compare = state.compare
        if (compare == null) Single(molecule, state, onEvent) else Compare(molecule, compare, state, onEvent)
        Text(
            str(R.string.rx_source, if (compare == null) molecule.source.get(state.language) else "${molecule.source.get(state.language)}；${compare.source.get(state.language)}"),
            style = PicoTheme.typography.bodySmall,
            color = PicoTheme.colorScheme.labelTertiary,
        )
    }
}

@Composable
private fun ColumnScope.Single(m: Molecule, state: MoleculeUiState, onEvent: (MoleculeEvent) -> Unit) {
    val lang = state.language
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(m.name.get(lang), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary)
        Text(m.formulaLine(), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
        Tag(m.skeleton.let { if (lang == AppLanguage.ZH) it.zh else it.en })
    }
    m.structural?.let { text ->
        Block(str(R.string.mol_structural)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PicoTheme.colorScheme.fillTertiary).horizontalScroll(rememberScrollState()).padding(14.dp)) {
                Text(text, style = PicoTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace), color = PicoTheme.colorScheme.labelPrimary)
            }
        }
    }
    Block(str(R.string.mol_shape)) {
        Text(m.geometry.get(lang), style = PicoTheme.typography.bodyLargeMultiline, color = PicoTheme.colorScheme.labelPrimary)
        if (m.angles.isNotEmpty()) {
            Mark(ElementColors.BondAngle, str(R.string.mol_angle_value, m.angles.joinToString(" · ") { it.label }))
        }
    }
    m.group?.let { Block(str(R.string.mol_group)) { Mark(ElementColors.ElectronTransferred, it.get(lang)) } }
    Block(str(R.string.mol_property)) { Text(m.property.get(lang), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary) }
    state.partner?.let { other ->
        ActionRow(str(R.string.mol_compare, other.name.get(lang)), R.drawable.ic_arrow_right) { onEvent(MoleculeEvent.CompareOpened(other.id)) }
    }
    if (state.reactions.isNotEmpty()) {
        Block(str(R.string.mol_reactions)) {
            state.reactions.forEach { reaction ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PicoTheme.colorScheme.fillTertiary).then(NoFocus)
                        .clickable { onEvent(MoleculeEvent.ReactionOpened(reaction.id)) }.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EquationLine(reaction, lang, big = false, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEvent(MoleculeEvent.ReactionOpened(reaction.id)) }, modifier = NoFocus) {
                        Icon(painterResource(R.drawable.ic_player_play), contentDescription = str(R.string.open_reaction))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Compare(a: Molecule, b: Molecule, state: MoleculeUiState, onEvent: (MoleculeEvent) -> Unit) {
    val lang = state.language
    Text(str(R.string.mol_compare_title), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary)
    if (state.isomers) Mark(ElementColors.ElectronTransferred, str(R.string.mol_isomers))
    // Left column = left molecule in the Volume.
    CompareRow("", a.name.get(lang), b.name.get(lang), header = true)
    CompareRow(str(R.string.mol_compare_formula), ChemicalFormula.display(a.formula), ChemicalFormula.display(b.formula))
    CompareRow(str(R.string.mol_compare_condensed), ChemicalFormula.display(a.condensed), ChemicalFormula.display(b.condensed))
    CompareRow(str(R.string.mol_compare_skeleton), if (lang == AppLanguage.ZH) a.skeleton.zh else a.skeleton.en, if (lang == AppLanguage.ZH) b.skeleton.zh else b.skeleton.en)
    CompareRow(str(R.string.mol_compare_difference), a.compareNote?.get(lang).orEmpty(), b.compareNote?.get(lang).orEmpty())
    Block(str(R.string.mol_shape)) {
        Text("${a.name.get(lang)}：${a.geometry.get(lang)}", style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary)
        Text("${b.name.get(lang)}：${b.geometry.get(lang)}", style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary)
    }
    ActionRow(str(R.string.mol_compare_exit, a.name.get(lang)), R.drawable.ic_arrow_left) { onEvent(MoleculeEvent.CompareClosed) }
}

@Composable
private fun CompareRow(label: String, left: String, right: String, header: Boolean = false) {
    val style = if (header) PicoTheme.typography.titleSmall else PicoTheme.typography.bodyMedium
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = PicoTheme.typography.labelMedium, color = PicoTheme.colorScheme.labelSecondary, modifier = Modifier.width(72.dp))
        Text(left, style = style, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1f))
        Text(right, style = style, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionRow(text: String, icon: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(20.dp)).background(PicoTheme.colorScheme.fillTertiary).then(NoFocus)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = PicoTheme.typography.titleSmall, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
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
private fun Mark(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Text(text, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelPrimary)
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = PicoTheme.typography.labelMedium,
        color = PicoTheme.colorScheme.labelSecondary,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
