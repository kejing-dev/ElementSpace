package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.ElementReactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollideElementsTest {
    private val elements = BundledElementRepository().elements().associateBy { it.symbol }
    private val collide = CollideElementsUseCase(BundledReactionRepository().all(), ElementReactions.NOTES)

    private fun hit(a: String, b: String) = collide(elements.getValue(a), elements.getValue(b))

    @Test
    fun `sodium and chlorine react and the reason names who loses and who gains`() {
        val r = hit("Na", "Cl") as CollideResult.Reacts
        assertTrue(r.reactions.any { it.id == "na_cl2" })
        assertTrue(r.why.zh, r.why.zh.contains("最外层只有 1 个电子") && r.why.zh.contains("再得 1 个就排满"))
        // Order of the two picks does not matter.
        assertEquals(r.reactions.map { it.id }, (hit("Cl", "Na") as CollideResult.Reacts).reactions.map { it.id })
    }

    @Test
    fun `non-metals share electrons and the pair leans to the more electronegative one`() {
        val r = hit("H", "O") as CollideResult.Reacts
        assertTrue(r.why.zh, r.why.zh.contains("共用电子对") && r.why.zh.contains("偏向得电子能力更强的氧"))
    }

    @Test
    fun `noble gases, two metals and atom-at-a-time elements do not react, each with its reason`() {
        assertTrue((hit("He", "O") as CollideResult.NoReaction).why.zh.contains("最外层 2 个电子已经排满"))
        assertTrue((hit("Ar", "Na") as CollideResult.NoReaction).why.zh.contains("最外层 8 个电子"))
        assertTrue((hit("Xe", "O") as CollideResult.NoReaction).why.zh.contains("只有氟"))
        assertTrue((hit("Fe", "Cu") as CollideResult.NoReaction).why.zh.contains("合金"))
        assertTrue(hit("Og", "H") is CollideResult.NoExperiment)
        assertTrue(hit("Xe", "F") is CollideResult.Reacts)
    }

    @Test
    fun `pairs without a sourced reaction never get an invented one`() {
        assertEquals(CollideResult.NoCommonReaction, hit("C", "N"))
    }

    @Test
    fun `the common elements give plenty of reacting pairs`() {
        val common = listOf("H", "C", "N", "O", "F", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "K", "Ca", "Fe", "Cu", "Zn", "Br", "I")
        var reacting = 0
        for (i in common.indices) for (j in i + 1 until common.size) if (hit(common[i], common[j]) is CollideResult.Reacts) reacting++
        assertTrue("$reacting reacting pairs", reacting >= 45)
    }

    @Test
    fun `ion text compares with the noble gas it now resembles`() {
        val na = IonText.explain(elements.getValue("Na"), 1, listOf(2, 8))
        assertTrue(na.zh, na.zh.contains("失去 1 个电子") && na.zh.contains("和氖原子一样"))
        val cl = IonText.explain(elements.getValue("Cl"), -1, listOf(2, 8, 8))
        assertTrue(cl.zh, cl.zh.contains("得到 1 个电子") && cl.zh.contains("和氩原子一样"))
    }
}
