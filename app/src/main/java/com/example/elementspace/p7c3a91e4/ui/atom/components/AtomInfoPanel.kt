package com.example.elementspace.p7c3a91e4.ui.atom.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.Isotope
import com.example.elementspace.p7c3a91e4.domain.model.Subshell
import com.example.elementspace.p7c3a91e4.ui.atom.AtomEvent
import com.example.elementspace.p7c3a91e4.ui.atom.AtomUiState
import com.example.elementspace.p7c3a91e4.ui.table.components.labelRes
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.ListItem
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Switch
import com.pico.spatial.ui.design.Text
import java.util.Locale

private const val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

private fun superscript(value: Int): String = value.toString().map { SUPERSCRIPT_DIGITS[it - '0'] }.joinToString("")

/** "Na⁺", "O²⁻" or the bare symbol for a neutral atom. */
private fun ionSymbol(symbol: String, charge: Int): String {
    if (charge == 0) return symbol
    val magnitude = if (kotlin.math.abs(charge) == 1) "" else superscript(kotlin.math.abs(charge))
    return symbol + magnitude + if (charge > 0) "⁺" else "⁻"
}

private const val SHELL_NAMES = "KLMNOPQ"
/** The fullest shell any known atom has (N of the heaviest elements); bars are drawn against it. */
private const val SHELL_BAR_FULL = 32f

/**
 * Info card beside the Volume: who this atom is, what it is made of, what the current view shows
 * and what can be changed. It is the only place for text; the Volume holds just the model. The
 * sections follow the selected view, so the card stays about one screen tall and scrolls only
 * for heavy elements.
 */
@Composable
fun AtomInfoPanel(state: AtomUiState, onEvent: (AtomEvent) -> Unit, modifier: Modifier = Modifier) {
    val element = state.element ?: return
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ElementHero(element, state.language)
        ViewNote(state)
        when (state.view) {
            AtomViewMode.SHELL -> {
                Section(str(R.string.particles)) { ParticleCounts(state) }
                state.configuration?.let { Section(str(R.string.electron_shells)) { ShellBars(it.shells) } }
                IsotopeList(state, element, onEvent)
            }
            AtomViewMode.NUCLEUS -> {
                Section(str(R.string.particles)) {
                    ParticleCounts(state)
                    MassNumberLine(state)
                }
                IsotopeList(state, element, onEvent)
            }
            AtomViewMode.CLOUD -> {
                state.configuration?.let { configuration ->
                    Section(str(R.string.subshells)) {
                        SubshellToggles(
                            subshells = configuration.subshells,
                            hidden = state.hiddenSubshells,
                            onToggle = { onEvent(AtomEvent.SubshellToggled(it)) },
                        )
                    }
                    Section(str(R.string.configuration)) {
                        Text(
                            text = configuration.notation(),
                            style = PicoTheme.typography.bodyLargeMultiline,
                            color = PicoTheme.colorScheme.labelPrimary,
                        )
                    }
                }
            }
            AtomViewMode.ALLOTROPE -> AllotropeList(state, onEvent)
        }
        if (state.advancedExpanded && state.view != AtomViewMode.ALLOTROPE) {
            ChargeStepper(state, element, onEvent)
        }
        Section(str(R.string.properties)) { PropertyTable(element) }
    }
}

/** What the current view shows and that it is a teaching picture, not a measurement. */
@Composable
private fun ViewNote(state: AtomUiState) {
    val caption =
        when (state.view) {
            AtomViewMode.SHELL -> str(R.string.caption_shell)
            AtomViewMode.NUCLEUS -> str(R.string.caption_nucleus) + " · " + str(R.string.nucleus_note)
            AtomViewMode.CLOUD -> str(R.string.caption_cloud)
            AtomViewMode.ALLOTROPE -> str(R.string.allotrope_note)
        }
    Text(caption, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary)
}

/** The one-line sum that ties the nucleus picture to the mass number. */
@Composable
private fun MassNumberLine(state: AtomUiState) {
    Text(
        text = str(R.string.mass_number, state.protons, state.neutrons, state.protons + state.neutrons),
        style = PicoTheme.typography.titleSmall,
        color = PicoTheme.colorScheme.labelPrimary,
    )
}

/** One switch per occupied subshell so each orbital shape can be studied on its own. */
@Composable
private fun SubshellToggles(subshells: List<Subshell>, hidden: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        subshells.sortedWith(compareBy({ it.n }, { it.l })).forEach { subshell ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(ElementColors.orbital(subshell.l)))
                Text(
                    text = subshell.label + superscript(subshell.electrons),
                    style = PicoTheme.typography.bodyLarge,
                    color = PicoTheme.colorScheme.labelPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = subshell.label !in hidden, onCheckedChange = { onToggle(subshell.label) })
            }
        }
    }
}

/** A titled block; every section of the panel uses the same title style and spacing. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        content()
    }
}

/** The element drawn as one large periodic-table cell across the full width of the panel. */
@Composable
private fun ElementHero(element: Element, language: AppLanguage) {
    val tint = ElementColors.category(element.category)
    val sheen = PicoTheme.colorScheme.labelPrimaryLight
    val onTint = PicoTheme.colorScheme.labelPrimaryLight
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(tint.copy(alpha = 0.8f))
                .background(Brush.verticalGradient(0f to sheen.copy(alpha = 0.22f), 0.6f to sheen.copy(alpha = 0.04f), 1f to Color.Transparent))
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(element.atomicNumber.toString(), style = PicoTheme.typography.headlineSmall, color = onTint)
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.US, "%.3f", element.atomicMass), style = PicoTheme.typography.titleMedium, color = onTint)
                Text(str(R.string.atomic_mass), style = PicoTheme.typography.labelMedium, color = onTint.copy(alpha = 0.8f))
            }
        }
        Text(element.symbol, style = PicoTheme.typography.displayLarge, color = onTint, maxLines = 1)
        Text(element.name(language), style = PicoTheme.typography.headlineSmall, color = onTint, maxLines = 1)
        if (language != AppLanguage.EN) {
            Text(element.nameEn, style = PicoTheme.typography.bodyLarge, color = onTint.copy(alpha = 0.9f), maxLines = 1)
        }
        val group = element.group
        val place = if (group != null) str(R.string.period_group, element.period, group) else str(R.string.period_fblock, element.period)
        Text(
            text = "${str(element.category.labelRes())} · $place",
            style = PicoTheme.typography.bodyMedium,
            color = onTint.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ParticleCounts(state: AtomUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParticleCell("p⁺", state.protons, str(R.string.proton), ElementColors.ParticleProton, null, Modifier.weight(1f))
        ParticleCell("n⁰", state.neutrons, str(R.string.neutron), ElementColors.ParticleNeutron, state.neutronDelta.takeIf { it != 0 }, Modifier.weight(1f))
        ParticleCell("e⁻", state.electrons, str(R.string.electron), ElementColors.ParticleElectron, (-state.charge).takeIf { it != 0 }, Modifier.weight(1f))
    }
}

@Composable
private fun ParticleCell(symbol: String, count: Int, label: String, color: Color, delta: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Text(symbol, style = PicoTheme.typography.labelLarge, color = PicoTheme.colorScheme.labelSecondary)
            if (delta != null) {
                Text(if (delta > 0) "+$delta" else delta.toString(), style = PicoTheme.typography.labelLarge, color = PicoTheme.colorScheme.passable)
            }
        }
        Text(count.toString(), style = PicoTheme.typography.headlineSmall, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Text(label, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
    }
}

/** One bar per electron shell, matching the rings of the shell view from the inside out. */
@Composable
private fun ShellBars(shells: List<Int>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shells.forEachIndexed { index, electrons ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = SHELL_NAMES.getOrNull(index)?.toString() ?: (index + 1).toString(),
                    style = PicoTheme.typography.titleSmall,
                    color = PicoTheme.colorScheme.labelSecondary,
                    modifier = Modifier.width(20.dp),
                )
                Box(modifier = Modifier.weight(1f).height(12.dp).clip(CircleShape).background(PicoTheme.colorScheme.fillTertiary)) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth((electrons / SHELL_BAR_FULL).coerceIn(0.04f, 1f))
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(ElementColors.ParticleElectron)
                    )
                }
                Text(
                    text = electrons.toString(),
                    style = PicoTheme.typography.titleSmall,
                    color = PicoTheme.colorScheme.labelPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}

/** Reference values for the element; the same numbers the trend colouring of the table uses. */
@Composable
private fun PropertyTable(element: Element) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PropertyRow(str(R.string.trend_electronegativity), element.electronegativity?.let { String.format(Locale.US, "%.2f", it) }, "")
        PropertyRow(str(R.string.trend_radius), element.atomicRadiusPm?.let { String.format(Locale.US, "%.0f", it) }, "pm")
        PropertyRow(str(R.string.ionization_first), element.ionizationEnergyKjMol?.let { String.format(Locale.US, "%.0f", it) }, "kJ/mol")
        PropertyRow(str(R.string.trend_density), element.densityGcm3?.let { if (it < 0.01) "<0.01" else String.format(Locale.US, "%.2f", it) }, "g/cm³")
    }
}

@Composable
private fun PropertyRow(label: String, value: String?, unit: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value ?: "—", style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
            if (value != null && unit.isNotEmpty()) {
                Text(unit, style = PicoTheme.typography.labelMedium, color = PicoTheme.colorScheme.labelTertiary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary)
}

@Composable
private fun IsotopeList(state: AtomUiState, element: Element, onEvent: (AtomEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(str(R.string.isotopes))
        state.isotopes.forEachIndexed { index, isotope ->
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onEvent(AtomEvent.IsotopeSelected(index)) },
                headlineContent = { Text("${element.name(state.language)}-${isotope.massNumber}", maxLines = 1) },
                supportingContent = { IsotopeDetail(isotope, state.language) },
                leadingContent = { Text(superscript(isotope.massNumber) + element.symbol, style = PicoTheme.typography.titleSmall) },
                trailingContent = { if (index == state.isotopeIndex) SelectedMark() },
            )
        }
    }
}

@Composable
private fun IsotopeDetail(isotope: Isotope, language: AppLanguage) {
    val abundance = isotope.abundancePercent
    val amount = if (abundance != null) str(R.string.abundance, formatAbundance(abundance)) else str(R.string.trace)
    val halfLife = if (language == AppLanguage.ZH) isotope.halfLifeZh else isotope.halfLifeEn
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!isotope.stable) {
            Icon(
                painter = painterResource(R.drawable.ic_exclamation_mark_fill),
                contentDescription = str(R.string.radioactive),
                modifier = Modifier.size(14.dp),
                tint = PicoTheme.colorScheme.alert,
            )
        }
        val stability = if (halfLife == null) str(R.string.stable) else "${str(R.string.radioactive)} · ${str(R.string.half_life, halfLife)}"
        Text("$amount · $stability", maxLines = 2)
    }
}

private fun formatAbundance(value: Double): String =
    if (value >= 10) String.format(Locale.US, "%.1f", value) else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

@Composable
private fun AllotropeList(state: AtomUiState, onEvent: (AtomEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(str(R.string.allotropes))
        if (state.allotropes.isEmpty()) {
            Text(str(R.string.no_allotrope), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary)
        }
        state.allotropes.forEachIndexed { index, allotrope ->
            val zh = state.language == AppLanguage.ZH
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onEvent(AtomEvent.AllotropeSelected(index)) },
                headlineContent = { Text(if (zh) allotrope.nameZh else allotrope.nameEn, maxLines = 1) },
                supportingContent = { Text(if (zh) allotrope.noteZh else allotrope.noteEn, maxLines = 2) },
                leadingContent = { Text(allotrope.badge, style = PicoTheme.typography.titleSmall) },
                trailingContent = { if (index == state.allotropeIndex) SelectedMark() },
            )
        }
    }
}

@Composable
private fun SelectedMark() {
    Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(24.dp))
}

@Composable
private fun ChargeStepper(state: AtomUiState, element: Element, onEvent: (AtomEvent) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(str(R.string.charge), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary)
            Text(
                text = if (state.charge == 0) str(R.string.neutral_atom) else str(R.string.ion, ionSymbol(element.symbol, state.charge)),
                style = PicoTheme.typography.bodyMedium,
                color = PicoTheme.colorScheme.labelSecondary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onEvent(AtomEvent.ChargeStepped(-1)) }, enabled = state.charge > state.minCharge) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = str(R.string.charge_decrease))
            }
            Text(
                text = if (state.charge > 0) "+${state.charge}" else state.charge.toString(),
                style = PicoTheme.typography.titleMedium,
                color = PicoTheme.colorScheme.labelPrimary,
            )
            IconButton(onClick = { onEvent(AtomEvent.ChargeStepped(1)) }, enabled = state.charge < state.maxCharge) {
                Icon(painterResource(R.drawable.ic_plus), contentDescription = str(R.string.charge_increase))
            }
        }
    }
}
