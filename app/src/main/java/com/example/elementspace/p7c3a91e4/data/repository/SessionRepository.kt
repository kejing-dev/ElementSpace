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
    }

    companion object {
        const val MAX_OPEN_ATOMS = 3

        /** Process-wide instance: windows live in one process and must share this state. */
        val shared: SessionRepository by lazy { InMemorySessionRepository() }
    }
}
