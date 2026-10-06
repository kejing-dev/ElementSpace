package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Chip
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/** Bottom row: which atom Volumes are open, and a one-line hint or the empty-search message. */
@Composable
fun StatusRow(
    openElements: List<Element>,
    maxOpen: Int,
    language: AppLanguage,
    noMatch: Boolean,
    onOpenElementClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = str(R.string.open_count, openElements.size, maxOpen),
                style = PicoTheme.typography.labelMedium,
                color = PicoTheme.colorScheme.labelSecondary,
            )
            openElements.forEach { element ->
                Chip(
                    label = { Text("${element.symbol} ${element.name(language)}", maxLines = 1) },
                    onClick = { onOpenElementClick(element.atomicNumber) },
                    leadingIcon = {
                        Box(
                            Modifier.size(10.dp)
                                .clip(CircleShape)
                                .background(ElementColors.category(element.category))
                        )
                    },
                )
            }
        }
        Text(
            text = str(if (noMatch) R.string.no_match else R.string.hint_open),
            style = PicoTheme.typography.labelMedium,
            color = PicoTheme.colorScheme.labelTertiary,
            maxLines = 1,
        )
    }
}
