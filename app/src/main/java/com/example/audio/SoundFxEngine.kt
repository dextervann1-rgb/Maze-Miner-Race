package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.engine.Biome
import com.example.engine.BlockKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Adaptive Procedural Soundtrack & Dynamic Sound Effects Engine.
 * - Real-time biome-adaptive synthwave subterranean soundtrack via streaming AudioTrack
 * - Dynamic intensity scaling (tempo & bright lead layer accelerate during low-timer panic or combos)
 * - Material-specific & combo-pitch-scaled sound effects + haptic impulses
 */
class SoundFxEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopSoundtrack()
        }

    @Volatile
    var hapticsEnabled: Boolean = true

    @Volatile
    private var activeBiome: Biome = Biome.CAVERN

    @Volatile
    private var urgencyLevel: Float = 0f // 0f normal -> 1f urgent (<30s timer or high combo)

    @Volatile
    private var musicPaused: Boolean = false

    @Volatile
    private var duckUntilMs: Long = 0L

    private fun triggerAudioDucking(durationMs: Long = 440L) {
        val target = System.currentTimeMillis() + durationMs
        if (target > duckUntilMs) duckUntilMs = target
    }

    fun updateSoundtrackContext(biome: Biome, timerRemaining: Int, combo: Int, paused: Boolean) {
        activeBiome = biome
        musicPaused = paused
        val timerUrgency = if (timerRemaining in 1..30) (31 - timerRemaining) / 30f else 0f
        val comboEnergy = (combo.coerceAtMost(8) / 8f) * 0.45f
        urgencyLevel = (timerUrgency + comboEnergy).coerceIn(0f, 1f)
    }

    fun startSoundtrack(biome: Biome) {
        activeBiome = biome
        musicPaused = false
        if (!musicEnabled) return
        if (musicJob?.isActive == true) return

        musicJob = scope.launch {
            runCatching {
                val sampleRate = 22050
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                var stepIndex = 0
                var lpfState = 0.0
                var subPhase = 0.0
                var padPhase = 0.0
                var duckGain = 1.0

                try {
                    while (isActive && musicEnabled) {
                        if (musicPaused) {
                            val silence = ShortArray(sampleRate / 10)
                            track.write(silence, 0, silence.size)
                            continue
                        }
                        val theme = themeForBiome(activeBiome)
                        val bpmBoost = 1.0 + (urgencyLevel * 0.24)
                        val stepDurationSec = (60.0 / (theme.bpm * bpmBoost)) / 2.0 // 8th notes
                        val samplesPerStep = (sampleRate * stepDurationSec).toInt().coerceIn(1800, 12000)
                        val buf = ShortArray(samplesPerStep)

                        val bassFreq = theme.bassPattern[stepIndex % theme.bassPattern.size]
                        val arpFreq = theme.arpPattern[stepIndex % theme.arpPattern.size]
                        val subFreq = bassFreq * 0.5
                        val padFreq = bassFreq * 1.498 // warm sustained fifth pad
                        val hasKick = stepIndex % 4 == 0
                        val hasLead = urgencyLevel > 0.25f && (stepIndex % 2 == 0)
                        val leadFreq = arpFreq * 2.0

                        // Slow warm low-pass filter sweep across 16 steps + opens up during urgency
                        val sweepLfo = 0.5 + 0.5 * sin(2.0 * PI * (stepIndex % 16) / 16.0)
                        val cutoffAlpha = (0.14 + 0.24 * sweepLfo + 0.22 * urgencyLevel).coerceIn(0.10, 0.72)

                        val isDucked = System.currentTimeMillis() < duckUntilMs
                        val targetDuck = if (isDucked) 0.30 else 1.0

                        for (i in 0 until samplesPerStep) {
                            val t = i.toDouble() / sampleRate
                            val norm = i.toDouble() / samplesPerStep

                            // Smooth audio ducking envelope (fast attack, gentle release)
                            val duckRate = if (targetDuck < duckGain) 0.012 else 0.0025
                            duckGain += (targetDuck - duckGain) * duckRate

                            // Continuous warm sub-bass & fifth pad (phase-continuous across steps)
                            subPhase += 2.0 * PI * subFreq / sampleRate
                            padPhase += 2.0 * PI * padFreq / sampleRate
                            if (subPhase > 2.0 * PI) subPhase -= 2.0 * PI
                            if (padPhase > 2.0 * PI) padPhase -= 2.0 * PI
                            val warmPad = sin(subPhase) * 0.085 + sin(padPhase) * 0.035

                            // Subterranean bass pulse
                            val bassEnv = exp(-4.0 * norm)
                            val bassWave = sin(2.0 * PI * bassFreq * t) + 0.28 * sin(4.0 * PI * bassFreq * t)

                            // Crystalline arpeggio voice
                            val arpEnv = exp(-7.5 * norm)
                            val arpWave = sin(2.0 * PI * arpFreq * t)

                            // Soft percussive pulse on downbeats
                            val kick = if (hasKick) {
                                val kickFreq = 115.0 * exp(-28.0 * t) + 38.0
                                sin(2.0 * PI * kickFreq * t) * exp(-18.0 * norm) * 0.40
                            } else 0.0

                            // Dynamic intensity lead shimmer
                            val lead = if (hasLead) {
                                sin(2.0 * PI * leadFreq * t) * exp(-9.0 * norm) * 0.13 * urgencyLevel
                            } else 0.0

                            // Apply sweeping 1-pole low-pass filter to synth layers for analog warmth
                            val synthRaw = bassWave * bassEnv * 0.095 + arpWave * arpEnv * 0.075 + lead
                            lpfState += cutoffAlpha * (synthRaw - lpfState)

                            val mixed = ((lpfState + warmPad + kick * 0.105) * duckGain)
                                .coerceIn(-0.85, 0.85)
                            buf[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                        }

                        track.write(buf, 0, buf.size)
                        stepIndex++
                    }
                } finally {
                    runCatching {
                        track.stop()
                        track.release()
                    }
                }
            }
        }
    }

    fun stopSoundtrack() {
        musicJob?.cancel()
        musicJob = null
    }

    // --- Dynamic Material & Combo-Scaled Sound Effects ---

    fun playStep() {
        playTone(freqHz = 300.0, durationMs = 32, volume = 0.12f)
    }

    fun playIceSlide() {
        playSweep(startHz = 720.0, endHz = 1120.0, durationMs = 95, volume = 0.18f)
    }

    fun playMineHit(kind: BlockKind = BlockKind.STONE, combo: Int = 0) {
        val baseFreq = when (kind) {
            BlockKind.DIRT -> 175.0
            BlockKind.STONE -> 235.0
            BlockKind.IRON -> 420.0
            BlockKind.CRYSTAL -> 690.0
            BlockKind.GAMBLE -> 510.0
        }
        val pitchBoost = (combo.coerceAtMost(10)) * 22.0
        playTone(freqHz = baseFreq + pitchBoost, durationMs = 55, volume = 0.24f)
        vibrate(18L)
    }

    fun playBlockBreak(kind: BlockKind = BlockKind.STONE, combo: Int = 0) {
        val baseFreq = when (kind) {
            BlockKind.DIRT -> 260.0
            BlockKind.STONE -> 340.0
            BlockKind.IRON -> 540.0
            BlockKind.CRYSTAL -> 880.0
            BlockKind.GAMBLE -> 620.0
        }
        val boost = (combo.coerceAtMost(10)) * 26.0
        playSequence(
            listOf((baseFreq + boost) to 45, (baseFreq * 1.25 + boost) to 75),
            volume = 0.26f
        )
        vibrate(28L)
    }

    fun playGem(combo: Int = 0) {
        val mult = 1.0 + (combo.coerceAtMost(6) * 0.04)
        playSequence(listOf((784.0 * mult) to 55, (1046.5 * mult) to 90), volume = 0.24f)
        vibrate(22L)
    }

    fun playPowerup() {
        triggerAudioDucking(340L)
        playSequence(listOf(523.25 to 55, 659.25 to 55, 783.99 to 60, 1046.5 to 110), volume = 0.26f)
        vibrate(36L)
    }

    fun playSonar() {
        triggerAudioDucking(420L)
        playSweep(startHz = 960.0, endHz = 1480.0, durationMs = 175, volume = 0.22f)
        vibrate(32L)
    }

    fun playExplosion() {
        triggerAudioDucking(520L)
        playSweep(startHz = 170.0, endHz = 48.0, durationMs = 185, volume = 0.36f)
        vibrate(60L)
    }

    fun playHazard() {
        triggerAudioDucking(300L)
        playSequence(listOf(245.0 to 70, 165.0 to 120), volume = 0.28f)
        vibrate(46L)
    }

    fun playWin() {
        triggerAudioDucking(750L)
        playSequence(
            listOf(523.25 to 75, 659.25 to 75, 783.99 to 85, 1046.5 to 120, 1318.5 to 220),
            volume = 0.30f
        )
        vibrate(85L)
    }

    fun playLose() {
        triggerAudioDucking(650L)
        playSequence(listOf(330.0 to 110, 261.6 to 120, 185.0 to 240), volume = 0.28f)
        vibrate(95L)
    }

    private data class BiomeTheme(
        val bpm: Double,
        val bassPattern: DoubleArray,
        val arpPattern: DoubleArray
    )

    private fun themeForBiome(biome: Biome): BiomeTheme = when (biome) {
        Biome.DESERT -> BiomeTheme(
            bpm = 104.0,
            bassPattern = doubleArrayOf(110.0, 110.0, 116.54, 110.0, 130.81, 116.54, 110.0, 98.0),
            arpPattern = doubleArrayOf(220.0, 233.08, 277.18, 329.63, 277.18, 233.08, 220.0, 196.0)
        )
        Biome.ICE -> BiomeTheme(
            bpm = 112.0,
            bassPattern = doubleArrayOf(130.81, 130.81, 146.83, 164.81, 196.0, 164.81, 146.83, 130.81),
            arpPattern = doubleArrayOf(392.0, 440.0, 523.25, 659.25, 587.33, 523.25, 440.0, 392.0)
        )
        Biome.VOLCANO -> BiomeTheme(
            bpm = 126.0,
            bassPattern = doubleArrayOf(82.41, 82.41, 98.0, 82.41, 110.0, 98.0, 92.5, 82.41),
            arpPattern = doubleArrayOf(164.81, 196.0, 220.0, 246.94, 220.0, 196.0, 185.0, 164.81)
        )
        Biome.CAVERN -> BiomeTheme(
            bpm = 108.0,
            bassPattern = doubleArrayOf(98.0, 98.0, 116.54, 130.81, 146.83, 130.81, 116.54, 98.0),
            arpPattern = doubleArrayOf(293.66, 349.23, 392.0, 466.16, 392.0, 349.23, 293.66, 261.63)
        )
        Biome.RUINS -> BiomeTheme(
            bpm = 116.0,
            bassPattern = doubleArrayOf(110.0, 123.47, 130.81, 146.83, 130.81, 123.47, 110.0, 98.0),
            arpPattern = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 293.66, 261.63, 246.94, 220.0)
        )
    }

    private fun vibrate(durationMs: Long) {
        if (!hapticsEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        if (!soundEnabled) return
        playSequence(listOf(freqHz to durationMs), volume)
    }

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float) {
        if (!soundEnabled) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = (sampleRate * durationMs) / 1000
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = startHz + (endHz - startHz) * progress
                    phase += 2.0 * PI * freq / sampleRate
                    val env = exp(-4.0 * progress)
                    val sample = sin(phase) * env * volume
                    buffer[i] = (sample * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                playStaticBuffer(buffer, sampleRate, durationMs.toLong())
            }
        }
    }

    private fun playSequence(notes: List<Pair<Double, Int>>, volume: Float) {
        if (!soundEnabled) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (_, ms) -> (sampleRate * ms) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sampleRate * ms) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val env = exp(-4.5 * (i.toDouble() / count))
                        val sample = sin(2.0 * PI * freq * t) * env * volume
                        buffer[offset + i] = (sample * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                    offset += count
                }
                val totalMs = notes.sumOf { it.second }.toLong()
                playStaticBuffer(buffer, sampleRate, totalMs)
            }
        }
    }

    private suspend fun playStaticBuffer(buffer: ShortArray, sampleRate: Int, durationMs: Long) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        kotlinx.coroutines.delay(durationMs + 40L)
        track.stop()
        track.release()
    }
}
