package com.example.elementspace.p7c3a91e4.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.elementspace.p7c3a91e4.data.repository.InMemorySessionRepository
import com.example.elementspace.p7c3a91e4.ui.navigation.WindowIds
import com.pico.spatial.ui.platform.containers.SpatialNavigator
import kotlin.system.exitProcess

/**
 * Exit app (profile D26): close every atom Volume and the reaction Volume, remove the tasks, then
 * end the process so nothing keeps running in the background.
 */
object AppExit {
    private const val TAG = "ElementSpace"

    fun exit(context: Context, navigator: SpatialNavigator?) {
        Log.i(TAG, "EXIT close windows + finishAndRemoveTask + exitProcess")
        val session = InMemorySessionRepository.shared
        runCatching {
            session.openAtoms.value.forEach { navigator?.closeWindowContainer(WindowIds.ATOM_VOLUME, WindowIds.atomTag(it)) }
            navigator?.closeWindowContainer(WindowIds.REACTION_VOLUME, WindowIds.REACTION_TAG)
        }
        session.closeAll()
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { runCatching { it.finishAndRemoveTask() } }
        }
        Handler(Looper.getMainLooper()).postDelayed({ exitProcess(0) }, 400)
    }
}
