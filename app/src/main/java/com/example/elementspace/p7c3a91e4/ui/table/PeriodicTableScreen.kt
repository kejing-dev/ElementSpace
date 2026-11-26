package com.example.elementspace.p7c3a91e4.ui.table

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.platform.AppExit
import com.example.elementspace.p7c3a91e4.platform.BackKeys
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.example.elementspace.p7c3a91e4.ui.navigation.DemoMode
import com.example.elementspace.p7c3a91e4.ui.table.components.ExitConfirmOverlay
import com.example.elementspace.p7c3a91e4.ui.table.components.HowToOverlay
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.example.elementspace.p7c3a91e4.ui.table.components.ColourControl
import com.example.elementspace.p7c3a91e4.domain.model.LearningLevel
import com.example.elementspace.p7c3a91e4.ui.table.components.CollideOverlay
import com.example.elementspace.p7c3a91e4.ui.table.components.FirstRunOverlay
import com.example.elementspace.p7c3a91e4.ui.table.components.GuideBar
import com.example.elementspace.p7c3a91e4.ui.table.components.GuideListOverlay
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
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

    // Demo recording only; does nothing unless the emulator setting is present.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        DemoMode.atoms(context).forEachIndexed { index, atomicNumber ->
            if (index > 0) kotlinx.coroutines.delay(DemoMode.SECOND_ATOM_DELAY_MS)
            viewModel.onEvent(PeriodicTableEvent.ElementSelected(atomicNumber))
        }
    }

    // Guided paths open the reaction / molecule Volume through here.
    LaunchedEffect(state.auxWindow) {
        val aux = state.auxWindow ?: return@LaunchedEffect
        runCatching {
            when (aux) {
                AuxWindow.REACTION -> navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG)
                AuxWindow.MOLECULE -> navigator.openWindowContainer(WindowIds.MOLECULE_VOLUME, tag = WindowIds.MOLECULE_TAG)
            }
        }
        viewModel.onEvent(PeriodicTableEvent.AuxWindowHandled)
    }

    // First launch only: a card with three things to try (v1.7.0).
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("element_space", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("first_run_done", false)) {
            prefs.edit().putBoolean("first_run_done", true).apply()
            viewModel.onEvent(PeriodicTableEvent.FirstRunVisible(true))
        }
    }

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

    // BACK / controller B closes the topmost overlay or menu; it never leaves the app (D60).
    LaunchedEffect(Unit) { BackKeys.presses.collect { viewModel.onEvent(PeriodicTableEvent.BackPressed) } }

    // QA screenshots: see DebugBus / AGENTS.md.
    DebugBus.Collect { command ->
        when (command.cmd) {
            "guide" -> if (command.arg == null) viewModel.onEvent(PeriodicTableEvent.GuideListVisible(true)) else viewModel.onEvent(PeriodicTableEvent.GuideStarted(command.arg))
            "gnext" -> viewModel.onEvent(PeriodicTableEvent.GuideNext)
            "gexit" -> viewModel.onEvent(PeriodicTableEvent.GuideExit)
            "firstrun" -> viewModel.onEvent(PeriodicTableEvent.FirstRunVisible(true))
            "level" -> LearningLevel.entries.firstOrNull { it.name.equals(command.arg, ignoreCase = true) }?.let { viewModel.onEvent(PeriodicTableEvent.LevelSelected(it)) }
            "atom" -> command.number?.let { viewModel.onEvent(PeriodicTableEvent.ElementSelected(it)) }
            // "collide 11,17": turn the mode on and pick both; "collide off" leaves it.
            "collide" -> {
                if (viewModel.state.value.collideMode) viewModel.onEvent(PeriodicTableEvent.CollideToggled)
                if (command.arg != "off") {
                    viewModel.onEvent(PeriodicTableEvent.CollideToggled)
                    command.arg?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.forEach { viewModel.onEvent(PeriodicTableEvent.ElementSelected(it)) }
                }
            }
            "reaction" ->
                command.arg?.let { id ->
                    InMemorySessionRepository.shared.showReaction(id)
                    runCatching { navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG) }
                }
            "molecule" ->
                command.arg?.let { id ->
                    InMemorySessionRepository.shared.showMolecule(id)
                    runCatching { navigator.openWindowContainer(WindowIds.MOLECULE_VOLUME, tag = WindowIds.MOLECULE_TAG) }
                }
            "compare" -> InMemorySessionRepository.shared.compareMolecule(command.arg?.takeIf { it != "none" })
            "settings" -> viewModel.onEvent(PeriodicTableEvent.LanguageMenuVisible(true))
            "howto" -> viewModel.onEvent(PeriodicTableEvent.HowToVisible(true))
            "exit" -> viewModel.onEvent(PeriodicTableEvent.ExitConfirmVisible(true))
            "lang" -> viewModel.onEvent(PeriodicTableEvent.LanguageSelected(if (command.arg == "en") AppLanguage.EN else AppLanguage.ZH))
            "reset" -> repeat(3) { viewModel.onEvent(PeriodicTableEvent.BackPressed) }
            // Same call as the [退出] button on the confirm card (the emulator CLI cannot tap in 3D).
            "exit-yes" -> AppExit.exit(context, navigator)
        }
    }

    AppLocale(state.language) {
        SnackbarHost {
            PeriodicTableContent(
                state = state,
                onEvent = { event ->
                    viewModel.onEvent(event)
                    if (event is PeriodicTableEvent.CollideReactionOpened) {
                        runCatching { navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG) }
                    }
                },
                onExit = { AppExit.exit(context, navigator) },
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun PeriodicTableContent(
    state: PeriodicTableUiState,
    onEvent: (PeriodicTableEvent) -> Unit,
    onExit: () -> Unit,
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

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TableTitleBar(
            query = state.query,
            onQueryChange = { onEvent(PeriodicTableEvent.QueryChanged(it)) },
            language = state.language,
            languageMenuOpen = state.languageMenuOpen,
            onLanguageMenuVisible = { onEvent(PeriodicTableEvent.LanguageMenuVisible(it)) },
            onLanguageSelected = { onEvent(PeriodicTableEvent.LanguageSelected(it)) },
            onHowTo = { onEvent(PeriodicTableEvent.HowToVisible(true)) },
            onExit = { onEvent(PeriodicTableEvent.ExitConfirmVisible(true)) },
            level = state.level,
            onLevelSelected = { onEvent(PeriodicTableEvent.LevelSelected(it)) },
            onGuide = { onEvent(PeriodicTableEvent.GuideListVisible(true)) },
            collideMode = state.collideMode,
            onCollideToggle = { onEvent(PeriodicTableEvent.CollideToggled) },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.collideMode) {
                val first = state.collidePicks.firstOrNull()?.let { z -> state.elements.getOrNull(z - 1) }
                Text(
                    text = if (first == null) str(R.string.collide_hint) else str(R.string.collide_hint_one, "${first.symbol} ${first.name(state.language)}"),
                    style = PicoTheme.typography.titleMedium,
                    color = PicoTheme.colorScheme.labelPrimary,
                )
            } else {
                ColourControl(
                    selected = state.trend,
                    onSelect = { onEvent(PeriodicTableEvent.TrendSelected(it)) },
                )
            }
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
                // Raised tiles would poke through the result card, so they drop back while it is open.
                picked = if (state.collideOutcome == null) state.collidePicks else emptyList(),
            )
            state.guide?.let { GuideBar(it, state.language, onEvent) }
            if (state.guide == null) StatusRow(
                openElements = state.openAtoms.mapNotNull { z -> state.elements.getOrNull(z - 1) },
                maxOpen = state.maxOpenAtoms,
                language = state.language,
                noMatch = state.noMatch,
                onOpenElementClick = { onEvent(PeriodicTableEvent.ElementSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.guideListOpen) GuideListOverlay(state.language, onEvent)
    if (state.firstRunOpen) FirstRunOverlay(onEvent)
    state.collideOutcome?.let { CollideOverlay(it, state.language, onEvent) }
    if (state.howToOpen) HowToOverlay(onClose = { onEvent(PeriodicTableEvent.HowToVisible(false)) })
    if (state.exitConfirmOpen) ExitConfirmOverlay(onCancel = { onEvent(PeriodicTableEvent.ExitConfirmVisible(false)) }, onExit = onExit)
    }
}
