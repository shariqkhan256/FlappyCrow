package com.example.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.PI

/**
 * A real-time, lightweight retro synthesizer using Android's native [AudioTrack].
 * Synthesizes casual, playful, arcade-style sound effects dynamically on a background thread
 * to avoid blocking the UI or requiring external audio assets.
 */
object RetroAudioEngine {
    private const val SAMPLE_RATE = 22050 // Clean, crisp sample rate for casual game SFX

    private enum class WaveType {
        SINE,
        TRIANGLE,
        SAWTOOTH
    }

    /**
     * Synthesizes and plays a cheerful, springy bird-flap / jump chirp sweep.
     */
    fun playFlap(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val durationMs = 85
                val numSamples = (durationMs.toFloat() / 1000f * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                val startFreq = 340.0
                val endFreq = 780.0

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    // Non-linear frequency sweep for a bouncy, springy bird chirp
                    val currentFreq = startFreq + (endFreq - startFreq) * (progress * progress)

                    phase += (2.0 * PI * currentFreq) / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI

                    // Mix sine and soft triangle for a warm, springy chirp
                    val sineVal = sin(phase)
                    val normPhase = phase / (2.0 * PI)
                    val triVal = 2.0 * abs(2.0 * (normPhase - floor(normPhase + 0.5))) - 1.0
                    val sampleValue = sineVal * 0.7 + triVal * 0.3

                    // Smooth envelope (attack + decay fade)
                    val attack = (progress / 0.15).coerceAtMost(1.0)
                    val decay = ((1.0 - progress) / 0.85).coerceAtLeast(0.0)
                    val envelope = attack * decay

                    buffer[i] = (sampleValue * 32767.0 * 0.38 * envelope).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Synthesizes and plays a bright, playful point-scoring ding when passing a pillar.
     */
    fun playPoint(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val duration1 = 0.05f
                val duration2 = 0.12f
                val numSamples1 = (duration1 * sampleRate).toInt()
                val numSamples2 = (duration2 * sampleRate).toInt()
                val totalSamples = numSamples1 + numSamples2
                val buffer = ShortArray(totalSamples)

                // High C6 (1046.5 Hz) -> High G6 (1567.98 Hz)
                val freq1 = 1046.5
                val freq2 = 1567.98

                for (i in 0 until numSamples1) {
                    val t = i.toDouble() / sampleRate
                    val env = (i.toDouble() / numSamples1).coerceAtMost(1.0)
                    buffer[i] = (sin(2.0 * PI * freq1 * t) * 32767.0 * 0.35 * env).toInt().toShort()
                }

                for (i in 0 until numSamples2) {
                    val t = i.toDouble() / sampleRate
                    val fade = (numSamples2 - i).toDouble() / numSamples2
                    buffer[numSamples1 + i] = (sin(2.0 * PI * freq2 * t) * 32767.0 * 0.4 * fade).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Synthesizes and plays a happy, sparkly multi-tone pickup chime for coins/gems/bugs.
     */
    fun playCoin(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val noteDuration = 0.055f
                val numSamplesPerNote = (noteDuration * sampleRate).toInt()
                val totalSamples = numSamplesPerNote * 3
                val buffer = ShortArray(totalSamples)

                // Playful major triad arpeggio: G5 (783.99 Hz) -> B5 (987.77 Hz) -> E6 (1318.51 Hz)
                val freqs = doubleArrayOf(783.99, 987.77, 1318.51)

                for (n in 0..2) {
                    val freq = freqs[n]
                    val startIndex = n * numSamplesPerNote
                    for (i in 0 until numSamplesPerNote) {
                        val t = i.toDouble() / sampleRate
                        val fade = if (n == 2) (numSamplesPerNote - i).toDouble() / numSamplesPerNote else 1.0
                        val sample = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                        buffer[startIndex + i] = (sample * 0.5 * 32767.0 * 0.32 * fade).toInt().toShort()
                    }
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Synthesizes and plays a funny, cartoon bonk/thud sound when colliding with pillars or ground.
     */
    fun playHit(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val durationMs = 240
                val numSamples = (durationMs.toFloat() / 1000f * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                // Initial sharp high thud frequency sweeping down to deep bass
                val startFreq = 480.0
                val endFreq = 65.0

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * (progress * progress)

                    phase += (2.0 * PI * currentFreq) / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI

                    // Mix sawtooth thud with sine bass impact
                    val normPhase = phase / (2.0 * PI)
                    val sawVal = 2.0 * (normPhase - floor(normPhase)) - 1.0
                    val sineVal = sin(phase)
                    val sampleVal = sawVal * 0.45 + sineVal * 0.55

                    // Rapid initial attack, gentle decay
                    val decay = (1.0 - progress) * (1.0 - progress)
                    buffer[i] = (sampleVal * 32767.0 * 0.42 * decay).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Synthesizes a soft, cheerful UI button click pop.
     */
    fun playButtonClick(enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = SAMPLE_RATE
                val durationMs = 35
                val numSamples = (durationMs.toFloat() / 1000f * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                val freq = 880.0 // A5 note
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = (numSamples - i).toDouble() / numSamples
                    buffer[i] = (sin(2.0 * PI * freq * t) * 32767.0 * 0.25 * decay).toInt().toShort()
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
            Thread.sleep(durationMs + 15)
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

