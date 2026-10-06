package com.example.elementspace.p7c3a91e4.ui.navigation

import android.content.Context
import android.provider.Settings
import com.example.elementspace.p7c3a91e4.domain.model.AtomViewMode
import com.example.elementspace.p7c3a91e4.ui.atom.AtomEvent
import kotlinx.coroutines.delay

/**
 * Scripted walkthrough for recording demo videos on the emulator, where nobody can click.
 *
 * Off unless the global setting `elementspace_demo` holds a comma-separated list of atomic
 * numbers, e.g. `adb shell settings put global elementspace_demo 79,6`. The table opens those
 * atoms one after another; the first atom walks through shells, nucleus and cloud, the later
 * ones show their allotropes. Remove the setting afterwards: `settings delete global elementspace_demo`.
 */
object DemoMode {
    private const val SETTING = "elementspace_demo"
    const val SECOND_ATOM_DELAY_MS = 19_000L

    fun atoms(context: Context): List<Int> =
        Settings.Global.getString(context.contentResolver, SETTING)
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }

    /** Drives one open atom; returns at once when the atom is not part of the demo. */
    suspend fun runAtomScript(context: Context, atomicNumber: Int, hasAllotropes: Boolean, onEvent: (AtomEvent) -> Unit) {
        val index = atoms(context).indexOf(atomicNumber)
        if (index < 0) return
        if (index == 0) {
            delay(6_000)
            onEvent(AtomEvent.ViewSelected(AtomViewMode.NUCLEUS))
            delay(5_000)
            onEvent(AtomEvent.AdvancedToggled)
            onEvent(AtomEvent.ViewSelected(AtomViewMode.CLOUD))
        } else if (hasAllotropes) {
            delay(1_500)
            onEvent(AtomEvent.AdvancedToggled)
            onEvent(AtomEvent.ViewSelected(AtomViewMode.ALLOTROPE))
            repeat(2) {
                delay(4_000)
                onEvent(AtomEvent.AllotropeStepped(1))
            }
        }
    }
}
