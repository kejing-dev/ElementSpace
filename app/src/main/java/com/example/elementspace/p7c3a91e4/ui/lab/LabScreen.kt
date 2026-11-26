package com.example.elementspace.p7c3a91e4.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.Experiments
import com.example.elementspace.p7c3a91e4.domain.model.Experiment
import com.example.elementspace.p7c3a91e4.ui.common.CardBody
import com.example.elementspace.p7c3a91e4.ui.common.CardSection
import com.example.elementspace.p7c3a91e4.ui.common.CardSource
import com.example.elementspace.p7c3a91e4.ui.common.PagedCard
import com.example.elementspace.p7c3a91e4.ui.common.SideCard
import com.example.elementspace.p7c3a91e4.ui.common.ThingModels
import com.example.elementspace.p7c3a91e4.ui.lab.components.LabScene
import com.example.elementspace.p7c3a91e4.ui.lab.components.LabSceneSpec
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.example.elementspace.p7c3a91e4.ui.reaction.components.EquationLine
import com.example.elementspace.p7c3a91e4.ui.theme.AppLocale
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton
import com.pico.spatial.ui.design.VerticalDivider
import com.pico.spatial.ui.design.windows.Toolbar
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialScaleGesture
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

private val NoFocus = Modifier.focusProperties { canFocus = false }

/** The lab bench Volume (v1.9.0): equipment on a bench, one button per step, the phenomenon in 3D. */
@Composable
fun LabScreen(modifier: Modifier = Modifier) {
    val viewModel: LabViewModel = viewModel(factory = LabViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSpatialNavigator.current
    val onEvent: (LabEvent) -> Unit = { event ->
        viewModel.onEvent(event)
        if (event is LabEvent.MicroOpened) runCatching { navigator.openWindowContainer(WindowIds.REACTION_VOLUME, tag = WindowIds.REACTION_TAG) }
    }

    // QA screenshots: lab steps are plain events, see AGENTS.md.
    DebugBus.Collect { command ->
        when (command.cmd) {
            "lnext" -> viewModel.onEvent(LabEvent.Next)
            "lreset" -> viewModel.onEvent(LabEvent.Restart)
            "lchoice" -> command.number?.let { viewModel.onEvent(LabEvent.ChoiceSelected(it)) }
            "lpower" -> command.number?.let { viewModel.onEvent(LabEvent.PowerSelected(it)) }
        }
    }

    // v1.10.0: sound follows what is on the bench; the switch is remembered.
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("element_space", android.content.Context.MODE_PRIVATE) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    val sound = remember { LabSound(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    val heard = state.experiment?.let { e -> if (state.stage == 0) e.start else e.steps.getOrNull(state.stage - 1)?.effects.orEmpty() }.orEmpty()
    var volume by remember { mutableIntStateOf(prefs.getInt("volume", 1).coerceIn(0, 2)) }
    LaunchedEffect(heard, state.powerFactor, soundOn, volume) {
        sound.set(if (soundOn) LabMix.of(heard, state.powerFactor).times(LabMix.VOLUME[volume]) else LabMix())
    }
    val onVolume: (Int) -> Unit = { level ->
        volume = level
        prefs.edit().putInt("volume", level).apply()
    }
    val onSound: (Boolean) -> Unit = { on ->
        soundOn = on
        prefs.edit().putBoolean("sound", on).apply()
    }

    AppLocale(state.language) {
        Box(modifier = modifier.fillMaxSize()) {
            val experiment = state.experiment
            if (experiment == null) {
                Text(str(R.string.lab_empty), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelPrimary, modifier = Modifier.align(Alignment.Center))
                return@Box
            }
            LabSceneView(
                spec = LabSceneSpec(experiment, state.stage, state.choice?.takeIf { state.stage > 0 }?.let { experiment.choices[it].flame }, state.powerFactor),
                resetToken = state.resetToken,
                modifier = Modifier.fillMaxSize(),
            )
            Toolbar { LabToolbar(state, experiment, soundOn, onSound, onEvent) }
            SideCard { LabCard(state, experiment, volume, onVolume, onEvent) }
        }
    }
}

/** Under the bench: the next step as one big button, start over, and the small / medium / large control. */
@Composable
private fun RowScope.LabToolbar(state: LabUiState, experiment: Experiment, soundOn: Boolean, onSound: (Boolean) -> Unit, onEvent: (LabEvent) -> Unit) {
    val next = experiment.steps.getOrNull(state.stage)
    if (next != null) {
        Button(onClick = { onEvent(LabEvent.Next) }, modifier = NoFocus) {
            Icon(painterResource(R.drawable.ic_player_play), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(next.button.get(state.language), maxLines = 1)
        }
    }
    Button(onClick = { onEvent(LabEvent.Restart) }, enabled = state.stage > 0, modifier = NoFocus) { Text(str(R.string.lab_restart), maxLines = 1) }
    experiment.adjust?.let { label ->
        VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
        Text(label.get(state.language), style = PicoTheme.typography.bodyLarge, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1)
        listOf(R.string.lab_small, R.string.lab_medium, R.string.lab_large).forEachIndexed { level, text ->
            ToggleButton(checked = state.power == level, onCheckedChange = { onEvent(LabEvent.PowerSelected(level)) }, modifier = NoFocus) { Text(str(text), maxLines = 1) }
        }
    }
    VerticalDivider(modifier = Modifier.size(width = 1.dp, height = 32.dp))
    ToggleButton(checked = soundOn, onCheckedChange = onSound, modifier = NoFocus) { Text(str(R.string.lab_sound), maxLines = 1) }
    IconButton(onClick = { onEvent(LabEvent.ResetView) }, modifier = NoFocus) {
        Icon(painter = painterResource(R.drawable.ic_rotate), contentDescription = str(R.string.reset_view))
    }
}

/** What you see and why, the reagents to pick, and the list of experiments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabCard(state: LabUiState, experiment: Experiment, volume: Int, onVolume: (Int) -> Unit, onEvent: (LabEvent) -> Unit) {
    val language = state.language
    PagedCard(
        key = experiment.id,
        tabs = listOf(str(R.string.lab_tab_now), str(R.string.lab_tab_all, Experiments.ALL.size)),
        header = { Text(experiment.name.get(language), style = PicoTheme.typography.headlineMedium, color = PicoTheme.colorScheme.labelPrimary) },
    ) { page ->
        if (page == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Experiments.ALL.forEach { other ->
                    Button(onClick = { onEvent(LabEvent.ExperimentSelected(other.id)) }, enabled = other.id != experiment.id, modifier = NoFocus.fillMaxWidth().height(64.dp)) {
                        Text(other.name.get(language), maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
            }
            return@PagedCard
        }
        // Step dots, then the sentence for where we are.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..experiment.steps.size).forEach { k ->
                val on = k == state.stage
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(if (on) PicoTheme.colorScheme.labelPrimary else PicoTheme.colorScheme.fillTertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (k == 0) "·" else "$k", style = PicoTheme.typography.titleMedium, color = if (on) ElementColors.OverlayCard else PicoTheme.colorScheme.labelSecondary)
                }
            }
        }
        val observation = if (state.stage == 0) experiment.setup else experiment.steps[state.stage - 1].observe
        val choiceNote = state.choice?.takeIf { state.stage > 0 }?.let { experiment.choices[it].note }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ElementColors.ConditionChip).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(str(if (state.stage == 0) R.string.lab_on_bench else R.string.lab_you_see), style = PicoTheme.typography.titleMedium, color = Color(0xFF5B6370))
            Text((choiceNote ?: observation).get(language), style = PicoTheme.typography.titleMedium, color = Color(0xFF1D2127))
        }
        experiment.steps.getOrNull(state.stage)?.let { next ->
            Button(onClick = { onEvent(LabEvent.Next) }, modifier = NoFocus.fillMaxWidth().height(64.dp)) {
                Icon(painterResource(R.drawable.ic_player_play), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(next.button.get(language), maxLines = 1)
            }
        }
        if (experiment.choices.isNotEmpty() && state.stage > 0) {
            CardSection(str(R.string.lab_choices)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    experiment.choices.forEachIndexed { i, choice ->
                        ToggleButton(checked = state.choice == i, onCheckedChange = { onEvent(LabEvent.ChoiceSelected(i)) }, modifier = NoFocus.height(56.dp)) {
                            Text(choice.label.get(language), maxLines = 1)
                        }
                    }
                }
            }
        }
        val reaction = experiment.reactionId?.let { id -> remember(id) { BundledReactionRepository.shared.reaction(id) } }
        if (reaction != null) {
            CardSection(str(R.string.lab_micro_title)) {
                EquationLine(reaction, language, big = false)
                Button(onClick = { onEvent(LabEvent.MicroOpened) }, modifier = NoFocus.height(56.dp)) { Text(str(R.string.lab_micro), maxLines = 1) }
            }
        }
        // Three-stop volume: bound to a free Float so a tap or a short drag is never swallowed, snapped on release.
        CardSection(str(R.string.lab_volume)) {
            var pos by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Slider(modifier = NoFocus.weight(1f).height(48.dp), value = pos,
                    onValueChange = { v ->
                        pos = v
                        val level = Math.round(v).coerceIn(0, 2)
                        if (level != volume) onVolume(level)
                    },
                    onValueChangeFinished = { pos = Math.round(pos).toFloat() },
                    valueRange = 0f..2f,
                )
                Text(str(listOf(R.string.lab_volume_low, R.string.lab_volume_mid, R.string.lab_volume_high)[volume]), style = PicoTheme.typography.titleMedium, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1)
            }
        }
        CardBody(str(R.string.lab_safety), PicoTheme.colorScheme.labelSecondary)
        CardSource(str(R.string.rx_source, experiment.source.get(language)))
    }
}

private const val START_YAW = 0.35f
private const val START_PITCH = 0.22f
private const val RADIANS_PER_PIXEL = 0.006f
private const val MAX_PITCH = 1.2f

/** Hosts the bench; a drag turns it, two hands scale it. Models load off the main thread. */
@Composable
private fun LabSceneView(spec: LabSceneSpec, resetToken: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val converter = LocalPhysicalLengthConverter.current
    val scene = remember { LabScene() }
    var yaw by remember { mutableFloatStateOf(START_YAW) }
    var pitch by remember { mutableFloatStateOf(START_PITCH) }
    var zoom by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(resetToken) {
        yaw = START_YAW
        pitch = START_PITCH
        zoom = 1f
    }
    DisposableEffect(scene) { onDispose { scene.destroy() } }
    // The bench and this experiment's equipment: fresh copies, placed once per experiment.
    LaunchedEffect(spec.experiment.id) {
        val experiment = spec.experiment
        scene.render(spec)
        val models = HashMap<String, Pair<com.pico.spatial.core.ecs.Entity, com.example.elementspace.p7c3a91e4.ui.common.ModelBox?>>()
        ThingModels.copy(context, "lab", "bench")?.let { models["bench"] = it to ThingModels.bounds(context, "lab", "bench") }
        experiment.props.forEachIndexed { i, prop ->
            ThingModels.copy(context, "lab", prop.model)?.let { models["${prop.model}#$i"] = it to ThingModels.bounds(context, "lab", prop.model) }
        }
        scene.setProps(experiment, models)
    }
    BoxWithConstraints(modifier = modifier) {
        val radiusMeters = converter.dpToLength(min(maxWidth, maxHeight) * 0.42f, LengthUnit.Meters)
        val y = yaw
        val p = pitch
        val z = zoom
        SpatialView(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        // gate:allow board-input drag only turns the whole bench; nothing on it is picked or hit-tested
                        detectSpatialDragGesture(context, targetedToEntity = TargetEntity.any()) {
                            yaw += it.dragAmount.x * RADIANS_PER_PIXEL
                            pitch = (pitch + it.dragAmount.y * RADIANS_PER_PIXEL).coerceIn(-0.2f, MAX_PITCH)
                        }
                    }
                    .pointerInput(Unit) { detectSpatialScaleGesture(context) { zoom = (zoom * it.scaleValue).coerceIn(0.6f, 1.8f) } },
            update = { _, _ ->
                scene.render(spec)
                scene.setPose(radiusMeters, y, p, z)
            },
        ) { content, _ ->
            scene.render(spec)
            scene.setPose(radiusMeters, y, p, z)
            content.addEntity(scene.root)
        }
    }
}
