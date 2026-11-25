package com.example.elementspace.p7c3a91e4.ui.molecule

import com.example.elementspace.p7c3a91e4.data.repository.BundledMoleculeRepository
import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.ui.molecule.components.MoleculeMode
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoleculeViewModelTest {
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

    private fun viewModel() = MoleculeViewModel(BundledMoleculeRepository(), BundledReactionRepository(), session)

    @Test
    fun `shows the molecule the session names, with its partner and reactions`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.molecule)
        session.showMolecule("ethanol")
        advanceUntilIdle()
        assertEquals("ethanol", vm.state.value.molecule?.id)
        assertEquals("ether", vm.state.value.partner?.id)
        assertEquals(listOf("esterification"), vm.state.value.reactions.map { it.id })
    }

    @Test
    fun `compare puts the partner beside it, flags isomers, and goes back`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showMolecule("butane")
        advanceUntilIdle()
        vm.onEvent(MoleculeEvent.CompareOpened("isobutane"))
        advanceUntilIdle()
        assertEquals("isobutane", vm.state.value.compare?.id)
        assertTrue(vm.state.value.isomers)
        vm.onEvent(MoleculeEvent.CompareClosed)
        advanceUntilIdle()
        assertFalse(vm.state.value.comparing)
        // Picking another molecule leaves any comparison.
        vm.onEvent(MoleculeEvent.CompareOpened("isobutane"))
        session.showMolecule("benzene")
        advanceUntilIdle()
        assertNull(vm.state.value.compare)
    }

    @Test
    fun `toggles and reset`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showMolecule("methane")
        advanceUntilIdle()
        val token = vm.state.value.resetToken
        vm.onEvent(MoleculeEvent.ModeSelected(MoleculeMode.SPACE_FILLING))
        vm.onEvent(MoleculeEvent.AnglesToggled)
        vm.onEvent(MoleculeEvent.ResetView)
        assertEquals(MoleculeMode.SPACE_FILLING, vm.state.value.mode)
        assertFalse(vm.state.value.showAngles)
        assertEquals(token + 1, vm.state.value.resetToken)
    }

    @Test
    fun `opening a related reaction shows it in the reaction Volume`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showMolecule("ethylene")
        advanceUntilIdle()
        vm.onEvent(MoleculeEvent.ReactionOpened("c2h4_br2"))
        assertEquals("c2h4_br2", session.openReaction.value)
    }
}
