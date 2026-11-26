package com.example.elementspace.p7c3a91e4.ui.lab

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.LabEffect

/** How loud each looping sound should be for a set of effects, 0..1. Plain data so it can be unit-tested. */
data class LabMix(val flame: Float = 0f, val sparks: Float = 0f, val fizz: Float = 0f) {
    companion object {
        fun of(effects: List<LabEffect>, power: Float): LabMix {
            var flame = 0f
            var sparks = 0f
            var fizz = 0f
            for (effect in effects) {
                when (effect) {
                    is LabEffect.Flame -> flame = maxOf(flame, 0.35f * effect.size * power)
                    is LabEffect.Glow -> flame = maxOf(flame, 0.3f + 0.25f * effect.size)
                    is LabEffect.Sparks -> sparks = maxOf(sparks, 0.25f + effect.rate / 80f)
                    is LabEffect.Bubbles -> fizz = maxOf(fizz, 0.2f + effect.rate / 60f)
                    is LabEffect.Skater -> fizz = maxOf(fizz, 0.7f)
                    else -> Unit
                }
            }
            return LabMix(flame.coerceIn(0f, 1f), sparks.coerceIn(0f, 1f), fizz.coerceIn(0f, 1f))
        }
    }
}

/**
 * The bench's sounds (v1.10.0): three loops (flame, sparks, fizz) and one shot when something catches
 * fire. The clips are synthesized noise (factory polish/M10-sounds.py), so there is nothing to license.
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
    private val ignite = pool.load(context, R.raw.lab_ignite, 1)
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
        val caughtFire = next.flame > 0f && mix.flame == 0f
        mix = next
        apply()
        if (caughtFire && ignite in loaded) pool.play(ignite, 0.6f, 0.6f, 1, 0, 1f)
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
