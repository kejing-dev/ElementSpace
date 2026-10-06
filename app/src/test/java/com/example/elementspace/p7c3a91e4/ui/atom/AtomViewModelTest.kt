package com.example.elementspace.p7c3a91e4.ui.atom

import com.example.elementspace.p7c3a91e4.data.repository.BundledElementRepository
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildAllotropeLatticeUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.BuildNucleusLayoutUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.ElectronConfigurationUseCase
import com.example.elementspace.p7c3a91e4.domain.usecase.SampleOrbitalCloudUseCase
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
class AtomViewModelTest {
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

    private fun viewModel(atomicNumber: Int) =
        AtomViewModel(
            atomicNumber,
            BundledElementRepository(),
            session,
            ElectronConfigurationUseCase(),
            BuildNucleusLayoutUseCase(),
            SampleOrbitalCloudUseCase(),
            BuildAllotropeLatticeUseCase(),
        )

    @Test
    fun `carbon opens on the basic shell view as carbon 12`() = runTest {
        val state = viewModel(6).state.value
        assertEquals("C", state.element?.symbol)
        assertEquals(AtomViewMode.SHELL, state.view)
        assertFalse(state.advancedExpanded)
        assertEquals(Triple(6, 6, 6), Triple(state.protons, state.neutrons, state.electrons))
        assertEquals(listOf(2, 4), state.configuration?.shells)
        assertEquals(12, state.nucleus!!.protons.size + state.nucleus!!.neutrons.size)
    }

    @Test
    fun `switching to carbon 14 adds two neutrons and leaves electrons alone`() = runTest {
        val vm = viewModel(6)
        val index = vm.state.value.isotopes.indexOfFirst { it.massNumber == 14 }
        vm.onEvent(AtomEvent.IsotopeSelected(index))

        val state = vm.state.value
        assertEquals(8, state.neutrons)
        assertEquals(2, state.neutronDelta)
        assertEquals(6, state.electrons)
        assertEquals(8, state.nucleus!!.neutrons.size)
    }

    @Test
    fun `sodium ion drops the valence electron and charge is clamped`() = runTest {
        val vm = viewModel(11)
        vm.onEvent(AtomEvent.AdvancedToggled)
        vm.onEvent(AtomEvent.ChargeStepped(1))
        assertEquals(1, vm.state.value.charge)
        assertEquals(10, vm.state.value.electrons)
        assertEquals(listOf(2, 8), vm.state.value.configuration?.shells)

        vm.onEvent(AtomEvent.ChargeStepped(1)) // sodium has one valence electron: stays at +1
        assertEquals(1, vm.state.value.charge)
        vm.onEvent(AtomEvent.ChargeStepped(-5)) // sodium does not form anions here
        assertEquals(0, vm.state.value.charge)
    }

    @Test
    fun `advanced views build their geometry and collapsing returns to basics`() = runTest {
        val vm = viewModel(6)
        vm.onEvent(AtomEvent.AdvancedToggled)
        vm.onEvent(AtomEvent.ViewSelected(AtomViewMode.CLOUD))
        assertEquals(3, vm.state.value.clouds.size) // 1s 2s 2p
        assertNull(vm.state.value.lattice)

        vm.onEvent(AtomEvent.ViewSelected(AtomViewMode.ALLOTROPE))
        assertNotNull(vm.state.value.lattice)
        vm.onEvent(AtomEvent.AllotropeStepped(-1)) // wraps to the last allotrope
        assertEquals(2, vm.state.value.allotropeIndex)

        vm.onEvent(AtomEvent.AdvancedToggled)
        assertEquals(AtomViewMode.SHELL, vm.state.value.view)
        assertFalse(vm.state.value.advancedExpanded)
        assertTrue(vm.state.value.clouds.isEmpty())
    }

    @Test
    fun `subshell toggles pause and reset only touch view state`() = runTest {
        val vm = viewModel(8)
        vm.onEvent(AtomEvent.SubshellToggled("2p"))
        assertEquals(setOf("2p"), vm.state.value.hiddenSubshells)
        vm.onEvent(AtomEvent.SubshellToggled("2p"))
        assertTrue(vm.state.value.hiddenSubshells.isEmpty())

        vm.onEvent(AtomEvent.PauseToggled)
        vm.onEvent(AtomEvent.ResetView)
        assertTrue(vm.state.value.paused)
        assertEquals(1, vm.state.value.resetToken)
        assertEquals(8, vm.state.value.electrons)
    }

    @Test
    fun `unknown element leaves an empty state and out of range selections are ignored`() = runTest {
        assertNull(viewModel(500).state.value.element)

        val vm = viewModel(2)
        vm.onEvent(AtomEvent.IsotopeSelected(99))
        vm.onEvent(AtomEvent.AllotropeStepped(1)) // helium has no bundled structure
        assertEquals(0, vm.state.value.isotopeIndex)
        assertEquals(0, vm.state.value.allotropeIndex)
        assertEquals(0 to 0, vm.state.value.minCharge to vm.state.value.maxCharge)
    }

    @Test
    fun `language follows the session and closing frees the slot`() = runTest {
        session.openAtom(6)
        val vm = viewModel(6)
        session.setLanguage(AppLanguage.EN)
        advanceUntilIdle()
        assertEquals(AppLanguage.EN, vm.state.value.language)

        vm.onEvent(AtomEvent.Closed)
        assertTrue(session.openAtoms.value.isEmpty())
    }
}
