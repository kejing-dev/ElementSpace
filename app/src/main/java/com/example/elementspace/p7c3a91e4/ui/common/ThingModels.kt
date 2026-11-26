package com.example.elementspace.p7c3a91e4.ui.common

import android.content.Context
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelFormat
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.ecs.OpacityControllerComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Size and centre of a model's bounding box, in the model's own units (assets/models/<dir>/index.json). */
data class ModelBox(val w: Float, val h: Float, val d: Float, val cx: Float, val cy: Float, val cz: Float) {
    val largest: Float
        get() = maxOf(w, h, d)
}

/**
 * Loads the GLB models in assets/models (everyday things, lab equipment). Templates are kept out of
 * the scene and cloned for every use, so a scene rebuild can destroy its copy safely.
 */
object ThingModels {
    private const val TAG = "ElementSpaceModel"
    private val templates = HashMap<String, Entity>()
    private val boxes = HashMap<String, Map<String, ModelBox>>()

    fun bounds(context: Context, dir: String, id: String): ModelBox? =
        boxes.getOrPut(dir) {
            runCatching {
                val json = JSONObject(context.assets.open("models/$dir/index.json").bufferedReader().use { it.readText() })
                json.keys().asSequence().associateWith { key ->
                    val o = json.getJSONObject(key)
                    fun f(name: String) = o.getDouble(name).toFloat()
                    ModelBox(f("w"), f("h"), f("d"), f("cx"), f("cy"), f("cz"))
                }
            }.onFailure { Log.w(TAG, "index.json of $dir unreadable: $it") }.getOrDefault(emptyMap())
        }[id]

    /** A fresh copy of models/[dir]/[id].glb, or null when it cannot be loaded. */
    suspend fun copy(context: Context, dir: String, id: String): Entity? {
        val key = "$dir/$id"
        val template =
            templates[key] ?: runCatching {
                val bytes = withContext(Dispatchers.IO) { context.assets.open("models/$key.glb").use { it.readBytes() } }
                Entity.loadSuspend(ByteArrayInputStream(bytes), ModelFormat.GLTF).also {
                    it.enabled = false
                    strip(it)
                    templates[key] = it
                    Log.i(TAG, "LOADED $key")
                }
            }.onFailure { Log.w(TAG, "FAILED $key: ${it.javaClass.simpleName} ${it.message}") }.getOrNull() ?: return null
        return runCatching { template.clone(Entity.CloneOptions(true, true))?.also { it.enabled = true } }.getOrNull()
    }

    /**
     * Turns every material of [root] into clear tinted glass. Tripo meshes of glassware come untextured
     * and would show as white models; the material is shared with the template, which is fine because a
     * glass object is always glass.
     */
    /** Makes every material give off [color], so a lamp looks switched on. */
    fun lit(root: Entity, color: Color4) {
        root.components[ModelComponent::class.java]?.let { model ->
            runCatching { model.materials.forEach { material -> if (material is PhysicallyBasedMaterial) material.setEmissiveColor(color) } }
        }
        runCatching { root.getChildren() }.getOrNull()?.forEach { lit(it, color) }
    }

    fun glass(root: Entity, alpha: Float = 0.28f) {
        val tint = Color4(0.82f, 0.93f, 1f, alpha)
        root.components[ModelComponent::class.java]?.let { model ->
            runCatching {
                model.materials.forEach { material ->
                    if (material is PhysicallyBasedMaterial) {
                        material.setBlendingMode(BlendingMode.TRANSPARENT)
                        material.setBaseColor(tint)
                        material.setOpacity(alpha)
                        material.setRoughness(0.06f)
                        material.setMetallic(0f)
                        // Glass must not hide what is inside it: without this, flames in a gas jar fail the depth test.
                        material.setDepthWrite(false)
                    }
                }
            }
        }
        runCatching { root.getChildren() }.getOrNull()?.forEach { glass(it, alpha) }
    }

    /** An opacity component, even at 1.0, sends a model down the blended path and makes it look see-through. */
    private fun strip(root: Entity) {
        if (root.components[OpacityControllerComponent::class.java] != null) root.components.remove(OpacityControllerComponent::class.java)
        runCatching { root.getChildren() }.getOrNull()?.forEach { strip(it) }
    }
}
