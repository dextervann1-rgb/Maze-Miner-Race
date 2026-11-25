package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Deterministic Ghost Racer frame rendered holographically on MazeCanvas.
 */
data class GhostFrame(
    val tick: Int,
    val gridX: Float,
    val gridY: Float,
    val activeAbility: MinerAbility? = null,
    val isSurgeActive: Boolean = false
)

/**
 * Compact deterministic encoder/decoder for Ghost Racer replay frames & input streams
 * stored under Firestore /daily_ranked/{dateKey}/entries/{userId} (strictly capped <= 32KB).
 */
object GhostReplayCodec {

    private const val MAX_WIRE_BYTES = 32_000

    /**
     * Encodes recorded GhostFrames into a compact semicolon-separated wire string:
     * `tick,x,y,surgeFlag;...`
     */
    fun encodeFrames(frames: List<GhostFrame>): String {
        if (frames.isEmpty()) return ""
        val sb = StringBuilder(frames.size * 14)
        for (i in frames.indices) {
            val f = frames[i]
            val token = "${f.tick},${f.gridX.toInt()},${f.gridY.toInt()},${if (f.isSurgeActive) 1 else 0}"
            if (sb.length + token.length + 1 > MAX_WIRE_BYTES) break
            if (sb.isNotEmpty()) sb.append(';')
            sb.append(token)
        }
        return sb.toString()
    }

    /**
     * Decodes a compact wire string back into deterministic GhostFrames.
     * Malformed tokens are safely skipped.
     */
    fun decodeFrames(raw: String): List<GhostFrame> {
        if (raw.isBlank()) return emptyList()
        val result = ArrayList<GhostFrame>()
        for (part in raw.split(';')) {
            val cols = part.split(',')
            if (cols.size < 3) continue
            val tick = cols[0].toIntOrNull() ?: continue
            val gx = cols[1].toFloatOrNull() ?: continue
            val gy = cols[2].toFloatOrNull() ?: continue
            val surge = cols.getOrNull(3) == "1"
            result.add(
                GhostFrame(
                    tick = tick.coerceAtLeast(0),
                    gridX = gx.coerceIn(0f, 30f),
                    gridY = gy.coerceIn(0f, 30f),
                    activeAbility = null,
                    isSurgeActive = surge
                )
            )
        }
        return result
    }

    /**
     * Builds deterministic GhostFrames from a player's trail + elapsed ticks when submitting a run.
     */
    fun framesFromTrail(trail: List<GridPos>, totalTicks: Int): List<GhostFrame> {
        if (trail.isEmpty()) return emptyList()
        val safeTicks = totalTicks.coerceAtLeast(1)
        val lastIdx = (trail.size - 1).coerceAtLeast(1)
        return trail.mapIndexed { idx, pos ->
            val tick = ((idx.toFloat() / lastIdx.toFloat()) * safeTicks).toInt()
            GhostFrame(
                tick = tick,
                gridX = pos.x.toFloat(),
                gridY = pos.y.toFloat(),
                activeAbility = null,
                isSurgeActive = false
            )
        }
    }

    /**
     * Interpolates a GhostFrame at the current race tick / step index.
     */
    fun frameAtTick(frames: List<GhostFrame>, tick: Int): GhostFrame? {
        if (frames.isEmpty()) return null
        if (tick <= frames.first().tick) return frames.first()
        if (tick >= frames.last().tick) return frames.last()
        var idx = frames.indexOfLast { it.tick <= tick }
        if (idx < 0) idx = 0
        return frames[idx]
    }
}

/**
 * Frame-accurate tactical impact juice controller:
 * - Brief Hitstop (18..45ms) freezes entity motion on TNT chain blasts while VFX/shake continue
 * - Camera Zoom Impulse punches in (1.03..1.08x) on detonations and pulls out (0.92x) on Kinetic Surge
 */
class TacticalJuiceController {
    var hitstopRemainingMs by mutableFloatStateOf(0f)
        private set
    var cameraZoomImpulse by mutableFloatStateOf(1f)
        private set

    fun triggerChainDetonation(chainCount: Int) {
        val safeChains = chainCount.coerceAtLeast(1)
        // Cap hitstop at 45ms so pacing stays snappy
        hitstopRemainingMs = (18f + (safeChains * 6f)).coerceAtMost(45f)
        cameraZoomImpulse = (1.03f + (safeChains * 0.01f)).coerceAtMost(1.08f)
    }

    fun triggerKineticSurge() {
        // Slight zoom-out so the player sees more of the labyrinth at high speed
        cameraZoomImpulse = 0.92f
    }

    fun update(deltaMs: Float): Boolean {
        val safeDelta = deltaMs.coerceIn(0f, 100f)
        if (hitstopRemainingMs <= 0f && abs(cameraZoomImpulse - 1f) < 0.0005f) {
            if (cameraZoomImpulse != 1f) cameraZoomImpulse = 1f
            return false
        }
        cameraZoomImpulse = lerp(cameraZoomImpulse, 1f, (safeDelta / 180f).coerceIn(0f, 1f))
        if (abs(cameraZoomImpulse - 1f) < 0.0005f) {
            cameraZoomImpulse = 1f
        }
        if (hitstopRemainingMs > 0f) {
            hitstopRemainingMs = (hitstopRemainingMs - safeDelta).coerceAtLeast(0f)
            return true // True = freeze entity movement this frame, render VFX/shake only
        }
        return false
    }
}

/**
 * Live-Ops tunable competitive balance configuration fetched from Firestore /live_config/balance
 * with safe built-in defaults.
 */
data class LiveBalanceConfig(
    val goldieSpeedMultiplier: Float = 1.15f,
    val vexDrillCooldownMs: Long = 1400L,
    val ruinsTrapCycleMs: Long = 2200L,
    val feverDecayRatePerSec: Float = 0.18f,
    val endlessModeUnlockStars: Int = 45
)

enum class BiomeType {
    DESERT,
    ICE,
    VOLCANO,
    CAVERN,
    RUINS;

    fun toBiome(): Biome = when (this) {
        DESERT -> Biome.DESERT
        ICE -> Biome.ICE
        VOLCANO -> Biome.VOLCANO
        CAVERN -> Biome.CAVERN
        RUINS -> Biome.RUINS
    }
}

data class AbyssalFloorConfig(
    val floorNumber: Int,
    val floorSeed: Long,
    val boardSize: Int,
    val beaconCount: Int,
    val biome: BiomeType,
    val hazardDensity: Float,      // 0.06f to 0.32f
    val aiAggressionScalar: Float, // 1.0f to 1.65f
    val gemMultiplier: Float,      // Scales reward pacing
    val parTimeSeconds: Int
)

/**
 * Deterministic Endless Abyssal Depth procedural floor generator (Floors 1..∞).
 * Uses 64-bit SplitMix bit-mixing on [runRootSeed] + [floorNumber] so players can share run seeds.
 */
object AbyssalDepthGenerator {

    private val oddBoardSizes = listOf(15, 19, 21, 25, 29)
    private val biomes = BiomeType.entries

    fun generateFloorConfig(runRootSeed: Long, floorNumber: Int): AbyssalFloorConfig {
        val safeFloor = floorNumber.coerceAtLeast(1)
        val floorSeed = mixSeed(runRootSeed, safeFloor.toLong())
        val rng = Mulberry32(floorSeed.toInt())

        // Progressive size ramp: Floors 1-2 (15x15), 3-5 (19x19), 6-9 (21x21), 10-14 (25x25), 15+ (29x29)
        val sizeIndex = when {
            safeFloor <= 2 -> 0
            safeFloor <= 5 -> 1
            safeFloor <= 9 -> 2
            safeFloor <= 14 -> 3
            else -> 4
        }
        val boardSize = oddBoardSizes[sizeIndex]

        // Beacons scale from 2 up to 4
        val beaconCount = when {
            safeFloor <= 3 -> 2
            safeFloor <= 8 -> 3
            else -> 4
        }

        // Deterministic biome rotation with pseudo-random shuffle per 5-floor bracket
        val biome = biomes[(safeFloor + rng.nextInt(biomes.size)) % biomes.size]

        // Asymptotic difficulty curves (challenging at depth 50+, never mathematically impossible)
        val hazardDensity = (0.06f + (safeFloor * 0.008f)).coerceAtMost(0.32f)
        val aiScalar = (1.0f + (safeFloor * 0.022f)).coerceAtMost(1.65f)
        val gemMultiplier = 1.0f + (safeFloor * 0.15f)
        val parTimeSeconds = (boardSize * 3) + (beaconCount * 12)

        return AbyssalFloorConfig(
            floorNumber = safeFloor,
            floorSeed = floorSeed,
            boardSize = boardSize,
            beaconCount = beaconCount,
            biome = biome,
            hazardDensity = hazardDensity,
            aiAggressionScalar = aiScalar,
            gemMultiplier = gemMultiplier,
            parTimeSeconds = parTimeSeconds
        )
    }

    private fun mixSeed(seed: Long, salt: Long): Long {
        var z = seed + salt * -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }
}

class Mulberry32(private var state: Int) {
    fun nextFloat(): Float {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
        return ((t xor (t ushr 14)) ushr 0).toFloat() / 4294967296f
    }

    fun nextInt(bound: Int): Int = (nextFloat() * bound).toInt().coerceIn(0, (bound - 1).coerceAtLeast(0))
}

/**
 * Diegetic, zero-wall-of-text First-Run Playable Gauntlet steps.
 */
enum class GauntletStep(
    val targetGridX: Int,
    val targetGridY: Int,
    val iconCue: String, // Minimal 1-2 word diegetic HUD tag
    val cameraFocusZoom: Float
) {
    DIG_CORRIDOR(targetGridX = 3, targetGridY = 7, iconCue = "SWIPE TO DIG", cameraFocusZoom = 1.15f),
    TRIGGER_TNT(targetGridX = 5, targetGridY = 7, iconCue = "DETONATE TNT", cameraFocusZoom = 1.10f),
    ACTIVATE_BEACON(targetGridX = 7, targetGridY = 5, iconCue = "CHARGE BEACON", cameraFocusZoom = 1.05f),
    UNLEASH_SURGE(targetGridX = 8, targetGridY = 9, iconCue = "TAP SURGE", cameraFocusZoom = 0.95f),
    ENTER_PORTAL(targetGridX = 8, targetGridY = 13, iconCue = "ESCAPE PORTAL", cameraFocusZoom = 1.0f),
    COMPLETED(targetGridX = -1, targetGridY = -1, iconCue = "", cameraFocusZoom = 1.0f);

    fun targetPosOrNull(): GridPos? =
        if (targetGridX >= 0 && targetGridY >= 0) GridPos(targetGridX, targetGridY) else null
}

class FirstRunGauntletController {
    private val _currentStep = MutableStateFlow(GauntletStep.DIG_CORRIDOR)
    val currentStep: StateFlow<GauntletStep> = _currentStep.asStateFlow()

    // Grants free Surge charge once player hits step 4 so they feel full power immediately
    var forceGrantSurgeCharge: Boolean = false
        private set

    fun reset() {
        forceGrantSurgeCharge = false
        _currentStep.value = GauntletStep.DIG_CORRIDOR
    }

    fun onTileDug(x: Int, y: Int) {
        if (_currentStep.value == GauntletStep.DIG_CORRIDOR && x >= 3) {
            _currentStep.value = GauntletStep.TRIGGER_TNT
        }
    }

    fun onTntDetonated() {
        if (_currentStep.value == GauntletStep.TRIGGER_TNT) {
            _currentStep.value = GauntletStep.ACTIVATE_BEACON
        }
    }

    fun onBeaconActivated() {
        if (_currentStep.value == GauntletStep.ACTIVATE_BEACON) {
            forceGrantSurgeCharge = true
            _currentStep.value = GauntletStep.UNLEASH_SURGE
        }
    }

    fun onKineticSurgeTriggered() {
        if (_currentStep.value == GauntletStep.UNLEASH_SURGE) {
            _currentStep.value = GauntletStep.ENTER_PORTAL
        }
    }

    fun onExitPortalEntered(onSaveCompletedFlag: () -> Unit) {
        if (_currentStep.value == GauntletStep.ENTER_PORTAL) {
            _currentStep.value = GauntletStep.COMPLETED
            onSaveCompletedFlag()
        }
    }

    companion object {
        const val GAUNTLET_SIZE = 15
        val GAUNTLET_START = GridPos(1, 7)

        /**
         * Builds the handcrafted, deterministic 15×15 First-Run Playable Gauntlet chamber:
         * - Start at (1,7) -> open hall to (2,7)
         * - Step 1 DIG_CORRIDOR: soft dirt wall at (3,7)
         * - Step 2 TRIGGER_TNT: explosive TNT cluster at (5,7) + chained TNT at (6,7)
         * - Step 3 ACTIVATE_BEACON: Relay Beacon at (7,5) with gems guiding upward
         * - Step 4 UNLEASH_SURGE: open surge runway down column x=8 toward (8,9)
         * - Step 5 ENTER_PORTAL: Arcane Exit Portal at (8,13)
         */
        fun buildGauntletMaze(): MazeState {
            val size = GAUNTLET_SIZE
            val maze = List(size) { IntArray(size) { 1 } }
            val blocks = LinkedHashMap<String, BlockState>()
            val gems = LinkedHashSet<String>()
            val powers = LinkedHashMap<String, PowerupType>()

            // Carve guided gauntlet corridors
            val openFloor = HashSet<GridPos>()
            // Horizontal shaft (1,7)..(8,7)
            for (x in 1..8) openFloor.add(GridPos(x, 7))
            // Beacon alcove up at x=7, y=5..7 and connector to x=8
            for (y in 5..7) openFloor.add(GridPos(7, y))
            for (y in 5..13) openFloor.add(GridPos(8, y))
            // Extra breathing pocket around beacon & portal
            openFloor.add(GridPos(6, 6))
            openFloor.add(GridPos(6, 8))
            openFloor.add(GridPos(7, 9))
            openFloor.add(GridPos(9, 10))
            openFloor.add(GridPos(7, 12))
            openFloor.add(GridPos(9, 12))

            for (y in 0 until size) {
                for (x in 0 until size) {
                    val pos = GridPos(x, y)
                    val isBorder = x == 0 || y == 0 || x == size - 1 || y == size - 1
                    if (!isBorder && pos in openFloor) {
                        maze[y][x] = 0
                    } else {
                        maze[y][x] = 1
                        val kind = if (isBorder) BlockKind.IRON else BlockKind.STONE
                        blocks[pos.key] = BlockState(kind = kind, hp = kind.maxHp, max = kind.maxHp)
                    }
                }
            }

            // Step 1: Soft 1-HP dirt wall blocking corridor at (3,7)
            blocks["3,7"] = BlockState(kind = BlockKind.DIRT, hp = 1, max = 1)

            // Step 2: Explosive TNT chain cluster at (5,7) & (6,7)
            blocks["5,7"] = BlockState(kind = BlockKind.DIRT, hp = 1, max = 1, special = BlockSpecial.EXPLOSIVE)
            blocks["6,7"] = BlockState(kind = BlockKind.DIRT, hp = 1, max = 1, special = BlockSpecial.EXPLOSIVE)
            blocks["6,6"] = BlockState(kind = BlockKind.DIRT, hp = 1, max = 1)
            blocks["6,8"] = BlockState(kind = BlockKind.DIRT, hp = 1, max = 1)

            // Gem breadcrumbs leading the eye along every gauntlet beat
            gems.add("2,7")
            gems.add("4,7")
            gems.add("7,6")
            gems.add("8,6")
            gems.add("8,8")
            gems.add("8,10")
            gems.add("8,11")
            gems.add("8,12")

            // Helpful Speed pickup before the portal sprint
            powers["8,9"] = PowerupType.SPEED

            return MazeState(
                maze = maze,
                blocks = blocks,
                gems = gems,
                powers = powers,
                boardSize = size,
                finishX = 8,
                finishY = 13,
                checkpoints = listOf(GridPos(7, 5))
            )
        }
    }
}
