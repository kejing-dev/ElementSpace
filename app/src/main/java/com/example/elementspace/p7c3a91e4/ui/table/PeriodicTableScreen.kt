package com.example.elementspace.p7c3a91e4.ui.table

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.example.elementspace.p7c3a91e4.ui.table.components.ColourControl
import com.example.elementspace.p7c3a91e4.ui.table.components.ElementGrid
import com.example.elementspace.p7c3a91e4.ui.table.components.StatusRow
import com.example.elementspace.p7c3a91e4.ui.table.components.TableTitleBar
import com.example.elementspace.p7c3a91e4.ui.theme.AppLocale
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.windows.LocalSnackbarHostState
import com.pico.spatial.ui.design.windows.SnackbarHost
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

/** Main window: the periodic table. Opens one atom Volume per selected element. */
@Composable
fun PeriodicTableScreen(modifier: Modifier = Modifier) {
    val viewModel: PeriodicTableViewModel = viewModel(factory = PeriodicTableViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSpatialNavigator.current

    LaunchedEffect(state.windowRequest) {
        val request = state.windowRequest ?: return@LaunchedEffect
        request.close?.let { navigator.closeWindowContainer(WindowIds.ATOM_VOLUME, WindowIds.atomTag(it)) }
        navigator.openWindowContainer(
            WindowIds.ATOM_VOLUME,
            tag = WindowIds.atomTag(request.open),
            bundle = Bundle().apply { putInt(WindowIds.ARG_ATOMIC_NUMBER, request.open) },
        )
        viewModel.onEvent(PeriodicTableEvent.WindowRequestHandled)
    }

    AppLocale(state.language) {
        SnackbarHost { PeriodicTableContent(state = state, onEvent = viewModel::onEvent, modifier = modifier) }
    }
}

@Composable
internal fun PeriodicTableContent(
    state: PeriodicTableUiState,
    onEvent: (PeriodicTableEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = LocalSnackbarHostState.current
    val replaced = state.replacedNotice
    val replacedMessage = replaced?.let { str(R.string.replaced_volume, "${it.symbol} ${it.name(state.language)}") }
    LaunchedEffect(replaced) {
        if (replacedMessage != null) {
            onEvent(PeriodicTableEvent.ReplacedNoticeShown)
            snackbar.show(message = replacedMessage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TableTitleBar(
            query = state.query,
            onQueryChange = { onEvent(PeriodicTableEvent.QueryChanged(it)) },
            language = state.language,
            languageMenuOpen = state.languageMenuOpen,
            onLanguageMenuVisible = { onEvent(PeriodicTableEvent.LanguageMenuVisible(it)) },
            onLanguageSelected = { onEvent(PeriodicTableEvent.LanguageSelected(it)) },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColourControl(
                selected = state.trend,
                onSelect = { onEvent(PeriodicTableEvent.TrendSelected(it)) },
            )
            ElementGrid(
                elements = state.elements,
                language = state.language,
                matches = state.matches,
                filtering = state.filtering,
                openAtoms = state.openAtoms,
                trend = state.trend,
                trendScale = state.trendScale,
                selectedCategory = state.category,
                onCategoryToggle = { onEvent(PeriodicTableEvent.CategoryToggled(it)) },
                onElementClick = { onEvent(PeriodicTableEvent.ElementSelected(it)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            StatusRow(
                openElements = state.openAtoms.mapNotNull { z -> state.elements.getOrNull(z - 1) },
                maxOpen = state.maxOpenAtoms,
                language = state.language,
                noMatch = state.noMatch,
                onOpenElementClick = { onEvent(PeriodicTableEvent.ElementSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
