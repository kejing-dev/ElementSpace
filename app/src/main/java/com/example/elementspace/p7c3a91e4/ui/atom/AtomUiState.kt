package com.example.elementspace.p7c3a91e4.ui.atom

import com.example.elementspace.p7c3a91e4.domain.model.Allotrope
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.domain.model.ElectronConfiguration
import com.example.elementspace.p7c3a91e4.domain.model.Element
import com.example.elementspace.p7c3a91e4.domain.model.EverydayThing
import com.example.elementspace.p7c3a91e4.domain.model.Isotope
import com.example.elementspace.p7c3a91e4.domain.model.Lattice
import com.example.elementspace.p7c3a91e4.domain.model.LearningLevel
import com.example.elementspace.p7c3a91e4.domain.model.NucleusLayout
import com.example.elementspace.p7c3a91e4.domain.model.OrbitalCloud
import com.example.elementspace.p7c3a91e4.domain.model.Reaction
import com.example.elementspace.p7c3a91e4.domain.model.Molecule
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalRef
import com.example.elementspace.p7c3a91e4.domain.usecase.OrbitalSample

data class AtomUiState(
    val element: Element? = null,
    val language: AppLanguage = AppLanguage.ZH,
    val level: LearningLevel = LearningLevel.BASIC,
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
    /** Cloud view: every occupied orbital at once, or one orbital with its phase and nodes. */
    val singleOrbital: Boolean = true,
    val orbital: OrbitalRef? = null,
    val showPhase: Boolean = true,
    val showNodes: Boolean = true,
    val showAxes: Boolean = true,
    val orbitalSample: OrbitalSample? = null,
    /** Common reactions that contain this element (info card section). */
    val reactions: List<Reaction> = emptyList(),
    /** Everyday things that contain this element (v1.8.0), and the one shown in the Volume instead of the atom. */
    val things: List<EverydayThing> = emptyList(),
    val thing: EverydayThing? = null,
    /** Molecules that contain this element, grouped by carbon skeleton on the card. */
    val molecules: List<Molecule> = emptyList(),
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

enum class OrbitalOption { PHASE, NODES, AXES }

sealed interface AtomEvent {
    data class ViewSelected(val view: AtomViewMode) : AtomEvent

    data object AdvancedToggled : AtomEvent

    data class IsotopeSelected(val index: Int) : AtomEvent

    /** +1 removes an electron, -1 adds one. */
    data class ChargeStepped(val delta: Int) : AtomEvent

    data class AllotropeStepped(val delta: Int) : AtomEvent

    data class AllotropeSelected(val index: Int) : AtomEvent

    data class SubshellToggled(val label: String) : AtomEvent

    /** Shows an everyday thing in the Volume; null goes back to the atom. */
    data class ThingSelected(val id: String?) : AtomEvent

    /** "看里面" for a thing whose inside is one of this element's allotropes. */
    data class ThingAllotropeOpened(val index: Int) : AtomEvent

    data class CloudModeSelected(val single: Boolean) : AtomEvent

    /** A box of the orbital diagram; also switches to the single-orbital view. */
    data class OrbitalSelected(val ref: OrbitalRef) : AtomEvent

    data class OrbitalOptionToggled(val option: OrbitalOption) : AtomEvent


    /** Shows the reaction in the reaction Volume; the screen opens the window. */
    data class ReactionOpened(val id: String) : AtomEvent

    /** Shows the molecule in the molecule Volume; the screen opens the window. */
    data class MoleculeOpened(val id: String) : AtomEvent

    data object PauseToggled : AtomEvent

    data object ResetView : AtomEvent

    /** Any interaction with the Volume, so it is the last to be replaced. */
    data object Touched : AtomEvent

    data object Closed : AtomEvent
}
