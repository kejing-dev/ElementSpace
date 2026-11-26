package com.example.elementspace.p7c3a91e4.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flat text labels in a 3D scene (v1.3.0): element symbols on atoms and condition tags. Each label
 * is a textured plane that always faces the front of the Volume, whatever the user's rotation.
 * Textures and the plane mesh are made per scene rebuild and dropped with it (see AtomScene for
 * why resources are never kept across rebuilds).
 */
class SceneLabels {
    enum class Style { SYMBOL, TAG }

    private class Placed(val entity: Entity, val base: Vec3, val lift: Float)

    private val placed = ArrayList<Placed>()
    private val materials = HashMap<Pair<String, Style>, Pair<UnlitMaterial, Float>>()
    private var plane: MeshResource? = null
    private var yaw = 0f
    private var pitch = 0f

    /** Forget everything made for the previous rebuild (its entities are destroyed with it). */
    fun reset() {
        placed.clear()
        materials.clear()
        plane = null
    }

    /**
     * A label of [height] (scene units) centred at [at], pushed [lift] towards the viewer so it sits
     * on the front of a ball of that radius. Add the returned entity to the parent [at] is in.
     */
    fun label(text: String, at: Vec3, height: Float, lift: Float, style: Style = Style.SYMBOL): Entity {
        val (material, aspect) = materials.getOrPut(text to style) { material(text, style) }
        val mesh = plane ?: MeshResource.createPlane(1f, 1f, 0f).also { plane = it }
        val entity = ModelEntity(mesh, material)
        entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(height * aspect, height, height))
        placed += Placed(entity, at, lift)
        face(placed.last())
        return entity
    }

    /** The parents' rotation is pitch about x after yaw about y; labels undo it. */
    fun setRotation(yaw: Float, pitch: Float) {
        this.yaw = yaw
        this.pitch = pitch
        placed.forEach(::face)
    }

    private fun face(p: Placed) {
        val front = towardsViewer(yaw, pitch, p.lift)
        p.entity.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(p.base.x + front.x, p.base.y + front.y, p.base.z + front.z))
            setQuaternion(inverse(yaw, pitch) * PLANE_TO_FRONT)
        }
    }

    private fun material(text: String, style: Style): Pair<UnlitMaterial, Float> {
        val bitmap = bitmap(text, style)
        val aspect = bitmap.width.toFloat() / bitmap.height
        val material =
            UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
                setBaseColor(Color4(1f, 1f, 1f, 1f))
                setBaseColorTexture(TextureResource(bitmap))
            }
        return material to aspect
    }

    companion object {
        /**
         * Rotation from the engine's plane to one facing +Z, the viewer in front of the Volume.
         * `createPlane` already makes an XY plane facing +Z (emulator, v1.3.0: a quarter turn about x
         * laid every label flat), so this is the identity.
         */
        val PLANE_TO_FRONT: Quat = Quat.identity()

        private const val TEXT_PX = 112f

        /** Inverse of Rx(pitch) * Ry(yaw) = Ry(-yaw) * Rx(-pitch). */
        fun inverse(yaw: Float, pitch: Float): Quat = axisAngle(0f, 1f, 0f, -yaw) * axisAngle(1f, 0f, 0f, -pitch)

        /** (0, 0, d) seen from inside the rotated parent: Ry(-yaw) * Rx(-pitch) * (0, 0, d). */
        fun towardsViewer(yaw: Float, pitch: Float, d: Float): Vec3 =
            Vec3(-d * cos(pitch) * sin(yaw), d * sin(pitch), d * cos(pitch) * cos(yaw))

        fun axisAngle(x: Float, y: Float, z: Float, radians: Float): Quat {
            val s = sin(radians / 2f)
            return Quat(x * s, y * s, z * s, cos(radians / 2f))
        }

        /**
         * White bold symbol with a dark outline, or an orange tag on a light pill (M3 sample).
         * Text after "_" is drawn as a subscript ("2p_x" → 2pₓ).
         */
        fun bitmap(text: String, style: Style): Bitmap {
            val main = text.substringBefore('_')
            val sub = text.substringAfter('_', "")
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = TEXT_PX
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
            val subPaint = Paint(paint).apply { textSize = TEXT_PX * 0.62f }
            val textWidth = paint.measureText(main) + if (sub.isEmpty()) 0f else subPaint.measureText(sub) + TEXT_PX * 0.04f
            val pad = if (style == Style.TAG) TEXT_PX * 0.55f else TEXT_PX * 0.18f
            val width = (textWidth + pad * 2).toInt().coerceAtLeast(8)
            val height = (TEXT_PX * 1.45f).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val baseline = height / 2f - (paint.descent() + paint.ascent()) / 2f
            val start = (width - textWidth) / 2f
            fun draw(p: Paint, sp: Paint) {
                canvas.drawText(main, start, baseline, p)
                if (sub.isNotEmpty()) canvas.drawText(sub, start + paint.measureText(main) + TEXT_PX * 0.04f, baseline + TEXT_PX * 0.2f, sp)
            }
            when (style) {
                Style.SYMBOL -> {
                    val stroke = { p: Paint -> Paint(p).apply { this.style = Paint.Style.STROKE; strokeWidth = TEXT_PX * 0.16f; color = 0xE6141619.toInt(); strokeJoin = Paint.Join.ROUND } }
                    draw(stroke(paint), stroke(subPaint))
                    paint.color = 0xFFFFFFFF.toInt()
                    subPaint.color = 0xFFFFFFFF.toInt()
                    draw(paint, subPaint)
                }
                Style.TAG -> {
                    val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF0FFFFFF.toInt() }
                    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), height / 2f, height / 2f, pill)
                    paint.color = 0xFFB25A00.toInt()
                    subPaint.color = 0xFFB25A00.toInt()
                    draw(paint, subPaint)
                }
            }
            return bitmap
        }
    }
}
