package com.example.elementspace.p7c3a91e4.ui.molecule.components

import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import com.example.elementspace.p7c3a91e4.domain.usecase.MoleculeBuilder
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshInstancesResource
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Sizes and stick geometry shared by the molecule Volume and the bond frames of the reaction Volume (Å). */
object BallStick {
    private val BALL = mapOf("C" to 0.36f, "H" to 0.24f, "O" to 0.34f, "Cl" to 0.44f, "Br" to 0.48f)
    private val VDW = mapOf("C" to 1.70f, "H" to 1.20f, "O" to 1.52f, "Cl" to 1.75f, "Br" to 1.85f)

    /** Stick radius. */
    const val STICK = 0.075f

    /** Gap between the sticks of a double / triple bond. */
    private const val SPREAD = 0.16f

    fun ball(symbol: String) = BALL[symbol] ?: 0.4f

    /** Space-filling radius, a little under the van der Waals radius so the shape stays readable. */
    fun fill(symbol: String) = (VDW[symbol] ?: 1.6f) * 0.82f

    /** A stick from [from] to [to]. */
    data class Stick(val from: Vec3, val to: Vec3)

    /**
     * The sticks of one bond: one per bond order, side by side; an aromatic bond is one stick (the
     * ring is drawn separately). [dashed] cuts each stick into short pieces.
     */
    fun sticks(a: Vec3, b: Vec3, order: Int, dashed: Boolean = false, spread: Float = SPREAD): List<Stick> {
        val d = b - a
        val dir = MoleculeBuilder.norm(d)
        val side = perpendicular(dir) * spread
        val offsets =
            when (order) {
                2 -> listOf(-0.5f, 0.5f)
                3 -> listOf(-1f, 0f, 1f)
                else -> listOf(0f)
            }
        return offsets.flatMap { o ->
            val s = a + side * o
            val e = b + side * o
            if (!dashed) listOf(Stick(s, e)) else dashes(s, e)
        }
    }

    private fun dashes(s: Vec3, e: Vec3, pieces: Int = 4): List<Stick> {
        val step = (e - s) * (1f / (pieces * 2 - 1))
        return (0 until pieces).map { k -> Stick(s + step * (2f * k), s + step * (2f * k + 1f)) }
    }

    /** A direction perpendicular to [dir], in the XY plane when possible (molecules are laid out there). */
    fun perpendicular(dir: Vec3): Vec3 {
        val c = MoleculeBuilder.cross(dir, Vec3(0f, 0f, 1f))
        return if (c.length() > 0.2f) MoleculeBuilder.norm(c) else MoleculeBuilder.norm(MoleculeBuilder.cross(dir, Vec3(0f, 1f, 0f)))
    }

    /** Beads along the arc of a bond angle at [at] between the directions to [a] and [b]. */
    fun arc(at: Vec3, a: Vec3, b: Vec3, radius: Float, beads: Int = 14): List<Vec3> {
        val u = MoleculeBuilder.norm(a - at)
        val v0 = MoleculeBuilder.norm(b - at)
        val cosT = MoleculeBuilder.dot(u, v0).coerceIn(-1f, 1f)
        val theta = acos(cosT)
        // Orthonormal pair (u, w) spanning the plane of the angle; a straight angle needs any perpendicular.
        val raw = v0 - u * cosT
        val w = if (raw.length() < 1e-3f) perpendicular(u) else MoleculeBuilder.norm(raw)
        return (0..beads).map { k ->
            val t = theta * k / beads
            at + (u * cos(t) + w * sin(t)) * radius
        }
    }

    /** Points around a ring centre, for the dashed circle inside a benzene ring. */
    fun ring(points: List<Vec3>, shrink: Float = 0.62f, beads: Int = 30): List<Vec3> {
        val c = points.fold(Vec3.ZERO) { s, p -> s + p } * (1f / points.size)
        val r = (points.first() - c).length() * shrink
        val u = MoleculeBuilder.norm(points.first() - c)
        val n = MoleculeBuilder.norm(MoleculeBuilder.cross(points[0] - c, points[1] - c))
        val w = MoleculeBuilder.cross(n, u)
        return (0 until beads).filter { it % 2 == 0 }.map { k ->
            val t = 2f * PI.toFloat() * k / beads
            c + (u * cos(t) + w * sin(t)) * r
        }
    }

    // ---- entity helpers --------------------------------------------------------------------------

    /** One instanced entity of cylinders for [sticks], [radius] in the same units as the points. */
    fun stickEntity(name: String, mesh: MeshResource, material: Material, sticks: List<Stick>, radius: Float): ModelEntity {
        val instances =
            sticks.mapIndexed { i, s ->
                val d = s.to - s.from
                val length = d.length()
                MeshInstancesResource.Instance(
                    "${name}_$i",
                    Transform(((s.from + s.to) * 0.5f).v3(), alignYTo(d * (1f / length)), Vector3(radius, length, radius)),
                )
            }
        val entity = ModelEntity(mesh, material)
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create(name, instances)
        return entity
    }

    /** One instanced entity of spheres at [points], each scaled by [radius]. */
    fun sphereEntity(name: String, mesh: MeshResource, material: Material, points: List<Vec3>, radius: Float): ModelEntity {
        val size = Vector3(radius)
        val identity = Quat.identity()
        val instances = points.mapIndexed { i, p -> MeshInstancesResource.Instance("${name}_$i", Transform(p.v3(), identity, size)) }
        val entity = ModelEntity(mesh, material)
        entity.components[ModelComponent::class.java]?.meshInstances = MeshInstancesResource.create(name, instances)
        return entity
    }

    fun Vec3.v3() = Vector3(x, y, z)

    /** Rotation taking the +Y axis onto the unit vector [direction]. */
    fun alignYTo(direction: Vec3): Quat {
        val axisX = direction.z
        val axisZ = -direction.x
        val axisLength = sqrt(axisX * axisX + axisZ * axisZ)
        if (axisLength < 1e-5f) return if (direction.y > 0f) Quat.identity() else axisAngle(1f, 0f, 0f, PI.toFloat())
        return axisAngle(axisX / axisLength, 0f, axisZ / axisLength, acos(direction.y.coerceIn(-1f, 1f)))
    }

    fun axisAngle(x: Float, y: Float, z: Float, radians: Float): Quat {
        val s = sin(radians / 2f)
        return Quat(x * s, y * s, z * s, cos(radians / 2f))
    }

}
