package com.example.elementspace.p7c3a91e4.ui.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.elementspace.p7c3a91e4.ui.reaction.ReactionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One debug command: `cmd` plus an optional argument. */
data class DebugCommand(val cmd: String, val arg: String?) {
    val number: Int?
        get() = arg?.toIntOrNull()

    fun reactionEvent(): ReactionEvent? =
        when (cmd) {
            "step" -> number?.let { ReactionEvent.StepSelected(it - 1) }
            "next" -> ReactionEvent.Next
            "prev" -> ReactionEvent.Previous
            "replay" -> ReactionEvent.Replay
            "play" -> ReactionEvent.PlayToggled
            "rview" -> ReactionEvent.ViewToggled
            else -> null
        }
}

/**
 * Debug broadcast for QA screenshots (debuggable builds only). Usage, see AGENTS.md:
 * `adb shell am broadcast -a com.example.elementspace.p7c3a91e4.DEBUG --es cmd reaction --es arg na_cl2`
 * Commands: atom <Z>, aview <view>, cloud whole|single, orbital 3d:2, phase, nodes, axes, reaction <id>, molecule <id>, compare <id|none>, mmode ball|fill, angles, groups, step <n>, next, prev, replay, play, settings, howto, exit (shows the confirm), exit-yes (= [退出]), reset, lang zh|en.
 */
object DebugBus {
    const val ACTION = "com.example.elementspace.p7c3a91e4.DEBUG"
    private const val TAG = "ElementSpaceDebug"
    private val flow = MutableSharedFlow<DebugCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<DebugCommand> = flow.asSharedFlow()

    fun send(command: DebugCommand) {
        Log.i(TAG, "DEBUG ${command.cmd} ${command.arg.orEmpty()}")
        flow.tryEmit(command)
    }

    fun register(context: Context) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val cmd = intent.getStringExtra("cmd") ?: return
                    send(DebugCommand(cmd, intent.getStringExtra("arg")))
                }
            }
        context.registerReceiver(receiver, IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
    }

    /** Runs [block] for every command while the calling composable is on screen. */
    @Composable
    fun Collect(block: (DebugCommand) -> Unit) {
        val current by rememberUpdatedState(block)
        LaunchedEffect(Unit) { commands.collect { current(it) } }
    }
}
