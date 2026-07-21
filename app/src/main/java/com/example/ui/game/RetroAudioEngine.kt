package com.example.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.abs
import kotlin.math.PI

/**
 * A real-time, lightweight retro synthesizer using Android's native [AudioTrack].
 * Synthesizes 8-bit game sound effects dynamically on a background thread
 * to avoid blocking the main UI/rendering thread.
 */
object RetroAudioEngine {
    private const val SAMPLE_RATE = 22050 // Low rate for 8-bit arcade aesthetics

    private enum class WaveType {
        SINE,
        TRIANGLE,
        SAWTOOTH
    }

    /**
     * Synthesizes and plays a quick bird-flap/jump sound sweep.
     */
    fun playFlap(enabled: Boolean) {
        if (!enabled) return
        // Quick pitch-rising sweep
        playTone(
            startFreq = 280f,
            endFreq = 580f,
            durationMs = 70,
            type = WaveType.SINE,
            volume = 0.35f
        )
    }

    /**
     * Synthesizes and plays a classic double-tone coin/point collection chime.
     */
    fun playCoin(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val duration1 = 0.06f
                val duration2 = 0.14f
                val numSamples1 = (duration1 * sampleRate).toInt()
                val numSamples2 = (duration2 * sampleRate).toInt()
                val totalSamples = numSamples1 + numSamples2
                val buffer = ShortArray(totalSamples)

                // Tone 1: 987.77 Hz (B5 note)
                val freq1 = 987.77
                for (i in 0 until numSamples1) {
                    val t = i.toDouble() / sampleRate
                    buffer[i] = (sin(2.0 * PI * freq1 * t) * 32767.0 * 0.3f).toInt().toShort()
                }

                // Tone 2: 1318.51 Hz (E6 note, forming a perfect fourth/fifth interval)
                val freq2 = 1318.51
                for (i in 0 until numSamples2) {
                    val t = i.toDouble() / sampleRate
                    // Fade out volume slightly near the end
                    val fade = (numSamples2 - i).toDouble() / numSamples2
                    buffer[numSamples1 + i] = (sin(2.0 * PI * freq2 * t) * 32767.0 * 0.3f * fade).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Synthesizes and plays a descending buzzer/crash sound when hitting an obstacle or the ground.
     */
    fun playHit(enabled: Boolean) {
        if (!enabled) return
        // Deep descending crunchy sweep
        playTone(
            startFreq = 320f,
            endFreq = 50f,
            durationMs = 280,
            type = WaveType.SAWTOOTH,
            volume = 0.4f
        )
    }

    private fun playTone(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        type: WaveType,
        volume: Float
    ) {
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val numSamples = (durationMs.toFloat() / 1000f * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * progress

                    // Continuous phase accumulation to support sweeps perfectly without pops or clicks
                    phase += (2.0 * PI * currentFreq) / sampleRate
                    if (phase > 2.0 * PI) {
                        phase -= 2.0 * PI
                    }

                    val sampleValue = when (type) {
                        WaveType.SINE -> sin(phase)
                        WaveType.TRIANGLE -> {
                            val normPhase = phase / (2.0 * PI)
                            2.0 * abs(2.0 * (normPhase - floor(normPhase + 0.5))) - 1.0
                        }
                        WaveType.SAWTOOTH -> {
                            val normPhase = phase / (2.0 * PI)
                            2.0 * (normPhase - floor(normPhase)) - 1.0
                        }
                    }

                    // Dynamic fade-out window at the end to prevent clicking
                    val fadeOut = if (numSamples - i < 250) {
                        (numSamples - i).toFloat() / 250f
                    } else {
                        1.0f
                    }

                    buffer[i] = (sampleValue * 32767.0 * volume * fadeOut).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun playBuffer(buffer: ShortArray) {
        var audioTrack: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack.play()
            audioTrack.write(buffer, 0, buffer.size)

            // Block current worker thread until playback duration has passed
            val durationMs = (buffer.size.toFloat() / SAMPLE_RATE * 1000f).toLong()
            Thread.sleep(durationMs + 20)
            audioTrack.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
}
