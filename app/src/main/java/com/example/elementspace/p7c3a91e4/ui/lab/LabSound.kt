package com.example.elementspace.p7c3a91e4.ui.lab

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.LabEffect

/** How loud each looping sound should be for a set of effects, 0..1. Plain data so it can be unit-tested. */
data class LabMix(val flame: Float = 0f, val sparks: Float = 0f, val fizz: Float = 0f) {
    fun times(volume: Float) = LabMix(flame * volume, sparks * volume, fizz * volume)

    companion object {
        /** Master volume of the three stops: low, medium (default), high. */
        val VOLUME = floatArrayOf(0.5f, 0.8f, 1f)

        fun of(effects: List<LabEffect>, power: Float): LabMix {
            var flame = 0f
            var sparks = 0f
            var fizz = 0f
            for (effect in effects) {
                when (effect) {
                    is LabEffect.Flame -> flame = maxOf(flame, (effect.size * power).coerceIn(0.45f, 1f))
                    is LabEffect.Glow -> flame = maxOf(flame, 1f)
                    is LabEffect.Sparks -> sparks = maxOf(sparks, 0.6f + effect.rate / 80f)
                    is LabEffect.Bubbles -> fizz = maxOf(fizz, 0.6f + effect.rate / 60f)
                    is LabEffect.Skater -> fizz = maxOf(fizz, 1f)
                    else -> Unit
                }
            }
            return LabMix(flame.coerceIn(0f, 1f), sparks.coerceIn(0f, 1f), fizz.coerceIn(0f, 1f))
        }
    }
}

/**
 * The bench's sounds: three loops (flame, sparks, fizz) cut from CC0 recordings (v1.10.1; sources in
 * the factory's apps/element-space/build/sound-sources.md, cut by polish/M10-sounds.py).
 */
class LabSound(context: Context) {
    private val pool =
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
    private val flame = pool.load(context, R.raw.lab_flame, 1)
    private val sparks = pool.load(context, R.raw.lab_sparks, 1)
    private val fizz = pool.load(context, R.raw.lab_fizz, 1)
    private val loaded = HashSet<Int>()
    private val streams = HashMap<Int, Int>()
    private var mix = LabMix()
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (status == 0) {
                loaded += sample
                apply()
            }
        }
    }

    /** Sets what should be heard now. Muted is simply an empty mix. */
    fun set(next: LabMix) {
        if (released) return
        mix = next
        apply()
    }

    private fun apply() {
        loop(flame, mix.flame)
        loop(sparks, mix.sparks)
        loop(fizz, mix.fizz)
    }

    private fun loop(sample: Int, volume: Float) {
        val stream = streams[sample]
        when {
            volume <= 0f -> if (stream != null) {
                pool.stop(stream)
                streams.remove(sample)
            }
            stream != null -> pool.setVolume(stream, volume, volume)
            sample in loaded -> pool.play(sample, volume, volume, 1, -1, 1f).takeIf { it != 0 }?.let { streams[sample] = it }
        }
    }

    fun release() {
        released = true
        runCatching { pool.release() }
    }
}

/**
 * Sound for lab effects shown outside the bench (a lit match in the atom Volume). Follows the lab's
 * own sound switch and volume, is silent in the background, and creates no player until something
 * actually sounds.
 */
@androidx.compose.runtime.Composable
fun AmbientLabSound(effects: List<LabEffect>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val holder = androidx.compose.runtime.remember { arrayOfNulls<LabSound>(1) }
    val foreground = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) foreground.value = false
                if (event == androidx.lifecycle.Lifecycle.Event.ON_START) foreground.value = true
            }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            holder[0]?.release()
            holder[0] = null
        }
    }
    androidx.compose.runtime.LaunchedEffect(effects, foreground.value) {
        val prefs = context.getSharedPreferences("element_space", Context.MODE_PRIVATE)
        val audible = effects.isNotEmpty() && foreground.value && prefs.getBoolean("sound", true)
        val mix = if (audible) LabMix.of(effects, 1f).times(LabMix.VOLUME[prefs.getInt("volume", 1).coerceIn(0, 2)]) else LabMix()
        if (mix == LabMix() && holder[0] == null) return@LaunchedEffect
        val sound = holder[0] ?: LabSound(context.applicationContext).also { holder[0] = it }
        sound.set(mix)
    }
}
