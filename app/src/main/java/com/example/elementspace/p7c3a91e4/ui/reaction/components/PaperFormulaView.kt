package com.example.elementspace.p7c3a91e4.ui.reaction.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elementspace.p7c3a91e4.domain.usecase.AtomLabelText
import com.example.elementspace.p7c3a91e4.domain.usecase.PaperFormula
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.pico.spatial.ui.design.PicoTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Largest symbol size; a picture with many atoms shrinks to the card's width. */
private const val FONT_SP = 26f
private const val FONT_MIN_SP = 13f

/**
 * The frame written on paper: dots for electrons ([lines] false, 电子式) or one line per shared
 * pair ([lines] true, 结构式). Atoms stand where they stand in the 3D picture.
 */
@Composable
fun PaperFormulaView(diagram: PaperFormula.Diagram, lines: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val ink = PicoTheme.colorScheme.labelPrimary
    val shared = ElementColors.ConditionInk
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        val xs = diagram.atoms.map { it.position.x }
        val ys = diagram.atoms.map { it.position.y }
        val em = diagram.em
        // The row starts at x = 0 (a multiplier or the first symbol's margin) and ends 1.4 em after the last symbol.
        val rowWidth = xs.max() + 1.4f * em
        val spanY = ys.max() - ys.min()
        val fontMax = with(density) { FONT_SP.sp.toPx() }
        val font = min(fontMax, width / (rowWidth / em)).coerceAtLeast(with(density) { FONT_MIN_SP.sp.toPx() })
        val scale = font / em
        val heightPx = spanY * scale + 3.4f * font
        Canvas(modifier = Modifier.fillMaxWidth().height(with(density) { heightPx.toDp() })) {
            val cy = (ys.max() + ys.min()) / 2f
            val x0 = (size.width - rowWidth * scale) / 2f
            fun at(i: Int) = Offset(x0 + xs[i] * scale, size.height / 2f - (ys[i] - cy) * scale)
            val style = TextStyle(color = ink, fontSize = with(density) { font.toSp() }, fontWeight = FontWeight.Medium)
            val layouts = diagram.atoms.map { measurer.measure(if (lines) it.symbol else AtomLabelText.ion(it.symbol, it.charge), style) }
            val dot = font * 0.085f

            diagram.bonds.forEach { bond ->
                val a = at(bond.a)
                val b = at(bond.b)
                val d = b - a
                val length = d.getDistance().coerceAtLeast(1e-3f)
                val dir = d / length
                val normal = Offset(-dir.y, dir.x)
                if (lines) {
                    val ra = layouts[bond.a].size.width / 2f + font * 0.22f
                    val rb = layouts[bond.b].size.width / 2f + font * 0.22f
                    repeat(bond.pairs) { k ->
                        val off = normal * ((k - (bond.pairs - 1) / 2f) * font * 0.26f)
                        drawLine(ink, a + dir * min(ra, length * 0.4f) + off, b - dir * min(rb, length * 0.4f) + off, strokeWidth = font * 0.085f, cap = StrokeCap.Round)
                    }
                } else {
                    val mid = (a + b) / 2f
                    repeat(bond.pairs) { k ->
                        val along = dir * ((k - (bond.pairs - 1) / 2f) * font * 0.36f)
                        drawCircle(shared, dot, mid + along + normal * (font * 0.19f))
                        drawCircle(shared, dot, mid + along - normal * (font * 0.19f))
                    }
                }
            }
            diagram.atoms.forEachIndexed { i, atom ->
                val p = at(i)
                val layout = layouts[i]
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                if (lines) return@forEachIndexed
                val rx = max(font * 0.72f, layout.size.width / 2f + font * 0.26f)
                val ry = font * 0.74f
                atom.lone.forEach { lone ->
                    val c = Offset(p.x + rx * cos(lone.angle), p.y - ry * sin(lone.angle))
                    // The two dots of a pair sit side by side across the direction they are written in.
                    val t = Offset(sin(lone.angle), cos(lone.angle)) * (font * 0.17f)
                    if (lone.dots == 1) drawCircle(ink, dot, c) else { drawCircle(ink, dot, c + t); drawCircle(ink, dot, c - t) }
                }
            }
            diagram.multipliers.forEach { m ->
                val label = measurer.measure("${m.count}×", style.copy(color = shared))
                drawText(label, topLeft = Offset(x0 + m.x * scale, size.height / 2f + cy * scale - label.size.height / 2f))
            }
            if (!lines) diagram.groups.forEach { group -> bracket(group, ::at, font, ink, measurer, style) }
        }
    }
}

private fun DrawScope.bracket(group: PaperFormula.Group, at: (Int) -> Offset, font: Float, ink: Color, measurer: TextMeasurer, style: TextStyle) {
    val points = group.atoms.map(at)
    val left = points.minOf { it.x } - font * 1.25f
    val right = points.maxOf { it.x } + font * 1.25f
    val top = points.minOf { it.y } - font * 1.2f
    val bottom = points.maxOf { it.y } + font * 1.2f
    val lip = font * 0.28f
    val stroke = font * 0.07f
    listOf(left to lip, right to -lip).forEach { (x, l) ->
        drawLine(ink, Offset(x, top), Offset(x, bottom), strokeWidth = stroke)
        drawLine(ink, Offset(x, top), Offset(x + l, top), strokeWidth = stroke)
        drawLine(ink, Offset(x, bottom), Offset(x + l, bottom), strokeWidth = stroke)
    }
    val charge = measurer.measure(AtomLabelText.charge(group.charge), style)
    drawText(charge, topLeft = Offset(right + font * 0.08f, top - charge.size.height * 0.45f))
}
