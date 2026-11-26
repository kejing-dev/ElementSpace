package com.example.elementspace.p7c3a91e4.ui.atom.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Subshell
import com.example.elementspace.p7c3a91e4.domain.usecase.ElectronConfigurationUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.NodalSurface
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalRef
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalTexts
import com.example.elementspace.p7c3a91e4.domain.usecase.Orbitals
import com.example.elementspace.p7c3a91e4.ui.atom.AtomEvent
import com.example.elementspace.p7c3a91e4.ui.atom.AtomUiState
import com.example.elementspace.p7c3a91e4.ui.atom.OrbitalOption
import com.example.elementspace.p7c3a91e4.ui.common.CardBody
import com.example.elementspace.p7c3a91e4.ui.common.CardSection
import com.example.elementspace.p7c3a91e4.ui.common.CardSource
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton

private val NoFocus = Modifier.focusProperties { canFocus = false }
private val Advanced = Color(0xFF7A4DD8)
private val AdvancedChip = Color(0xFFF1EAFE)
private val BoxLine = Color(0xFF9AA1AB)
private val BoxSelected = Color(0xFFE7EFFF)

/** "2p_x" as 2pₓ in Compose text. */
@Composable
private fun orbitalName(ref: OrbitalRef) =
    buildAnnotatedString {
        val name = Orbitals.name(ref)
        append(name.substringBefore('_'))
        val sub = name.substringAfter('_', "")
        if (sub.isNotEmpty()) withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)) { append(sub) }
    }

/** The cloud view's orbital page (M4 sample): diagram, the chosen orbital, then the advanced part. */
@Composable
internal fun OrbitalPage(state: AtomUiState, onEvent: (AtomEvent) -> Unit) {
    val configuration = state.configuration ?: return
    val element = state.element ?: return
    val selected = state.orbital
    val lang = state.language

    if (state.singleOrbital) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                Triple(OrbitalOption.PHASE, state.showPhase, R.string.orb_phase),
                Triple(OrbitalOption.NODES, state.showNodes, R.string.orb_nodes),
                Triple(OrbitalOption.AXES, state.showAxes, R.string.orb_axes),
            ).forEach { (option, on, label) ->
                ToggleButton(checked = on, onCheckedChange = { onEvent(AtomEvent.OrbitalOptionToggled(option)) }, modifier = NoFocus) {
                    Text(str(label), maxLines = 1)
                }
            }
        }
        Legend()
    }

    CardSection(str(R.string.orb_boxes)) {
        // Highest energy on top, as the textbook draws it; filling order is the configuration's order.
        configuration.subshells.reversed().forEach { subshell -> BoxRow(subshell, if (state.singleOrbital) selected else null) { onEvent(AtomEvent.OrbitalSelected(it)) } }
        Text(str(R.string.orb_rules), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary)
    }

    if (selected != null) {
        val subshell = configuration.subshells.firstOrNull { it.n == selected.n && it.l == selected.l }
        val electrons = subshell?.let { Orbitals.hund(it.l, it.electrons)[selected.index] } ?: 0
        val shape = Orbitals.orbital(selected)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(str(R.string.orb_selected), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
                Text(orbitalName(selected), style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuantumCell("${selected.n}", str(R.string.qn_n), Modifier.weight(1f))
                QuantumCell("${selected.l}", str(R.string.qn_l), Modifier.weight(1f))
                QuantumCell(shape.ml, str(R.string.qn_ml), Modifier.weight(1f))
                QuantumCell(when (electrons) { 2 -> "±½"; 1 -> "+½"; else -> "—" }, str(R.string.qn_ms), Modifier.weight(1f))
            }
            Fact(str(R.string.orb_shape), shape.shape.get(lang))
            Fact(str(R.string.orb_electrons), str(R.string.orb_count, electrons))
            val radial = selected.n - selected.l - 1
            Fact(str(R.string.orb_radial_nodes), str(R.string.orb_count, radial) + if (radial > 0) str(R.string.orb_spheres) else "")
            val coneText = str(R.string.orb_cone, "{deg}")
            val planes = shape.angularNodes.joinToString(lang.sep()) { node ->
                when (node) {
                    is NodalSurface.Plane -> node.name.get(lang)
                    is NodalSurface.Cone -> coneText.replace("{deg}", "%.1f".format(node.halfAngleDegrees))
                    is NodalSurface.Sphere -> ""
                }
            }
            val colon = if (lang == AppLanguage.ZH) "：" else ": "
            Fact(str(R.string.orb_angular_nodes), str(R.string.orb_count, selected.l) + if (planes.isNotEmpty()) colon + planes else "")
            Text(str(R.string.orb_ml_note), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelTertiary)
        }

        AdvancedSection(str(R.string.orb_radial_title)) {
            RadialChart(selected.n, selected.l)
            CardBody(OrbitalTexts.radialNote(selected.n, selected.l).get(lang), PicoTheme.colorScheme.labelSecondary)
        }
        AdvancedSection(str(R.string.orb_zeff_title)) {
            CardBody(OrbitalTexts.zEff(element.atomicNumber, configuration.subshells, selected.n, selected.l).get(lang))
        }
    }
    val aufbau = remember(element.atomicNumber) { ElectronConfigurationUseCase().aufbauOnly(element.atomicNumber) }
    AdvancedSection(str(R.string.orb_why_title)) {
        OrbitalTexts.why(element.atomicNumber, state.charge, configuration.subshells, aufbau).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("•", style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary)
                CardBody(line.get(lang))
            }
        }
    }
    CardSource(str(R.string.orb_source))
}

private fun AppLanguage.sep() = if (this == AppLanguage.ZH) "、" else ", "

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(ElementColors.PhasePositive to "ψ > 0", ElementColors.PhaseNegative to "ψ < 0", ElementColors.NodalSurface to str(R.string.orb_node_legend)).forEach { (color, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(color))
                Text(text, style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary)
            }
        }
    }
}

/** One subshell: its label, then touching boxes with ↑ / ↑↓ (Hund's rule). */
@Composable
private fun BoxRow(subshell: Subshell, selected: OrbitalRef?, onSelect: (OrbitalRef) -> Unit) {
    val counts = Orbitals.hund(subshell.l, subshell.electrons)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(subshell.label, style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.width(44.dp))
        // gate:allow button-gap the boxes of a textbook orbital diagram touch by design; each box is 56 dp square
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            counts.forEachIndexed { i, count ->
                val ref = OrbitalRef(subshell.n, subshell.l, i)
                val isSelected = ref == selected
                val shape =
                    when {
                        counts.size == 1 -> RoundedCornerShape(12.dp)
                        i == 0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        i == counts.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                Column(
                    modifier =
                        Modifier.size(56.dp)
                            .clip(shape)
                            .background(if (isSelected) BoxSelected else Color.White)
                            .border(if (isSelected) 3.dp else 1.5.dp, if (isSelected) ElementColors.PhasePositive else BoxLine, shape)
                            .then(NoFocus)
                            .clickable { onSelect(ref) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(when (count) { 2 -> "↑↓"; 1 -> "↑"; else -> " " }, style = PicoTheme.typography.titleLarge, color = Color(0xFF1D2127))
                    val suffix = Orbitals.orbital(ref).suffix
                    if (suffix.isNotEmpty() && suffix.length <= 5) Text(suffix, style = PicoTheme.typography.bodySmall, color = Color(0xFF6B7280), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun QuantumCell(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(PicoTheme.colorScheme.fillTertiary).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
        Text(label, style = PicoTheme.typography.labelLarge, color = PicoTheme.colorScheme.labelSecondary, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, modifier = Modifier.weight(1f))
        Text(value, style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.weight(1.3f))
    }
}

@Composable
private fun AdvancedSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                str(R.string.orb_adv),
                style = PicoTheme.typography.labelLarge,
                color = Advanced,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AdvancedChip).padding(horizontal = 10.dp, vertical = 2.dp),
            )
            Text(title, style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelSecondary)
        }
        content()
    }
}

/** P(r) = r²R² for Z = 1, with the radial nodes marked and the most probable radius written above its peak. */
@Composable
private fun RadialChart(n: Int, l: Int) {
    val extent = Orbitals.extent(n, l)
    val values = remember(n, l) { (0..200).map { i -> Orbitals.radialProbability(n, l, i * extent / 200.0) } }
    val nodes = remember(n, l) { Orbitals.radialNodes(n, l) }
    val peak = remember(n, l) { Orbitals.mostProbableRadius(n, l) }
    val max = values.max()
    val line = ElementColors.PhasePositive
    val axis = PicoTheme.colorScheme.labelTertiary
    Text(str(R.string.orb_peak, "%.1f".format(peak)), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelPrimary)
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val w = size.width
        val h = size.height
        fun x(r: Float) = r / extent * w
        fun y(v: Double) = (h - 6f) - (v / max).toFloat() * (h - 16f)
        drawLine(axis, Offset(0f, h - 6f), Offset(w, h - 6f), strokeWidth = 2f)
        val path = Path()
        values.forEachIndexed { i, v -> val px = i / 200f * w; if (i == 0) path.moveTo(px, y(v)) else path.lineTo(px, y(v)) }
        val fill = Path().apply { addPath(path); lineTo(w, h - 6f); lineTo(0f, h - 6f); close() }
        drawPath(fill, line.copy(alpha = 0.12f))
        drawPath(path, line, style = Stroke(width = 5f))
        nodes.forEach { r -> drawCircle(ElementColors.NodalSurface, radius = 8f, center = Offset(x(r), h - 6f)) }
        drawLine(Color(0xFF1D2127).copy(alpha = 0.35f), Offset(x(peak), 8f), Offset(x(peak), h - 6f), strokeWidth = 2f)
    }
    Text(str(R.string.orb_axis_r), style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelTertiary)
}
