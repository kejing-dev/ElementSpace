package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScale
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import java.util.Locale

private const val COLUMNS = 18
private const val MAIN_ROWS = 7
private val Gap = 4.dp
private val FBlockGap = 16.dp
private val MinTile = 56.dp

/**
 * The 18-column periodic table. Tile size follows the window so the whole table always fits; the
 * empty block above the transition metals holds the category filter or the trend legend.
 */
@Composable
fun ElementGrid(
    elements: List<Element>,
    language: AppLanguage,
    matches: Set<Int>,
    filtering: Boolean,
    openAtoms: List<Int>,
    trend: TrendProperty,
    trendScale: TrendScale?,
    selectedCategory: ElementCategory?,
    onCategoryToggle: (ElementCategory) -> Unit,
    onElementClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        val byWidth = (maxWidth - Gap * (COLUMNS - 1)) / COLUMNS
        val byHeight = (maxHeight - Gap * (MAIN_ROWS + 1) - FBlockGap) / (MAIN_ROWS + 2)
        val tile = min(byWidth, byHeight).coerceAtLeast(MinTile)
        val pitch = tile + Gap
        val gridWidth = pitch * COLUMNS - Gap
        val gridHeight = pitch * (MAIN_ROWS + 2) - Gap + FBlockGap

        fun x(column: Int): Dp = pitch * (column - 1)
        fun y(row: Int): Dp = if (row <= MAIN_ROWS) pitch * (row - 1) else pitch * (row - 1) + FBlockGap

        Box(modifier = Modifier.size(gridWidth, gridHeight)) {
            // Legend or filter in the empty block: columns 3..12 of rows 1..3.
            // Inset on both sides so the chips do not run into the tiles next to the block.
            Box(
                modifier =
                    Modifier.offset(x(3), y(1))
                        .size(pitch * 10 - Gap, pitch * 3 - Gap)
                        .padding(horizontal = pitch * 0.45f, vertical = 4.dp)
            ) {
                if (trendScale != null) {
                    TrendLegend(trend = trend, scale = trendScale)
                } else {
                    CategoryLegend(selected = selectedCategory, onToggle = onCategoryToggle)
                }
            }

            FBlockPlaceholder("57–71", str(R.string.lanthanides_short), Modifier.offset(x(3), y(6)).size(tile))
            FBlockPlaceholder("89–103", str(R.string.actinides_short), Modifier.offset(x(3), y(7)).size(tile))

            elements.forEach { element ->
                val normalized = trendScale?.normalized?.get(element.atomicNumber)
                val model =
                    ElementTileModel(
                        atomicNumber = element.atomicNumber,
                        symbol = element.symbol,
                        caption = caption(element, language, trend, trendScale != null),
                        fullName = element.name(language),
                        tint = if (normalized != null) ElementColors.trend(normalized) else ElementColors.category(element.category),
                        matched = !filtering || element.atomicNumber in matches,
                        opened = element.atomicNumber in openAtoms,
                        height = if (trendScale != null) normalized else null,
                        noData = trendScale != null && normalized == null,
                    )
                ElementTile(
                    model = model,
                    onClick = { onElementClick(element.atomicNumber) },
                    modifier = Modifier.offset(x(element.tableColumn), y(element.tableRow)).size(tile),
                )
            }
        }
    }
}

private fun caption(element: Element, language: AppLanguage, trend: TrendProperty, trendMode: Boolean): String {
    if (!trendMode) return if (language == AppLanguage.ZH) element.nameZh else ""
    val value = element.trendValue(trend) ?: return "—"
    return when {
        trend == TrendProperty.ELECTRONEGATIVITY -> String.format(Locale.US, "%.2f", value)
        trend == TrendProperty.DENSITY && value < 0.01 -> "<0.01"
        trend == TrendProperty.DENSITY -> String.format(Locale.US, "%.2f", value)
        else -> String.format(Locale.US, "%.0f", value)
    }
}

@Composable
private fun FBlockPlaceholder(range: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(range, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary, maxLines = 1)
        Text(label, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary, maxLines = 1)
    }
}

@Composable
private fun TrendLegend(trend: TrendProperty, scale: TrendScale, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(str(trend.labelRes()), style = PicoTheme.typography.titleSmall, color = PicoTheme.colorScheme.labelPrimary)
        Text(str(R.string.trend_hint), style = PicoTheme.typography.bodySmall, color = PicoTheme.colorScheme.labelSecondary)
        Box(
            modifier =
                Modifier.width(420.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(ElementColors.TrendLow, ElementColors.TrendMid, ElementColors.TrendHigh)))
        )
        Row(modifier = Modifier.width(420.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(str(R.string.trend_low, compact(scale.min)), style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary)
            Text(str(R.string.trend_high, compact(scale.max)), style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary)
        }
    }
}

private fun compact(value: Double): String =
    if (value >= 100) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.2f", value)
