package com.example.elementspace.p7c3a91e4.ui.atom.components

import androidx.compose.ui.graphics.Color
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.Lattice
import com.example.elementspace.p7c3a91e4.domain.model.NucleusLayout
import com.example.elementspace.p7c3a91e4.domain.model.OrbitalCloud
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
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
)

/** Spins an entity about its local Y axis after a fixed tilt; driven by [OrbitSystem]. */
class OrbitComponent(val speed: Float = 0f, val tiltX: Float = 0f, val tiltZ: Float = 0f) : Component() {
    var angle: Float = 0f
    var paused: Boolean = false

    fun rotation(): Quat = axisAngle(1f, 0f, 0f, tiltX) * axisAngle(0f, 0f, 1f, tiltZ) * axisAngle(0f, 1f, 0f, angle)

    override fun clone(): Component = OrbitComponent(speed, tiltX, tiltZ)
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
                collisionShape = listOf(ShapeResource.createSphere(1f)),
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
    }

    fun setPaused(value: Boolean) {
        paused = value
        orbits.forEach { it.paused = value }
    }

    fun render(newSpec: AtomSceneSpec) {
        if (newSpec == spec) return
        spec = newSpec
        content?.destroy(true)
        orbits.clear()
        sphereMesh = null
        cubeMesh = null
        cylinderMesh = null
        val next = Entity()
        when (newSpec.view) {
            AtomViewMode.SHELL -> buildShells(next, newSpec)
            AtomViewMode.NUCLEUS -> newSpec.nucleus?.let { next.addChild(nucleus(it, 0.62f)) }
            AtomViewMode.CLOUD -> buildCloud(next, newSpec)
            AtomViewMode.ALLOTROPE -> newSpec.lattice?.let { buildLattice(next, it) }
        }
        root.addChild(next)
        content = next
        setPaused(paused)
    }

    fun destroy() {
        orbits.clear()
        root.destroy(true)
    }

    // ---- builders -------------------------------------------------------------------------

    private fun buildShells(parent: Entity, spec: AtomSceneSpec) {
        spec.nucleus?.let { parent.addChild(nucleus(it, 0.14f)) }
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

    /** Rotation taking the +Y axis onto the unit vector [direction]. */
    private fun alignYTo(direction: Vec3): Quat {
        val axisX = direction.z
        val axisZ = -direction.x
        val axisLength = sqrt(axisX * axisX + axisZ * axisZ)
        if (axisLength < 1e-5f) return if (direction.y > 0f) Quat.identity() else axisAngle(1f, 0f, 0f, PI.toFloat())
        return axisAngle(axisX / axisLength, 0f, axisZ / axisLength, acos(direction.y.coerceIn(-1f, 1f)))
    }

    private companion object {
        var registered = false

        fun ensureRegistered() {
            if (registered) return
            registerComponent<OrbitComponent>()
            registerSystem<OrbitSystem>()
            registered = true
        }
    }
}
