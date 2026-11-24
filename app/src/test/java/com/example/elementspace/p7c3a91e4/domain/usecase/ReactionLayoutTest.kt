package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.domain.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactionLayoutTest {
    @Test
    fun `vacancies and moved electrons take the left slots, where flights land`() {
        val slots = ReactionLayout.slots(electrons = 7, vacancies = 1, gold = 0, radius = 0.2f)
        assertEquals(8, slots.size)
        assertEquals(ReactionLayout.SlotKind.VACANCY, slots[0].kind)
        assertEquals(-0.2f, slots[0].position.x, 1e-4f)
        val gold = ReactionLayout.slots(electrons = 8, vacancies = 0, gold = 1, radius = 0.2f)
        assertEquals(1, gold.count { it.kind == ReactionLayout.SlotKind.GOLD })
        assertEquals(-0.2f, gold.first { it.kind == ReactionLayout.SlotKind.GOLD }.position.x, 1e-4f)
    }

    @Test
    fun `a flight rests, arcs over and ends on the landing slot`() {
        val from = Vec3(-0.5f, 0f, 0f)
        val to = Vec3(0.5f, 0f, 0f)
        assertEquals(from, ReactionLayout.flightPoint(from, to, 0.2f, 0f))
        val mid = ReactionLayout.flightPoint(from, to, 0.2f, 0.3f + ReactionLayout.FLIGHT_TIME / 2f)
        assertTrue(mid.y > 0.15f)
        val end = ReactionLayout.flightPoint(from, to, 0.2f, ReactionLayout.FLIGHT_CYCLE - 0.01f)
        assertEquals(0.5f, end.x, 1e-4f)
        assertTrue(ReactionLayout.FLIGHT_TIME + 0.3f < ReactionLayout.FLIGHT_CYCLE)
    }
}
