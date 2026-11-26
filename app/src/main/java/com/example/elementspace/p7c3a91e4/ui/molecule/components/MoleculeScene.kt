package com.example.elementspace.p7c3a91e4.ui.molecule.components

import androidx.compose.ui.graphics.Color
import com.example.elementspace.p7c3a91e4.domain.model.MolBond
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.ui.molecule.components.BallStick.v3
import com.example.elementspace.p7c3a91e4.ui.theme.ElementColors
import com.pico.spatial.core.ecs.CollisionComponent
import com.example.elementspace.p7c3a91e4.ui.common.SceneLabels
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import kotlin.math.max

enum class MoleculeMode { BALL_AND_STICK, SPACE_FILLING }

/** What the molecule Volume draws: one molecule, or two side by side when comparing. */
data class MoleculeSceneSpec(
    val molecules: List<Molecule>,
    val mode: MoleculeMode,
    val showAngles: Boolean,
    val showGroups: Boolean,
    val showLabels: Boolean = true,
    val colors: Map<String, Color>,
)

/**
 * Entity tree of the molecule Volume. [root] takes the user's rotation and zoom; the content is
 * rebuilt whenever the spec changes and is scaled so the molecule(s) fill about 70 % of the Volume.
 */
class MoleculeScene {
    val root: Entity = Entity()

    private var content: Entity? = null
    private var spec: MoleculeSceneSpec? = null

    // Created per rebuild, never kept across rebuilds (see AtomScene for the crash this avoids).
    private var sphereMesh: MeshResource? = null
    private var cylinderMesh: MeshResource? = null

    private fun sphere() = sphereMesh ?: MeshResource.createSphere(1f).also { sphereMesh = it }

    private fun cylinder() = cylinderMesh ?: MeshResource.createCylinder(1f, 1f).also { cylinderMesh = it }

    init {
        root.components.set(InteractableComponent())
        root.components.set(
            CollisionComponent(
                collisionShape = listOf(ShapeResource.createSphere(1f).also { it.toGlobal() }),
                physicsMaterial = PhysicsMaterialResource(),
            )
        )
    }

    private val holders = ArrayList<Entity>()
    private val labels = SceneLabels()
    private var rotation = BallStick.axisAngle(0f, 1f, 0f, 0f)

    /** Zoom scales the whole Volume; rotation turns each molecule about its own centre, so a compared pair never swings into each other. */
    fun setPose(radiusMeters: Float, yaw: Float, pitch: Float, zoom: Float) {
        root.components[TransformComponent::class.java]?.setScaleVector(Vector3(radiusMeters * zoom))
        rotation = BallStick.axisAngle(1f, 0f, 0f, pitch) * BallStick.axisAngle(0f, 1f, 0f, yaw)
        holders.forEach { it.components[TransformComponent::class.java]?.setQuaternion(rotation) }
        labels.setRotation(yaw, pitch)
    }

    fun render(newSpec: MoleculeSceneSpec) {
        if (newSpec == spec) return
        spec = newSpec
        android.util.Log.i("ElementSpaceMol", "RENDER ${newSpec.molecules.joinToString { it.id }} ${newSpec.mode} angles=${newSpec.showAngles} groups=${newSpec.showGroups}")
        content?.destroy(true)
        holders.clear()
        labels.reset()
        sphereMesh = null
        cylinderMesh = null
        val next = Entity()
        val placed = layout(newSpec.molecules)
        // Any rotation of a molecule stays inside its own radius, so fit offset + radius.
        val extent = placed.maxOf { (m, offset) -> offset.length() + m.atoms.maxOf { it.position.length() + BallStick.fill(it.symbol) } }
        val scale = 0.72f / max(extent, 1f)
        next.components[TransformComponent::class.java]?.setScaleVector(Vector3(scale))
        placed.forEachIndexed { index, (molecule, offset) ->
            val holder = molecule(index, molecule, offset, newSpec)
            holder.components[TransformComponent::class.java]?.setQuaternion(rotation)
            holders += holder
            next.addChild(holder)
        }
        root.addChild(next)
        content = next
    }

    fun destroy() {
        root.destroy(true)
    }

    /** Side by side along x with a gap, centred as a pair. */
    private fun layout(molecules: List<Molecule>): List<Pair<Molecule, Vec3>> {
        if (molecules.size < 2) return molecules.map { it to Vec3.ZERO }
        val widths = molecules.map { m -> m.atoms.maxOf { it.position.length() + BallStick.ball(it.symbol) } }
        val gap = 0.6f
        return listOf(molecules[0] to Vec3(-(widths[0] + gap / 2f), 0f, 0f), molecules[1] to Vec3(widths[1] + gap / 2f, 0f, 0f))
    }

    private fun molecule(index: Int, m: Molecule, offset: Vec3, spec: MoleculeSceneSpec): Entity {
        val holder = Entity()
        holder.components[TransformComponent::class.java]?.setPosition(offset.v3())
        val fill = spec.mode == MoleculeMode.SPACE_FILLING
        m.atoms.withIndex().groupBy { it.value.symbol }.forEach { (symbol, atoms) ->
            val radius = if (fill) BallStick.fill(symbol) else BallStick.ball(symbol)
            val color = spec.colors[symbol] ?: Color.Gray
            holder.addChild(BallStick.sphereEntity("atom_${index}_$symbol", sphere(), lit(color), atoms.map { it.value.position }, radius))
            if (spec.showLabels) {
                atoms.forEach { holder.addChild(labels.label(symbol, it.value.position, height = radius * if (fill) 0.6f else 1.15f, lift = radius * 1.04f)) }
            }
        }
        if (fill) return holder

        val gold = ElementColors.ElectronTransferred
        val (groupBonds, plainBonds) = m.bonds.partition { spec.showGroups && m.atoms[it.a].group && m.atoms[it.b].group }
        fun sticks(bonds: List<MolBond>) = bonds.flatMap { BallStick.sticks(m.atoms[it.a].position, m.atoms[it.b].position, it.order) }
        if (plainBonds.isNotEmpty()) holder.addChild(BallStick.stickEntity("bonds_$index", cylinder(), lit(ElementColors.MoleculeBond), sticks(plainBonds), BallStick.STICK))
        if (groupBonds.isNotEmpty()) holder.addChild(BallStick.stickEntity("gbonds_$index", cylinder(), glow(gold), sticks(groupBonds), BallStick.STICK * 1.15f))

        if (m.aromaticRing.isNotEmpty()) {
            val ring = BallStick.ring(m.aromaticRing.map { m.atoms[it].position })
            holder.addChild(BallStick.sphereEntity("ring_$index", sphere(), glow(if (spec.showGroups) gold else ElementColors.MoleculeBond), ring, 0.06f))
        }
        if (spec.showGroups) {
            val marked = m.atoms.filter { it.group }
            marked.groupBy { it.symbol }.forEach { (symbol, atoms) ->
                holder.addChild(BallStick.sphereEntity("halo_${index}_$symbol", sphere(), halo(gold, 0.28f), atoms.map { it.position }, BallStick.ball(symbol) * 1.55f))
            }
        }
        if (spec.showAngles) {
            m.angles.forEachIndexed { k, angle ->
                val beads = BallStick.arc(m.atoms[angle.at].position, m.atoms[angle.a].position, m.atoms[angle.b].position, radius = 0.55f)
                holder.addChild(BallStick.sphereEntity("arc_${index}_$k", sphere(), glow(ElementColors.BondAngle), beads, 0.045f))
            }
        }
        return holder
    }

    private fun lit(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(Color4(color.red, color.green, color.blue, 1f))
            setRoughness(0.35f)
            setMetallic(0f)
            setEmissiveColor(Color4(color.red * 0.18f, color.green * 0.18f, color.blue * 0.18f, 1f))
        }

    private fun glow(color: Color): Material =
        PhysicallyBasedMaterial.create().apply {
            setBaseColor(Color4(color.red, color.green, color.blue, 1f))
            setRoughness(0.3f)
            setMetallic(0f)
            setEmissiveColor(Color4(color.red * 0.6f, color.green * 0.6f, color.blue * 0.6f, 1f))
        }

    private fun halo(color: Color, alpha: Float): Material =
        UnlitMaterial.create(BlendingMode.TRANSPARENT).apply { setBaseColor(Color4(color.red, color.green, color.blue, alpha)) }
}
