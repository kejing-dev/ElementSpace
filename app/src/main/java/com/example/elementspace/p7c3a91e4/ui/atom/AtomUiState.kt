package com.example.elementspace.p7c3a91e4.ui.atom

import com.example.elementspace.p7c3a91e4.domain.model.Allotrope
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.ElectronConfiguration
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.Isotope
import com.example.elementspace.p7c3a91e4.domain.model.Lattice
import com.example.elementspace.p7c3a91e4.domain.model.NucleusLayout
import com.example.elementspace.p7c3a91e4.domain.model.OrbitalCloud

data class AtomUiState(
    val element: Element? = null,
    val language: AppLanguage = AppLanguage.ZH,
    val view: AtomViewMode = AtomViewMode.SHELL,
    /** Advanced views and panel rows stay hidden until the user asks for them. */
    val advancedExpanded: Boolean = false,
    val isotopes: List<Isotope> = emptyList(),
    val isotopeIndex: Int = 0,
    val charge: Int = 0,
    val minCharge: Int = 0,
    val maxCharge: Int = 0,
    val configuration: ElectronConfiguration? = null,
    val allotropes: List<Allotrope> = emptyList(),
    val allotropeIndex: Int = 0,
    /** Subshell labels (e.g. "2p") switched off in the cloud view. */
    val hiddenSubshells: Set<String> = emptySet(),
    val paused: Boolean = false,
    /** Bumped to ask the 3D view to return to its starting orientation and scale. */
    val resetToken: Int = 0,
    // 3D content derived from the fields above; rebuilt by the ViewModel when they change.
    val nucleus: NucleusLayout? = null,
    val clouds: List<OrbitalCloud> = emptyList(),
    val lattice: Lattice? = null,
) {
    val isotope: Isotope?
        get() = isotopes.getOrNull(isotopeIndex)

    val protons: Int
        get() = element?.atomicNumber ?: 0

    val neutrons: Int
        get() = isotope?.neutrons(protons) ?: 0

    val electrons: Int
        get() = protons - charge

    val allotrope: Allotrope?
        get() = allotropes.getOrNull(allotropeIndex)

    /** Neutron difference against the most abundant isotope, for the "+2" badge. */
    val neutronDelta: Int
        get() = neutrons - (isotopes.firstOrNull()?.neutrons(protons) ?: neutrons)
}

sealed interface AtomEvent {
    data class ViewSelected(val view: AtomViewMode) : AtomEvent

    data object AdvancedToggled : AtomEvent

    data class IsotopeSelected(val index: Int) : AtomEvent

    /** +1 removes an electron, -1 adds one. */
    data class ChargeStepped(val delta: Int) : AtomEvent

    data class AllotropeStepped(val delta: Int) : AtomEvent

    data class AllotropeSelected(val index: Int) : AtomEvent

    data class SubshellToggled(val label: String) : AtomEvent

    data object PauseToggled : AtomEvent

    data object ResetView : AtomEvent

    /** Any interaction with the Volume, so it is the last to be replaced. */
    data object Touched : AtomEvent

    data object Closed : AtomEvent
}
