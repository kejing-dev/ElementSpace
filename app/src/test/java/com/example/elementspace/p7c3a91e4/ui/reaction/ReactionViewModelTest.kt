package com.example.elementspace.p7c3a91e4.ui.reaction

import com.example.elementspace.p7c3a91e4.data.repository.BundledReactionRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class ReactionViewModelTest {
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

    private fun viewModel() = ReactionViewModel(BundledReactionRepository(), session, stepMillis = { _, _ -> 1_000L })

    @Test
    fun `shows nothing until a reaction is picked, then starts playing from step 1`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceTimeBy(10)
        assertNull(vm.state.value.reaction)
        session.showReaction("na_cl2")
        advanceTimeBy(10)
        assertEquals("na_cl2", vm.state.value.reaction?.id)
        assertEquals(0, vm.state.value.step)
        assertTrue(vm.state.value.playing)
    }

    @Test
    fun `auto play advances one step at a time and stops on the last`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showReaction("fe_cuso4")
        advanceTimeBy(1_100)
        assertEquals(1, vm.state.value.step)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.step)
        assertFalse(vm.state.value.playing)
    }

    @Test
    fun `next and previous stop playback and stay in range`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showReaction("h2_o2")
        advanceTimeBy(10)
        vm.onEvent(ReactionEvent.Previous)
        assertEquals(0, vm.state.value.step)
        assertFalse(vm.state.value.playing)
        repeat(6) { vm.onEvent(ReactionEvent.Next) }
        assertEquals(3, vm.state.value.step)
        advanceTimeBy(5_000)
        assertEquals(3, vm.state.value.step)
    }

    @Test
    fun `play on the last step replays from the start`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showReaction("na_cl2")
        advanceTimeBy(10)
        vm.onEvent(ReactionEvent.StepSelected(3))
        vm.onEvent(ReactionEvent.PlayToggled)
        assertEquals(0, vm.state.value.step)
        assertTrue(vm.state.value.playing)
    }

    @Test
    fun `picking another reaction swaps the content and starts over`() = runTest(dispatcher) {
        val vm = viewModel()
        session.showReaction("na_cl2")
        advanceTimeBy(2_100)
        assertEquals(2, vm.state.value.step)
        assertEquals("na_cl2", session.showReaction("fe_cuso4"))
        advanceTimeBy(10)
        assertEquals("fe_cuso4", vm.state.value.reaction?.id)
        assertEquals(0, vm.state.value.step)
    }
}
