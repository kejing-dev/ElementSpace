package com.example.elementspace.p7c3a91e4.ui.atom.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton
import com.pico.spatial.ui.design.VerticalDivider

@StringRes
fun AtomViewMode.labelRes(): Int =
    when (this) {
        AtomViewMode.SHELL -> R.string.view_shell
        AtomViewMode.NUCLEUS -> R.string.view_nucleus
        AtomViewMode.CLOUD -> R.string.view_cloud
        AtomViewMode.ALLOTROPE -> R.string.view_allotrope
    }

/**
 * Content of the window-level Toolbar under the Volume: the two basic views, the Advanced entry
 * that reveals the cloud and allotrope views, then playback and reset.
 */
@Composable
fun RowScope.AtomToolbarContent(
    view: AtomViewMode,
    advancedExpanded: Boolean,
    allotropeAvailable: Boolean,
    paused: Boolean,
    onViewSelected: (AtomViewMode) -> Unit,
    onAdvancedToggle: () -> Unit,
    onPauseToggle: () -> Unit,
    onReset: () -> Unit,
) {
    AtomViewMode.entries.filter { !it.advanced }.forEach { mode ->
        ViewToggle(mode, selected = view == mode, enabled = true, onViewSelected)
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    Button(onClick = onAdvancedToggle) {
        if (advancedExpanded) {
            Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(str(if (advancedExpanded) R.string.advanced_collapse else R.string.advanced), maxLines = 1)
        if (!advancedExpanded) {
            Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
    if (advancedExpanded) {
        ViewToggle(AtomViewMode.CLOUD, selected = view == AtomViewMode.CLOUD, enabled = true, onViewSelected)
        ViewToggle(AtomViewMode.ALLOTROPE, selected = view == AtomViewMode.ALLOTROPE, enabled = allotropeAvailable, onViewSelected)
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    IconButton(onClick = onPauseToggle) {
        Icon(
            painter = painterResource(if (paused) R.drawable.ic_player_play else R.drawable.ic_player_pause),
            contentDescription = str(if (paused) R.string.play else R.string.pause),
        )
    }
    IconButton(onClick = onReset) {
        Icon(painter = painterResource(R.drawable.ic_rotate), contentDescription = str(R.string.reset_view))
    }
}

@Composable
private fun ViewToggle(
    mode: AtomViewMode,
    selected: Boolean,
    enabled: Boolean,
    onSelected: (AtomViewMode) -> Unit,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onSelected(mode) },
        enabled = enabled,
    ) {
        Text(str(mode.labelRes()), maxLines = 1)
    }
}
