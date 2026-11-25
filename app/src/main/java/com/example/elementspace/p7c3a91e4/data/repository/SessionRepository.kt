package com.example.elementspace.p7c3a91e4.data.repository

import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Result of asking to open an atom Volume. */
data class OpenAtomResult(
    /** True when the element already had a Volume, so the existing one is focused instead. */
    val alreadyOpen: Boolean,
    /** Atomic number whose Volume was evicted to respect the limit, or null. */
    val replaced: Int?,
)

/** What the molecule Volume shows: [id], and [compare] beside it when comparing. */
data class MoleculeView(val id: String, val compare: String? = null)

/** App-wide state shared by the main window and every atom Volume. */
interface SessionRepository {
    val language: StateFlow<AppLanguage>

    /** Atomic numbers with an open Volume, least recently used first. */
    val openAtoms: StateFlow<List<Int>>

    fun setLanguage(language: AppLanguage)

    fun openAtom(atomicNumber: Int): OpenAtomResult

    /** Marks a Volume as just used so it is the last to be replaced. */
    fun touchAtom(atomicNumber: Int)

    fun closeAtom(atomicNumber: Int)

    fun closeAll()

    /** Id of the reaction shown in the single reaction Volume, or null when it is closed. */
    val openReaction: StateFlow<String?>

    /** Shows [id] in the reaction Volume; returns the id it replaced, or null. */
    fun showReaction(id: String): String?

    fun closeReaction()

    /** The single molecule Volume's content, or null when it is closed. */
    val openMolecule: StateFlow<MoleculeView?>

    /** Shows molecule [id] (leaving any comparison). */
    fun showMolecule(id: String)

    /** Puts [id] beside the current molecule; null goes back to the single molecule. */
    fun compareMolecule(id: String?)

    fun closeMolecule()
}

class InMemorySessionRepository(initialLanguage: AppLanguage = AppLanguage.ZH) : SessionRepository {
    private val _language = MutableStateFlow(initialLanguage)
    override val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _openAtoms = MutableStateFlow<List<Int>>(emptyList())
    override val openAtoms: StateFlow<List<Int>> = _openAtoms.asStateFlow()

    override fun setLanguage(language: AppLanguage) {
        _language.value = language
    }

    override fun openAtom(atomicNumber: Int): OpenAtomResult {
        val current = _openAtoms.value
        if (atomicNumber in current) {
            touchAtom(atomicNumber)
            return OpenAtomResult(alreadyOpen = true, replaced = null)
        }
        val replaced = if (current.size >= MAX_OPEN_ATOMS) current.first() else null
        _openAtoms.value = current.filter { it != replaced } + atomicNumber
        return OpenAtomResult(alreadyOpen = false, replaced = replaced)
    }

    override fun touchAtom(atomicNumber: Int) {
        _openAtoms.update { list ->
            if (atomicNumber in list) list.filter { it != atomicNumber } + atomicNumber else list
        }
    }

    override fun closeAtom(atomicNumber: Int) {
        _openAtoms.update { list -> list.filter { it != atomicNumber } }
    }

    override fun closeAll() {
        _openAtoms.value = emptyList()
        _openReaction.value = null
        _openMolecule.value = null
    }

    private val _openMolecule = MutableStateFlow<MoleculeView?>(null)
    override val openMolecule: StateFlow<MoleculeView?> = _openMolecule.asStateFlow()

    override fun showMolecule(id: String) {
        _openMolecule.value = MoleculeView(id)
    }

    override fun compareMolecule(id: String?) {
        _openMolecule.update { current -> current?.copy(compare = id?.takeIf { it != current.id }) }
    }

    override fun closeMolecule() {
        _openMolecule.value = null
    }

    private val _openReaction = MutableStateFlow<String?>(null)
    override val openReaction: StateFlow<String?> = _openReaction.asStateFlow()

    override fun showReaction(id: String): String? {
        val previous = _openReaction.value
        _openReaction.value = id
        return previous?.takeIf { it != id }
    }

    override fun closeReaction() {
        _openReaction.value = null
    }

    companion object {
        const val MAX_OPEN_ATOMS = 3

        /** Process-wide instance: windows live in one process and must share this state. */
        val shared: SessionRepository by lazy { InMemorySessionRepository() }
    }
}
