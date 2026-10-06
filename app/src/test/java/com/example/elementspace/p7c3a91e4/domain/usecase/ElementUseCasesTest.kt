package com.example.elementspace.p7c3a91e4.domain.usecase

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterElementsUseCaseTest {
    private val elements = BundledElementRepository().elements()
    private val filter = FilterElementsUseCase()

    @Test
    fun `no query and no category keeps every element`() {
        assertEquals(118, filter(elements, "", null).size)
    }

    @Test
    fun `chinese name english name symbol and number find the same element`() {
        for (query in listOf("铁", "iron", "Fe", "fe", "26")) {
            assertEquals("query=$query", setOf(26), filter(elements, query, null))
        }
    }

    @Test
    fun `single letter symbol does not match longer symbols`() {
        // "C" is carbon only as a symbol; Ca, Cl, Cu … must not match by prefix.
        val result = filter(elements, "C", null)
        assertTrue(6 in result)
        assertFalse(20 in result)
    }

    @Test
    fun `category filter combines with the query`() {
        val noble = filter(elements, "", ElementCategory.NOBLE_GAS)
        assertEquals(setOf(2, 10, 18, 36, 54, 86, 118), noble)
        assertTrue(filter(elements, "铁", ElementCategory.NOBLE_GAS).isEmpty())
    }

    @Test
    fun `unknown query matches nothing`() {
        assertTrue(filter(elements, "zzzz", null).isEmpty())
    }
}

class ElectronConfigurationUseCaseTest {
    private val configuration = ElectronConfigurationUseCase()

    @Test
    fun `carbon fills K2 L4`() {
        val carbon = configuration(6)
        assertEquals(listOf(2, 4), carbon.shells)
        assertEquals("1s² 2s² 2p²", carbon.notation())
        assertEquals(4, carbon.valenceElectrons)
    }

    @Test
    fun `chromium and copper use the observed exceptions`() {
        assertEquals("1s² 2s² 2p⁶ 3s² 3p⁶ 3d⁵ 4s¹", configuration(24).notation())
        assertEquals(listOf(2, 8, 18, 1), configuration(29).shells)
    }

    @Test
    fun `sodium ion loses its single valence electron`() {
        assertEquals(listOf(2, 8, 1), configuration(11).shells)
        assertEquals(listOf(2, 8), configuration(11, charge = 1).shells)
    }

    @Test
    fun `chloride gains one electron to complete the shell`() {
        assertEquals(listOf(2, 8, 8), configuration(17, charge = -1).shells)
    }

    @Test
    fun `electron total always equals atomic number minus charge`() {
        for (z in 1..118) assertEquals(z, configuration(z).shells.sum())
        assertEquals(24, configuration(26, charge = 2).shells.sum())
    }
}

class TrendScaleUseCaseTest {
    private val elements = BundledElementRepository().elements()
    private val scale = TrendScaleUseCase()

    @Test
    fun `category has no scale`() {
        assertNull(scale(elements, TrendProperty.CATEGORY))
    }

    @Test
    fun `fluorine is the most electronegative and elements without data are absent`() {
        val result = scale(elements, TrendProperty.ELECTRONEGATIVITY)!!
        assertEquals(1f, result.normalized.getValue(9), 1e-6f)
        assertEquals(3.98, result.max, 1e-9)
        assertFalse(2 in result.normalized) // helium has no Pauling value
        assertTrue(result.normalized.values.all { it in 0f..1f })
    }
}
