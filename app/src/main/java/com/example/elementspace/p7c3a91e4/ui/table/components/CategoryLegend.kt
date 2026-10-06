package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleableChip

@StringRes
fun ElementCategory.labelRes(): Int =
    when (this) {
        ElementCategory.ALKALI_METAL -> R.string.cat_alkali_metal
        ElementCategory.ALKALINE_EARTH -> R.string.cat_alkaline_earth
        ElementCategory.TRANSITION_METAL -> R.string.cat_transition_metal
        ElementCategory.POST_TRANSITION -> R.string.cat_post_transition
        ElementCategory.METALLOID -> R.string.cat_metalloid
        ElementCategory.NONMETAL -> R.string.cat_nonmetal
        ElementCategory.HALOGEN -> R.string.cat_halogen
        ElementCategory.NOBLE_GAS -> R.string.cat_noble_gas
        ElementCategory.LANTHANIDE -> R.string.cat_lanthanide
        ElementCategory.ACTINIDE -> R.string.cat_actinide
    }

/** The ten category filter chips, wrapped inside the empty top of the table. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    selected: ElementCategory?,
    onToggle: (ElementCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = str(R.string.filter_by_category),
            style = PicoTheme.typography.bodyMedium,
            color = PicoTheme.colorScheme.labelSecondary,
        )
        // Chips wrap inside the empty block so longer labels never spill onto the tiles.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ElementCategory.entries.forEach { category ->
                ToggleableChip(
                    label = { Text(str(category.labelRes()), maxLines = 1) },
                    isToggleOn = selected == category,
                    onClick = { onToggle(category) },
                    leadingIcon = {
                        Box(
                            Modifier.size(10.dp)
                                .clip(CircleShape)
                                .background(ElementColors.category(category))
                        )
                    },
                )
            }
        }
    }
}
