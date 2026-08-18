package com.sambhavdwivedi.musicplayer.playback

import android.media.audiofx.Equalizer

object EqualizerController {

    private var equalizer: Equalizer? = null

    val isAvailable: Boolean
        get() = equalizer != null

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (_: Exception) {
            equalizer = null
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {
        }
    }

    fun numberOfBands(): Short = equalizer?.numberOfBands ?: 0

    fun bandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)

    fun centerFrequency(band: Short): Int = try {
        equalizer?.getCenterFreq(band) ?: 0
    } catch (_: Exception) {
        0
    }

    fun getBandLevel(band: Short): Short = try {
        equalizer?.getBandLevel(band) ?: 0
    } catch (_: Exception) {
        0
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (_: Exception) {
        }
    }

    fun presetCount(): Short = equalizer?.numberOfPresets ?: 0

    fun presetName(index: Short): String = try {
        equalizer?.getPresetName(index) ?: ""
    } catch (_: Exception) {
        ""
    }

    fun usePreset(index: Short) {
        try {
            equalizer?.usePreset(index)
        } catch (_: Exception) {
        }
    }
}
