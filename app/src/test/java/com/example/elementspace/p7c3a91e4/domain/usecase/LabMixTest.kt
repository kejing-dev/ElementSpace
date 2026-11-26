package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.Experiments
import com.example.elementspace.p7c3a91e4.ui.lab.LabMix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabMixTest {
    @Test
    fun `nothing on the bench is silent and every level stays within range`() {
        assertEquals(LabMix(), LabMix.of(emptyList(), 1f))
        Experiments.ALL.forEach { e ->
            (listOf(e.start) + e.steps.map { it.effects }).forEach { effects ->
                listOf(0.6f, 1f, 1.5f).forEach { power ->
                    val mix = LabMix.of(effects, power)
                    assertTrue(e.id, mix.flame in 0f..1f && mix.sparks in 0f..1f && mix.fizz in 0f..1f)
                }
            }
        }
    }

    @Test
    fun `flames are heard, sodium on water fizzes, a bigger flame is louder`() {
        val flameTest = Experiments.experiment("flame_test")!!
        val lit = flameTest.steps.first().effects
        assertTrue(LabMix.of(lit, 1f).flame > 0f)
        assertTrue(LabMix.of(lit, 1.5f).flame > LabMix.of(lit, 0.6f).flame)
        val sodium = Experiments.experiment("na_water")!!
        assertTrue(sodium.steps.any { LabMix.of(it.effects, 1f).fizz > 0f })
        assertTrue(Experiments.experiment("fe_o2")!!.steps.any { LabMix.of(it.effects, 1f).sparks > 0f })
    }
}
