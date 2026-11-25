package com.example.elementspace.p7c3a91e4.ui.navigation

/** Ids and bundle keys shared by the window declarations and the code that opens them. */
object WindowIds {
    /** Must match pico.spatial.windowcontainer.id in AndroidManifest.xml. */
    const val MAIN_WINDOW = "PeriodicTableWindow"
    const val ATOM_VOLUME = "AtomVolume"
    const val ARG_ATOMIC_NUMBER = "atomicNumber"

    /** One Volume per element: the tag makes the navigator reuse an already open one. */
    /** The single reaction Volume; what it shows comes from SessionRepository.openReaction. */
    const val REACTION_VOLUME = "ReactionVolume"
    const val REACTION_TAG = "reaction"

    /** The single molecule Volume; what it shows comes from SessionRepository.openMolecule. */
    const val MOLECULE_VOLUME = "MoleculeVolume"
    const val MOLECULE_TAG = "molecule"

    fun atomTag(atomicNumber: Int): String = "atom_$atomicNumber"
}
