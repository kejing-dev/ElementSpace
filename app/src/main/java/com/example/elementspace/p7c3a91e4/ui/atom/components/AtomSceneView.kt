package com.example.elementspace.p7c3a91e4.ui.atom.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.min
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.gesture.detectSpatialScaleGesture
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter

private const val START_YAW = 0.5f
private const val START_PITCH = 0.35f
private const val RADIANS_PER_PIXEL = 0.006f
private const val MAX_PITCH = 1.4f
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 1.6f

/**
 * Hosts the 3D model and turns spatial input into rotation and zoom.
 *
 * A pinch-and-drag (eye + hand), a direct hand drag or a controller trigger-drag all arrive through
 * the same spatial drag gesture, so one handler covers every input mode. Two hands or two
 * controllers scale the model. Orientation is view-only state and never leaves this composable.
 */
@Composable
fun AtomSceneView(
    spec: AtomSceneSpec,
    paused: Boolean,
    resetToken: Int,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val converter = LocalPhysicalLengthConverter.current
    val scene = remember { AtomScene() }
    val interact by rememberUpdatedState(onInteract)

    var yaw by remember { mutableFloatStateOf(START_YAW) }
    var pitch by remember { mutableFloatStateOf(START_PITCH) }
    var zoom by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(resetToken) {
        yaw = START_YAW
        pitch = START_PITCH
        zoom = 1f
    }
    DisposableEffect(scene) { onDispose { scene.destroy() } }

    BoxWithConstraints(modifier = modifier) {
        // Scene radius 1 maps to a little under half of the smaller side of the Volume.
        val radiusMeters = converter.dpToLength(min(maxWidth, maxHeight) * 0.42f, LengthUnit.Meters)
        val currentYaw = yaw
        val currentPitch = pitch
        val currentZoom = zoom

        SpatialView(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectSpatialDragGesture(context, targetedToEntity = TargetEntity.any()) {
                            yaw += it.dragAmount.x * RADIANS_PER_PIXEL
                            pitch = (pitch + it.dragAmount.y * RADIANS_PER_PIXEL).coerceIn(-MAX_PITCH, MAX_PITCH)
                            interact()
                        }
                    }
                    .pointerInput(Unit) {
                        detectSpatialScaleGesture(context) {
                            zoom = (zoom * it.scaleValue).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        }
                    },
            update = { _, _ ->
                scene.render(spec)
                scene.setPaused(paused)
                scene.setPose(radiusMeters, currentYaw, currentPitch, currentZoom)
            },
        ) { content, _ ->
            scene.render(spec)
            scene.setPaused(paused)
            scene.setPose(radiusMeters, currentYaw, currentPitch, currentZoom)
            content.addEntity(scene.root)
        }
    }
}
