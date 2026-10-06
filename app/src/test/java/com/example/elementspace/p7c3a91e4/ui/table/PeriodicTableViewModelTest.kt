package com.example.elementspace.p7c3a91e4.ui.table

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.ElementCategory
import com.example.elementspace.p7c3a91e4.domain.model.TrendProperty
import com.example.elementspace.p7c3a91e4.domain.usecase.FilterElementsUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.TrendScaleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodicTableViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var session: InMemorySessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        session = InMemorySessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        PeriodicTableViewModel(BundledElementRepository(), session, FilterElementsUseCase(), TrendScaleUseCase())

    @Test
    fun `initial state shows all elements in the basic category colouring`() = runTest {
        val state = viewModel().state.value
        assertEquals(118, state.elements.size)
        assertEquals(118, state.matches.size)
        assertFalse(state.filtering)
        assertEquals(TrendProperty.CATEGORY, state.trend)
        assertNull(state.trendScale)
    }

    @Test
    fun `search narrows the matches and an unknown query reports no match`() = runTest {
        val vm = viewModel()
        vm.onEvent(PeriodicTableEvent.QueryChanged("Fe"))
        assertEquals(setOf(26), vm.state.value.matches)
        assertTrue(vm.state.value.filtering)

        vm.onEvent(PeriodicTableEvent.QueryChanged("zzzz"))
        assertTrue(vm.state.value.noMatch)

        vm.onEvent(PeriodicTableEvent.QueryChanged(""))
        assertEquals(118, vm.state.value.matches.size)
    }

    @Test
    fun `toggling a category twice clears the filter`() = runTest {
        val vm = viewModel()
        vm.onEvent(PeriodicTableEvent.CategoryToggled(ElementCategory.HALOGEN))
        assertEquals(setOf(9, 17, 35, 53, 85, 117), vm.state.value.matches)
        vm.onEvent(PeriodicTableEvent.CategoryToggled(ElementCategory.HALOGEN))
        assertNull(vm.state.value.category)
        assertEquals(118, vm.state.value.matches.size)
    }

    @Test
    fun `selecting a trend colours by it and selecting category returns to the basic view`() = runTest {
        val vm = viewModel()
        vm.onEvent(PeriodicTableEvent.TrendSelected(TrendProperty.ELECTRONEGATIVITY))
        assertEquals(TrendProperty.ELECTRONEGATIVITY, vm.state.value.trend)
        assertNotNull(vm.state.value.trendScale)

        vm.onEvent(PeriodicTableEvent.TrendSelected(TrendProperty.CATEGORY))
        assertEquals(TrendProperty.CATEGORY, vm.state.value.trend)
        assertNull(vm.state.value.trendScale)
    }

    @Test
    fun `selecting elements requests volumes and the fourth replaces the oldest`() = runTest {
        val vm = viewModel()
        for (z in listOf(6, 8, 11)) {
            vm.onEvent(PeriodicTableEvent.ElementSelected(z))
            assertEquals(AtomWindowRequest(open = z, close = null), vm.state.value.windowRequest)
            vm.onEvent(PeriodicTableEvent.WindowRequestHandled)
        }
        vm.onEvent(PeriodicTableEvent.ElementSelected(26))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(AtomWindowRequest(open = 26, close = 6), state.windowRequest)
        assertEquals(6, state.replacedNotice?.atomicNumber)
        assertEquals(listOf(8, 11, 26), state.openAtoms)
    }

    @Test
    fun `selecting an unknown element does nothing`() = runTest {
        val vm = viewModel()
        vm.onEvent(PeriodicTableEvent.ElementSelected(999))
        assertNull(vm.state.value.windowRequest)
    }

    @Test
    fun `language choice is shared through the session and closes the menu`() = runTest {
        val vm = viewModel()
        vm.onEvent(PeriodicTableEvent.LanguageMenuVisible(true))
        vm.onEvent(PeriodicTableEvent.LanguageSelected(AppLanguage.EN))
        advanceUntilIdle()

        assertEquals(AppLanguage.EN, session.language.value)
        assertEquals(AppLanguage.EN, vm.state.value.language)
        assertFalse(vm.state.value.languageMenuOpen)
    }
}
