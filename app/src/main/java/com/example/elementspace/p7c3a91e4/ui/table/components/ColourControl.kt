package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text

@StringRes
fun TrendProperty.labelRes(): Int =
    when (this) {
        TrendProperty.CATEGORY -> R.string.colour_category
        TrendProperty.ATOMIC_RADIUS -> R.string.trend_radius
        TrendProperty.ELECTRONEGATIVITY -> R.string.trend_electronegativity
        TrendProperty.IONIZATION_ENERGY -> R.string.trend_ionization
        TrendProperty.DENSITY -> R.string.trend_density
    }

private val SegmentWidth = 136.dp

/**
 * Chooses what tiles are coloured by: the element category or one of the trend properties. All
 * options sit in one control so it reads as a single either-or choice.
 */
@Composable
fun ColourControl(
    selected: TrendProperty,
    onSelect: (TrendProperty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = str(R.string.colour_by),
            style = PicoTheme.typography.bodyMedium,
            color = PicoTheme.colorScheme.labelSecondary,
            maxLines = 1,
        )
        // SegmentControl fills the row unless it is given a width.
        SegmentControl(modifier = Modifier.width(SegmentWidth * TrendProperty.entries.size)) {
            TrendProperty.entries.forEach { option ->
                SegmentItem(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    title = { Text(str(option.labelRes()), maxLines = 1) },
                )
            }
        }
    }
}
