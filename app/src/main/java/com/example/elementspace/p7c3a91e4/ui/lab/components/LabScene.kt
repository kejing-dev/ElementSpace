package com.example.elementspace.p7c3a91e4.ui.lab.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.elementspace.p7c3a91e4.domain.model.Experiment
import com.example.elementspace.p7c3a91e4.domain.model.LabColor
import com.example.elementspace.p7c3a91e4.domain.model.LabEffect
import com.example.elementspace.p7c3a91e4.domain.model.LabProp
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.ui.common.ModelBox
import com.example.elementspace.p7c3a91e4.ui.common.SceneLabels
import com.example.elementspace.p7c3a91e4.ui.common.TOUCH_RADIUS
import com.example.elementspace.p7c3a91e4.ui.common.ThingModels
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.PointLightComponent
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.dsl.registerComponent
import com.pico.spatial.ui.foundation.dsl.registerSystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** What the bench shows: an experiment, how far through it, the reagent picked and the flame setting. */
data class LabSceneSpec(
    val experiment: Experiment,
    /** 0 = set up, k = after step k. */
    val stage: Int,
    /** Tint of every flame (the flame test's salt), or null for the flame's own colour. */
    val flameTint: LabColor?,
    /** 0.6 / 1 / 1.5: small, medium, large. */
    val power: Float,
    /** Shown instead of the stage's own effects (the picked choice of step 1). */
    val override: List<LabEffect>? = null,
)

/** Calls [tick] every frame; the lab's particles, liquids and moving solids all advance from it. */
class LabTickComponent(val tick: (Float) -> Unit = {}) : Component() {
    override fun clone(): Component = LabTickComponent(tick)
}

class LabTickSystem : System() {
    override fun update(context: SceneUpdateContext) {
        super.update(context)
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(LabTickComponent::class.java))) {
            entity.components[LabTickComponent::class.java]?.tick?.invoke(context.deltaTime.coerceAtMost(0.25f))
        }
    }
}

/**
 * The lab bench Volume (v1.9.0). [root] takes the user's rotation and zoom. The bench and the
 * equipment are Tripo models that stay for the whole experiment; the phenomena are rebuilt for every
 * stage: additive sprite particles (flames, sparks, glows), alpha sprites (smoke, bubbles, flakes),
 * a point light for whatever burns, PBR liquids and small solids.
 *
 * The SDK's ParticleComponent has no documented way to be built in code (it is authored in the
 * Spatial Editor), so the particles here are a pool of camera-facing planes moved by [LabTickSystem].
 */
/** [embedded]: only the effects, in the host scene's own units, with no bench offset and nothing to grab (the atom Volume's thing view). */
class LabScene(private val embedded: Boolean = false) {
    val root: Entity = Entity()

    /** Everything on show hangs under this, scaled up so the bench fills the Volume instead of sitting small in its middle. */
    private val stage: Entity = Entity()
    private var spec: LabSceneSpec? = null
    private var propsLayer: Entity? = null
    private var propsFor: String? = null
    private var effectsLayer: Entity? = null
    private val emitters = ArrayList<Emitter>()
    private val tickers = ArrayList<(Float) -> Unit>()
    private var billboard: Quat = Quat.identity()
    private val random = Random(7)
    private var lastLiquid: LabColor? = null

    // Created per rebuild of the effects layer (see AtomScene for why meshes are not kept).
    private var planeMesh: MeshResource? = null
    private var sphereMesh: MeshResource? = null
    private var boxMesh: MeshResource? = null
    private var cylinderMesh: MeshResource? = null
    private val textures = HashMap<Sprite, TextureResource>()

    init {
        ensureRegistered()
        if (!embedded) {
            root.components.set(InteractableComponent())
            root.components.set(CollisionComponent(collisionShape = listOf(ShapeResource.createSphere(TOUCH_RADIUS).also { it.toGlobal() }), physicsMaterial = PhysicsMaterialResource()))
            stage.components[TransformComponent::class.java]?.setScaleVector(Vector3(STAGE_SCALE))
        }
        root.components.set(LabTickComponent(::tick))
        root.addChild(stage)
    }

    fun setPose(radiusMeters: Float, yaw: Float, pitch: Float, zoom: Float) {
        root.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(radiusMeters * zoom))
            setQuaternion(SceneLabels.axisAngle(1f, 0f, 0f, pitch) * SceneLabels.axisAngle(0f, 1f, 0f, yaw))
        }
        billboard = SceneLabels.inverse(yaw, pitch)
    }

    /** For an embedded scene: the host's rotation, so sprites keep facing the viewer. */
    fun setBillboard(value: Quat) {
        billboard = value
    }

    /** The experiment whose equipment is wanted but not yet on the bench, or null. */
    fun missingProps(): Experiment? = spec?.experiment?.takeIf { it.id != propsFor }

    /** Puts the bench and the equipment in place; [models] are fresh copies keyed by model id ("bench" included). */
    fun setProps(experiment: Experiment, models: Map<String, Pair<Entity, ModelBox?>>) {
        if (spec?.experiment?.id != experiment.id) return
        propsLayer?.destroy(true)
        val layer = Entity()
        models["bench"]?.let { (bench, box) ->
            // The bench's long side faces the viewer: Tripo's desk came out long along z, so it is turned a quarter.
            val turned = (box?.d ?: 0f) > (box?.w ?: 0f)
            val long = max(box?.w ?: 1f, box?.d ?: 1f).coerceAtLeast(1e-4f)
            val scale = BENCH_WIDTH / long
            val top = (box?.cy ?: 0f) + (box?.h ?: 0f) / 2f
            val holder = fitted(bench, scale, Vector3(0f, BENCH_Y - top * scale, 0f))
            bench.components[TransformComponent::class.java]?.setPosition(Vector3(-(box?.cx ?: 0f), 0f, -(box?.cz ?: 0f)))
            if (turned) holder.components[TransformComponent::class.java]?.setQuaternion(SceneLabels.axisAngle(0f, 1f, 0f, (PI / 2).toFloat()))
            layer.addChild(holder)
        }
        experiment.props.forEachIndexed { i, prop ->
            val (model, box) = models["${prop.model}#$i"] ?: return@forEachIndexed
            val scale = prop.height / (box?.h ?: 1f).coerceAtLeast(1e-4f)
            val bottom = (box?.cy ?: 0f) - (box?.h ?: 0f) / 2f
            if (prop.glass) ThingModels.glass(model, 0.24f)
            layer.addChild(fitted(model, scale, Vector3(prop.x - (box?.cx ?: 0f) * scale, BENCH_Y - bottom * scale, prop.z - (box?.cz ?: 0f) * scale)))
        }
        stage.addChild(layer)
        propsLayer = layer
        propsFor = experiment.id
    }

    private fun fitted(model: Entity, scale: Float, position: Vector3): Entity {
        val holder = Entity()
        holder.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(scale))
            setPosition(position)
        }
        holder.addChild(model)
        return holder
    }

    /** Solids of the stage before the step just taken, or null when the scene was not reached by a step. */
    private var movedFrom: List<LabEffect.Solid>? = null

    fun render(newSpec: LabSceneSpec) {
        if (newSpec == spec) return
        val previous = spec
        spec = newSpec
        if (previous?.experiment?.id != newSpec.experiment.id) {
            propsLayer?.destroy(true)
            propsLayer = null
            propsFor = null
            lastLiquid = null
        }
        effectsLayer?.destroy(true)
        emitters.clear()
        tickers.clear()
        planeMesh = null
        sphereMesh = null
        boxMesh = null
        cylinderMesh = null
        textures.clear()
        val layer = Entity()
        layer.components[TransformComponent::class.java]?.setPosition(Vector3(0f, if (embedded) 0f else BENCH_Y, 0f))
        val experiment = newSpec.experiment
        val effects = newSpec.override ?: if (newSpec.stage == 0) experiment.start else experiment.steps.getOrNull(newSpec.stage - 1)?.effects.orEmpty()
        // v1.13.0: when a step has just been done, things move: a wire is lowered, a lump is dropped in.
        val stepped = previous != null && previous.experiment.id == experiment.id && newSpec.stage == previous.stage + 1
        movedFrom =
            if (!stepped) null
            else (if (newSpec.stage == 1) experiment.start else experiment.steps[newSpec.stage - 2].effects).filterIsInstance<LabEffect.Solid>()
        // Picking another choice puts a new thing in: nothing was there a moment ago, so whatever is marked as dropped falls in.
        if (previous != null && previous.experiment.id == experiment.id && previous.stage == newSpec.stage && previous.override != newSpec.override && newSpec.override != null) movedFrom = emptyList()
        effects.forEach { build(layer, it, newSpec) }
        stage.addChild(layer)
        effectsLayer = layer
    }

    fun destroy() {
        emitters.clear()
        tickers.clear()
        root.destroy(true)
    }

    private fun tick(dt: Float) {
        tickers.forEach { it(dt) }
        emitters.forEach { it.step(dt, billboard) }
    }

    // ---- effects ---------------------------------------------------------------------------

    private fun build(layer: Entity, effect: LabEffect, spec: LabSceneSpec) {
        when (effect) {
            is LabEffect.Flame -> {
                val color = spec.flameTint ?: effect.color
                val size = effect.size * spec.power
                // v1.11.0: additive sprites vanish against a bright wall (a blue sulfur flame most of all), so an
                // alpha body in the flame's colour goes underneath; it is what you see in a bright room.
                emit(layer, Sprite.SOFT, BlendingMode.TRANSPARENT, LabColor(color.r, color.g, color.b, 0.5f), max = 40, rate = 36f, life = 0.5f, size0 = 0.1f * size, size1 = 0.015f) { p ->
                    p.p = effect.at + Vec3(rnd(0.01f), rnd(0.006f), rnd(0.01f))
                    p.v = Vec3(rnd(0.02f), 0.3f * size + rnd(0.04f), rnd(0.02f))
                }
                // Outer body in the flame's colour, a hotter pale core, and light on the bench.
                emit(layer, Sprite.SOFT, BlendingMode.ADD, color.dim(0.8f), max = 60, rate = 52f, life = 0.62f, size0 = 0.15f * size, size1 = 0.02f) { p ->
                    p.p = effect.at + Vec3(rnd(0.014f), rnd(0.008f), rnd(0.014f))
                    p.v = Vec3(rnd(0.03f), 0.34f * size + rnd(0.05f), rnd(0.03f))
                }
                emit(layer, Sprite.SOFT, BlendingMode.ADD, color.towardsWhite(0.6f).dim(0.75f), max = 30, rate = 34f, life = 0.4f, size0 = 0.085f * size, size1 = 0.015f) { p ->
                    p.p = effect.at + Vec3(rnd(0.008f), 0f, rnd(0.008f))
                    p.v = Vec3(rnd(0.015f), 0.26f * size, rnd(0.015f))
                }
                light(layer, effect.at + Vec3(0f, 0.08f, 0f), color, 700f * effect.light * spec.power, flicker = true)
            }
            is LabEffect.Sparks ->
                emit(layer, Sprite.SOFT, BlendingMode.ADD, effect.color, max = 130, rate = effect.rate * 1.6f, life = 0.8f, size0 = 0.07f, size1 = 0.012f, gravity = -1.1f) { p ->
                    val a = random.nextFloat() * 2f * PI.toFloat()
                    val up = random.nextFloat() * 0.9f - 0.15f
                    val s = effect.speed * (0.5f + random.nextFloat() * 0.7f)
                    p.p = effect.at
                    p.v = Vec3(cos(a) * s, up * s + 0.15f, sin(a) * s)
                }
            is LabEffect.Smoke ->
                // Alpha-blended white puffs. The texture is straight (not premultiplied) white, or the edges go grey.
                emit(layer, Sprite.SOFT, BlendingMode.TRANSPARENT, LabColor(effect.color.r, effect.color.g, effect.color.b, effect.color.a * 0.7f), max = 30, rate = effect.rate, life = 2.4f, size0 = 0.07f, size1 = 0.26f) { p ->
                    p.p = effect.at + Vec3(rnd(0.02f), 0f, rnd(0.02f))
                    p.v = Vec3(rnd(0.03f), 0.13f + rnd(0.03f), rnd(0.03f))
                }
            is LabEffect.Glow -> {
                // A wide halo and a hot white core: one additive sprite alone is too faint in a lit room.
                val glow = sprite(Sprite.SOFT, BlendingMode.ADD, effect.color)
                val core = sprite(Sprite.SOFT, BlendingMode.ADD, effect.color.towardsWhite(0.7f))
                layer.addChild(glow)
                layer.addChild(core)
                var t = 0f
                tickers += { dt ->
                    t += dt
                    val pulse = effect.size * (0.9f + 0.12f * sin(t * 31f) + 0.06f * sin(t * 53f))
                    glow.components[TransformComponent::class.java]?.apply {
                        setPosition(effect.at.v3())
                        setScaleVector(Vector3(pulse * 1.5f))
                        setQuaternion(billboard)
                    }
                    core.components[TransformComponent::class.java]?.apply {
                        setPosition(effect.at.v3())
                        setScaleVector(Vector3(pulse * 0.6f))
                        setQuaternion(billboard)
                    }
                }
                light(layer, effect.at, effect.color, 900f * effect.light, flicker = true)
            }
            is LabEffect.Bubbles ->
                emit(layer, Sprite.RING, BlendingMode.TRANSPARENT, LabColor(1f, 1f, 1f, 0.85f), max = 140, rate = effect.rate, life = max(0.2f, (effect.top - effect.at.y) / 0.16f), size0 = 0.014f, size1 = 0.02f) { p ->
                    val a = random.nextFloat() * 2f * PI.toFloat()
                    val r = effect.spread * kotlin.math.sqrt(random.nextFloat())
                    p.p = effect.at + Vec3(cos(a) * r, 0f, sin(a) * r)
                    p.v = Vec3(rnd(0.025f), 0.13f + random.nextFloat() * 0.07f, rnd(0.025f))
                }
            is LabEffect.Flakes ->
                emit(layer, Sprite.SOFT, BlendingMode.TRANSPARENT, effect.color, max = 80, rate = effect.rate, life = max(0.5f, (effect.at.y - effect.bottom) / 0.035f), size0 = 0.02f, size1 = 0.034f) { p ->
                    val a = random.nextFloat() * 2f * PI.toFloat()
                    val r = effect.spread * kotlin.math.sqrt(random.nextFloat())
                    p.p = effect.at + Vec3(cos(a) * r, rnd(0.02f), sin(a) * r)
                    p.v = Vec3(rnd(0.006f), -0.035f, rnd(0.006f))
                }
            is LabEffect.Liquid -> {
                val from = lastLiquid?.takeIf { effect.seconds > 0f } ?: effect.color
                val material = PhysicallyBasedMaterial.create().apply {
                    setBlendingMode(BlendingMode.TRANSPARENT)
                    setRoughness(0.08f)
                    setMetallic(0f)
                }
                fun paint(c: LabColor) {
                    material.setBaseColor(Color4(c.r, c.g, c.b, c.a))
                    material.setOpacity(c.a)
                }
                paint(from)
                val column = ModelEntity(cylinderMesh ?: MeshResource.createCylinder(1f, 1f).also { cylinderMesh = it }, material) // gate:allow geometry a liquid filling a round beaker is a plain column; the beaker itself is a Tripo model. gate:allow primitive-hero same reason
                column.components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(effect.at.x, effect.at.y + effect.height / 2f, effect.at.z))
                    setScaleVector(Vector3(effect.radius, effect.height, effect.radius))
                }
                layer.addChild(column)
                if (effect.seconds > 0f) {
                    var t = 0f
                    tickers += { dt ->
                        if (t < effect.seconds) {
                            t = min(effect.seconds, t + dt)
                            paint(from.mix(effect.color, t / effect.seconds))
                        }
                    }
                }
                lastLiquid = effect.color
            }
            is LabEffect.Solid -> {
                val entity = solid(effect.at, effect.size, effect.color, effect.round, effect.metal, effect.glowing)
                layer.addChild(entity)
                val before = movedFrom
                if (before != null && effect !in before) {
                    // The same piece somewhere else a moment ago (a wire or splint being lowered): glide. A piece
                    // marked as dropped in: fall from above the container. Anything else simply appears.
                    val same = before.firstOrNull { it.color == effect.color && it.round == effect.round && it.metal == effect.metal && it.size.x == effect.size.x && it.size.z == effect.size.z }
                    val from = same?.at ?: if (effect.drops) effect.at + Vec3(0f, 0.42f, 0f) else null
                    if (from != null) {
                        val seconds = if (same != null) 0.7f else 0.5f
                        var t = 0f
                        val transform = entity.components[TransformComponent::class.java]
                        transform?.setPosition(from.v3())
                        tickers += { dt ->
                            if (t < seconds) {
                                t = min(seconds, t + dt)
                                val u = t / seconds
                                // Lowered by hand: ease in and out. Dropped: speeds up like a falling thing.
                                val k = if (same != null) u * u * (3f - 2f * u) else u * u
                                transform?.setPosition((from + (effect.at - from) * k).v3())
                            }
                        }
                    }
                }
            }
            is LabEffect.Skater -> {
                val pellet = solid(effect.at, Vec3(effect.size, effect.size, effect.size), effect.color, round = true, metal = true, glowing = false)
                layer.addChild(pellet)
                var angle = 0f
                var turn = 2.6f
                var wobble = 0f
                tickers += { dt ->
                    wobble += dt
                    if (random.nextFloat() < dt * 1.5f) turn = (1.5f + random.nextFloat() * 3f) * if (random.nextBoolean()) 1f else -1f
                    angle += turn * dt
                    val r = effect.radius * (0.55f + 0.4f * sin(wobble * 1.7f))
                    pellet.components[TransformComponent::class.java]?.setPosition(Vector3(effect.at.x + cos(angle) * r, effect.at.y, effect.at.z + sin(angle) * r))
                }
            }
            is LabEffect.Crystals -> {
                // Small bright grains up the wire, appearing from the bottom as time passes.
                val grains = (0 until 46).map { k ->
                    val h = effect.height * (k + 1) / 46f
                    val a = k * 2.4f
                    val out = 0.012f + 0.03f * ((k * 37) % 10) / 10f
                    val size = 0.012f + 0.012f * ((k * 13) % 7) / 7f
                    solid(effect.at + Vec3(cos(a) * out, h, sin(a) * out), Vec3(size, size, size), effect.color, round = true, metal = true, glowing = false) to size
                }
                grains.forEach { (g, _) ->
                    g.components[TransformComponent::class.java]?.setScaleVector(Vector3(if (effect.seconds > 0f) 0f else 1f))
                    layer.addChild(g)
                }
                if (effect.seconds > 0f) {
                    var t = 0f
                    tickers += { dt ->
                        if (t < effect.seconds) {
                            t = min(effect.seconds, t + dt)
                            grains.forEachIndexed { k, (g, size) ->
                                val appear = ((t / effect.seconds) * grains.size - k).coerceIn(0f, 1f)
                                g.components[TransformComponent::class.java]?.setScaleVector(Vector3(size * appear))
                            }
                        }
                    }
                } else {
                    grains.forEach { (g, size) -> g.components[TransformComponent::class.java]?.setScaleVector(Vector3(size)) }
                }
            }
        }
    }

    private fun solid(at: Vec3, size: Vec3, color: LabColor, round: Boolean, metal: Boolean, glowing: Boolean): Entity {
        val material: Material =
            if (glowing) UnlitMaterial.create().apply { setBaseColor(Color4(color.r, color.g, color.b, 1f)) }
            else PhysicallyBasedMaterial.create().apply {
                setBaseColor(Color4(color.r, color.g, color.b, 1f))
                setRoughness(if (metal) 0.28f else 0.75f)
                setMetallic(if (metal) 0.9f else 0f)
                setEmissiveColor(Color4(color.r * 0.08f, color.g * 0.08f, color.b * 0.08f, 1f))
            }
        // Reagents and products (a metal strip, a pellet, a lump of powder) are plain shapes; the equipment is Tripo models.
        val ball = { sphereMesh ?: MeshResource.createSphere(0.5f).also { sphereMesh = it } } // gate:allow geometry a pellet, crystal grain or lump of a reagent / product; all equipment is Tripo models. gate:allow primitive-hero same reason
        val slab = { boxMesh ?: MeshResource.createBox(Vector3(1f, 1f, 1f)).also { boxMesh = it } } // gate:allow geometry a metal strip, wire or splint of a reagent; all equipment is Tripo models. gate:allow primitive-hero same reason
        // A flat round patch is a powder (MnO₂ in the beaker): a low heap of grains, not a disc (v1.16.1).
        if (round && size.y < size.x * 0.12f) {
            val heap = Entity()
            heap.components[TransformComponent::class.java]?.setPosition(at.v3())
            val grains = java.util.Random(7)
            repeat(150) {
                val r = size.x * 0.42f * grains.nextFloat() * grains.nextFloat()
                val angle = grains.nextFloat() * 6.2832f
                val grain = size.x * (0.02f + 0.022f * grains.nextFloat())
                // Higher in the middle, so it reads as a small pile.
                val lift = (1f - r / (size.x * 0.5f)) * size.x * 0.07f * grains.nextFloat()
                heap.addChild(ModelEntity(ball(), material).apply {
                    components[TransformComponent::class.java]?.apply {
                        setPosition(Vector3(r * kotlin.math.cos(angle), lift, r * kotlin.math.sin(angle) * size.z / size.x))
                        setScaleVector(Vector3(grain, grain * 0.8f, grain))
                    }
                })
            }
            return heap
        }
        // A tall piece with a square section is a rod: a candle, a splint, a wire (v1.16.1; it was a square bar).
        val rod = !round && size.x == size.z && size.y >= size.x * 2.5f
        val stick = { cylinderMesh ?: MeshResource.createCylinder(1f, 1f).also { cylinderMesh = it } } // gate:allow geometry a candle, splint or wire of a reagent; all equipment is Tripo models. gate:allow primitive-hero same reason
        val mesh = if (round) ball() else if (rod) stick() else slab()
        val entity = ModelEntity(mesh, material)
        if (rod) {
            entity.components[TransformComponent::class.java]?.apply {
                setPosition(at.v3())
                setScaleVector(Vector3(size.x * 0.5f, size.y, size.z * 0.5f))
            }
            return entity
        }
        entity.components[TransformComponent::class.java]?.apply {
            setPosition(at.v3())
            setScaleVector(Vector3(size.x, size.y, size.z))
        }
        return entity
    }

    private fun light(layer: Entity, at: Vec3, color: LabColor, intensity: Float, flicker: Boolean) {
        val lamp = Entity()
        lamp.components[TransformComponent::class.java]?.setPosition(at.v3())
        val component = PointLightComponent(Color4(color.r, color.g, color.b, 1f), intensity, 1.6f)
        lamp.components.set(component)
        layer.addChild(lamp)
        if (flicker) {
            var t = random.nextFloat() * 10f
            tickers += { dt ->
                t += dt
                lamp.components[PointLightComponent::class.java]?.intensity = intensity * (0.86f + 0.1f * sin(t * 23f) + 0.06f * sin(t * 41f))
            }
        }
    }

    // ---- sprite particles --------------------------------------------------------------------

    /** SOFT fades through alpha (for alpha blending); GLOW fades to black on an opaque texture, because additive blending ignores alpha. */
    private enum class Sprite { SOFT, GLOW, RING }

    private class Particle(val entity: Entity) {
        var p = Vec3.ZERO
        var v = Vec3.ZERO
        var age = 0f
        var alive = false
    }

    private class Emitter(
        val pool: List<Particle>,
        val rate: Float,
        val life: Float,
        val size0: Float,
        val size1: Float,
        val gravity: Float,
        val spawn: (Particle) -> Unit,
    ) {
        private var owed = 0f

        fun step(dt: Float, billboard: Quat) {
            owed += rate * dt
            for (p in pool) {
                if (!p.alive) {
                    if (owed >= 1f) {
                        owed -= 1f
                        p.alive = true
                        p.age = 0f
                        spawn(p)
                    } else continue
                }
                p.age += dt
                if (p.age >= life) {
                    p.alive = false
                    p.entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(0f))
                    continue
                }
                p.v = Vec3(p.v.x, p.v.y + gravity * dt, p.v.z)
                p.p = p.p + p.v * dt
                val u = p.age / life
                // Grow in fast, then run to the end size; an additive sprite fades by shrinking.
                val size = (size0 + (size1 - size0) * u) * min(1f, u * 8f)
                p.entity.components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(p.p.x, p.p.y, p.p.z))
                    setScaleVector(Vector3(size))
                    setQuaternion(billboard)
                }
            }
            if (owed > 3f) owed = 3f
        }
    }

    private fun emit(
        layer: Entity, sprite: Sprite, blending: BlendingMode, color: LabColor, max: Int, rate: Float, life: Float,
        size0: Float, size1: Float, gravity: Float = 0f, spawn: (Particle) -> Unit,
    ) {
        val material = spriteMaterial(sprite, blending, color)
        val mesh = planeMesh ?: MeshResource.createPlane(1f, 1f, 0f).also { planeMesh = it }
        val pool = List(max) {
            val e = ModelEntity(mesh, material)
            e.components[TransformComponent::class.java]?.setScaleVector(Vector3(0f))
            layer.addChild(e)
            Particle(e)
        }
        emitters += Emitter(pool, rate, life, size0, size1, gravity, spawn)
    }

    private fun sprite(sprite: Sprite, blending: BlendingMode, color: LabColor): Entity =
        ModelEntity(planeMesh ?: MeshResource.createPlane(1f, 1f, 0f).also { planeMesh = it }, spriteMaterial(sprite, blending, color))

    private fun spriteMaterial(sprite: Sprite, blending: BlendingMode, color: LabColor): UnlitMaterial =
        UnlitMaterial.create(blending).apply {
            // Additive sprites are always opaque: a half-transparent additive material darkened what was behind it.
            setBaseColor(Color4(color.r, color.g, color.b, if (blending == BlendingMode.ADD) 1f else color.a))
            val kind = if (blending == BlendingMode.ADD && sprite == Sprite.SOFT) Sprite.GLOW else sprite
            setBaseColorTexture(textures.getOrPut(kind) { TextureResource(bitmap(kind)) })
        }

    private fun rnd(range: Float): Float = (random.nextFloat() * 2f - 1f) * range

    private fun Vec3.v3(): Vector3 = Vector3(x, y, z)

    private fun LabColor.dim(k: Float) = LabColor(r * k, g * k, b * k, a)

    private fun LabColor.towardsWhite(k: Float) = LabColor(r + (1f - r) * k, g + (1f - g) * k, b + (1f - b) * k, a)

    private fun LabColor.mix(to: LabColor, t: Float) = LabColor(r + (to.r - r) * t, g + (to.g - g) * t, b + (to.b - b) * t, a + (to.a - a) * t)

    companion object {
        /** Height of the bench top in the Volume, and how wide the bench is drawn (scene units). */
        const val BENCH_Y = -0.42f
        const val BENCH_WIDTH = 1.2f
        const val STAGE_SCALE = 1.5f
        private var registered = false

        private fun ensureRegistered() {
            if (registered) return
            registerComponent<LabTickComponent>()
            registerSystem<LabTickSystem>()
            registered = true
        }

        /** The additive glow sprite, for other scenes that need one (the lit bulb). */
        fun glowBitmap(): Bitmap = bitmap(Sprite.GLOW)

        /** White sprites, tinted by the material: a soft round glow, or a thin ring for a bubble. */
        private fun bitmap(sprite: Sprite): Bitmap {
            val n = 64
            val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            when (sprite) {
                Sprite.SOFT -> {
                    // Written pixel by pixel as straight alpha: white everywhere, only alpha falls off.
                    bitmap.isPremultiplied = false
                    val pixels = IntArray(n * n)
                    for (y in 0 until n) for (x in 0 until n) {
                        val d = kotlin.math.hypot(x - (n - 1) / 2f, y - (n - 1) / 2f) / (n / 2f)
                        val alpha = ((1f - d).coerceIn(0f, 1f).let { it * it } * 255f).toInt()
                        pixels[y * n + x] = (alpha shl 24) or 0x00FFFFFF
                    }
                    bitmap.setPixels(pixels, 0, n, 0, 0, n, n)
                }
                Sprite.GLOW -> {
                    canvas.drawColor(0xFF000000.toInt())
                    paint.shader = RadialGradient(n / 2f, n / 2f, n / 2f, intArrayOf(0xFFFFFFFF.toInt(), 0xFF8C8C8C.toInt(), 0xFF000000.toInt()), floatArrayOf(0f, 0.38f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawCircle(n / 2f, n / 2f, n / 2f, paint)
                }
                Sprite.RING -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f
                    paint.color = 0xE6FFFFFF.toInt()
                    canvas.drawCircle(n / 2f, n / 2f, n / 2f - 5f, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = 0x30FFFFFF
                    canvas.drawCircle(n / 2f, n / 2f, n / 2f - 5f, paint)
                    paint.color = 0xCCFFFFFF.toInt()
                    canvas.drawCircle(n * 0.36f, n * 0.34f, 5f, paint)
                }
            }
            return bitmap
        }
    }
}
