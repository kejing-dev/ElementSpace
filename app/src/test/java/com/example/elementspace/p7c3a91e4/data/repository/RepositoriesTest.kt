package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledElementRepositoryTest {
    private val repository = BundledElementRepository()

    @Test
    fun `dataset holds 118 elements in order with unique symbols and positions`() {
        val elements = repository.elements()
        assertEquals(118, elements.size)
        assertEquals((1..118).toList(), elements.map { it.atomicNumber })
        assertEquals(118, elements.map { it.symbol }.toSet().size)
        assertEquals(118, elements.map { it.tableRow to it.tableColumn }.toSet().size)
        assertTrue(elements.all { it.tableColumn in 1..18 && it.tableRow in 1..9 })
    }

    @Test
    fun `period and group follow the table position and the f block has no group`() {
        fun z(n: Int) = repository.element(n)!!
        assertEquals(1 to 1, z(1).period to z(1).group)
        assertEquals(4 to 8, z(26).period to z(26).group)
        assertEquals(6 to 11, z(79).period to z(79).group)
        assertEquals(6, z(64).period)
        assertNull(z(64).group)
        assertEquals(7, z(92).period)
        assertNull(z(92).group)
    }

    @Test
    fun `well known elements land in the right place and category`() {
        val carbon = repository.element(6)!!
        assertEquals("C", carbon.symbol)
        assertEquals(2 to 14, carbon.tableRow to carbon.tableColumn)
        assertEquals(ElementCategory.NONMETAL, carbon.category)
        assertEquals(ElementCategory.LANTHANIDE, repository.element(57)!!.category)
        assertEquals(8 to 3, repository.element(57)!!.let { it.tableRow to it.tableColumn })
        assertEquals(7 to 18, repository.element(118)!!.let { it.tableRow to it.tableColumn })
    }

    @Test
    fun `isotopes are never empty and carbon 14 is radioactive`() {
        for (z in 1..118) assertTrue("z=$z", repository.isotopes(z).isNotEmpty())
        val carbon14 = repository.isotopes(6).first { it.massNumber == 14 }
        assertFalse(carbon14.stable)
        assertEquals(8, carbon14.neutrons(6))
    }

    @Test
    fun `unknown atomic number returns nothing`() {
        assertNull(repository.element(0))
        assertNull(repository.element(119))
        assertTrue(repository.isotopes(119).isEmpty())
        assertEquals(3, repository.allotropes(6).size)
        assertTrue(repository.allotropes(2).isEmpty())
    }
}

class InMemorySessionRepositoryTest {
    @Test
    fun `opening the same element twice reuses its volume`() {
        val session = InMemorySessionRepository()
        assertEquals(OpenAtomResult(alreadyOpen = false, replaced = null), session.openAtom(6))
        assertEquals(OpenAtomResult(alreadyOpen = true, replaced = null), session.openAtom(6))
        assertEquals(listOf(6), session.openAtoms.value)
    }

    @Test
    fun `a fourth element replaces the least recently used one`() {
        val session = InMemorySessionRepository()
        session.openAtom(6)
        session.openAtom(8)
        session.openAtom(11)
        session.touchAtom(6) // carbon was just used, so oxygen is now the oldest
        val result = session.openAtom(26)
        assertEquals(8, result.replaced)
        assertEquals(listOf(11, 6, 26), session.openAtoms.value)
    }

    @Test
    fun `closing frees a slot and language changes are published`() {
        val session = InMemorySessionRepository()
        session.openAtom(6)
        session.closeAtom(6)
        assertTrue(session.openAtoms.value.isEmpty())
        session.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, session.language.value)
    }
}
