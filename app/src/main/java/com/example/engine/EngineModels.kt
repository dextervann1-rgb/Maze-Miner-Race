package com.example.engine

/**
 * Canonical strictly-typed data models for Maze Miner Race v12.0.0.
 * Mirrors the simulation module JSDoc typedef specifications with full Kotlin type safety.
 */

const val ENGINE_VERSION = "12.0.0"
const val MAZE_SIZE = 21
const val START_X = 1
const val START_Y = 1
const val FINISH_X = 19
const val FINISH_Y = 19

data class GridPos(val x: Int, val y: Int) {
    val key: String get() = "$x,$y"
}

enum class BlockKind(val maxHp: Int, val staminaCost: Int, val wireName: String) {
    DIRT(1, 5, "dirt"),
    STONE(2, 8, "stone"),
    IRON(3, 12, "iron"),
    CRYSTAL(4, 16, "crystal"),
    GAMBLE(2, 8, "gamble")
}

enum class BlockSpecial(val wireName: String) {
    EXPLOSIVE("explosive"),
    ICE("ice"),
    LAVA("lava"),
    LOCKED("locked"),
    HIDDEN("hidden")
}

data class BlockState(
    val kind: BlockKind,
    val hp: Int,
    val max: Int,
    val special: BlockSpecial? = null
)

enum class PowerupType(val wireName: String) {
    PICK("pick"),
    SPEED("speed"),
    LANTERN("lantern"),
    SHIELD("shield"),
    SONAR("sonar"),
    CLOCK("clock")
}

data class MazeState(
    val maze: List<IntArray>,
    val blocks: Map<String, BlockState>,
    val gems: Set<String>,
    val powers: Map<String, PowerupType>,
    val boardSize: Int = MAZE_SIZE,
    val finishX: Int = FINISH_X,
    val finishY: Int = FINISH_Y
)

data class BiomeRule(
    val visionDelta: Int,
    val staminaPerTick: Double,
    val slideChance: Double = 0.0,
    val gemMultiplier: Int = 1,
    val trapDamage: Int = 0,
    val trapCount: Int = 0
)

enum class Biome(val wireName: String, val rule: BiomeRule) {
    DESERT("Desert", BiomeRule(visionDelta = -1, staminaPerTick = -0.5)),
    ICE("Ice", BiomeRule(visionDelta = 0, staminaPerTick = 0.5, slideChance = 0.18)),
    VOLCANO("Volcano", BiomeRule(visionDelta = 0, staminaPerTick = -0.4)),
    CAVERN("Cavern", BiomeRule(visionDelta = -1, staminaPerTick = 0.0, gemMultiplier = 2)),
    RUINS("Ruins", BiomeRule(visionDelta = 0, staminaPerTick = 0.0, trapDamage = 12, trapCount = 5))
}

enum class Modifier(val wireName: String) {
    NORMAL("Normal"),
    DARKNESS("Darkness"),
    MINING_X2("Mining x2"),
    NO_SONAR("No Sonar"),
    DOUBLE_GEMS("Double Gems"),
    HEAT_WAVE("Heat Wave")
}

enum class Difficulty(val wireName: String, val startTimer: Int) {
    EASY("easy", 260),
    NORMAL("normal", 200),
    HARD("hard", 150)
}

enum class RivalPersonality(
    val wireName: String,
    val stepIntervalMs: Long,
    val mineBias: Double,
    val seeksGems: Boolean
) {
    RUBBLE("Rubble", 900L, 0.50, false),
    GOLDIE("Goldie", 780L, 0.30, true),
    VEX("Vex", 660L, 0.95, false),
    MOSS("Moss", 1100L, 0.20, false)
}

sealed interface ReplayAction {
    val tick: Int

    data class Move(
        val dx: Int,
        val dy: Int,
        override val tick: Int
    ) : ReplayAction

    data class Mine(
        val x: Int,
        val y: Int,
        override val tick: Int
    ) : ReplayAction
}

data class RunSubmission(
    val sessionId: String,
    val token: String,
    val seed: String,
    val version: String = ENGINE_VERSION,
    val difficulty: Difficulty,
    val modifier: Modifier,
    val biome: Biome,
    val actions: List<ReplayAction>,
    val ticks: Int,
    val clientScore: Int
)

data class VerifyResult(
    val ok: Boolean,
    val verified: Boolean = false,
    val serverScore: Int? = null,
    val rank: Int? = null,
    val fingerprint: String? = null,
    val reason: String? = null,
    val source: VerificationSource = VerificationSource.CANONICAL_LOCAL_SQLITE
)

enum class VerificationSource {
    REMOTE_SERVER,
    CANONICAL_LOCAL_SQLITE,
    PRACTICE_UNRANKED,
    REJECTED
}

data class ScoreBreakdown(
    val totalScore: Int,
    val baseScore: Int,
    val timeBonus: Int,
    val gemsCollected: Int,
    val blocksBroken: Int,
    val bestCombo: Int,
    val cellsExplored: Int,
    val elapsedTicks: Int,
    val walletGemsEarned: Int
)

enum class UpgradeType(val maxLevel: Int, val baseCost: Int, val costStep: Int) {
    TITANIUM_PICK(5, 35, 25),
    LANTERN_OPTICS(5, 30, 20),
    STAMINA_FLASK(5, 30, 20),
    SONAR_RIG(5, 40, 25),
    GEM_HARVESTER(5, 45, 30),
    BLAST_SHIELD(3, 50, 40);

    fun costForNextLevel(currentLevel: Int): Int = baseCost + (currentLevel * costStep)
}

enum class AchievementId(val rewardGems: Int) {
    FIRST_WIN(50),
    GEM_HUNTER(30),
    COMBO_MASTER(30),
    EXPLORER(40),
    SPEEDSTER(80),
    UPGRADE_MASTER(60)
}

data class EngineSelfTestResult(
    val name: String,
    val passed: Boolean,
    val detail: String
)
