package com.example.elementspace.p7c3a91e4.ui.atom.components

import com.example.elementspace.p7c3a91e4.ui.common.TOUCH_RADIUS
import androidx.compose.ui.graphics.Color
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.Lattice
import com.example.elementspace.p7c3a91e4.domain.model.NucleusLayout
import com.example.elementspace.p7c3a91e4.domain.model.OrbitalCloud
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.domain.usecase.NodalSurface
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalSample
import com.example.elementspace.p7c3a91e4.ui.common.ModelBox
import com.example.elementspace.p7c3a91e4.ui.lab.components.LabScene
import com.pico.spatial.core.ecs.PointLightComponent
import com.pico.spatial.core.ecs.resource.TextureResource
import com.example.elementspace.p7c3a91e4.ui.common.ThingModels
import com.example.elementspace.p7c3a91e4.ui.common.SceneLabels
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshInstancesResource
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.dsl.registerComponent
import com.pico.spatial.ui.foundation.dsl.registerSystem
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Everything the 3D view needs, in scene units where the whole model fits radius 1. */
data class AtomSceneSpec(
    val view: AtomViewMode,
    val shells: List<Int>,
    val nucleus: NucleusLayout?,
    val clouds: List<OrbitalCloud>,
    val hiddenSubshells: Set<String>,
    val lattice: Lattice?,
    /** Cloud view showing one orbital instead of the whole cloud. */
    val orbital: OrbitalView? = null,
    /** Net charge of the atom (v1.6.0): a tinted halo and the ion's symbol in the shell view. */
    val charge: Int = 0,
    val ionLabel: String? = null,
    /** Everyday thing shown instead of the atom (v1.8.0): model id in assets/models/things, and whether it floats. */
    val thing: String? = null,
    val thingFloats: Boolean = false,
    val thingGlass: Boolean = false,
    /** The thing gives off light (a lit bulb): a warm glow and a point light around it. */
    val thingGlows: Boolean = false,
)

/** One orbital in the cloud view: sampled points, where ψ = 0, and what to draw. */
data class OrbitalView(
    val sample: OrbitalSample,
    val nodes: List<NodalSurface>,
    val name: String,
    val phase: Boolean,
    val showNodes: Boolean,
    val axes: Boolean,
)

/** Spins an entity about its local Y axis after a fixed tilt; driven by [OrbitSystem]. */
class OrbitComponent(val speed: Float = 0f, val tiltX: Float = 0f, val tiltZ: Float = 0f) : Component() {
    var angle: Float = 0f
    var paused: Boolean = false

    fun rotation(): Quat = axisAngle(1f, 0f, 0f, tiltX) * axisAngle(0f, 0f, 1f, tiltZ) * axisAngle(0f, 1f, 0f, angle)

    override fun clone(): Component = OrbitComponent(speed, tiltX, tiltZ)
}

/** Moves an entity gently up and down (a balloon on its string). */
class BobComponent(val amplitude: Float = 0f, val speed: Float = 1f) : Component() {
    var time: Float = 0f

    override fun clone(): Component = BobComponent(amplitude, speed)
}

/** An electron leaving or joining the atom: moves from [from] to [to] once, shrinking away if it [leaves]. */
class FlyComponent(val from: Vec3 = Vec3.ZERO, val to: Vec3 = Vec3.ZERO, val duration: Float = 1.6f, val size: Float = 0.06f, val leaves: Boolean = true) : Component() {
    var time: Float = 0f

    override fun clone(): Component = FlyComponent(from, to, duration, size, leaves)
}

/** Advances every [OrbitComponent] each frame, so motion never depends on recomposition. */
class OrbitSystem : System() {
    override fun update(context: SceneUpdateContext) {
        super.update(context)
        val entities = context.scene.queryEntity(EntityQueryCondition.hasComponent(OrbitComponent::class.java))
        for (entity in entities) {
            val orbit = entity.components[OrbitComponent::class.java] ?: continue
            if (orbit.paused) continue
            orbit.angle = (orbit.angle + orbit.speed * context.deltaTime) % TWO_PI
            entity.components[TransformComponent::class.java]?.setQuaternion(orbit.rotation())
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(FlyComponent::class.java))) {
            val fly = entity.components[FlyComponent::class.java] ?: continue
            if (fly.time >= fly.duration) continue
            fly.time = (fly.time + context.deltaTime).coerceAtMost(fly.duration)
            val u = fly.time / fly.duration
            val t = u * u * (3f - 2f * u)
            val p = fly.from + (fly.to - fly.from) * t
            entity.components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(p.x, p.y + 0.25f * sin(PI.toFloat() * t), p.z))
                // Leaving: fades out by shrinking. Arriving: disappears into the shell at the end.
                setScaleVector(Vector3(if (u >= 1f) 0f else if (fly.leaves) fly.size * (1f - t * 0.6f) else fly.size))
            }
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(BobComponent::class.java))) {
            val bob = entity.components[BobComponent::class.java] ?: continue
            bob.time += context.deltaTime
            entity.components[TransformComponent::class.java]?.setPosition(Vector3(0f, bob.amplitude * sin(bob.time * bob.speed), 0f))
        }
    }
}

private const val TWO_PI = (2.0 * PI).toFloat()

private fun axisAngle(x: Float, y: Float, z: Float, radians: Float): Quat {
    val s = sin(radians / 2f)
    return Quat(x * s, y * s, z * s, cos(radians / 2f))
}

private fun Color.toColor4(alpha: Float = 1f): Color4 = Color4(red, green, blue, alpha)

private fun Vec3.toVector3(): Vector3 = Vector3(x, y, z)

/**
 * Owns the entity tree of one atom Volume.
 *
 * [root] carries the user's rotation and zoom and is the single interaction target; its children
 * are rebuilt whenever the [AtomSceneSpec] changes. Repeated particles are drawn with instanced
 * meshes so even the heaviest atom is a handful of draw calls.
 */
class AtomScene {
    val root: Entity = Entity()

    private var content: Entity? = null
    private var spec: AtomSceneSpec? = null
    private var paused: Boolean = false
    private val orbits = ArrayList<OrbitComponent>()
    private val labels = SceneLabels()
    private var thingHolder: Entity? = null
    private var thingGlow: Entity? = null
    private var billboard: Quat = Quat.identity()
    private var pendingThing: Triple<String, Entity, ModelBox?>? = null
    private var planeMesh: MeshResource? = null

    // Meshes are created per rebuild, never kept across rebuilds: destroying the previous content
    // releases the meshes it used, and building with a released mesh throws.
    private var sphereMesh: MeshResource? = null
    private var cubeMesh: MeshResource? = null
    private var cylinderMesh: MeshResource? = null

    private fun sphere(): MeshResource = sphereMesh ?: MeshResource.createSphere(1f).also { sphereMesh = it }

    private fun cube(): MeshResource = cubeMesh ?: MeshResource.createBox(Vector3(1f, 1f, 1f)).also { cubeMesh = it }

    private fun cylinder(): MeshResource = cylinderMesh ?: MeshResource.createCylinder(1f, 1f).also { cylinderMesh = it }

    init {
        ensureRegistered()
        root.components.set(InteractableComponent())
        root.components.set(
            CollisionComponent(
                // Radius 0.7, not 1: the side card starts 504 dp from the centre of the 1600 dp Volume (x = 0.8 + 24 dp)
                // and a radius-1 sphere (672 dp) sat in front of its left third, so taps there hit the model (v1.4.0 headset).
                collisionShape = listOf(ShapeResource.createSphere(TOUCH_RADIUS).also { it.toGlobal() }),
                physicsMaterial = PhysicsMaterialResource(),
            )
        )
    }

    /** Applies the user's orientation and zoom. [radiusMeters] maps scene radius 1 to metres. */
    fun setPose(radiusMeters: Float, yaw: Float, pitch: Float, zoom: Float) {
        root.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(radiusMeters * zoom))
            setQuaternion(axisAngle(1f, 0f, 0f, pitch) * axisAngle(0f, 1f, 0f, yaw))
        }
        labels.setRotation(yaw, pitch)
        billboard = SceneLabels.inverse(yaw, pitch)
        thingGlow?.components?.get(TransformComponent::class.java)?.setQuaternion(billboard)
    }

    fun setPaused(value: Boolean) {
        paused = value
        orbits.forEach { it.paused = value }
    }

    fun render(newSpec: AtomSceneSpec) {
        if (newSpec == spec) return
        val before = spec
        // The thing view depends on nothing else in the spec; rebuilding would throw its model away.
        if (newSpec.thing != null && newSpec.thing == spec?.thing) {
            spec = newSpec
            return
        }
        spec = newSpec
        content?.destroy(true)
        orbits.clear()
        sphereMesh = null
        cubeMesh = null
        cylinderMesh = null
        planeMesh = null
        labels.reset()
        val next = Entity()
        thingHolder = null
        thingGlow = null
        if (newSpec.thing != null) {
            // The model arrives from ThingModels (async); the holder turns it slowly so every side is seen.
            val holder = orbiting(speed = 0.35f, tiltX = 0f, tiltZ = 0f)
            next.addChild(holder)
            thingHolder = holder
            root.addChild(next)
            content = next
            setPaused(paused)
            attachThing()
            return
        }
        when (newSpec.view) {
            AtomViewMode.SHELL -> buildShells(next, newSpec)
            AtomViewMode.NUCLEUS -> newSpec.nucleus?.let { next.addChild(nucleus(it, 0.62f)) }
            AtomViewMode.CLOUD -> newSpec.orbital?.let { buildOrbital(next, it) } ?: buildCloud(next, newSpec)
            AtomViewMode.ALLOTROPE -> newSpec.lattice?.let { buildLattice(next, it) }
        }
        // v1.9.2: when the charge changes, one gold electron visibly flies off, or flies in from outside.
        if (before != null && before.thing == null && before.view == AtomViewMode.SHELL && newSpec.view == AtomViewMode.SHELL && before.charge != newSpec.charge) {
            val shells = if (newSpec.charge > before.charge) before.shells else newSpec.shells
            val count = shells.size
            val outer = if (count <= 1) 0.6f else 0.92f
            val edge = Vec3(outer * 0.7f, outer * 0.7f, 0f)
            val away = Vec3(0.55f, 1.15f, 0.2f)
            val removed = newSpec.charge > before.charge
            val electron = ModelEntity(sphere(), glow(ElementColors.ElectronTransferred))
            electron.components[TransformComponent::class.java]?.apply {
                setPosition((if (removed) edge else away).toVector3())
                setScaleVector(Vector3(0.06f))
            }
            electron.components.set(FlyComponent(if (removed) edge else away, if (removed) away else edge, leaves = removed))
            next.addChild(electron)
        }
        root.addChild(next)
        content = next
        setPaused(paused)
    }

    /** Hands over a fresh copy of the model for [id]; it is shown if that thing is still the one wanted. */
    fun showThing(id: String, model: Entity, box: ModelBox?) {
        pendingThing = Triple(id, model, box)
        attachThing()
    }

    private fun attachThing() {
        val holder = thingHolder ?: return
        val (id, model, box) = pendingThing ?: return
        if (spec?.thing != id) return
        pendingThing = null
        // Fit the largest side to 1.1 scene units and put the box centre on the origin.
        val scale = 1.1f / (box?.largest ?: 1f).coerceAtLeast(1e-4f)
        val fitted = Entity()
        fitted.components[TransformComponent::class.java]?.setScaleVector(Vector3(scale))
        model.components[TransformComponent::class.java]?.setPosition(Vector3(-(box?.cx ?: 0f), -(box?.cy ?: 0f), -(box?.cz ?: 0f)))
        if (spec?.thingGlass == true) ThingModels.glass(model)
        fitted.addChild(model)
        if (spec?.thingFloats == true) fitted.components.set(BobComponent(0.06f, 0.9f))
        holder.addChild(fitted)
        if (spec?.thingGlows == true) {
            val warm = Color4(1f, 0.82f, 0.5f, 1f)
            ThingModels.lit(fitted, Color4(0.9f, 0.62f, 0.25f, 1f))
            val halo = ModelEntity(MeshResource.createPlane(1f, 1f, 0f), UnlitMaterial.create(BlendingMode.ADD).apply { setBaseColor(warm); setBaseColorTexture(TextureResource(LabScene.glowBitmap())) })
            halo.components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(0f, 0.18f, 0f))
                setScaleVector(Vector3(2.6f))
                setQuaternion(billboard)
            }
            // Outside the turning holder so it keeps facing the viewer.
            content?.addChild(halo)
            thingGlow = halo
            val lamp = Entity()
            lamp.components[TransformComponent::class.java]?.setPosition(Vector3(0f, 0.2f, 0.3f))
            lamp.components.set(PointLightComponent(warm, 900f, 1.8f))
            content?.addChild(lamp)
        }
    }

    fun destroy() {
        orbits.clear()
        root.destroy(true)
    }

    // ---- builders -------------------------------------------------------------------------

    private fun buildShells(parent: Entity, spec: AtomSceneSpec) {
        spec.nucleus?.let { parent.addChild(nucleus(it, 0.14f)) }
        if (spec.charge != 0) {
            // Same colours as the ions of the reaction Volume: red = lost electrons, blue = gained.
            val tint = if (spec.charge > 0) ElementColors.IonPositive else ElementColors.IonNegative
            val halo = ModelEntity(sphere(), UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(tint.toColor4(0.10f)) })
            halo.components[TransformComponent::class.java]?.setScaleVector(Vector3(0.97f))
            parent.addChild(halo)
            spec.ionLabel?.let { parent.addChild(labels.label(it, Vec3(0f, -1.08f, 0f), height = 0.13f, lift = 0.1f, style = SceneLabels.Style.TAG)) }
        }
        val count = spec.shells.size
        spec.shells.forEachIndexed { index, electrons ->
            if (electrons == 0) return@forEachIndexed
            val radius = if (count == 1) 0.6f else 0.3f + 0.62f * index / (count - 1)
            val valence = index == count - 1
            val shell =
                orbiting(
                    speed = 0.9f / (index + 1),
                    tiltX = (0.35f + index * 0.9f) % 1.3f - 0.4f,
                    tiltZ = (index * 1.7f) % 1.2f - 0.6f,
                )
            // Dotted orbit so the path reads in 3D from any angle.
            shell.addChild(instanced("orbit_$index", cube(), unlit(Color.White, 0.75f), ring(radius, 120), 0.007f))
            // An electron has no size or colour of its own; it is drawn as a small metallic bead in
            // the electron colour, with a faint halo so it stays findable against a bright room.
            val electrons3d = ring(radius, electrons)
            shell.addChild(instanced("electron_halo_$index", sphere(), halo(ElementColors.ParticleElectron), electrons3d, if (valence) 0.036f else 0.028f))
            shell.addChild(instanced("electrons_$index", sphere(), bead(ElementColors.ParticleElectron), electrons3d, if (valence) 0.019f else 0.014f))
            parent.addChild(shell)
        }
    }

    private fun buildCloud(parent: Entity, spec: AtomSceneSpec) {
        val spin = orbiting(speed = 0.12f, tiltX = 0f, tiltZ = 0f)
        spin.addChild(instanced("nucleus_dot", sphere(), glow(Color.White), listOf(Vec3.ZERO), 0.018f))
        spec.clouds.forEach { cloud ->
            if (cloud.subshell.label in spec.hiddenSubshells || cloud.points.isEmpty()) return@forEach
            spin.addChild(
                instanced(
                    "cloud_${cloud.subshell.label}",
                    cube(),
                    unlit(ElementColors.orbital(cloud.subshell.l)),
                    cloud.points,
                    0.0075f,
                )
            )
        }
        parent.addChild(spin)
    }

    /**
     * One orbital, standing still so its axes and nodes stay put (the user turns it by hand).
     * Chemistry axes (z up) map to the scene as x → x, y → −z (away from the viewer), z → y.
     */
    private fun buildOrbital(parent: Entity, view: OrbitalView) {
        val holder = Entity()
        holder.components[TransformComponent::class.java]?.setScaleVector(Vector3(ORBITAL_SCALE))
        val sample = view.sample
        val positive = if (view.phase) ElementColors.PhasePositive else ElementColors.orbital(sample.ref.l)
        val negative = if (view.phase) ElementColors.PhaseNegative else ElementColors.orbital(sample.ref.l)
        if (sample.positive.isNotEmpty()) holder.addChild(instanced("psi_pos", cube(), unlit(positive), sample.positive.map(::scene), 0.0085f))
        if (sample.negative.isNotEmpty()) holder.addChild(instanced("psi_neg", cube(), unlit(negative), sample.negative.map(::scene), 0.0085f))
        holder.addChild(instanced("nucleus_dot", sphere(), glow(ElementColors.ParticleProton), listOf(Vec3.ZERO), 0.022f))
        if (view.showNodes) view.nodes.forEachIndexed { i, node -> nodal(holder, i, node, sample.extent) }
        if (view.axes) {
            val axes = listOf(Vec3(1f, 0f, 0f) to "x", Vec3(0f, 1f, 0f) to "y", Vec3(0f, 0f, 1f) to "z")
            val sticks =
                axes.mapIndexed { i, (axis, _) ->
                    MeshInstancesResource.Instance("axis_$i", Transform(Vector3(0f, 0f, 0f), alignYTo(scene(axis)), Vector3(0.0035f, 2.1f, 0.0035f)))
                }
            holder.addChild(instancedEntity("axes", cylinder(), unlit(ElementColors.OrbitalAxis, 0.9f), sticks))
            axes.forEach { (axis, name) -> holder.addChild(labels.label(name, scene(axis) * 1.12f, height = 0.09f, lift = 0.02f)) }
        }
        holder.addChild(labels.label(view.name, Vec3(0f, -1.12f, 0f), height = 0.11f, lift = 0.1f, style = SceneLabels.Style.TAG))
        parent.addChild(holder)
    }

    private fun nodal(holder: Entity, index: Int, node: NodalSurface, extent: Float) {
        val violet = ElementColors.NodalSurface
        when (node) {
            is NodalSurface.Plane -> {
                // A translucent disc, drawn from both sides.
                val mesh = planeMesh ?: MeshResource.createPlane(1.9f, 1.9f, 0.94f).also { planeMesh = it }
                val facing = alignZTo(scene(node.normal))
                listOf(facing, facing * axisAngle(1f, 0f, 0f, PI.toFloat())).forEach { q ->
                    val disc = ModelEntity(mesh, UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(violet.toColor4(0.16f)) })
                    disc.components[TransformComponent::class.java]?.setQuaternion(q)
                    holder.addChild(disc)
                }
            }
            is NodalSurface.Cone -> {
                // Rings of beads on both nappes; a mesh cone would hide the lobes inside it.
                val t = Math.toRadians(node.halfAngleDegrees.toDouble())
                val beads =
                    listOf(0.25f, 0.45f, 0.65f, 0.85f).flatMap { s ->
                        listOf(1f, -1f).flatMap { side ->
                            val h = (s * cos(t)).toFloat() * side
                            val r = (s * sin(t)).toFloat()
                            (0 until 48).map { k ->
                                val a = TWO_PI * k / 48
                                scene(Vec3(r * cos(a), r * sin(a), h))
                            }
                        }
                    }
                holder.addChild(instanced("cone_$index", sphere(), unlit(violet), beads, 0.006f))
            }
            is NodalSurface.Sphere -> {
                val shell = ModelEntity(sphere(), UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(violet.toColor4(0.14f)) })
                shell.components[TransformComponent::class.java]?.setScaleVector(Vector3(node.radius / extent))
                holder.addChild(shell)
            }
        }
    }

    private fun buildLattice(parent: Entity, lattice: Lattice) {
        val spin = orbiting(speed = 0.15f, tiltX = 0f, tiltZ = 0f)
        val atomRadius = if (lattice.atoms.size > 40) 0.06f else 0.09f
        spin.addChild(instanced("atoms", sphere(), lit(ElementColors.LatticeAtom), lattice.atoms, atomRadius))
        if (lattice.bonds.isNotEmpty()) {
            val bonds =
                lattice.bonds.mapIndexed { i, bond ->
                    val a = lattice.atoms[bond.from]
                    val b = lattice.atoms[bond.to]
                    val d = b - a
                    val length = d.length()
                    val mid = (a + b) * 0.5f
                    MeshInstancesResource.Instance(
                        "bond_$i",
                        Transform(mid.toVector3(), alignYTo(d * (1f / length)), Vector3(atomRadius * 0.28f, length, atomRadius * 0.28f)),
                    )
                }
            spin.addChild(instancedEntity("bonds", cylinder(), lit(ElementColors.LatticeBond), bonds))
        }
        parent.addChild(spin)
    }

    private fun nucleus(layout: NucleusLayout, scale: Float): Entity {
        val holder = Entity()
        holder.components[TransformComponent::class.java]?.setScaleVector(Vector3(scale))
        if (layout.protons.isNotEmpty()) {
            holder.addChild(instanced("protons", sphere(), lit(ElementColors.ParticleProton), layout.protons, layout.nucleonRadius))
        }
        if (layout.neutrons.isNotEmpty()) {
            holder.addChild(instanced("neutrons", sphere(), lit(ElementColors.ParticleNeutron), layout.neutrons, layout.nucleonRadius))
        }
        return holder
    }

    private fun orbiting(speed: Float, tiltX: Float, tiltZ: Float): Entity {
        val entity = Entity()
        val orbit = OrbitComponent(speed, tiltX, tiltZ)
        entity.components.set(orbit)
        entity.components[TransformComponent::class.java]?.setQuaternion(orbit.rotation())
        orbits.add(orbit)
        return entity
    }

    private fun ring(radius: Float, count: Int): List<Vec3> =
        (0 until count).map { i ->
            val a = TWO_PI * i / count
            Vec3(radius * cos(a), 0f, radius * sin(a))
        }

    private fun instanced(name: String, mesh: MeshResource, material: Material, points: List<Vec3>, scale: Float): Entity {
        val size = Vector3(scale)
        val identity = Quat.identity()
        val instances = points.mapIndexed { i, p -> MeshInstancesResource.Instance("${name}_$i", Transform(p.toVector3(), identity, size)) }
        return instancedEntity(name, mesh, material, instances)
    }

    private fun instancedEntity(name: String, mesh: MeshResource, material: Material, instances: List<MeshInstancesResource.Instance>): Entity {
        val entity = ModelEntity(mesh, material)
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create(name, instances)
        return entity
    }

    private fun unlit(color: Color, alpha: Float = 1f): Material =
        UnlitMaterial.create().apply { setBaseColor(color.toColor4(alpha)) }

    /** Soft translucent glow around a point; kept faint and small to limit overdraw. */
    private fun halo(color: Color): Material =
        UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(color.toColor4(0.28f)) }

    /**
     * Shaded (PBR) material lit by the room's environment lighting, with a trace of emission so it
     * stays readable in a dim room. Emission is kept low: more flattens the shading that makes the
     * nucleons read as a ball of spheres.
     */
    private fun lit(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.toColor4())
            setRoughness(0.4f)
            setMetallic(0f)
            setEmissiveColor(Color4(color.red * 0.1f, color.green * 0.1f, color.blue * 0.1f, 1f))
        }

    /**
     * Small metallic bead: a mostly reflective PBR surface with a trace of emission so the bead
     * keeps its colour in a dim room instead of going black.
     */
    private fun bead(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.toColor4())
            setRoughness(0.2f)
            setMetallic(0.9f)
            setEmissiveColor(Color4(color.red * 0.2f, color.green * 0.2f, color.blue * 0.2f, 1f))
        }

    /** Self-lit PBR point: a bright core that still picks up a highlight from the room. */
    private fun glow(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.toColor4())
            setRoughness(0.25f)
            setMetallic(0f)
            setEmissiveColor(color.toColor4())
        }

    /** Chemistry axes (z up) to scene axes (y up, viewer on +z). */
    private fun scene(p: Vec3): Vec3 = Vec3(p.x, p.z, -p.y)

    /** Rotation taking +Z (the plane's facing) onto the unit vector [n]. */
    private fun alignZTo(n: Vec3): Quat {
        val ax = -n.y
        val ay = n.x
        val length = sqrt(ax * ax + ay * ay)
        if (length < 1e-5f) return if (n.z > 0f) Quat.identity() else axisAngle(1f, 0f, 0f, PI.toFloat())
        return axisAngle(ax / length, ay / length, 0f, acos(n.z.coerceIn(-1f, 1f)))
    }

    /** Rotation taking the +Y axis onto the unit vector [direction]. */
    private fun alignYTo(direction: Vec3): Quat {
        val axisX = direction.z
        val axisZ = -direction.x
        val axisLength = sqrt(axisX * axisX + axisZ * axisZ)
        if (axisLength < 1e-5f) return if (direction.y > 0f) Quat.identity() else axisAngle(1f, 0f, 0f, PI.toFloat())
        return axisAngle(axisX / axisLength, 0f, axisZ / axisLength, acos(direction.y.coerceIn(-1f, 1f)))
    }

    private companion object {
        /** The orbital's 99 % radius fills a little less than the Volume's sphere. */
        const val ORBITAL_SCALE = 0.86f
        var registered = false

        fun ensureRegistered() {
            if (registered) return
            registerComponent<OrbitComponent>()
            registerComponent<BobComponent>()
            registerComponent<FlyComponent>()
            registerSystem<OrbitSystem>()
            registered = true
        }
    }
}
