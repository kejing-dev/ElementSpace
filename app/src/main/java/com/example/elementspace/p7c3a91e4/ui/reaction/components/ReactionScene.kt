package com.example.elementspace.p7c3a91e4.ui.reaction.components

import androidx.compose.ui.graphics.Color
import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.FrameParticle
import com.example.elementspace.p7c3a91e4.domain.model.ReactionFrame
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.domain.usecase.ReactionLayout
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
import kotlin.math.cos
import kotlin.math.sin

/** What the reaction Volume draws; the scene glides from the frame it drew last. */
data class ReactionSceneSpec(
    val reactionId: String,
    val step: Int,
    val frame: ReactionFrame,
    /** Nucleus colour per element symbol (category colour, as on the table). */
    val nucleusColors: Map<String, Color>,
)

/** Moves an entity from [from] to [to] once, eased; driven by [ReactionMotionSystem]. */
class GlideComponent(val from: Vec3 = Vec3.ZERO, val to: Vec3 = Vec3.ZERO, val duration: Float = 0.9f) : Component() {
    var time: Float = 0f

    override fun clone(): Component = GlideComponent(from, to, duration)
}

/**
 * An electron flying along an arc from [from] to [to], then resting there for a moment and
 * starting again, so the transfer can be watched as long as the step stays open.
 */
class FlightComponent(
    val from: Vec3 = Vec3.ZERO,
    val to: Vec3 = Vec3.ZERO,
    val lift: Float = 0.25f,
    val delay: Float = 0f,
) : Component() {
    var time: Float = 0f
    var paused: Boolean = false

    override fun clone(): Component = FlightComponent(from, to, lift, delay)
}

class ReactionMotionSystem : System() {
    override fun update(context: SceneUpdateContext) {
        super.update(context)
        val dt = context.deltaTime
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(GlideComponent::class.java))) {
            val glide = entity.components[GlideComponent::class.java] ?: continue
            if (glide.time >= glide.duration) continue
            glide.time = (glide.time + dt).coerceAtMost(glide.duration)
            val t = ReactionLayout.ease(glide.time / glide.duration)
            entity.components[TransformComponent::class.java]?.setPosition(lerp(glide.from, glide.to, t).v3())
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(FlightComponent::class.java))) {
            val flight = entity.components[FlightComponent::class.java] ?: continue
            if (flight.paused) continue
            flight.time = (flight.time + dt) % ReactionLayout.FLIGHT_CYCLE
            val p = ReactionLayout.flightPoint(flight.from, flight.to, flight.lift, flight.time - flight.delay)
            entity.components[TransformComponent::class.java]?.setPosition(p.v3())
        }
    }

    private fun lerp(a: Vec3, b: Vec3, t: Float) = a + (b - a) * t
}

private fun Vec3.v3() = Vector3(x, y, z)

private fun Color.c4(alpha: Float = 1f) = Color4(red, green, blue, alpha)

/**
 * Entity tree of the reaction Volume. Each atom is a flat Bohr model facing the viewer (rings in
 * the XY plane), like the diagrams in the textbook, so electrons can be counted. [root] takes the
 * user's rotation and zoom; its children are rebuilt for every step and glide from where they
 * were in the previous step.
 */
class ReactionScene {
    val root: Entity = Entity()

    private var content: Entity? = null
    private var spec: ReactionSceneSpec? = null
    private var paused = false
    private val flights = ArrayList<FlightComponent>()

    // Created per rebuild, never kept across rebuilds (see AtomScene for the crash this avoids).
    private var sphereMesh: MeshResource? = null
    private var cubeMesh: MeshResource? = null

    private fun sphere() = sphereMesh ?: MeshResource.createSphere(1f).also { sphereMesh = it }

    private fun cube() = cubeMesh ?: MeshResource.createBox(Vector3(1f, 1f, 1f)).also { cubeMesh = it }

    init {
        ensureRegistered()
        root.components.set(InteractableComponent())
        root.components.set(
            CollisionComponent(
                collisionShape = listOf(ShapeResource.createSphere(1f).also { it.toGlobal() }),
                physicsMaterial = PhysicsMaterialResource(),
            )
        )
    }

    fun setPose(radiusMeters: Float, yaw: Float, pitch: Float, zoom: Float) {
        root.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(radiusMeters * zoom))
            setQuaternion(axisAngle(1f, 0f, 0f, pitch) * axisAngle(0f, 1f, 0f, yaw))
        }
    }

    fun setPaused(value: Boolean) {
        paused = value
        flights.forEach { it.paused = value }
    }

    fun render(newSpec: ReactionSceneSpec) {
        if (newSpec == spec) return
        // Glide from whatever was drawn last (also when the user jumps steps); a new reaction starts in place.
        val previous = spec?.takeIf { it.reactionId == newSpec.reactionId }?.frame
        spec = newSpec
        android.util.Log.i("ElementSpaceRx", "RENDER ${newSpec.reactionId} step ${newSpec.step + 1} glideFrom=${previous != null}")
        content?.destroy(true)
        flights.clear()
        sphereMesh = null
        cubeMesh = null
        val next = Entity()
        val frame = newSpec.frame
        val before = previous?.particles?.associateBy { it.id }.orEmpty()
        frame.particles.forEach { particle ->
            next.addChild(particleEntity(particle, before[particle.id]?.position ?: particle.position, newSpec.nucleusColors))
        }
        frame.pairs.forEachIndexed { index, pair ->
            val target = ReactionLayout.pairElectrons(frame, pair)
            val origin = previous?.let { prev -> ReactionLayout.pairElectrons(prev.copy(pairs = emptyList()), pair).takeIf { it.size == target.size } }
            target.forEachIndexed { i, p ->
                next.addChild(gliding(bead("pair_${index}_$i", ElementColors.ElectronTransferred, ELECTRON), origin?.get(i) ?: p, p))
            }
        }
        frame.flights.forEachIndexed { index, flight ->
            val (start, end) = ReactionLayout.flightEnds(frame, flight) ?: return@forEachIndexed
            repeat(flight.count) { k ->
                val offset = Vec3(0f, k * 0.035f, 0.02f * k)
                val component = FlightComponent(start + offset, end + offset, lift = 0.22f, delay = k * 0.25f)
                val electron = bead("flight_${index}_$k", ElementColors.ElectronTransferred, ELECTRON * 1.25f)
                electron.addChild(instanced("flight_halo_${index}_$k", sphere(), halo(ElementColors.ElectronTransferred, 0.35f), listOf(Vec3.ZERO), ELECTRON * 2.6f))
                electron.components.set(component)
                electron.components[TransformComponent::class.java]?.setPosition(start.v3())
                flights.add(component)
                next.addChild(electron)
            }
        }
        root.addChild(next)
        content = next
        setPaused(paused)
    }

    fun destroy() {
        flights.clear()
        root.destroy(true)
    }

    // ---- builders -------------------------------------------------------------------------

    private fun particleEntity(particle: FrameParticle, from: Vec3, colors: Map<String, Color>): Entity {
        val holder = gliding(Entity(), from, particle.position)
        if (particle.cluster.isNotEmpty()) {
            buildCluster(holder, particle)
            return holder
        }
        val nucleusColor = colors[particle.symbol] ?: Color.Gray
        holder.addChild(instanced("nucleus_${particle.id}", sphere(), lit(nucleusColor), listOf(Vec3.ZERO), NUCLEUS))
        val outer = particle.shells.lastIndex
        particle.shells.forEachIndexed { shell, electrons ->
            val radius = ReactionLayout.shellRadius(shell)
            val valence = shell == outer
            holder.addChild(
                instanced(
                    "ring_${particle.id}_$shell",
                    cube(),
                    unlit(Color.White, if (valence) 0.85f else 0.45f),
                    ReactionLayout.ring(radius, (radius * 260).toInt().coerceAtLeast(36)),
                    if (valence) 0.0055f else 0.004f,
                )
            )
            val vacancies = if (valence) particle.vacancies else 0
            val slots = ReactionLayout.slots(electrons, vacancies, particle.gold[shell] ?: 0, radius)
            val plain = slots.filter { it.kind == ReactionLayout.SlotKind.ELECTRON }.map { it.position }
            val gold = slots.filter { it.kind == ReactionLayout.SlotKind.GOLD }.map { it.position }
            val empty = slots.filter { it.kind == ReactionLayout.SlotKind.VACANCY }.map { it.position }
            val size = if (valence) ELECTRON else ELECTRON * 0.75f
            if (plain.isNotEmpty()) {
                holder.addChild(instanced("e_halo_${particle.id}_$shell", sphere(), halo(ElementColors.ParticleElectron, 0.25f), plain, size * 1.9f))
                holder.addChild(instanced("e_${particle.id}_$shell", sphere(), beadMaterial(ElementColors.ParticleElectron), plain, size))
            }
            if (gold.isNotEmpty()) {
                holder.addChild(instanced("g_halo_${particle.id}_$shell", sphere(), halo(ElementColors.ElectronTransferred, 0.35f), gold, ELECTRON * 2.2f))
                holder.addChild(instanced("g_${particle.id}_$shell", sphere(), beadMaterial(ElementColors.ElectronTransferred), gold, ELECTRON * 1.2f))
            }
            if (empty.isNotEmpty()) {
                holder.addChild(instanced("v_${particle.id}_$shell", sphere(), halo(ElementColors.ElectronTransferred, 0.45f), empty, ELECTRON * 1.5f))
            }
        }
        tintHalo(particle.tint, ReactionLayout.particleRadius(particle))?.let { holder.addChild(it) }
        return holder
    }

    private fun buildCluster(holder: Entity, particle: FrameParticle) {
        val centre = particle.cluster.first()
        val arms = particle.cluster.drop(1)
        holder.addChild(instanced("cluster_c_${particle.id}", sphere(), lit(ElementColors.clusterCentre(centre)), listOf(Vec3.ZERO), 0.05f))
        holder.addChild(instanced("cluster_a_${particle.id}", sphere(), lit(ElementColors.ClusterArm), ReactionLayout.tetrahedron(0.075f).take(arms.size), 0.036f))
        tintHalo(particle.tint, 0.13f)?.let { holder.addChild(it) }
    }

    private fun tintHalo(tint: ChargeTint, radius: Float): Entity? {
        val (color, alpha) =
            when (tint) {
                ChargeTint.NONE -> return null
                ChargeTint.POSITIVE -> ElementColors.IonPositive to 0.16f
                ChargeTint.NEGATIVE -> ElementColors.IonNegative to 0.16f
                ChargeTint.PARTIAL_POSITIVE -> ElementColors.IonPositive to 0.09f
                ChargeTint.PARTIAL_NEGATIVE -> ElementColors.IonNegative to 0.09f
            }
        return instanced("tint", sphere(), halo(color, alpha), listOf(Vec3.ZERO), radius + 0.03f)
    }

    private fun gliding(entity: Entity, from: Vec3, to: Vec3): Entity {
        entity.components[TransformComponent::class.java]?.setPosition(from.v3())
        if (from != to) entity.components.set(GlideComponent(from, to)) else entity.components[TransformComponent::class.java]?.setPosition(to.v3())
        return entity
    }

    private fun bead(name: String, color: Color, size: Float): Entity = instanced(name, sphere(), beadMaterial(color), listOf(Vec3.ZERO), size)

    private fun instanced(name: String, mesh: MeshResource, material: Material, points: List<Vec3>, scale: Float): Entity {
        val size = Vector3(scale)
        val identity = Quat.identity()
        val instances = points.mapIndexed { i, p -> MeshInstancesResource.Instance("${name}_$i", Transform(p.v3(), identity, size)) }
        val entity = ModelEntity(mesh, material)
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create(name, instances)
        return entity
    }

    private fun unlit(color: Color, alpha: Float = 1f): Material =
        UnlitMaterial.create(if (alpha < 1f) BlendingMode.TRANSPARENT else BlendingMode.OPAQUE).apply { setBaseColor(color.c4(alpha)) }

    private fun halo(color: Color, alpha: Float): Material =
        UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(color.c4(alpha)) }

    private fun lit(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.c4())
            setRoughness(0.4f)
            setMetallic(0f)
            setEmissiveColor(Color4(color.red * 0.15f, color.green * 0.15f, color.blue * 0.15f, 1f))
        }

    private fun beadMaterial(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.c4())
            setRoughness(0.25f)
            setMetallic(0.6f)
            setEmissiveColor(Color4(color.red * 0.35f, color.green * 0.35f, color.blue * 0.35f, 1f))
        }

    private companion object {
        const val NUCLEUS = 0.045f
        const val ELECTRON = 0.016f
        var registered = false

        fun ensureRegistered() {
            if (registered) return
            registerComponent<GlideComponent>()
            registerComponent<FlightComponent>()
            registerSystem<ReactionMotionSystem>()
            registered = true
        }

        fun axisAngle(x: Float, y: Float, z: Float, radians: Float): Quat {
            val s = sin(radians / 2f)
            return Quat(x * s, y * s, z * s, cos(radians / 2f))
        }
    }
}
