package com.example.elementspace.p7c3a91e4.ui.reaction.components

import com.example.elementspace.p7c3a91e4.ui.common.TOUCH_RADIUS
import androidx.compose.ui.graphics.Color
import com.example.elementspace.p7c3a91e4.data.repository.OrganicReactions
import com.example.elementspace.p7c3a91e4.domain.model.BondMark
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.ChargeTint
import com.example.elementspace.p7c3a91e4.domain.model.ConditionKind
import com.example.elementspace.p7c3a91e4.domain.model.ConditionVisual
import com.example.elementspace.p7c3a91e4.domain.usecase.AtomLabelText
import com.example.elementspace.p7c3a91e4.ui.common.SceneLabels
import com.example.elementspace.p7c3a91e4.ui.molecule.components.BallStick
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
    /** Conditions acting in this step (light, heat, ...), drawn around the molecules. */
    val conditions: List<ConditionVisual> = emptyList(),
    val language: AppLanguage = AppLanguage.ZH,
    /** Atomic number per symbol, for the charge printed on ions. */
    val atomicNumbers: Map<String, Int> = emptyMap(),
)

/** Scales an entity's y by a flicker around [base]; flames. */
class FlickerComponent(val base: Vec3 = Vec3(1f, 1f, 1f), val phase: Float = 0f) : Component() {
    var time: Float = 0f

    override fun clone(): Component = FlickerComponent(base, phase)
}

/** Breathes an entity's uniform scale around [base]; the glow of the sun. */
class PulseComponent(val base: Float = 1f) : Component() {
    var time: Float = 0f

    override fun clone(): Component = PulseComponent(base)
}

/** Turns an entity about its y axis; sparks around an ignition flame. */
class SpinComponent(val speed: Float = 2f) : Component() {
    var angle: Float = 0f

    override fun clone(): Component = SpinComponent(speed)
}

/** Keeps an entity hidden (scale 0) for [delay] seconds, then shows it; used for bonds that form after atoms glide. */
class RevealComponent(val delay: Float = 0.9f) : Component() {
    var time: Float = 0f
    var shown: Boolean = false

    override fun clone(): Component = RevealComponent(delay)
}

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
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(RevealComponent::class.java))) {
            val reveal = entity.components[RevealComponent::class.java] ?: continue
            if (reveal.shown) continue
            reveal.time += dt
            if (reveal.time >= reveal.delay) {
                reveal.shown = true
                entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(1f))
            }
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(FlickerComponent::class.java))) {
            val flicker = entity.components[FlickerComponent::class.java] ?: continue
            flicker.time += dt
            val t = flicker.time + flicker.phase
            val k = 1f + 0.09f * sin(t * 9f) + 0.05f * sin(t * 14.3f)
            val w = 1f - 0.04f * sin(t * 11f)
            entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(flicker.base.x * w, flicker.base.y * k, flicker.base.z * w))
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(PulseComponent::class.java))) {
            val pulse = entity.components[PulseComponent::class.java] ?: continue
            pulse.time += dt
            entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(pulse.base * (1f + 0.12f * sin(pulse.time * 3f))))
        }
        for (entity in context.scene.queryEntity(EntityQueryCondition.hasComponent(SpinComponent::class.java))) {
            val spin = entity.components[SpinComponent::class.java] ?: continue
            spin.angle += dt * spin.speed
            entity.components[TransformComponent::class.java]?.setQuaternion(Quat(0f, sin(spin.angle / 2f), 0f, cos(spin.angle / 2f)))
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
    private val labels = SceneLabels()

    // Created per rebuild, never kept across rebuilds (see AtomScene for the crash this avoids).
    private var sphereMesh: MeshResource? = null
    private var cubeMesh: MeshResource? = null

    private fun sphere() = sphereMesh ?: MeshResource.createSphere(1f).also { sphereMesh = it }

    private fun cube() = cubeMesh ?: MeshResource.createBox(Vector3(1f, 1f, 1f)).also { cubeMesh = it }

    private var cylinderMesh: MeshResource? = null
    private var coneMesh: MeshResource? = null

    private fun cone() = coneMesh ?: MeshResource.createCone(1f, 1f).also { coneMesh = it }

    private fun cylinder() = cylinderMesh ?: MeshResource.createCylinder(1f, 1f).also { cylinderMesh = it }

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

    fun setPose(radiusMeters: Float, yaw: Float, pitch: Float, zoom: Float) {
        root.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(radiusMeters * zoom))
            setQuaternion(axisAngle(1f, 0f, 0f, pitch) * axisAngle(0f, 1f, 0f, yaw))
        }
        labels.setRotation(yaw, pitch)
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
        labels.reset()
        sphereMesh = null
        cubeMesh = null
        cylinderMesh = null
        coneMesh = null
        val next = Entity()
        if (newSpec.frame.atoms.isNotEmpty()) buildBonds(next, newSpec, previous)
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
        if (newSpec.conditions.isNotEmpty()) buildConditions(next, newSpec)
        root.addChild(next)
        content = next
        setPaused(paused)
    }

    fun destroy() {
        flights.clear()
        labels.reset()
        root.destroy(true)
    }

    // ---- builders -------------------------------------------------------------------------

    /**
     * Ball-and-stick frame ([ReactionKind.BONDS]): every atom glides from where it was in the
     * frame drawn last; a bond appears at once when neither end moves, otherwise once the atoms
     * have arrived. Red dashed = about to break, gold = newly formed.
     */
    private fun buildBonds(next: Entity, spec: ReactionSceneSpec, previous: ReactionFrame?) {
        val frame = spec.frame
        val s = frame.atomScale
        val before = previous?.atoms?.associateBy { it.id }.orEmpty()
        val moved = frame.atoms.filter { a -> before[a.id]?.let { it.position != a.position } ?: false }.map { it.id }.toSet()
        frame.atoms.forEach { atom ->
            val color = spec.nucleusColors[atom.symbol] ?: Color.Gray
            val material = if (atom.faded) halo(color, 0.3f) else lit(color)
            val radius = BallStick.ball(atom.symbol) * s
            val holder = Entity()
            holder.addChild(instanced("atom_${atom.id}", sphere(), material, listOf(Vec3.ZERO), radius))
            if (!atom.faded) holder.addChild(labels.label(atom.symbol, Vec3.ZERO, height = radius * 1.15f, lift = radius * 1.04f))
            next.addChild(gliding(holder, before[atom.id]?.position ?: atom.position, atom.position))
        }
        val at = frame.atoms.associateBy { it.id }
        val groups = frame.bonds.groupBy { (it.a in moved || it.b in moved) to it.mark }
        groups.forEach { (key, bonds) ->
            val (late, mark) = key
            val sticks =
                bonds.flatMap { bond ->
                    val a = at.getValue(bond.a).position
                    val b = at.getValue(bond.b).position
                    when (bond.mark) {
                        BondMark.BREAK -> BallStick.sticks(a, b, bond.order, dashed = true, spread = 0.16f * s)
                        // One stick of a double bond stays, the other is red and dashed.
                        BondMark.BREAK_ONE -> BallStick.sticks(a, b, 2, spread = 0.16f * s).take(1)
                        else -> BallStick.sticks(a, b, bond.order, spread = 0.16f * s)
                    }
                }
            val extra =
                if (mark == BondMark.BREAK_ONE) {
                    bonds.flatMap { bond -> BallStick.sticks(at.getValue(bond.a).position, at.getValue(bond.b).position, 2, dashed = true, spread = 0.16f * s).drop(4) }
                } else {
                    emptyList()
                }
            val name = "bonds_${mark.name}_$late"
            val color =
                when (mark) {
                    BondMark.NEW -> ElementColors.ElectronTransferred
                    BondMark.BREAK -> ElementColors.BondBreak
                    else -> ElementColors.MoleculeBond
                }
            val radius = BallStick.STICK * s * if (mark == BondMark.NEW) 1.3f else 1f
            val entities = listOfNotNull(
                sticks.takeIf { it.isNotEmpty() }?.let { BallStick.stickEntity(name, cylinder(), if (mark == BondMark.NEW) glow(color) else lit(color), it, radius) },
                extra.takeIf { it.isNotEmpty() }?.let { BallStick.stickEntity("${name}_red", cylinder(), lit(ElementColors.BondBreak), it, radius) },
            )
            entities.forEach { entity ->
                if (late) {
                    entity.components[TransformComponent::class.java]?.setScaleVector(Vector3(0f))
                    entity.components.set(RevealComponent())
                }
                next.addChild(entity)
            }
        }
    }

    // ---- reaction conditions (v1.3.0) ---------------------------------------------------------

    /** Where the drawn particles are, with their radii, to place condition props around them. */
    private fun bodies(frame: ReactionFrame): List<Pair<Vec3, Float>> =
        frame.atoms.map { it.position to BallStick.ball(it.symbol) * frame.atomScale } +
            frame.particles.map { it.position to ReactionLayout.particleRadius(it) }

    private fun buildConditions(next: Entity, spec: ReactionSceneSpec) {
        val bodies = bodies(spec.frame)
        if (bodies.isEmpty()) return
        val centre = bodies.fold(Vec3.ZERO) { a, (p, _) -> a + p } * (1f / bodies.size)
        val bottom = bodies.minOf { (p, r) -> p.y - r }
        val right = bodies.maxOf { (p, r) -> p.x + r }
        val floor = (bottom - 0.08f).coerceAtLeast(-0.86f)
        var tagY = floor - 0.02f
        spec.conditions.forEach { visual ->
            val text = "${visual.kind.icon} ${visual.label.get(spec.language)}"
            when (visual.kind) {
                ConditionKind.LIGHT -> {
                    val sun = Vec3(-0.7f, 0.66f, 0.18f)
                    val target = lightTarget(spec.frame) ?: centre
                    val glowBall = instanced("sun_halo", sphere(), halo(ElementColors.Sun, 0.35f), listOf(Vec3.ZERO), 1f) // gate:allow primitive-hero schematic light mark in a textbook diagram (M3 sample), not a scene sun
                    glowBall.components.set(PulseComponent(0.13f))
                    val sunHolder = Entity().also { it.components[TransformComponent::class.java]?.setPosition(sun.v3()) }
                    sunHolder.addChild(instanced("sun", sphere(), glow(ElementColors.Sun), listOf(Vec3.ZERO), 0.065f)) // gate:allow primitive-hero schematic light mark (M3 sample)
                    sunHolder.addChild(glowBall)
                    next.addChild(sunHolder)
                    val dir = target - sun
                    val side = BallStick.perpendicular(dir * (1f / dir.length())) * 0.05f
                    val rays = listOf(-1f, 0f, 1f).map { k -> BallStick.Stick(sun + dir * 0.14f + side * k, sun + dir * 0.92f + side * (k * 1.6f)) }
                    next.addChild(BallStick.stickEntity("rays", cylinder(), halo(ElementColors.Sun, 0.5f), rays, 0.007f))
                    next.addChild(labels.label(text, sun + Vec3(0f, -0.14f, 0f), height = 0.07f, lift = 0.02f, style = SceneLabels.Style.TAG))
                }
                ConditionKind.HEAT -> {
                    val at = Vec3(centre.x, floor - 0.16f, 0f)
                    val burner = ModelEntity(cube(), lit(ElementColors.Burner))
                    burner.components[TransformComponent::class.java]?.apply {
                        setPosition((at + Vec3(0f, -0.035f, 0f)).v3())
                        setScaleVector(Vector3(0.2f, 0.06f, 0.12f))
                    }
                    next.addChild(burner)
                    next.addChild(flame(at, 0.13f, 0.055f))
                    next.addChild(labels.label(text, Vec3(at.x + 0.3f, at.y + 0.04f, 0f), height = 0.07f, lift = 0.05f, style = SceneLabels.Style.TAG))
                    tagY = at.y - 0.06f
                }
                ConditionKind.IGNITE -> {
                    // Where the reactants meet, if that spot is free; otherwise under them.
                    val free = bodies.all { (p, r) -> (p - centre).length() > r + 0.12f }
                    val at = if (free) centre + Vec3(0f, -0.06f, 0.05f) else Vec3(centre.x, floor - 0.1f, 0.05f)
                    next.addChild(flame(at, 0.1f, 0.04f))
                    val sparks = Entity().also { it.components[TransformComponent::class.java]?.setPosition((at + Vec3(0f, 0.07f, 0f)).v3()) }
                    val points = (0 until 6).map { i -> val a = i * 1.047f; Vec3(0.085f * cos(a), 0.02f * sin(i * 2.1f), 0.085f * sin(a)) }
                    sparks.addChild(instanced("sparks", sphere(), glow(ElementColors.FlameInner), points, 0.009f))
                    sparks.components.set(SpinComponent(2.4f))
                    next.addChild(sparks)
                    next.addChild(labels.label(text, Vec3(at.x + 0.24f, at.y + 0.04f, 0.05f), height = 0.07f, lift = 0.05f, style = SceneLabels.Style.TAG))
                }
                ConditionKind.CATALYST, ConditionKind.NOTE -> {
                    next.addChild(labels.label(text, Vec3((centre.x + 0.36f).coerceAtMost(right), tagY, 0f), height = 0.065f, lift = 0.05f, style = SceneLabels.Style.TAG))
                }
            }
        }
    }

    /** The bond light breaks: the first red bond between two atoms of the same element (Cl–Cl), else any red bond. */
    private fun lightTarget(frame: ReactionFrame): Vec3? {
        val at = frame.atoms.associateBy { it.id }
        val broken = frame.bonds.filter { it.mark == BondMark.BREAK }
        val bond = broken.firstOrNull { at[it.a]?.symbol == at[it.b]?.symbol } ?: broken.firstOrNull() ?: return null
        val a = at[bond.a]?.position ?: return null
        val b = at[bond.b]?.position ?: return null
        return (a + b) * 0.5f
    }

    /** Two nested cones (orange outside, yellow inside) standing on [base], flickering. */
    private fun flame(base: Vec3, height: Float, radius: Float): Entity {
        val holder = Entity().also { it.components[TransformComponent::class.java]?.setPosition(base.v3()) }
        listOf(Triple(ElementColors.FlameOuter, 1f, 0.75f), Triple(ElementColors.FlameInner, 0.55f, 0.95f)).forEachIndexed { i, (color, k, alpha) ->
            val h = height * k
            val r = radius * k
            val cone = ModelEntity(cone(), UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(color.c4(alpha)) })
            cone.components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(0f, h / 2f, 0f))
                setScaleVector(Vector3(r, h, r))
            }
            cone.components.set(FlickerComponent(Vec3(r, h, r), phase = i * 0.7f))
            holder.addChild(cone)
        }
        return holder
    }

    private fun glow(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(color.c4())
            setRoughness(0.3f)
            setMetallic(0f)
            setEmissiveColor(Color4(color.red * 0.6f, color.green * 0.6f, color.blue * 0.6f, 1f))
        }

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
        // An ion's charge halo is a transparent sphere that hides anything inside it: sit in front of it.
        val lift = if (particle.tint == ChargeTint.NONE) NUCLEUS + 0.03f else ReactionLayout.particleRadius(particle) + 0.05f
        holder.addChild(labels.label(AtomLabelText.particle(particle, spec?.atomicNumbers?.get(particle.symbol)), Vec3.ZERO, height = 0.085f, lift = lift))
        return holder
    }

    private fun buildCluster(holder: Entity, particle: FrameParticle) {
        val centre = particle.cluster.first()
        val arms = particle.cluster.drop(1)
        holder.addChild(instanced("cluster_c_${particle.id}", sphere(), lit(ElementColors.clusterCentre(centre)), listOf(Vec3.ZERO), 0.05f))
        holder.addChild(instanced("cluster_a_${particle.id}", sphere(), lit(ElementColors.ClusterArm), ReactionLayout.tetrahedron(0.075f).take(arms.size), 0.036f))
        tintHalo(particle.tint, 0.13f)?.let { holder.addChild(it) }
        holder.addChild(labels.label(AtomLabelText.particle(particle, null), Vec3(0f, -0.17f, 0f), height = 0.07f, lift = 0.12f))
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
            registerComponent<RevealComponent>()
            registerComponent<FlickerComponent>()
            registerComponent<PulseComponent>()
            registerComponent<SpinComponent>()
            registerSystem<ReactionMotionSystem>()
            registered = true
        }

        fun axisAngle(x: Float, y: Float, z: Float, radians: Float): Quat {
            val s = sin(radians / 2f)
            return Quat(x * s, y * s, z * s, cos(radians / 2f))
        }
    }
}
