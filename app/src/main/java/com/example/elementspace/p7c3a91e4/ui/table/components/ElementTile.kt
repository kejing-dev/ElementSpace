package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.hover.spring
import com.pico.spatial.ui.foundation.layout.zOffset

/** Everything one tile needs to draw itself; computed by the grid from the screen state. */
data class ElementTileModel(
    val atomicNumber: Int,
    val symbol: String,
    /** Short line under the symbol: the Chinese name, a trend value, or empty. */
    val caption: String,
    val fullName: String,
    val tint: Color,
    /** False when a filter is active and this element does not match. */
    val matched: Boolean,
    /** True while this element has an open atom Volume. */
    val opened: Boolean,
    /** Trend height in 0..1, or null when tiles are flat. */
    val height: Float?,
    /** True in trend mode when the element has no value. */
    val noData: Boolean,
)

private val TileShape = RoundedCornerShape(12.dp)
// Kept small on purpose: a flat tile lifted far toward the viewer shifts sideways in perspective
// and the grid stops reading as a table.
private val TrendLift = 28.dp
private val OpenedLift = 16.dp

/**
 * A translucent "crystal" tile. Hovering floats it up through the system hover effect; an opened
 * element stays lifted, and in trend mode the tile rises along Z with its value.
 */
@Composable
fun ElementTile(model: ElementTileModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lift =
        when {
            model.height != null -> TrendLift * model.height
            model.opened -> OpenedLift
            else -> 0.dp
        }
    // The system glass is bright in a lit room, so the tint has to be strong enough for the
    // light label colour to stay readable on top of it.
    val fillAlpha =
        when {
            model.noData -> 0.08f
            model.opened -> 0.96f
            model.height != null -> 0.86f
            else -> 0.8f
        }
    // A tile without data stays a quiet placeholder: system fill with a dark-on-glass label, so
    // it is still readable and the table keeps its shape.
    val fill = if (model.noData) PicoTheme.colorScheme.fillTertiary else model.tint.copy(alpha = fillAlpha)
    val sheen = PicoTheme.colorScheme.labelPrimaryLight
    val foreground = if (model.noData) PicoTheme.colorScheme.labelTertiary else PicoTheme.colorScheme.labelPrimaryLight

    Box(
        modifier =
            modifier
                .zOffset { lift.toPx() }
                .alpha(if (model.matched) 1f else 0.3f)
                // The hover effect comes before clip/background so the whole tile, not just its
                // text, swells and lifts. A soft spring gives it a breathing feel. Growth stays
                // inside the gap between tiles so neighbours are never covered.
                .spatialHoverEffect {
                    animation(spring(dampingRatio = 0.42f, stiffness = 220f)) {
                        scale(if (it.isActive) 1.1f else 1f)
                        offset(y = if (it.isActive) (-4).dp else 0.dp)
                    }
                }
                .clip(TileShape)
                .background(fill)
                .background(
                    Brush.verticalGradient(
                        0f to sheen.copy(alpha = if (model.noData) 0f else 0.18f),
                        0.6f to sheen.copy(alpha = if (model.noData) 0f else 0.04f),
                        1f to Color.Transparent,
                    )
                )
                .clickable(onClick = onClick)
                .semantics { contentDescription = "${model.atomicNumber} ${model.fullName}" }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                text = model.atomicNumber.toString(),
                style = PicoTheme.typography.labelSmall,
                color = foreground.copy(alpha = 0.8f),
                maxLines = 1,
            )
            Text(
                text = model.symbol,
                style = PicoTheme.typography.titleMedium,
                color = foreground,
                maxLines = 1,
            )
            if (model.caption.isNotEmpty()) {
                Text(
                    text = model.caption,
                    style = PicoTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.9f),
                    maxLines = 1,
                )
            }
        }
        if (model.opened) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(foreground)
            )
        }
    }
}
