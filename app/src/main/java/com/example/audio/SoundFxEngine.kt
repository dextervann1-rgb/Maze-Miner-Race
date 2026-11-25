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
    private var urgencyLevel: Float = 0f // 0f calm -> 1f maximum race intensity

    @Volatile
    private var overdriveBoost: Boolean = false

    @Volatile
    private var finishClimax: Float = 0f // 0f -> 1f as player approaches Finish Portal

    @Volatile
    private var musicPaused: Boolean = false

    @Volatile
    private var duckUntilMs: Long = 0L

    private fun triggerAudioDucking(durationMs: Long = 440L) {
        val target = System.currentTimeMillis() + durationMs
        if (target > duckUntilMs) duckUntilMs = target
    }

    fun updateSoundtrackContext(
        biome: Biome,
        timerRemaining: Int,
        combo: Int,
        paused: Boolean,
        rivalDistToFinish: Int = 99,
        playerDistToFinish: Int = 99,
        overdriveActive: Boolean = false
    ) {
        activeBiome = biome
        musicPaused = paused
        overdriveBoost = overdriveActive

        val timerUrgency = when {
            timerRemaining in 1..25 -> 0.35f + ((26 - timerRemaining) / 25f) * 0.65f
            timerRemaining in 26..60 -> ((60 - timerRemaining) / 35f) * 0.35f
            else -> 0f
        }
        val comboEnergy = (combo.coerceAtMost(8) / 8f) * 0.42f + (if (overdriveActive) 0.24f else 0f)
        val rivalThreat = when {
            rivalDistToFinish <= 6 -> 0.48f
            rivalDistToFinish < playerDistToFinish -> 0.28f
            rivalDistToFinish <= 12 -> 0.16f
            else -> 0f
        }
        val climax = if (playerDistToFinish in 1..8) ((9 - playerDistToFinish) / 8f) * 0.52f else 0f
        finishClimax = climax

        urgencyLevel = maxOf(
            timerUrgency,
            (timerUrgency * 0.65f + comboEnergy + rivalThreat + climax * 0.65f)
        ).coerceIn(0f, 1f)
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
                var chordPhase = 0.0
                var duckGain = 1.0
                var noiseState = 0x13579BDF

                try {
                    while (isActive && musicEnabled) {
                        if (musicPaused) {
                            val silence = ShortArray(sampleRate / 10)
                            track.write(silence, 0, silence.size)
                            continue
                        }
                        val theme = themeForBiome(activeBiome)
                        val intensity = urgencyLevel.toDouble()
                        val climax = finishClimax.toDouble()
                        val fever = overdriveBoost

                        val bpmBoost = 1.0 + (intensity * 0.30) + (if (fever) 0.05 else 0.0)
                        val stepDurationSec = (60.0 / (theme.bpm * bpmBoost)) / 2.0 // 8th notes
                        val samplesPerStep = (sampleRate * stepDurationSec).toInt().coerceIn(1600, 12000)
                        val buf = ShortArray(samplesPerStep)

                        val bassFreq = theme.bassPattern[stepIndex % theme.bassPattern.size]
                        val arpFreq = theme.arpPattern[stepIndex % theme.arpPattern.size]
                        val subFreq = bassFreq * 0.5
                        val padFreq = bassFreq * 1.498 // warm sustained fifth pad
                        val chordFreq = arpFreq * 1.5 // heroic fifth/octave shimmer during Fever/Climax

                        val hasKick = (stepIndex % 4 == 0) || (intensity > 0.65 && stepIndex % 8 == 6)
                        val hasSnare = intensity > 0.42 && (stepIndex % 4 == 2)
                        val hasHat = intensity > 0.25 && (stepIndex % 2 == 1 || intensity > 0.60)
                        val doubleHat = intensity > 0.62
                        val hasLead = (intensity > 0.22 || fever || climax > 0.2) && (stepIndex % 2 == 0 || fever)
                        val leadFreq = if (fever && stepIndex % 4 == 3) arpFreq * 2.5 else arpFreq * 2.0

                        // Dynamic low-pass filter sweep opens progressively with intensity & fever
                        val sweepLfo = 0.5 + 0.5 * sin(2.0 * PI * (stepIndex % 16) / 16.0)
                        val cutoffAlpha = (0.14 + 0.22 * sweepLfo + 0.36 * intensity + (if (fever) 0.12 else 0.0))
                            .coerceIn(0.10, 0.84)

                        val isDucked = System.currentTimeMillis() < duckUntilMs
                        val targetDuck = if (isDucked) 0.28 else 1.0

                        for (i in 0 until samplesPerStep) {
                            val t = i.toDouble() / sampleRate
                            val norm = i.toDouble() / samplesPerStep

                            // Smooth audio ducking envelope (fast attack, gentle release)
                            val duckRate = if (targetDuck < duckGain) 0.014 else 0.0028
                            duckGain += (targetDuck - duckGain) * duckRate

                            // Fast deterministic white-noise generator for hi-hat & snare percussion
                            noiseState = noiseState xor (noiseState shl 13)
                            noiseState = noiseState xor (noiseState ushr 17)
                            noiseState = noiseState xor (noiseState shl 5)
                            val noise = (noiseState and 0xFFFF) / 32768.0 - 1.0

                            // Continuous warm sub-bass & fifth pad (phase-continuous across steps)
                            subPhase += 2.0 * PI * subFreq / sampleRate
                            padPhase += 2.0 * PI * padFreq / sampleRate
                            chordPhase += 2.0 * PI * chordFreq / sampleRate
                            if (subPhase > 2.0 * PI) subPhase -= 2.0 * PI
                            if (padPhase > 2.0 * PI) padPhase -= 2.0 * PI
                            if (chordPhase > 2.0 * PI) chordPhase -= 2.0 * PI
                            val climaxSwell = (climax * 0.055 + if (fever) 0.035 else 0.0) * sin(chordPhase)
                            val warmPad = sin(subPhase) * (0.085 + intensity * 0.025) +
                                sin(padPhase) * (0.035 + climax * 0.025) +
                                climaxSwell

                            // Driving bass pulse with richer overtones as intensity climbs
                            val bassEnv = exp(-3.8 * norm)
                            val bassWave = sin(2.0 * PI * bassFreq * t) +
                                (0.28 + 0.16 * intensity) * sin(4.0 * PI * bassFreq * t) +
                                (0.14 * intensity) * sin(6.0 * PI * bassFreq * t)

                            // Crystalline arpeggio voice
                            val arpEnv = exp(-7.2 * norm)
                            val arpWave = sin(2.0 * PI * arpFreq * t) +
                                0.15 * intensity * sin(4.0 * PI * arpFreq * t)

                            // Punchy driving kick drum
                            val kick = if (hasKick) {
                                val kickFreq = (120.0 + 25.0 * intensity) * exp(-28.0 * t) + 38.0
                                sin(2.0 * PI * kickFreq * t) * exp(-17.0 * norm) * (0.42 + 0.14 * intensity)
                            } else 0.0

                            // Crisp snare/clap backbeat at medium-high intensity
                            val snare = if (hasSnare) {
                                val snareTone = sin(2.0 * PI * 185.0 * t) * exp(-24.0 * norm) * 0.35
                                val snareNoise = noise * exp(-16.0 * norm) * 0.65
                                (snareTone + snareNoise) * (0.055 + 0.055 * intensity)
                            } else 0.0

                            // Metallic hi-hat / 16th-note pulse for adrenaline momentum
                            val hat = if (hasHat) {
                                val subNorm = if (doubleHat) (norm * 2.0) % 1.0 else norm
                                noise * exp(-28.0 * subNorm) * (0.028 + 0.035 * intensity)
                            } else 0.0

                            // Dynamic intensity lead shimmer
                            val leadLevel = (0.14 * intensity + if (fever) 0.08 else 0.0 + climax * 0.07)
                            val lead = if (hasLead) {
                                (sin(2.0 * PI * leadFreq * t) + 0.25 * sin(4.0 * PI * leadFreq * t)) *
                                    exp(-8.5 * norm) * leadLevel
                            } else 0.0

                            // Apply sweeping 1-pole low-pass filter to synth layers for analog warmth
                            val synthRaw = bassWave * bassEnv * (0.095 + 0.02 * intensity) +
                                arpWave * arpEnv * (0.075 + 0.02 * intensity) + lead
                            lpfState += cutoffAlpha * (synthRaw - lpfState)

                            val mixed = ((lpfState + warmPad + kick * 0.115 + snare + hat) * duckGain)
                                .coerceIn(-0.88, 0.88)
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
        playTone(freqHz = 290.0 + urgencyLevel * 35.0, durationMs = 28, volume = 0.11f)
    }

    fun playIceSlide() {
        playSweep(startHz = 740.0, endHz = 1280.0, durationMs = 105, volume = 0.19f)
    }

    fun playMineHit(kind: BlockKind = BlockKind.STONE, combo: Int = 0, remainingHp: Int = 1, maxHp: Int = 2) {
        val baseFreq = when (kind) {
            BlockKind.DIRT -> 170.0
            BlockKind.STONE -> 240.0
            BlockKind.IRON -> 440.0
            BlockKind.CRYSTAL -> 720.0
            BlockKind.GAMBLE -> 530.0
        }
        val damageFrac = (1.0 - (remainingHp.coerceAtLeast(0).toDouble() / maxHp.coerceAtLeast(1))).coerceIn(0.0, 1.0)
        val hpPitchRise = damageFrac * 145.0
        val pitchBoost = (combo.coerceAtMost(12)) * 24.0
        playImpactHit(
            baseHz = baseFreq + pitchBoost + hpPitchRise,
            metallic = kind == BlockKind.IRON || kind == BlockKind.CRYSTAL || kind == BlockKind.GAMBLE,
            durationMs = 64,
            volume = 0.27f
        )
        vibrate(14L)
    }

    /**
     * Crisp final-10-second race countdown tick with escalating urgency pitch in the final 3 seconds.
     */
    fun playTimerTick(secondsLeft: Int) {
        val urgent = secondsLeft in 1..3
        val hz = if (urgent) 1180.0 + (4 - secondsLeft) * 110.0 else 880.0
        playTone(freqHz = hz, durationMs = if (urgent) 55 else 34, volume = if (urgent) 0.24f else 0.16f)
        if (urgent) vibrate(16L)
    }

    fun playBlockBreak(kind: BlockKind = BlockKind.STONE, combo: Int = 0) {
        val baseFreq = when (kind) {
            BlockKind.DIRT -> 255.0
            BlockKind.STONE -> 345.0
            BlockKind.IRON -> 560.0
            BlockKind.CRYSTAL -> 900.0
            BlockKind.GAMBLE -> 640.0
        }
        val boost = (combo.coerceAtMost(12)) * 28.0
        val notes = if (kind == BlockKind.CRYSTAL || kind == BlockKind.GAMBLE) {
            listOf(
                (baseFreq + boost) to 38,
                (baseFreq * 1.25 + boost) to 46,
                (baseFreq * 1.5 + boost) to 72
            )
        } else {
            listOf(
                (baseFreq + boost) to 42,
                (baseFreq * 1.26 + boost) to 76
            )
        }
        playSequence(notes, volume = 0.28f)
        vibrate(30L)
    }

    fun playGem(combo: Int = 0) {
        val mult = 1.0 + (combo.coerceAtMost(8) * 0.045)
        playSequence(
            listOf((784.0 * mult) to 45, (1046.5 * mult) to 55, (1318.5 * mult) to 85),
            volume = 0.26f
        )
        vibrate(24L)
    }

    fun playPowerup() {
        triggerAudioDucking(360L)
        playSequence(
            listOf(523.25 to 50, 659.25 to 50, 783.99 to 55, 1046.5 to 65, 1318.5 to 110),
            volume = 0.28f
        )
        vibrate(38L)
    }

    /**
     * Crystalline arpeggio chime on Strategic Relay Beacon capture; escalates into a
     * grand portal-unlock harmonic chord with dual haptic pulse when the final beacon is synced.
     */
    fun playBeaconSync(portalUnlocked: Boolean) {
        if (portalUnlocked) {
            triggerAudioDucking(560L)
            playSequence(
                listOf(523.25 to 55, 659.25 to 55, 783.99 to 65, 1046.5 to 80, 1318.5 to 90, 1567.98 to 180),
                volume = 0.34f
            )
            vibrateWaveform(longArrayOf(0L, 30L, 35L, 60L), intArrayOf(0, 175, 0, 245))
        } else {
            triggerAudioDucking(340L)
            playSequence(
                listOf(523.25 to 45, 659.25 to 45, 783.99 to 55, 987.77 to 105),
                volume = 0.30f
            )
            vibrate(42L)
        }
    }

    /**
     * Tactile metallic anvil strike + ascending crystalline forge chord scaled by upgrade tier,
     * making every earned Forge upgrade feel weighty and celebratory.
     */
    fun playForgeUpgrade(newLevel: Int) {
        triggerAudioDucking(480L)
        val pitchBoost = 1.0 + (newLevel.coerceIn(1, 5) - 1) * 0.08
        playImpactHit(baseHz = 360.0 * pitchBoost, metallic = true, durationMs = 75, volume = 0.34f)
        playSequence(
            listOf(
                (440.0 * pitchBoost) to 55,
                (554.37 * pitchBoost) to 55,
                (659.25 * pitchBoost) to 65,
                (880.0 * pitchBoost) to 145
            ),
            volume = 0.30f
        )
        vibrateWaveform(longArrayOf(0L, 26L, 28L, 52L), intArrayOf(0, 170, 0, 240))
    }

    /**
     * Kinetic sonic whoosh + punchy burst for the active Miner Surge / Power Strike capability.
     */
    fun playDashSurge() {
        triggerAudioDucking(240L)
        playSweep(startHz = 220.0, endHz = 920.0, durationMs = 155, volume = 0.28f)
        vibrateWaveform(longArrayOf(0L, 22L, 20L, 30L), intArrayOf(0, 150, 0, 220))
    }

    fun playFeverIgnite() {
        triggerAudioDucking(380L)
        playSequence(
            listOf(440.0 to 45, 554.37 to 45, 659.25 to 50, 880.0 to 120),
            volume = 0.30f
        )
        vibrateWaveform(longArrayOf(0L, 24L, 32L, 44L), intArrayOf(0, 150, 0, 235))
    }

    fun playAbilityUnlock() {
        triggerAudioDucking(650L)
        playSequence(
            listOf(587.33 to 65, 739.99 to 65, 880.0 to 75, 1174.66 to 95, 1479.98 to 190),
            volume = 0.32f
        )
        vibrateWaveform(longArrayOf(0L, 28L, 35L, 38L, 35L, 70L), intArrayOf(0, 140, 0, 190, 0, 255))
    }

    fun playNextRace() {
        playSequence(listOf(440.0 to 45, 659.25 to 85), volume = 0.24f)
        vibrate(22L)
    }

    fun playSonar() {
        triggerAudioDucking(440L)
        playSweep(startHz = 920.0, endHz = 1560.0, durationMs = 195, volume = 0.25f)
        vibrate(34L)
    }

    fun playExplosion() {
        triggerAudioDucking(580L)
        playExplosionBlast(durationMs = 240, volume = 0.42f)
        vibrateWaveform(longArrayOf(0L, 52L, 36L, 68L), intArrayOf(0, 255, 0, 175))
    }

    /**
     * Escalating layered detonation for recursive TNT Chain Blasts (depth 1..3).
     * Deepens sub-bass punch, adds a bright golden overtone sting, and intensifies haptic shockwave.
     */
    fun playChainExplosion(depth: Int) {
        val tier = depth.coerceIn(1, 3)
        triggerAudioDucking(620L + tier * 90L)
        playExplosionBlast(durationMs = 250 + tier * 35, volume = (0.42f + tier * 0.05f).coerceAtMost(0.56f))
        val stingHz = 360.0 + tier * 135.0
        playSequence(
            listOf(stingHz to 42, (stingHz * 1.35) to 78),
            volume = 0.24f + tier * 0.03f
        )
        vibrateWaveform(
            longArrayOf(0L, 48L + tier * 12L, 28L, 64L + tier * 14L, 24L, 40L),
            intArrayOf(0, 255, 0, 225, 0, 165)
        )
    }

    fun playHazard() {
        triggerAudioDucking(320L)
        playSequence(listOf(255.0 to 65, 185.0 to 70, 140.0 to 115), volume = 0.30f)
        vibrate(48L)
    }

    fun playWin() {
        triggerAudioDucking(780L)
        playSequence(
            listOf(523.25 to 70, 659.25 to 70, 783.99 to 80, 1046.5 to 105, 1318.5 to 120, 1567.98 to 230),
            volume = 0.32f
        )
        vibrateWaveform(longArrayOf(0L, 32L, 42L, 38L, 42L, 88L), intArrayOf(0, 165, 0, 210, 0, 255))
    }

    fun playLose() {
        triggerAudioDucking(650L)
        playSequence(listOf(330.0 to 105, 261.6 to 115, 220.0 to 125, 165.0 to 240), volume = 0.28f)
        vibrate(95L)
    }

    private fun playImpactHit(baseHz: Double, metallic: Boolean, durationMs: Int, volume: Float) {
        if (!soundEnabled) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = (sampleRate * durationMs) / 1000
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var noiseState = 0x51A7E291
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / totalSamples
                    noiseState = noiseState xor (noiseState shl 13)
                    noiseState = noiseState xor (noiseState ushr 17)
                    noiseState = noiseState xor (noiseState shl 5)
                    val noise = (noiseState and 0xFFFF) / 32768.0 - 1.0

                    val env = exp(-5.8 * progress)
                    val crack = noise * exp(-28.0 * progress) * 0.42
                    val body = sin(2.0 * PI * baseHz * t) * 0.65 +
                        (if (metallic) sin(2.0 * PI * baseHz * 2.41 * t) * 0.32 else sin(2.0 * PI * baseHz * 0.5 * t) * 0.28)
                    val sample = ((body * env) + crack) * volume
                    buffer[i] = (sample * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                playStaticBuffer(buffer, sampleRate, durationMs.toLong())
            }
        }
    }

    private fun playExplosionBlast(durationMs: Int, volume: Float) {
        if (!soundEnabled) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = (sampleRate * durationMs) / 1000
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var phase = 0.0
                var lpf = 0.0
                var noiseState = 0x7F31C4B5
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val subFreq = 155.0 * exp(-4.8 * progress) + 34.0
                    phase += 2.0 * PI * subFreq / sampleRate
                    noiseState = noiseState xor (noiseState shl 13)
                    noiseState = noiseState xor (noiseState ushr 17)
                    noiseState = noiseState xor (noiseState shl 5)
                    val rawNoise = (noiseState and 0xFFFF) / 32768.0 - 1.0
                    lpf += 0.22 * (rawNoise - lpf)

                    val subBoom = sin(phase) * exp(-3.4 * progress) * 0.68
                    val blastRumble = lpf * exp(-4.2 * progress) * 0.65
                    val sample = (subBoom + blastRumble) * volume
                    buffer[i] = (sample * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                playStaticBuffer(buffer, sampleRate, durationMs.toLong())
            }
        }
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

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!hapticsEnabled) return
        runCatching {
            val vibrator = resolveVibrator()
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

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        if (!hapticsEnabled) return
        runCatching {
            val vibrator = resolveVibrator()
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (vibrator.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings.sum().coerceAtMost(120L))
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
