package com.example.elementspace.p7c3a91e4.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.foundation.geometry.DpOffset3D
import com.pico.spatial.ui.foundation.geometry.NormalizedPoint3D
import com.pico.spatial.ui.foundation.geometry.Rotation3D
import com.pico.spatial.ui.foundation.geometry.RotationAxis3D
import com.pico.spatial.ui.foundation.window.Augment
import com.pico.spatial.ui.foundation.window.AugmentContentAlignment
import com.pico.spatial.ui.platform.ViewPoint

/** Reading-sized card of every Volume (v1.3.0: 440 -> 560 dp wide so body text can be larger). */
val SideCardWidth = 560.dp
val SideCardHeight = 920.dp

/**
 * Where the model ends inside its Volume, as a fraction of the Volume width. The scenes fit their
 * content to about 72 % of the half-size (x = 0.86) but the drawn atoms rarely reach it (emulator:
 * x = 0.72 for Na + Cl₂), so the card starts at x = 0.8 instead of outside the 1600 dp cube (v1.2.0 headset: "离着模型很远").
 */
private const val MODEL_EDGE_X = 0.8f
private val CardGap = 24.dp

/**
 * Turned towards a viewer standing in front of the model (v1.2.0 headset: "没有很好的对准用户"),
 * about the card's left edge so that edge stays next to the model.
 */
const val SIDE_CARD_TURN_DEGREES = -20f

/**
 * The info card beside a Volume's model: close to the model, turned towards the viewer and
 * following the viewer to whichever side of the Volume they walk to.
 */
@Composable
fun SideCard(content: @Composable () -> Unit) {
    Augment(
        anchor = NormalizedPoint3D(MODEL_EDGE_X, 0.5f, 0.5f),
        alignment = AugmentContentAlignment.CenterLeft,
        offset = DpOffset3D(CardGap, 0.dp, 0.dp),
        rotation3D = Rotation3D(SIDE_CARD_TURN_DEGREES, RotationAxis3D(0f, 1f, 0f), NormalizedPoint3D(0f, 0.5f, 0.5f)),
        followViewpoints = ViewPoint.All,
    ) {
        Box(modifier = Modifier.size(SideCardWidth, SideCardHeight)) { content() }
    }
}
