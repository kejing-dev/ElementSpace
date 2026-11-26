package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.L10n
import com.example.elementspace.p7c3a91e4.domain.model.Subshell
import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/** One real orbital: shell [n], subshell [l] and [index] into [Orbitals.shapes] for that l. */
data class OrbitalRef(val n: Int, val l: Int, val index: Int)

/** Where ψ = 0. Chemistry axes: z up. */
sealed interface NodalSurface {
    /** Plane through the nucleus with this normal. */
    data class Plane(val normal: Vec3, val name: L10n) : NodalSurface

    /** Double cone about the z axis, [halfAngleDegrees] measured from +z. */
    data class Cone(val halfAngleDegrees: Float) : NodalSurface

    /** Radial node: sphere of this radius, in Bohr radii (hydrogen-like, Z = 1). */
    data class Sphere(val radius: Float) : NodalSurface
}

/**
 * A real (not complex) orbital shape. [angular] is a homogeneous polynomial of degree l in x, y, z,
 * evaluated on a unit direction; [ml] is the |m| it is built from (real orbitals mix +m and −m).
 */
class RealOrbital(
    /** Subscript after "2p" etc.; empty for s. */
    val suffix: String,
    val ml: String,
    val shape: L10n,
    val angularNodes: List<NodalSurface>,
    val angular: (Float, Float, Float) -> Float,
)

/** Points sampled from |ψ|² of one orbital, in units where the orbital fits radius 1. */
data class OrbitalSample(val ref: OrbitalRef, val positive: List<Vec3>, val negative: List<Vec3>, val extent: Float)

/**
 * Hydrogen-like orbitals: ψ = R_nl(r) · A(direction). R uses the associated Laguerre polynomials
 * (Atkins' Physical Chemistry, "Hydrogenic atomic orbitals"); A is the real spherical harmonic.
 * Many-electron orbitals keep these shapes and node counts; only their size changes with Z*.
 */
object Orbitals {
    private val XY = NodalSurface.Plane(Vec3(0f, 0f, 1f), L10n("xy 平面", "xy plane"))
    private val XZ = NodalSurface.Plane(Vec3(0f, 1f, 0f), L10n("xz 平面", "xz plane"))
    private val YZ = NodalSurface.Plane(Vec3(1f, 0f, 0f), L10n("yz 平面", "yz plane"))
    private val DIAG_1 = NodalSurface.Plane(Vec3(1f, -1f, 0f).unit(), L10n("x = y 平面", "x = y plane"))
    private val DIAG_2 = NodalSurface.Plane(Vec3(1f, 1f, 0f).unit(), L10n("x = −y 平面", "x = −y plane"))
    private val S3 = sqrt(3f)

    /** cos²θ = 1/3 for d_z²; 3/5 for f_z³; 1/5 for f_xz², f_yz². */
    private fun cone(cos2: Float) = NodalSurface.Cone(Math.toDegrees(kotlin.math.acos(sqrt(cos2).toDouble())).toFloat())

    private val SPHERE = L10n("球形", "Sphere")
    private val DUMBBELL = L10n("哑铃形：两瓣，正负相反", "Dumbbell: two lobes of opposite sign")
    private val CLOVER = L10n("四瓣（花瓣形），相邻两瓣正负相反", "Four lobes (clover), neighbours of opposite sign")
    private val DZ2 = L10n("沿 z 轴两瓣 + 腰上一个环（环与两瓣正负相反）", "Two lobes along z and a ring round the waist (ring has the opposite sign)")
    private val F_SHAPE = L10n("多瓣：瓣数 = 被节面分成的区域数，相邻瓣正负相反", "Many lobes, one per region between nodes; neighbours of opposite sign")

    val shapes: Map<Int, List<RealOrbital>> =
        mapOf(
            0 to listOf(RealOrbital("", "0", SPHERE, emptyList()) { _, _, _ -> 1f }),
            1 to
                listOf(
                    RealOrbital("x", "±1", DUMBBELL, listOf(YZ)) { x, _, _ -> x },
                    RealOrbital("y", "±1", DUMBBELL, listOf(XZ)) { _, y, _ -> y },
                    RealOrbital("z", "0", DUMBBELL, listOf(XY)) { _, _, z -> z },
                ),
            2 to
                listOf(
                    RealOrbital("xy", "±2", CLOVER, listOf(XZ, YZ)) { x, y, _ -> x * y },
                    RealOrbital("yz", "±1", CLOVER, listOf(XY, XZ)) { _, y, z -> y * z },
                    RealOrbital("z²", "0", DZ2, listOf(cone(1f / 3f))) { x, y, z -> 3f * z * z - (x * x + y * y + z * z) },
                    RealOrbital("xz", "±1", CLOVER, listOf(XY, YZ)) { x, _, z -> x * z },
                    RealOrbital("x²−y²", "±2", CLOVER, listOf(DIAG_1, DIAG_2)) { x, y, _ -> x * x - y * y },
                ),
            3 to
                listOf(
                    RealOrbital("y(3x²−y²)", "±3", F_SHAPE, listOf(XZ, plane(S3, -1f, "y = √3x", "y = √3x"), plane(S3, 1f, "y = −√3x", "y = −√3x"))) { x, y, _ -> y * (3f * x * x - y * y) },
                    RealOrbital("xyz", "±2", F_SHAPE, listOf(XY, XZ, YZ)) { x, y, z -> x * y * z },
                    RealOrbital("yz²", "±1", F_SHAPE, listOf(XZ, cone(1f / 5f))) { x, y, z -> y * (5f * z * z - (x * x + y * y + z * z)) },
                    RealOrbital("z³", "0", F_SHAPE, listOf(XY, cone(3f / 5f))) { x, y, z -> z * (5f * z * z - 3f * (x * x + y * y + z * z)) },
                    RealOrbital("xz²", "±1", F_SHAPE, listOf(YZ, cone(1f / 5f))) { x, y, z -> x * (5f * z * z - (x * x + y * y + z * z)) },
                    RealOrbital("z(x²−y²)", "±2", F_SHAPE, listOf(XY, DIAG_1, DIAG_2)) { x, y, z -> z * (x * x - y * y) },
                    RealOrbital("x(x²−3y²)", "±3", F_SHAPE, listOf(YZ, plane(1f, -S3, "x = √3y", "x = √3y"), plane(1f, S3, "x = −√3y", "x = −√3y"))) { x, y, _ -> x * (x * x - 3f * y * y) },
                ),
        )

    private fun plane(a: Float, b: Float, zh: String, en: String) = NodalSurface.Plane(Vec3(a, b, 0f).unit(), L10n("$zh 平面", "$en plane"))

    fun orbital(ref: OrbitalRef): RealOrbital = shapes.getValue(ref.l)[ref.index]

    /** "2p" + "_x", split so labels can draw the subscript smaller. */
    fun name(ref: OrbitalRef): String {
        val suffix = orbital(ref).suffix
        return "${ref.n}${"spdf"[ref.l]}" + if (suffix.isEmpty()) "" else "_$suffix"
    }

    /** Generalised Laguerre polynomial L_k^α(x) by the three-term recurrence. */
    fun laguerre(k: Int, alpha: Int, x: Double): Double {
        if (k == 0) return 1.0
        var previous = 1.0
        var current = 1.0 + alpha - x
        for (i in 1 until k) {
            val next = ((2 * i + 1 + alpha - x) * current - (i + alpha) * previous) / (i + 1)
            previous = current
            current = next
        }
        return current
    }

    /** Unnormalised R_nl(r) for Z = 1, r in Bohr radii: ρ^l e^(−ρ/2) L_(n−l−1)^(2l+1)(ρ), ρ = 2r/n. */
    fun radial(n: Int, l: Int, r: Double): Double {
        val rho = 2.0 * r / n
        return rho.pow(l) * exp(-rho / 2.0) * laguerre(n - l - 1, 2 * l + 1, rho)
    }

    /** Radial probability P(r) = r² R², unnormalised. */
    fun radialProbability(n: Int, l: Int, r: Double): Double {
        val value = radial(n, l, r)
        return r * r * value * value
    }

    private val extentCache = HashMap<Pair<Int, Int>, Float>()

    /** Radius (Bohr radii) holding 99 % of the electron, so the drawn cloud has a fair edge. */
    fun extent(n: Int, l: Int): Float =
        extentCache.getOrPut(n to l) {
            val end = 6.0 * n * n + 20.0
            val steps = 4000
            val dr = end / steps
            val cumulative = DoubleArray(steps + 1)
            for (i in 1..steps) cumulative[i] = cumulative[i - 1] + radialProbability(n, l, i * dr) * dr
            val total = cumulative[steps]
            val i = cumulative.indexOfFirst { it >= 0.99 * total }
            (i * dr).toFloat()
        }

    /** Radii (Bohr) where R changes sign: n − l − 1 of them. */
    fun radialNodes(n: Int, l: Int): List<Float> {
        val end = extent(n, l).toDouble()
        val roots = ArrayList<Float>()
        val dr = end / 6000
        var r = dr
        var previous = radial(n, l, r)
        while (r < end) {
            r += dr
            val value = radial(n, l, r)
            if (value == 0.0 || value * previous < 0) roots += (r - dr / 2).toFloat()
            previous = value
        }
        return roots
    }

    /** Peak of P(r), the most probable radius (Bohr); 1 a₀ for hydrogen 1s, 4 a₀ for 2p. */
    fun mostProbableRadius(n: Int, l: Int): Float {
        val end = extent(n, l).toDouble()
        var best = 0.0
        var bestR = 0.0
        val dr = end / 6000
        var r = dr
        while (r < end) {
            val p = radialProbability(n, l, r)
            if (p > best) {
                best = p
                bestR = r
            }
            r += dr
        }
        return bestR.toFloat()
    }

    fun psi(ref: OrbitalRef, x: Float, y: Float, z: Float): Double {
        val r = sqrt(x * x + y * y + z * z)
        if (r < 1e-6f) return if (ref.l == 0) radial(ref.n, 0, 0.0) else 0.0
        val a = orbital(ref).angular(x / r, y / r, z / r)
        return radial(ref.n, ref.l, r.toDouble()) * a
    }

    /** Electrons per orbital of a subshell under Hund's rule: singly first, then pair up. */
    fun hund(l: Int, electrons: Int): IntArray {
        val counts = IntArray(2 * l + 1)
        repeat(electrons.coerceAtMost(2 * counts.size)) { counts[it % counts.size]++ }
        return counts
    }
}

private fun Vec3.unit(): Vec3 {
    val length = length()
    return Vec3(x / length, y / length, z / length)
}

/**
 * Samples |ψ|² of one orbital. Because ψ = R(r)·A(direction), the radius is drawn from P(r) by its
 * cumulative table and the direction from A² by rejection, independently. Points come back in
 * chemistry axes (z up), scaled so [OrbitalSample.extent] maps to 1.
 */
class SampleOrbitalUseCase {
    operator fun invoke(ref: OrbitalRef, count: Int = 3600, seed: Int = 11): OrbitalSample {
        val random = kotlin.random.Random(seed * 131 + ref.n * 17 + ref.l * 5 + ref.index)
        val extent = Orbitals.extent(ref.n, ref.l)
        val steps = 1200
        val dr = extent / steps
        val cumulative = DoubleArray(steps + 1)
        for (i in 1..steps) cumulative[i] = cumulative[i - 1] + Orbitals.radialProbability(ref.n, ref.l, (i - 0.5) * dr)
        val total = cumulative[steps]
        val shape = Orbitals.orbital(ref)
        // Bound of A² on the unit sphere, found by probing; 10 % headroom keeps the rejection exact enough.
        var bound = 0f
        repeat(4000) {
            val d = direction(random)
            bound = maxOf(bound, shape.angular(d.x, d.y, d.z).let { it * it })
        }
        bound *= 1.1f
        val positive = ArrayList<Vec3>()
        val negative = ArrayList<Vec3>()
        while (positive.size + negative.size < count) {
            val target = random.nextDouble() * total
            var lo = 0
            var hi = steps
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (cumulative[mid] < target) lo = mid + 1 else hi = mid
            }
            val r = ((lo - random.nextDouble()) * dr).toFloat().coerceAtLeast(0f)
            var d: Vec3
            var a: Float
            do {
                d = direction(random)
                a = shape.angular(d.x, d.y, d.z)
            } while (random.nextFloat() * bound > a * a)
            val sign = Orbitals.radial(ref.n, ref.l, r.toDouble()) * a
            val point = Vec3(d.x * r / extent, d.y * r / extent, d.z * r / extent)
            if (sign >= 0) positive += point else negative += point
        }
        return OrbitalSample(ref, positive, negative, extent)
    }

    private fun direction(random: kotlin.random.Random): Vec3 {
        val z = 2f * random.nextFloat() - 1f
        val ring = sqrt(1f - z * z)
        val t = (2.0 * Math.PI * random.nextFloat()).toFloat()
        return Vec3(ring * kotlin.math.cos(t), ring * kotlin.math.sin(t), z)
    }
}

/**
 * Slater's rules (J. C. Slater, Phys. Rev. 36, 57, 1930) for the effective nuclear charge felt by
 * one electron in subshell (n, l). Groups: (1s)(2s,2p)(3s,3p)(3d)(4s,4p)(4d)(4f)(5s,5p)…
 */
object Slater {
    data class Result(
        val sameGroup: Int,
        val sameFactor: Float,
        /** s/p: electrons in shell n − 1 (×0.85); d/f: 0. */
        val nMinusOne: Int,
        /** s/p: shells n − 2 and below; d/f: every group to the left (×1.00). */
        val inner: Int,
        val sigma: Float,
        val zEff: Float,
    )

    private fun groupKey(n: Int, l: Int): Pair<Int, Int> = n to (if (l <= 1) 0 else l)

    private fun before(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean = a.first < b.first || (a.first == b.first && a.second < b.second)

    fun zEff(z: Int, subshells: List<Subshell>, n: Int, l: Int): Result {
        val own = groupKey(n, l)
        var same = 0
        var nMinusOne = 0
        var inner = 0
        for (s in subshells) {
            val key = groupKey(s.n, s.l)
            when {
                key == own -> same += s.electrons
                l <= 1 && s.n == n - 1 -> nMinusOne += s.electrons
                l <= 1 && s.n < n - 1 -> inner += s.electrons
                l >= 2 && before(key, own) -> inner += s.electrons
            }
        }
        same = (same - 1).coerceAtLeast(0)
        val factor = if (n == 1) 0.30f else 0.35f
        val sigma = same * factor + nMinusOne * 0.85f + inner * 1.00f
        return Result(same, factor, nMinusOne, inner, sigma, z - sigma)
    }
}

private fun Float.fmt(digits: Int = 2): String = "%.${digits}f".format(java.util.Locale.ROOT, this)

/** Card text for the orbital page: generated from the configuration, so it holds for every element. */
object OrbitalTexts {
    private val AUFBAU_LABELS = listOf("1s", "2s", "2p", "3s", "3p", "4s", "3d", "4p", "5s", "4d", "5p", "6s", "4f", "5d", "6p", "7s", "5f", "6d", "7p")

    fun zEff(z: Int, subshells: List<Subshell>, n: Int, l: Int): L10n {
        val r = Slater.zEff(z, subshells, n, l)
        val label = "$n${"spdf"[l]}"
        val parts = ArrayList<Pair<String, String>>()
        if (r.sameGroup > 0) parts += "同组另 ${r.sameGroup} 个 × ${r.sameFactor.fmt()}" to "${r.sameGroup} others in the same group × ${r.sameFactor.fmt()}"
        if (r.nMinusOne > 0) parts += "n−1 层 ${r.nMinusOne} 个 × 0.85" to "${r.nMinusOne} in shell n−1 × 0.85"
        if (r.inner > 0) {
            parts +=
                if (l <= 1) "更内层 ${r.inner} 个 × 1.00" to "${r.inner} further in × 1.00"
                else "左边各组 ${r.inner} 个 × 1.00" to "${r.inner} in groups to the left × 1.00"
        }
        val zh = if (parts.isEmpty()) "$label 电子：没有别的电子屏蔽，Z* = Z = $z。"
        else "$label 电子：${parts.joinToString("，") { it.first }}。σ = ${r.sigma.fmt()}，Z* = $z − ${r.sigma.fmt()} = ${r.zEff.fmt()}。"
        val en = if (parts.isEmpty()) "$label electron: nothing shields it, Z* = Z = $z."
        else "$label electron: ${parts.joinToString(", ") { it.second }}. σ = ${r.sigma.fmt()}, Z* = $z − ${r.sigma.fmt()} = ${r.zEff.fmt()}."
        return L10n(zh, en)
    }

    /** Why the boxes are filled this way: Aufbau, Hund, exceptions, which electrons leave first. */
    fun why(z: Int, charge: Int, actual: List<Subshell>, aufbau: List<Subshell>): List<L10n> {
        val out = ArrayList<L10n>()
        val last = actual.lastOrNull() ?: return out
        val upTo = AUFBAU_LABELS.indexOf(last.label).coerceAtLeast(0)
        val order = AUFBAU_LABELS.take(upTo + 1).joinToString(" → ")
        out += L10n("构造原理（能量最低原理）：按 $order 的顺序填。", "Aufbau principle: fill in the order $order.")
        val unpaired = actual.sumOf { s -> Orbitals.hund(s.l, s.electrons).count { it == 1 } }
        out +=
            if (unpaired > 0) L10n("未成对电子 $unpaired 个：同一能级里电子先分占不同轨道、自旋平行（洪特规则）。", "$unpaired unpaired electron(s): within a subshell electrons first occupy separate orbitals with parallel spins (Hund's rule).")
            else L10n("没有未成对电子：每个轨道都是一对自旋相反的电子（泡利原理）。", "No unpaired electrons: every orbital holds a pair of opposite spins (Pauli principle).")
        if (charge == 0 && actual != aufbau) {
            val count = { list: List<Subshell>, label: String -> list.firstOrNull { it.label == label }?.electrons ?: 0 }
            // Written the textbook way, by shell: 3d⁵ 4s¹.
            val differs = AUFBAU_LABELS.filter { count(actual, it) != count(aufbau, it) }.sortedWith(compareBy({ it[0] }, { "spdf".indexOf(it[1]) }))
            val shown = { list: List<Subshell> -> differs.joinToString(" ") { it + sup(count(list, it)) } }
            val dHalfOrFull = actual.firstOrNull { it.l == 2 && (it.electrons == 5 || it.electrons == 10) && aufbau.none { a -> a.n == it.n && a.l == 2 && a.electrons == it.electrons } }
            val reasonZh = when {
                dHalfOrFull?.electrons == 5 -> "${dHalfOrFull.label} 半充满（5 个电子分占、自旋平行），整体能量更低。"
                dHalfOrFull?.electrons == 10 -> "${dHalfOrFull.label} 全充满更稳定。"
                else -> "原因在于电子之间相互作用的细节，课本只要求记住。"
            }
            val reasonEn = when {
                dHalfOrFull?.electrons == 5 -> "a half-filled ${dHalfOrFull.label} (five parallel spins) lowers the total energy."
                dHalfOrFull?.electrons == 10 -> "a full ${dHalfOrFull.label} is more stable."
                else -> "the reason lies in finer electron–electron interactions; textbooks only ask you to remember it."
            }
            out += L10n("例外：实际是 ${shown(actual)}，按构造原理应为 ${shown(aufbau)}。$reasonZh", "Exception: actually ${shown(actual)}, not ${shown(aufbau)} as the Aufbau order predicts; $reasonEn")
        }
        val d = actual.lastOrNull { it.l == 2 }
        val s = actual.lastOrNull { it.l == 0 }
        if (d != null && s != null && s.n == d.n + 1) {
            out += L10n("能级交错：${s.label} 比 ${d.label} 先填；但失电子时先失 ${s.label}，再失 ${d.label}。", "Energy levels interleave: ${s.label} fills before ${d.label}, yet ${s.label} electrons are lost first when the atom ionises.")
        }
        val p = actual.lastOrNull { it.l == 1 }
        if (charge == 0 && p != null && p.n == last.n && p.n <= 4 && p.electrons == 3 && actual.none { it.l == 2 && it.n == p.n - 1 && it.electrons in 1..9 }) {
            out += L10n("${p.label}³ 半充满，较稳定：第一电离能比后一个元素还大（N > O，P > S，As > Se）。", "${p.label}³ is half-filled and fairly stable: its first ionisation energy beats the next element's (N > O, P > S, As > Se).")
        }
        return out
    }

    fun radialNote(n: Int, l: Int): L10n {
        val nodes = n - l - 1
        val label = "$n${"spdf"[l]}"
        var zh = "类氢原子（Z = 1）的 $label：曲线有 ${nodes + 1} 个峰、$nodes 个零点（径向节点）。"
        var en = "$label of a hydrogen-like atom (Z = 1): ${nodes + 1} peak(s) and $nodes zero(s) (radial nodes)."
        if (n == 1) {
            zh += "氢原子 1s 的最可能半径正好是玻尔半径 a₀。"
            en += " For hydrogen 1s the most probable radius is exactly the Bohr radius a₀."
        }
        if (n >= 2 && l == 0) {
            zh += "靠近核的小峰叫「钻穿」：所以同一层里 ns 能量比 np 低。"
            en += " The small peak near the nucleus is penetration: that is why ns lies below np in the same shell."
        }
        zh += "多电子原子里轨道形状不变，大小约按 n²a₀ / Z* 缩放。"
        en += " In many-electron atoms the shape stays; the size scales roughly as n²a₀ / Z*."
        return L10n(zh, en)
    }

    private fun sup(value: Int): String = value.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")
}
