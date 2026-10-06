package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.ElectronConfiguration
import com.example.elementspace.p7c3a91e4.domain.model.Subshell

/**
 * Ground-state electron configuration of an atom or simple ion.
 *
 * Neutral atoms follow the Aufbau order with the experimentally observed exceptions (Cr, Cu, …)
 * applied on top. Cations lose electrons from the outermost shell first; anions keep filling.
 */
class ElectronConfigurationUseCase {

    operator fun invoke(atomicNumber: Int, charge: Int = 0): ElectronConfiguration {
        require(atomicNumber in 1..118) { "atomic number out of range: $atomicNumber" }
        val occupancy = neutral(atomicNumber)
        when {
            charge > 0 -> remove(occupancy, minOf(charge, atomicNumber))
            charge < 0 -> add(occupancy, -charge)
        }
        val subshells =
            AUFBAU.mapNotNull { (n, l) ->
                val e = occupancy[n to l] ?: 0
                if (e > 0) Subshell(n, l, e) else null
            }
        val maxN = subshells.maxOfOrNull { it.n } ?: 0
        val shells = (1..maxN).map { n -> subshells.filter { it.n == n }.sumOf { it.electrons } }
        return ElectronConfiguration(shells = shells, subshells = subshells)
    }

    private fun neutral(z: Int): MutableMap<Pair<Int, Int>, Int> {
        val occupancy = mutableMapOf<Pair<Int, Int>, Int>()
        add(occupancy, z)
        EXCEPTIONS[z]?.forEach { (subshell, electrons) -> occupancy[subshell] = electrons }
        return occupancy
    }

    private fun add(occupancy: MutableMap<Pair<Int, Int>, Int>, count: Int) {
        var remaining = count
        for (subshell in AUFBAU) {
            if (remaining == 0) break
            val capacity = 2 * (2 * subshell.second + 1)
            val current = occupancy[subshell] ?: 0
            val take = minOf(capacity - current, remaining)
            if (take > 0) {
                occupancy[subshell] = current + take
                remaining -= take
            }
        }
    }

    private fun remove(occupancy: MutableMap<Pair<Int, Int>, Int>, count: Int) {
        var remaining = count
        val outermostFirst = AUFBAU.sortedWith(compareBy({ -it.first }, { -it.second }))
        for (subshell in outermostFirst) {
            if (remaining == 0) break
            val current = occupancy[subshell] ?: 0
            val take = minOf(current, remaining)
            if (take > 0) {
                occupancy[subshell] = current - take
                remaining -= take
            }
        }
    }

    private companion object {
        // (n, l) in filling order: 1s 2s 2p 3s 3p 4s 3d 4p 5s 4d 5p 6s 4f 5d 6p 7s 5f 6d 7p
        val AUFBAU: List<Pair<Int, Int>> =
            listOf(
                1 to 0, 2 to 0, 2 to 1, 3 to 0, 3 to 1, 4 to 0, 3 to 2, 4 to 1, 5 to 0, 4 to 2,
                5 to 1, 6 to 0, 4 to 3, 5 to 2, 6 to 1, 7 to 0, 5 to 3, 6 to 2, 7 to 1,
            )

        private fun fix(vararg entries: Pair<Pair<Int, Int>, Int>) = entries.toMap()

        /** Observed ground states that differ from the plain Aufbau result. */
        val EXCEPTIONS: Map<Int, Map<Pair<Int, Int>, Int>> =
            mapOf(
                24 to fix((3 to 2) to 5, (4 to 0) to 1),
                29 to fix((3 to 2) to 10, (4 to 0) to 1),
                41 to fix((4 to 2) to 4, (5 to 0) to 1),
                42 to fix((4 to 2) to 5, (5 to 0) to 1),
                44 to fix((4 to 2) to 7, (5 to 0) to 1),
                45 to fix((4 to 2) to 8, (5 to 0) to 1),
                46 to fix((4 to 2) to 10, (5 to 0) to 0),
                47 to fix((4 to 2) to 10, (5 to 0) to 1),
                57 to fix((4 to 3) to 0, (5 to 2) to 1),
                58 to fix((4 to 3) to 1, (5 to 2) to 1),
                64 to fix((4 to 3) to 7, (5 to 2) to 1),
                78 to fix((5 to 2) to 9, (6 to 0) to 1),
                79 to fix((5 to 2) to 10, (6 to 0) to 1),
                89 to fix((5 to 3) to 0, (6 to 2) to 1),
                90 to fix((5 to 3) to 0, (6 to 2) to 2),
                91 to fix((5 to 3) to 2, (6 to 2) to 1),
                92 to fix((5 to 3) to 3, (6 to 2) to 1),
                93 to fix((5 to 3) to 4, (6 to 2) to 1),
                96 to fix((5 to 3) to 7, (6 to 2) to 1),
                103 to fix((6 to 2) to 0, (7 to 1) to 1),
            )
    }
}
