package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundFxEngine
import com.example.billing.BillingManager
import com.example.billing.BillingUiState
import com.example.billing.StorePackSpec
import com.example.cloud.CloudProfileSave
import com.example.cloud.CommunityMazeSeed
import com.example.cloud.DailyRankedGhostEntry
import com.example.cloud.PvpMatchRoom
import com.example.cloud.PvpRoomStatus
import com.example.data.CampaignProgressEntity
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.LeaderboardEntity
import com.example.data.PlayerProfileEntity
import com.example.data.SessionResponseDto
import com.example.data.hasAchievement
import com.example.data.withAchievement
import com.example.engine.AbyssalDepthGenerator
import com.example.engine.AchievementId
import com.example.engine.Biome
import com.example.engine.BlockKind
import com.example.engine.BlockSpecial
import com.example.engine.BlockState
import com.example.engine.BoardArchetype
import com.example.engine.CampaignCatalog
import com.example.engine.CampaignLevel
import com.example.engine.DeterministicRng
import com.example.engine.Difficulty
import com.example.engine.EngineSelfTestResult
import com.example.engine.FINISH_X
import com.example.engine.FINISH_Y
import com.example.engine.FirstRunGauntletController
import com.example.engine.GauntletStep
import com.example.engine.GhostFrame
import com.example.engine.GhostReplayCodec
import com.example.engine.GridPos
import com.example.engine.LiveBalanceConfig
import com.example.engine.MAZE_SIZE
import com.example.engine.MazeGenerator
import com.example.engine.MinerAbility
import com.example.engine.Modifier
import com.example.engine.MoveDir
import com.example.engine.PowerupType
import com.example.engine.Replay16BitCodec
import com.example.engine.ReplayAction
import com.example.engine.ReplaySimulator
import com.example.engine.ReplayTickInput
import com.example.engine.RivalPersonality
import com.example.engine.START_X
import com.example.engine.START_Y
import com.example.engine.ScoreBreakdown
import com.example.engine.UpgradeType
import com.example.engine.VerifyResult
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.LocalizedStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class AppScreen {
    LAUNCH,
    PLAYING,
    HUB
}

enum class HubTab {
    RACE,
    CLOUD,
    FORGE,
    RANKS,
    SETTINGS
}

enum class RaceStatus {
    PLAYING,
    WON,
    LOST
}

data class FloatingPopup(
    val id: Long,
    val gridX: Int,
    val gridY: Int,
    val text: String,
    val isPositive: Boolean = true
)

data class LiveRaceState(
    val seed: String,
    val difficulty: Difficulty,
    val modifier: Modifier,
    val biome: Biome,
    val rivalPersonality: RivalPersonality,
    val isPractice: Boolean,
    val boardSize: Int = MAZE_SIZE,
    val finishPos: GridPos = GridPos(FINISH_X, FINISH_Y),
    val checkpoints: List<GridPos> = emptyList(),
    val playerCheckpointsCleared: Set<String> = emptySet(),
    val rivalCheckpointsCleared: Set<String> = emptySet(),
    val totalCircuitSpan: Int = 1,
    val campaignLevelId: Int? = null,
    val campaignTitle: String? = null,
    val starsEarned: Int = 0,
    val firstClearGemsEarned: Int = 0,
    val playerPos: GridPos = GridPos(START_X, START_Y),
    val rivalPos: GridPos = GridPos(START_X, START_Y),
    val myTrail: List<GridPos> = listOf(GridPos(START_X, START_Y)),
    val ghostTrail: List<GridPos> = emptyList(),
    val ghostIndex: Int = 0,
    val blocks: Map<String, BlockState> = emptyMap(),
    val gems: Set<String> = emptySet(),
    val powers: Map<String, PowerupType> = emptyMap(),
    val traps: Set<String> = emptySet(),
    val lavaPuddles: Set<String> = emptySet(),
    val sandDrifts: Set<String> = emptySet(),
    val explored: Set<String> = setOf("1,1"),
    val hiddenRevealed: Set<String> = emptySet(),
    val targetBlock: GridPos? = null,
    val facingDir: GridPos = GridPos(1, 0),
    val hintPath: List<GridPos> = emptyList(),
    val hitPulseTick: Long = 0L,
    val lastHitPos: GridPos? = null,
    val lastHitKind: BlockKind? = null,
    val lastHitBroke: Boolean = false,
    val lastHitExploded: Boolean = false,
    val lastBlastOrigins: List<GridPos> = emptyList(),
    val lastChainDepth: Int = 0,
    val feverCharge: Int = 0,
    val feverRemainingSec: Int = 0,
    val comboTimerTicks: Int = 0,
    val surgeCooldownTicks: Int = 0,
    val surgePulseTick: Long = 0L,
    val overdriveActive: Boolean = false,
    val unlockedAbilities: Set<MinerAbility> = setOf(MinerAbility.QUICK_STEP),
    val newlyUnlockedAbility: MinerAbility? = null,
    val score: Int = 0,
    val timer: Int = 200,
    val stamina: Double = 100.0,
    val tickCount: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val brokenCount: Int = 0,
    val gemsCollected: Int = 0,
    val activePower: PowerupType? = null,
    val powerUntilTick: Int = -1,
    val hasKey: Boolean = false,
    val sonarCharges: Int = 1,
    val sonarActive: Boolean = false,
    val viewSize: Int = 7,
    val baseVision: Int = 3,
    val currentVision: Int = 3,
    val questBreakDone: Int = 0,
    val questGemDone: Int = 0,
    val paused: Boolean = false,
    val status: RaceStatus = RaceStatus.PLAYING,
    val loseReason: String? = null,
    val popups: List<FloatingPopup> = emptyList(),
    val lastVerifyResult: VerifyResult? = null,
    val lastBreakdown: ScoreBreakdown? = null,
    val pvpRoomId: String? = null,
    val pvpRoomCode: String? = null,
    val pvpOpponentName: String? = null,
    val pvpAsHost: Boolean = false,
    val pvpOpponentScore: Int = 0,
    val pvpOpponentBeacons: Int = 0,
    val ghostFrames: List<GhostFrame> = emptyList(),
    val ghostRacerName: String? = null,
    val compressedReplay16Bit: String? = null,
    val clearTimeMs: Long = 0L,
    val abyssalFloor: Int? = null,
    val abyssalRootSeed: Long? = null,
    val abyssalCumulativeScore: Int = 0,
    val abyssalAiScalar: Float = 1f,
    val abyssalGemMultiplier: Float = 1f,
    val abyssalFingerprint: String? = null,
    val isGauntlet: Boolean = false,
    val gauntletStep: GauntletStep = GauntletStep.COMPLETED,
    val liveBalance: LiveBalanceConfig = LiveBalanceConfig()
) {
    val remainingPlayerCheckpoints: List<GridPos>
        get() = checkpoints.filter { it.key !in playerCheckpointsCleared }

    val remainingRivalCheckpoints: List<GridPos>
        get() = checkpoints.filter { it.key !in rivalCheckpointsCleared }

    val portalUnlocked: Boolean
        get() = checkpoints.isEmpty() || playerCheckpointsCleared.size >= checkpoints.size

    val nextPlayerObjective: GridPos
        get() = remainingPlayerCheckpoints.minByOrNull {
            abs(it.x - playerPos.x) + abs(it.y - playerPos.y)
        } ?: finishPos
}

data class GameUiState(
    val screen: AppScreen = AppScreen.LAUNCH,
    val activeTab: HubTab = HubTab.RACE,
    val seedInput: String = todayUtcSeed(),
    val selectedDifficulty: Difficulty = Difficulty.NORMAL,
    val selectedRival: RivalPersonality = RivalPersonality.RUBBLE,
    val selectedArchetype: BoardArchetype = BoardArchetype.CLASSIC_21,
    val selectedBiomeOverride: Biome? = null,
    val liveRace: LiveRaceState? = null,
    val lastVerifiedReceipt: VerifyResult? = null,
    val selfTestResults: List<EngineSelfTestResult> = emptyList(),
    val remoteServerStatus: String? = null,
    val showTutorialModal: Boolean = false,
    val showLanguageSheet: Boolean = false,
    val toastMessage: String? = null
)

fun todayUtcSeed(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = GameDatabase.getInstance(application).gameDao()
    private val repository = GameRepository(dao)
    private val fx = SoundFxEngine(application)
    private val billingManager = BillingManager(
        context = application,
        verifyPurchaseWithServer = { pack, token, orderId ->
            repository.verifyPlayPurchaseToken(
                baseUrl = profile.value.verifierServerUrl,
                packageName = application.packageName,
                productId = pack.productId,
                purchaseToken = token,
                orderId = orderId
            )
        },
        onPurchaseVerified = { pack -> onVerifiedPackPurchase(pack) },
        onNonConsumableRestored = { pack ->
            if (pack.unlocksFounderAura) {
                viewModelScope.launch {
                    repository.updateProfile { cur ->
                        if (cur.founderPackOwned) cur else cur.copy(founderPackOwned = true)
                    }
                }
            }
        },
        onBillingNotice = { msg -> showToast(msg) }
    )

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val billingState: StateFlow<BillingUiState> = billingManager.state

    val profile: StateFlow<PlayerProfileEntity> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerProfileEntity()
    )

    val leaderboard: StateFlow<List<LeaderboardEntity>> = repository.leaderboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val campaignProgress: StateFlow<List<CampaignProgressEntity>> = repository.campaignProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Internal deterministic simulation runners for active race
    private var gameplayRng: () -> Double = { 0.5 }
    private var rivalRng: () -> Double = { 0.5 }
    private val recordedActions = ArrayList<ReplayAction>()
    private val recordedTickInputs = ArrayList<ReplayTickInput>()
    private val gauntletController = FirstRunGauntletController()
    private var activeLiveBalance: LiveBalanceConfig = LiveBalanceConfig()
    private var raceStartElapsedMs: Long = 0L
    private var remoteSession: SessionResponseDto? = null
    private var timerJob: Job? = null
    private var rivalJob: Job? = null
    private var sonarJob: Job? = null
    private var autoStepJob: Job? = null
    private var toastJob: Job? = null
    private var popupSeq = 0L
    private var hitPulseSeq = 0L
    private var quickRaceCounter = 0

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            val loaded = repository.ensureProfileInitialized()
            fx.soundEnabled = loaded.soundEnabled
            fx.musicEnabled = loaded.musicEnabled
            fx.hapticsEnabled = loaded.hapticsEnabled
        }
        viewModelScope.launch {
            profile.collect { p ->
                fx.soundEnabled = p.soundEnabled
                fx.musicEnabled = p.musicEnabled
                fx.hapticsEnabled = p.hapticsEnabled
            }
        }
    }

    private fun currentStrings(): LocalizedStrings =
        AppStrings.forLanguage(AppLanguage.fromCode(profile.value.languageCode))

    // --- Navigation ---

    fun openHub(tab: HubTab = HubTab.RACE) {
        stopRaceLoops()
        fx.stopSoundtrack()
        _uiState.update { it.copy(screen = AppScreen.HUB, activeTab = tab) }
    }

    fun selectHubTab(tab: HubTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun returnToLaunch() {
        stopRaceLoops()
        fx.stopSoundtrack()
        _uiState.update { it.copy(screen = AppScreen.LAUNCH) }
    }

    fun setTutorialVisible(visible: Boolean) {
        _uiState.update { it.copy(showTutorialModal = visible) }
    }

    fun setLanguageSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showLanguageSheet = visible) }
    }

    // --- Race Setup & Campaign ---

    fun setSeedInput(seed: String) {
        _uiState.update { it.copy(seedInput = seed.take(24)) }
    }

    fun useTodaySeed() {
        _uiState.update { it.copy(seedInput = todayUtcSeed()) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun setRival(rival: RivalPersonality) {
        _uiState.update { it.copy(selectedRival = rival) }
    }

    fun setBoardArchetype(archetype: BoardArchetype) {
        _uiState.update { it.copy(selectedArchetype = archetype) }
    }

    fun setBiomeOverride(biome: Biome?) {
        _uiState.update { it.copy(selectedBiomeOverride = biome) }
    }

    fun startRace(practice: Boolean = false, seedOverride: String? = null) {
        val state = _uiState.value
        val seed = (seedOverride ?: state.seedInput.trim()).ifEmpty { todayUtcSeed() }
        val biome = if (practice && state.selectedBiomeOverride != null) {
            state.selectedBiomeOverride
        } else {
            MazeGenerator.biomeForSeed(seed)
        }
        val modifier = MazeGenerator.modifierForSeed(seed)
        val boardSize = if (practice) state.selectedArchetype.size else MAZE_SIZE
        launchBoardSession(
            seed = seed,
            difficulty = state.selectedDifficulty,
            modifier = modifier,
            biome = biome,
            rival = state.selectedRival,
            isPractice = practice,
            boardSize = boardSize,
            campaignLevel = null
        )
    }

    fun startCampaignLevel(level: CampaignLevel) {
        launchBoardSession(
            seed = level.seed,
            difficulty = level.difficulty,
            modifier = level.modifier,
            biome = level.biome,
            rival = level.rival,
            isPractice = true, // Enables Forge upgrades, unlocked abilities & multi-board sizing while awarding campaign stars
            boardSize = level.archetype.size,
            campaignLevel = level
        )
    }

    fun startNextPlayableCampaignLevel() {
        val cleared = campaignProgress.value.filter { it.stars > 0 }.map { it.levelId }.toSet()
        val nextLevel = CampaignCatalog.nextRecommendedLevel(cleared)
        startCampaignLevel(nextLevel)
    }

    fun startBoardQuickPlay(archetype: BoardArchetype) {
        _uiState.update { it.copy(selectedArchetype = archetype) }
        val state = _uiState.value
        val baseSeed = state.seedInput.trim().ifEmpty { todayUtcSeed() }
        val boardSeed = "$baseSeed-${archetype.size}x${archetype.size}"
        val biome = state.selectedBiomeOverride ?: MazeGenerator.biomeForSeed(boardSeed)
        val modifier = MazeGenerator.modifierForSeed(boardSeed)
        launchBoardSession(
            seed = boardSeed,
            difficulty = state.selectedDifficulty,
            modifier = modifier,
            biome = biome,
            rival = state.selectedRival,
            isPractice = true,
            boardSize = archetype.size,
            campaignLevel = null
        )
    }

    fun replayCurrentRace() {
        val lr = _uiState.value.liveRace
        if (lr?.isGauntlet == true) {
            startFirstRunGauntlet()
            return
        }
        if (lr?.abyssalFloor != null && lr.abyssalRootSeed != null) {
            startAbyssalDescent(
                rootSeed = lr.abyssalRootSeed,
                floorNumber = lr.abyssalFloor,
                cumulativeScore = (lr.abyssalCumulativeScore - lr.score).coerceAtLeast(0)
            )
            return
        }
        if (lr?.campaignLevelId != null) {
            val lvl = CampaignCatalog.byId(lr.campaignLevelId)
            if (lvl != null) {
                startCampaignLevel(lvl)
                return
            }
        }
        startRace(practice = lr?.isPractice ?: false, seedOverride = lr?.seed)
    }

    fun startNextCampaignLevel() {
        startNextRace()
    }

    /**
     * One-tap Next Race across all modes:
     * - First-Run Gauntlet: launches straight into Expedition Lv. 1
     * - Endless Abyssal Depth: descends to Floor N+1 with deterministic bit-mixed seed
     * - Expedition Campaign: advances cleanly to Lv. X+1
     * - Quick-Play / Practice / Daily Ranked: launches a fresh randomized maze
     */
    fun startNextRace() {
        fx.playNextRace()
        val lr = _uiState.value.liveRace
        if (lr?.isGauntlet == true) {
            startNextPlayableCampaignLevel()
            return
        }
        if (lr?.abyssalFloor != null && lr.abyssalRootSeed != null) {
            startAbyssalDescent(
                rootSeed = lr.abyssalRootSeed,
                floorNumber = lr.abyssalFloor + 1,
                cumulativeScore = lr.abyssalCumulativeScore
            )
            return
        }
        val currentCampaignId = lr?.campaignLevelId
        if (currentCampaignId != null) {
            val next = CampaignCatalog.byId(currentCampaignId + 1)
            if (next != null) {
                startCampaignLevel(next)
                return
            }
        }

        quickRaceCounter++
        val state = _uiState.value
        val archetype = if (lr != null) {
            BoardArchetype.entries.firstOrNull { it.size == lr.boardSize } ?: state.selectedArchetype
        } else {
            state.selectedArchetype
        }
        val salt = (System.currentTimeMillis() % 9973L).toInt()
        val freshSeed = "race-${quickRaceCounter}-$salt-${archetype.size}"
        // Guarantee a fresh biome transition from the previous race so Next Race always feels visually distinct
        val biomes = Biome.entries
        val derivedBiome = MazeGenerator.biomeForSeed(freshSeed)
        val nextBiome = if (lr != null && derivedBiome == lr.biome) {
            biomes[(derivedBiome.ordinal + 1) % biomes.size]
        } else {
            derivedBiome
        }
        val nextModifier = MazeGenerator.modifierForSeed(freshSeed)
        launchBoardSession(
            seed = freshSeed,
            difficulty = lr?.difficulty ?: state.selectedDifficulty,
            modifier = nextModifier,
            biome = nextBiome,
            rival = lr?.rivalPersonality ?: state.selectedRival,
            isPractice = true,
            boardSize = archetype.size,
            campaignLevel = null
        )
    }

    fun startPvpRace(room: PvpMatchRoom, currentUid: String) {
        val asHost = room.hostUid == currentUid
        val opponentName = if (asHost) {
            room.guestName.ifBlank { "Challenger" }
        } else {
            room.hostName.ifBlank { "Host" }
        }
        launchBoardSession(
            seed = room.seed,
            difficulty = room.parsedDifficulty(),
            modifier = Modifier.NORMAL,
            biome = room.parsedBiome(),
            rival = RivalPersonality.VEX,
            isPractice = true,
            boardSize = room.boardSize.toInt(),
            campaignLevel = null,
            pvpRoomId = room.id,
            pvpRoomCode = room.roomCode,
            pvpOpponentName = opponentName,
            pvpAsHost = asHost
        )
        showToast("⚔️ LIVE PVP ${room.roomCode} vs $opponentName! Race to sync Beacons & Portal!")
    }

    fun startCommunitySeedRace(seed: CommunityMazeSeed) {
        launchBoardSession(
            seed = seed.seed,
            difficulty = seed.parsedDifficulty(),
            modifier = MazeGenerator.modifierForSeed(seed.seed),
            biome = seed.parsedBiome(),
            rival = _uiState.value.selectedRival,
            isPractice = true,
            boardSize = seed.boardSize.toInt(),
            campaignLevel = null
        )
        showToast("🌍 Community Challenge: ${seed.title} by ${seed.authorName}")
    }

    fun syncLivePvpOpponentState(room: PvpMatchRoom, currentUid: String) {
        val lr = _uiState.value.liveRace ?: return
        if (lr.pvpRoomId != room.id || lr.status != RaceStatus.PLAYING) return
        val asHost = room.hostUid == currentUid
        val oppX = (if (asHost) room.guestX else room.hostX).toInt().coerceIn(0, lr.boardSize - 1)
        val oppY = (if (asHost) room.guestY else room.hostY).toInt().coerceIn(0, lr.boardSize - 1)
        val oppScore = (if (asHost) room.guestScore else room.hostScore).toInt()
        val oppBeacons = (if (asHost) room.guestBeacons else room.hostBeacons).toInt()
        val oppFinished = if (asHost) room.guestFinished else room.hostFinished
        val oppName = if (asHost) room.guestName.ifBlank { "Challenger" } else room.hostName.ifBlank { "Host" }

        val syncedRivalBeacons = lr.checkpoints.take(oppBeacons.coerceAtMost(lr.checkpoints.size)).map { it.key }.toSet()
        _uiState.update { cur ->
            val current = cur.liveRace ?: return@update cur
            if (current.pvpRoomId != room.id) return@update cur
            cur.copy(
                liveRace = current.copy(
                    rivalPos = GridPos(oppX, oppY),
                    rivalCheckpointsCleared = syncedRivalBeacons,
                    pvpOpponentName = oppName,
                    pvpOpponentScore = oppScore,
                    pvpOpponentBeacons = oppBeacons
                )
            )
        }

        if (room.parsedStatus() == PvpRoomStatus.FINISHED && room.winnerUid.isNotBlank() && room.winnerUid != currentUid && oppFinished) {
            finishAsLoss("⚔️ $oppName finished first in PvP Room ${room.roomCode}!", lr.tickCount, lr.stamina)
        }
    }

    fun applyCloudMergedSave(cloud: CloudProfileSave) {
        viewModelScope.launch {
            repository.applyCloudMergedSave(cloud)
        }
    }

    fun updateLiveBalanceConfig(config: LiveBalanceConfig) {
        activeLiveBalance = config
        _uiState.update { cur ->
            val lr = cur.liveRace ?: return@update cur
            cur.copy(liveRace = lr.copy(liveBalance = config))
        }
    }

    /**
     * Launches today's canonical 21×21 Daily Ranked maze racing live against a verified cloud Ghost Racer.
     */
    fun startDailyWithGhost(ghost: DailyRankedGhostEntry, dateSeed: String = todayUtcSeed()) {
        val decodedFrames = runCatching {
            val ticks = Replay16BitCodec.decodeFromBase64(ghost.compressedInputs)
            Replay16BitCodec.toGhostFrames(ticks, boardSize = MAZE_SIZE)
        }.getOrElse {
            GhostReplayCodec.decodeFrames(ghost.compressedInputs)
        }
        val biome = MazeGenerator.biomeForSeed(dateSeed)
        val modifier = MazeGenerator.modifierForSeed(dateSeed)
        launchBoardSession(
            seed = dateSeed,
            difficulty = _uiState.value.selectedDifficulty,
            modifier = modifier,
            biome = biome,
            rival = _uiState.value.selectedRival,
            isPractice = false,
            boardSize = MAZE_SIZE,
            campaignLevel = null,
            ghostFramesOverride = decodedFrames,
            ghostRacerName = ghost.displayName
        )
    }

    /**
     * Launches an Endless Abyssal Depth procedural floor (1..∞) using deterministic SplitMix + Mulberry32.
     */
    fun startAbyssalDescent(
        rootSeed: Long = (System.currentTimeMillis() % 899_999L) + 100_000L,
        floorNumber: Int = 1,
        cumulativeScore: Int = 0
    ) {
        val cfg = AbyssalDepthGenerator.generateFloorConfig(rootSeed, floorNumber)
        val floorSeedStr = "abyss-$rootSeed-f${cfg.floorNumber}-${cfg.floorSeed.toString(16).takeLast(6)}"
        val biome = cfg.biome.toBiome()
        val difficulty = when {
            cfg.floorNumber >= 10 -> Difficulty.HARD
            cfg.floorNumber >= 4 -> Difficulty.NORMAL
            else -> Difficulty.EASY
        }
        val rival = RivalPersonality.entries[cfg.floorNumber % RivalPersonality.entries.size]
        launchBoardSession(
            seed = floorSeedStr,
            difficulty = difficulty,
            modifier = Modifier.NORMAL,
            biome = biome,
            rival = rival,
            isPractice = true,
            boardSize = cfg.boardSize,
            campaignLevel = null,
            abyssalFloor = cfg.floorNumber,
            abyssalRootSeed = rootSeed,
            abyssalCumulativeScore = cumulativeScore,
            abyssalAiScalar = cfg.aiAggressionScalar,
            abyssalGemMultiplier = cfg.gemMultiplier,
            abyssalParTimerOverride = cfg.parTimeSeconds
        )
    }

    /**
     * Launches the interactive, diegetic First-Run Playable Gauntlet (zero walls of text):
     * Swipe to Dig -> Detonate TNT -> Charge Beacon -> Tap Surge -> Escape Portal.
     */
    fun startFirstRunGauntlet() {
        stopRaceLoops()
        gauntletController.reset()
        recordedActions.clear()
        recordedTickInputs.clear()
        raceStartElapsedMs = System.currentTimeMillis()
        remoteSession = null

        val generated = FirstRunGauntletController.buildGauntletMaze()
        val startPos = FirstRunGauntletController.GAUNTLET_START
        val finishPos = GridPos(generated.finishX, generated.finishY)
        val baseVision = 5
        val initialExplored = computeVisibleKeys(startPos, baseVision, generated.boardSize)
        val totalSpan = MazeGenerator.computeCircuitDistance(startPos, generated.checkpoints, finishPos).coerceAtLeast(1)

        val live = LiveRaceState(
            seed = "first-run-gauntlet",
            difficulty = Difficulty.EASY,
            modifier = Modifier.NORMAL,
            biome = Biome.CAVERN,
            rivalPersonality = RivalPersonality.MOSS,
            isPractice = true,
            boardSize = generated.boardSize,
            finishPos = finishPos,
            checkpoints = generated.checkpoints,
            totalCircuitSpan = totalSpan,
            playerPos = startPos,
            rivalPos = GridPos(1, 1),
            myTrail = listOf(startPos),
            unlockedAbilities = setOf(MinerAbility.QUICK_STEP, MinerAbility.FEVER_OVERDRIVE),
            blocks = generated.blocks,
            gems = generated.gems,
            powers = generated.powers,
            explored = initialExplored,
            timer = 180,
            sonarCharges = 2,
            viewSize = 7,
            baseVision = baseVision,
            currentVision = baseVision,
            isGauntlet = true,
            gauntletStep = gauntletController.currentStep.value,
            liveBalance = activeLiveBalance
        )

        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                liveRace = live
            )
        }
        syncSoundtrackContext(live, pausedOverride = false)
        fx.startSoundtrack(Biome.CAVERN)
        showToast("⛏️ GAUNTLET: Swipe right to dig through the dirt wall!")
        startRaceLoops(RivalPersonality.MOSS, aiScalar = 0f)
    }

    private fun launchBoardSession(
        seed: String,
        difficulty: Difficulty,
        modifier: Modifier,
        biome: Biome,
        rival: RivalPersonality,
        isPractice: Boolean,
        boardSize: Int,
        campaignLevel: CampaignLevel?,
        pvpRoomId: String? = null,
        pvpRoomCode: String? = null,
        pvpOpponentName: String? = null,
        pvpAsHost: Boolean = false,
        ghostFramesOverride: List<GhostFrame> = emptyList(),
        ghostRacerName: String? = null,
        abyssalFloor: Int? = null,
        abyssalRootSeed: Long? = null,
        abyssalCumulativeScore: Int = 0,
        abyssalAiScalar: Float = 1f,
        abyssalGemMultiplier: Float = 1f,
        abyssalParTimerOverride: Int? = null
    ) {
        stopRaceLoops()
        val prof = profile.value
        // Daily Ranked 21x21 stays strictly canonical at (19,19) for 0-tolerance SHA-256 verification;
        // Expedition Campaign & Quick-Play/Practice boards build strategic braided labyrinths with
        // multi-quadrant Relay Beacons and unpredictable far finish portals!
        val generated = MazeGenerator.genMaze(
            seed = seed,
            boardSize = boardSize,
            randomEndpoint = isPractice,
            strategicMaze = isPractice
        )
        val finishPos = GridPos(generated.finishX, generated.finishY)
        val checkpointKeys = generated.checkpoints.map { it.key }.toSet()

        // Separate RNG streams: gameplayRng matches ReplaySimulator bit-for-bit on 21x21 Ranked!
        gameplayRng = DeterministicRng.mulberry(
            DeterministicRng.fnv1aHash("$seed|gameplay|${difficulty.wireName}|${modifier.wireName}")
        )
        rivalRng = DeterministicRng.mulberry(
            DeterministicRng.fnv1aHash("$seed|rival|${rival.wireName}")
        )
        recordedActions.clear()
        recordedTickInputs.clear()
        raceStartElapsedMs = System.currentTimeMillis()
        remoteSession = null

        val traps = if (biome == Biome.RUINS) {
            MazeGenerator.generateRuinsTraps(seed, generated.maze, generated.boardSize) - finishPos.key - checkpointKeys
        } else {
            emptySet()
        }
        val lavaPuddles = if (isPractice && biome == Biome.VOLCANO) {
            MazeGenerator.generateVolcanoLavaPuddles(seed, generated.maze, generated.boardSize) - finishPos.key - "1,1" - checkpointKeys
        } else {
            emptySet()
        }
        val sandDrifts = if (isPractice && biome == Biome.DESERT) {
            MazeGenerator.generateDesertSandDrifts(seed, generated.maze, generated.boardSize) - finishPos.key - "1,1" - checkpointKeys
        } else {
            emptySet()
        }

        var baseVision = if (modifier == Modifier.DARKNESS) 2 else 3
        if (biome == Biome.CAVERN || biome == Biome.DESERT) {
            baseVision = max(2, baseVision - 1)
        }
        if (isPractice) {
            baseVision += prof.lanternLevel.coerceAtMost(3)
        }

        val clearedLevels = campaignProgress.value.filter { it.stars > 0 }.map { it.levelId }.toSet()
        val activeAbilities = if (isPractice) {
            CampaignCatalog.unlockedAbilities(clearedLevels)
        } else {
            setOf(MinerAbility.QUICK_STEP)
        }

        val bonusSonarFromAbility = if (isPractice && MinerAbility.CRYSTAL_MAGNET in activeAbilities) 1 else 0
        val startingSonar = if (modifier == Modifier.NO_SONAR) {
            (if (isPractice && prof.sonarLevel > 0) 1 else 0) + bonusSonarFromAbility
        } else {
            1 + (if (isPractice) prof.sonarLevel.coerceAtMost(3) else 0) + bonusSonarFromAbility
        }

        // Expand race clock for multi-beacon strategic labyrinths while keeping Daily Ranked canonical
        val strategicTimeExpansion = if (isPractice) {
            when {
                generated.boardSize >= 29 -> 140
                generated.boardSize >= 25 -> 95
                generated.boardSize >= 19 -> 55
                else -> 20
            }
        } else {
            0
        }
        val startTimer = abyssalParTimerOverride ?: (
            difficulty.startTimer +
                strategicTimeExpansion +
                if (isPractice && MinerAbility.MASTER_DRILL in activeAbilities) 15 else 0
            )
        val startShield = isPractice && MinerAbility.AEGIS_START in activeAbilities

        val initialExplored = computeVisibleKeys(GridPos(START_X, START_Y), baseVision, generated.boardSize)
        val totalCircuitSpan = MazeGenerator.computeCircuitDistance(
            from = GridPos(START_X, START_Y),
            uncapturedBeacons = generated.checkpoints,
            finishPos = finishPos
        ).coerceAtLeast(1)

        val live = LiveRaceState(
            seed = seed,
            difficulty = difficulty,
            modifier = modifier,
            biome = biome,
            rivalPersonality = rival,
            isPractice = isPractice,
            boardSize = generated.boardSize,
            finishPos = finishPos,
            checkpoints = generated.checkpoints,
            totalCircuitSpan = totalCircuitSpan,
            campaignLevelId = campaignLevel?.id,
            campaignTitle = campaignLevel?.title,
            unlockedAbilities = activeAbilities,
            blocks = generated.blocks,
            gems = generated.gems,
            powers = generated.powers,
            traps = traps,
            lavaPuddles = lavaPuddles,
            sandDrifts = sandDrifts,
            explored = initialExplored,
            timer = startTimer,
            activePower = if (startShield) PowerupType.SHIELD else null,
            powerUntilTick = if (startShield) 15 else -1,
            sonarCharges = startingSonar,
            baseVision = baseVision,
            currentVision = baseVision,
            pvpRoomId = pvpRoomId,
            pvpRoomCode = pvpRoomCode,
            pvpOpponentName = pvpOpponentName,
            pvpAsHost = pvpAsHost,
            ghostFrames = ghostFramesOverride,
            ghostRacerName = ghostRacerName,
            abyssalFloor = abyssalFloor,
            abyssalRootSeed = abyssalRootSeed,
            abyssalCumulativeScore = abyssalCumulativeScore,
            abyssalAiScalar = abyssalAiScalar,
            abyssalGemMultiplier = abyssalGemMultiplier,
            liveBalance = activeLiveBalance
        )

        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                seedInput = if (campaignLevel == null && abyssalFloor == null) seed else it.seedInput,
                liveRace = live
            )
        }

        // Load saved ghost trail & optional remote session asynchronously
        viewModelScope.launch {
            val ghost = repository.getGhostTrail(seed)
            _uiState.update { cur ->
                val lr = cur.liveRace ?: return@update cur
                val fallbackFrames = if (lr.ghostFrames.isEmpty() && ghost.isNotEmpty()) {
                    GhostReplayCodec.framesFromTrail(ghost, max(ghost.size, 20))
                } else {
                    lr.ghostFrames
                }
                cur.copy(liveRace = lr.copy(ghostTrail = ghost, ghostFrames = fallbackFrames))
            }
        }
        if (!isPractice && prof.verifierServerUrl.isNotBlank()) {
            viewModelScope.launch {
                remoteSession = repository.requestRemoteSession(prof.verifierServerUrl)
            }
        }

        // Start adaptive biome soundtrack with live spatial race telemetry
        syncSoundtrackContext(live, pausedOverride = false)
        fx.startSoundtrack(biome)

        val beaconHint = if (generated.checkpoints.isNotEmpty()) {
            " • Sync ${generated.checkpoints.size} Relay Beacons to unlock Portal!"
        } else {
            ""
        }
        val banner = when {
            abyssalFloor != null -> "🕳️ ABYSSAL FLOOR $abyssalFloor • ${biome.wireName} ${generated.boardSize}×${generated.boardSize} (${String.format(Locale.US, "%.2f", abyssalGemMultiplier)}× Gems)"
            ghostRacerName != null -> "👻 DAILY RANKED vs Ghost $ghostRacerName • ${biome.wireName}"
            campaignLevel != null -> "Lv.${campaignLevel.id} ${campaignLevel.title} • ${campaignLevel.archetype.label}$beaconHint"
            else -> "${biome.wireName} • ${AppStrings.biomeDesc(currentStrings(), biome)}$beaconHint"
        }
        showToast(banner)
        startRaceLoops(rival, aiScalar = abyssalAiScalar)
    }

    private fun syncSoundtrackContext(race: LiveRaceState, pausedOverride: Boolean = race.paused) {
        val playerDist = MazeGenerator.computeCircuitDistance(
            from = race.playerPos,
            uncapturedBeacons = race.remainingPlayerCheckpoints,
            finishPos = race.finishPos
        )
        val rivalDist = MazeGenerator.computeCircuitDistance(
            from = race.rivalPos,
            uncapturedBeacons = race.remainingRivalCheckpoints,
            finishPos = race.finishPos
        )
        fx.updateSoundtrackContext(
            biome = race.biome,
            timerRemaining = race.timer,
            combo = race.combo,
            paused = pausedOverride,
            rivalDistToFinish = rivalDist,
            playerDistToFinish = playerDist,
            overdriveActive = race.overdriveActive
        )
    }

    private fun startRaceLoops(rival: RivalPersonality, aiScalar: Float = 1f) {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                onGameTick()
            }
        }
        if (aiScalar <= 0f) return
        val tunedBaseMs = when (rival) {
            RivalPersonality.GOLDIE -> (rival.stepIntervalMs / activeLiveBalance.goldieSpeedMultiplier).toLong()
            RivalPersonality.VEX -> (activeLiveBalance.vexDrillCooldownMs / 2L).coerceIn(360L, 1200L)
            else -> rival.stepIntervalMs
        }
        val finalDelayMs = (tunedBaseMs / aiScalar.coerceIn(0.6f, 1.8f)).toLong().coerceAtLeast(240L)
        rivalJob = viewModelScope.launch {
            while (true) {
                delay(finalDelayMs)
                onRivalTick()
            }
        }
    }

    private fun stopRaceLoops() {
        timerJob?.cancel()
        rivalJob?.cancel()
        sonarJob?.cancel()
        autoStepJob?.cancel()
        timerJob = null
        rivalJob = null
        sonarJob = null
        autoStepJob = null
    }

    fun onLifecyclePause() {
        val lr = _uiState.value.liveRace ?: return
        syncSoundtrackContext(lr, pausedOverride = true)
        if (_uiState.value.screen == AppScreen.PLAYING && lr.status == RaceStatus.PLAYING && !lr.paused) {
            _uiState.update { it.copy(liveRace = lr.copy(paused = true)) }
            showToast(currentStrings().toastAutoPaused)
        }
    }

    fun onLifecycleResume() {
        val lr = _uiState.value.liveRace ?: return
        if (_uiState.value.screen == AppScreen.PLAYING && lr.status == RaceStatus.PLAYING && !lr.paused) {
            syncSoundtrackContext(lr, pausedOverride = false)
            if (profile.value.musicEnabled) fx.startSoundtrack(lr.biome)
        }
    }

    fun togglePause() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING) return
        val nextPaused = !lr.paused
        syncSoundtrackContext(lr, pausedOverride = nextPaused)
        if (!nextPaused && profile.value.musicEnabled) {
            fx.startSoundtrack(lr.biome)
        }
        _uiState.update { it.copy(liveRace = lr.copy(paused = nextPaused)) }
    }

    fun cycleZoom() {
        val lr = _uiState.value.liveRace ?: return
        val next = when (lr.viewSize) {
            7 -> 5
            5 -> 9
            9 -> 11
            else -> 7
        }
        _uiState.update { it.copy(liveRace = lr.copy(viewSize = next)) }
    }

    /**
     * Triggers Sonar Pulse + Intelligent BFS Pathfinder Breadcrumbs toward the Finish Portal!
     */
    fun triggerSonar() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return
        if (lr.sonarCharges <= 0) {
            showToast(currentStrings().toastNoSonar)
            return
        }
        fx.playSonar()
        val expandedVision = 9
        val beaconPingKeys = lr.remainingPlayerCheckpoints.flatMap { cp ->
            computeVisibleKeys(cp, 1, lr.boardSize)
        }.toSet()
        val newExplored = lr.explored +
            computeVisibleKeys(lr.playerPos, expandedVision, lr.boardSize) +
            beaconPingKeys

        // Compute optimal BFS pathfinder trail toward the next strategic objective (nearest Beacon or unlocked Portal)
        val objective = lr.nextPlayerObjective
        val bestPath = MazeGenerator.bfsPath(
            blocks = lr.blocks,
            sx = lr.playerPos.x,
            sy = lr.playerPos.y,
            tx = objective.x,
            ty = objective.y,
            allowMine = false,
            boardSize = lr.boardSize
        ) ?: MazeGenerator.bfsPath(
            blocks = lr.blocks,
            sx = lr.playerPos.x,
            sy = lr.playerPos.y,
            tx = objective.x,
            ty = objective.y,
            allowMine = true,
            boardSize = lr.boardSize
        ) ?: emptyList()

        _uiState.update {
            it.copy(
                liveRace = lr.copy(
                    sonarCharges = lr.sonarCharges - 1,
                    sonarActive = true,
                    currentVision = expandedVision,
                    explored = newExplored,
                    hintPath = bestPath.take(18)
                )
            )
        }
        sonarJob?.cancel()
        sonarJob = viewModelScope.launch {
            delay(2800L)
            _uiState.update { cur ->
                val current = cur.liveRace ?: return@update cur
                val restoredVision = if (current.activePower == PowerupType.LANTERN) 5 else current.baseVision
                cur.copy(
                    liveRace = current.copy(
                        sonarActive = false,
                        currentVision = restoredVision,
                        hintPath = emptyList()
                    )
                )
            }
        }
    }

    private fun onGameTick() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return

        val nextTick = lr.tickCount + 1
        val drain = ReplaySimulator.timerDrainForTick(nextTick, lr.activePower)
        val nextTimer = lr.timer - drain

        var nextStamina = (lr.stamina + lr.biome.rule.staminaPerTick).coerceIn(0.0, 100.0)
        if (lr.biome == Biome.VOLCANO && lr.activePower == PowerupType.SHIELD) {
            nextStamina = min(100.0, nextStamina + 0.4)
        }

        var nextPower = lr.activePower
        var nextVision = lr.currentVision
        if (nextPower != null && nextTick > lr.powerUntilTick) {
            nextPower = null
            if (!lr.sonarActive) nextVision = lr.baseVision
        }

        if (nextTimer <= 0) {
            finishAsLoss(currentStrings().timeExpiredReason, nextTick, nextStamina)
            return
        }

        // Urgent final-10-second race countdown tick
        if (nextTimer in 1..10) {
            fx.playTimerTick(nextTimer)
        }

        // Fever 5x 10-second countdown & 3-second timed Combo decay (Practice/Campaign/Quick-Play)
        var nextFeverSec = lr.feverRemainingSec
        var nextFeverCharge = lr.feverCharge
        var nextOverdrive = lr.overdriveActive
        var nextCombo = lr.combo
        var nextComboTimer = lr.comboTimerTicks
        if (lr.isPractice) {
            if (nextFeverSec > 0) {
                nextFeverSec = (nextFeverSec - 1).coerceAtLeast(0)
                if (nextFeverSec == 0) {
                    nextFeverCharge = 0
                    nextOverdrive = false
                }
            } else if (nextFeverCharge > 0 && nextComboTimer == 0) {
                // Live-Ops tunable Fever charge decay when player stops chaining actions
                val decayPoints = ceil(activeLiveBalance.feverDecayRatePerSec * 15f).toInt().coerceAtLeast(1)
                nextFeverCharge = (nextFeverCharge - decayPoints).coerceAtLeast(0)
            }
            if (nextComboTimer > 0) {
                nextComboTimer = (nextComboTimer - 1).coerceAtLeast(0)
                if (nextComboTimer == 0 && nextFeverSec == 0) {
                    nextCombo = 0
                    nextOverdrive = false
                }
            }
        }

        val nextSurgeCooldown = (lr.surgeCooldownTicks - 1).coerceAtLeast(0)
        val updated = lr.copy(
            tickCount = nextTick,
            timer = nextTimer,
            stamina = nextStamina,
            activePower = nextPower,
            currentVision = nextVision,
            combo = nextCombo,
            comboTimerTicks = nextComboTimer,
            surgeCooldownTicks = nextSurgeCooldown,
            feverCharge = nextFeverCharge,
            feverRemainingSec = nextFeverSec,
            overdriveActive = nextOverdrive
        )
        syncSoundtrackContext(updated, pausedOverride = false)

        _uiState.update { it.copy(liveRace = updated) }
    }

    /**
     * Active Kinetic SURGE Capability (Expedition & Instant Play boards):
     * - Facing open floor / sand drift: dashes up to 2 tiles forward (sweeps sand automatically!)
     * - Facing a rock block: delivers a Heavy Power Strike (+2 bonus damage shatter blow!)
     * Costs 18 Stamina with a 6-second tactical cooldown.
     */
    fun triggerMinerSurge() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return
        if (!lr.isPractice) {
            showToast("Daily Ranked 21×21 is pure canonical skill (no active Surge).")
            return
        }
        if (lr.surgeCooldownTicks > 0) {
            fx.playHazard()
            showToast("⚡ Surge recharging (${lr.surgeCooldownTicks}s)")
            return
        }
        if (lr.stamina < 18.0) {
            fx.playHazard()
            showToast("⚡ Need 18 Stamina to trigger Kinetic Surge")
            return
        }

        val dx = lr.facingDir.x.coerceIn(-1, 1)
        val dy = lr.facingDir.y.coerceIn(-1, 1)
        val dirX = if (dx == 0 && dy == 0) 1 else dx
        val dirY = if (dx == 0 && dy == 0) 0 else dy
        val frontX = lr.playerPos.x + dirX
        val frontY = lr.playerPos.y + dirY
        if (frontX !in 0 until lr.boardSize || frontY !in 0 until lr.boardSize) {
            fx.playHazard()
            return
        }

        val frontKey = "$frontX,$frontY"
        val frontBlock = lr.blocks[frontKey]
        if (frontBlock != null && frontBlock.special == BlockSpecial.LOCKED && !lr.hasKey) {
            fx.playHazard()
            showToast(currentStrings().toastLocked)
            return
        }

        autoStepJob?.cancel()
        fx.playDashSurge()
        hitPulseSeq++
        recordedTickInputs.add(
            ReplayTickInput(
                direction = MoveDir.fromDelta(dirX, dirY),
                triggerSurge = true
            )
        )
        if (lr.isGauntlet) {
            gauntletController.onKineticSurgeTriggered()
        }
        val nextGauntletStep = if (lr.isGauntlet) gauntletController.currentStep.value else lr.gauntletStep

        if (frontBlock != null) {
            // Heavy Power Strike: shave 2 extra HP off the target block, then execute a mining swing!
            val updatedBlocks = lr.blocks.toMutableMap()
            val reducedHp = (frontBlock.hp - 2).coerceAtLeast(1)
            updatedBlocks[frontKey] = frontBlock.copy(hp = reducedHp)
            val pop = nextPopup(frontX, frontY, "⚡ POWER STRIKE!", isPositive = true)
            _uiState.update {
                it.copy(
                    liveRace = lr.copy(
                        blocks = updatedBlocks,
                        hiddenRevealed = lr.hiddenRevealed + frontKey,
                        stamina = (lr.stamina - 18.0).coerceAtLeast(0.0),
                        surgeCooldownTicks = 6,
                        surgePulseTick = hitPulseSeq,
                        gauntletStep = nextGauntletStep,
                        popups = (lr.popups + pop).takeLast(10)
                    )
                )
            }
            schedulePopupCleanup(listOf(pop.id))
            mineAt(GridPos(frontX, frontY))
            return
        }

        // Open Corridor / Sand Drift Dash: clear sand drift if present & dash up to 2 tiles forward!
        val pop = nextPopup(lr.playerPos.x, lr.playerPos.y, "⚡ SURGE DASH!", isPositive = true)
        val clearedSand = lr.sandDrifts - frontKey - "${frontX + dirX},${frontY + dirY}"
        _uiState.update {
            it.copy(
                liveRace = lr.copy(
                    sandDrifts = clearedSand,
                    stamina = (lr.stamina - 18.0).coerceAtLeast(0.0),
                    surgeCooldownTicks = 6,
                    surgePulseTick = hitPulseSeq,
                    gauntletStep = nextGauntletStep,
                    popups = (lr.popups + pop).takeLast(10)
                )
            )
        }
        schedulePopupCleanup(listOf(pop.id))
        tryMove(dirX, dirY)
        val afterFirst = _uiState.value.liveRace ?: return
        if (afterFirst.status == RaceStatus.PLAYING) {
            val secondX = afterFirst.playerPos.x + dirX
            val secondY = afterFirst.playerPos.y + dirY
            if (secondX in 0 until afterFirst.boardSize &&
                secondY in 0 until afterFirst.boardSize &&
                !afterFirst.blocks.containsKey("$secondX,$secondY")
            ) {
                tryMove(dirX, dirY)
            }
        }
    }

    private fun onRivalTick() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused || lr.pvpRoomId != null || lr.isGauntlet) return

        val personality = lr.rivalPersonality
        val rivalRemainingBeacons = lr.remainingRivalCheckpoints
        val playerRemDist = MazeGenerator.computeCircuitDistance(
            from = lr.playerPos,
            uncapturedBeacons = lr.remainingPlayerCheckpoints,
            finishPos = lr.finishPos
        )
        val rivalRemDist = MazeGenerator.computeCircuitDistance(
            from = lr.rivalPos,
            uncapturedBeacons = rivalRemainingBeacons,
            finishPos = lr.finishPos
        )
        val rivalTrailing = lr.isPractice && rivalRemDist > playerRemDist + 5
        val rivalWayAhead = lr.isPractice && playerRemDist > rivalRemDist + 11

        // Subtle competitive pacing: when far ahead, Rival takes an occasional tactical scan beat
        // so skilled wall-breaching shortcuts by the player can produce dramatic comebacks.
        if (rivalWayAhead && rivalRng() < 0.22) {
            return
        }

        // Strategic multi-leg objective: hunt nearest uncaptured Beacon first, then sprint to Finish Portal
        var target = rivalRemainingBeacons.minByOrNull {
            abs(it.x - lr.rivalPos.x) + abs(it.y - lr.rivalPos.y)
        } ?: lr.finishPos

        // Only detour for gems if not trailing in a competitive showdown
        if (personality.seeksGems && !rivalTrailing && lr.gems.isNotEmpty()) {
            var bestGem: GridPos? = null
            var bestDist = Int.MAX_VALUE
            for (gKey in lr.gems) {
                val parts = gKey.split(",")
                val gx = parts[0].toInt()
                val gy = parts[1].toInt()
                val d = abs(gx - lr.rivalPos.x) + abs(gy - lr.rivalPos.y)
                if (d < bestDist) {
                    bestDist = d
                    bestGem = GridPos(gx, gy)
                }
            }
            if (bestGem != null && bestDist < 7) target = bestGem
        }

        val path = MazeGenerator.bfsPath(
            blocks = lr.blocks,
            sx = lr.rivalPos.x,
            sy = lr.rivalPos.y,
            tx = target.x,
            ty = target.y,
            allowMine = false,
            boardSize = lr.boardSize
        )

        // Distinct 4-Rival Personality Behaviors + Strategic Wall-Breaching Shortcuts:
        // - VEX: Aggressive breacher, smashes walls up to HP 3 directly toward beacons/exit
        // - GOLDIE: Fast sprinter, dashes 2 tiles along open corridors
        // - RUBBLE: Chaotic wildcard, mixes surprise feints with opportunistic wall breaches
        // - MOSS: Disciplined maze tactician, cuts shortcuts when corridor detour is too long
        val maxBreakHp = if (personality == RivalPersonality.VEX || rivalTrailing) 3 else 2
        val effectiveBias = (personality.mineBias + if (rivalTrailing) 0.22 else 0.0).coerceAtMost(0.95)
        val wantsMineShortcut = lr.isPractice && (
            personality == RivalPersonality.VEX ||
                path == null ||
                path.size > 12
            ) && rivalRng() < effectiveBias * (if (personality == RivalPersonality.VEX) 0.80 else 0.58)

        if (wantsMineShortcut) {
            val preferredDx = (target.x - lr.rivalPos.x).coerceIn(-1, 1)
            val preferredDy = (target.y - lr.rivalPos.y).coerceIn(-1, 1)
            val candidateDirs = listOfNotNull(
                if (preferredDx != 0) preferredDx to 0 else null,
                if (preferredDy != 0) 0 to preferredDy else null,
                1 to 0, 0 to 1, -1 to 0, 0 to -1
            ).distinct()
            for ((dx, dy) in candidateDirs) {
                val nx = lr.rivalPos.x + dx
                val ny = lr.rivalPos.y + dy
                if (nx !in 1 until lr.boardSize - 1 || ny !in 1 until lr.boardSize - 1) continue
                val k = "$nx,$ny"
                val b = lr.blocks[k]
                if (b != null && b.special != BlockSpecial.LOCKED && b.hp <= maxBreakHp) {
                    val updatedBlocks = lr.blocks.toMutableMap()
                    if (b.hp <= 1) updatedBlocks.remove(k)
                    else updatedBlocks[k] = b.copy(hp = b.hp - 1)
                    _uiState.update { it.copy(liveRace = lr.copy(blocks = updatedBlocks)) }
                    return
                }
            }
        }

        if (path.isNullOrEmpty()) return

        // Rubble chaotic feint (suppressed when trailing hard so races stay tense)
        if (lr.isPractice && personality == RivalPersonality.RUBBLE && !rivalTrailing && rivalRng() < 0.12) {
            val openNeighbors = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).mapNotNull { (dx, dy) ->
                val nx = lr.rivalPos.x + dx
                val ny = lr.rivalPos.y + dy
                if (nx in 1 until lr.boardSize - 1 && ny in 1 until lr.boardSize - 1 && !lr.blocks.containsKey("$nx,$ny")) {
                    GridPos(nx, ny)
                } else null
            }
            if (openNeighbors.isNotEmpty()) {
                val feint = openNeighbors[(rivalRng() * openNeighbors.size).toInt().coerceIn(0, openNeighbors.lastIndex)]
                _uiState.update { it.copy(liveRace = lr.copy(rivalPos = feint)) }
                return
            }
        }

        // Goldie fast sprinter (45% burst) OR trailing Rival pursuit surge (28% burst) along open corridors
        val burstChance = when {
            personality == RivalPersonality.GOLDIE -> 0.45
            rivalTrailing -> 0.28
            else -> 0.0
        }
        val canBurst = lr.isPractice &&
            burstChance > 0.0 &&
            path.size >= 2 &&
            !lr.blocks.containsKey(path[1].key) &&
            path[0] != lr.finishPos &&
            path[0].key !in rivalRemainingBeacons.map { it.key }.toSet() &&
            rivalRng() < burstChance

        val nextStep = if (canBurst) path[1] else path.first()
        if (lr.blocks.containsKey(nextStep.key)) return

        // Check if Rival captured an uncaptured Strategic Relay Beacon
        var nextRivalBeacons = lr.rivalCheckpointsCleared
        val capturedBeacon = rivalRemainingBeacons.any { it == nextStep }
        if (capturedBeacon) {
            nextRivalBeacons = nextRivalBeacons + nextStep.key
            val rCount = nextRivalBeacons.size
            val total = lr.checkpoints.size
            val rivalAlert = if (rCount >= total) {
                "🚨 ${personality.wireName} unlocked the Finish Portal!"
            } else {
                "🤖 ${personality.wireName} synced Beacon $rCount/$total!"
            }
            showToast(rivalAlert)
        }

        val rivalPortalUnlocked = lr.checkpoints.isEmpty() || nextRivalBeacons.size >= lr.checkpoints.size
        if (nextStep == lr.finishPos && rivalPortalUnlocked) {
            _uiState.update {
                it.copy(
                    liveRace = lr.copy(
                        rivalPos = nextStep,
                        rivalCheckpointsCleared = nextRivalBeacons
                    )
                )
            }
            finishAsLoss(currentStrings().rivalWonReason, lr.tickCount, lr.stamina)
        } else {
            val updated = lr.copy(
                rivalPos = nextStep,
                rivalCheckpointsCleared = nextRivalBeacons
            )
            syncSoundtrackContext(updated, pausedOverride = false)
            _uiState.update { it.copy(liveRace = updated) }
        }
    }

    /**
     * Handles tapping directly on any tile on the Maze Canvas.
     * Supports 1-tap adjacent move/mine AND smart multi-tile corridor auto-stepping!
     */
    fun onBoardCellTapped(x: Int, y: Int) {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return
        if (x !in 0 until lr.boardSize || y !in 0 until lr.boardSize) return
        autoStepJob?.cancel()

        val dx = x - lr.playerPos.x
        val dy = y - lr.playerPos.y
        val dist = abs(dx) + abs(dy)
        if (dist == 1) {
            val key = "$x,$y"
            val block = lr.blocks[key]
            if (block != null) {
                if (block.special == BlockSpecial.HIDDEN && key !in lr.hiddenRevealed) {
                    fx.playStep()
                    _uiState.update {
                        it.copy(
                            liveRace = lr.copy(
                                hiddenRevealed = lr.hiddenRevealed + key,
                                targetBlock = GridPos(x, y),
                                facingDir = GridPos(dx, dy)
                            )
                        )
                    }
                    showToast(currentStrings().toastHiddenRevealed)
                    return
                }
                _uiState.update { it.copy(liveRace = lr.copy(facingDir = GridPos(dx, dy))) }
                mineAt(GridPos(x, y))
            } else {
                tryMove(dx, dy)
            }
        } else if (dist == 0) {
            mineTarget()
        } else {
            // Intelligent multi-tile corridor pathfinder (up to 12 smooth steps along explored open path)
            val tapKey = "$x,$y"
            val maxAutoSteps = if (lr.isPractice) 12 else 5
            if (tapKey in lr.explored && !lr.blocks.containsKey(tapKey) && dist <= maxAutoSteps) {
                val miniPath = MazeGenerator.bfsPath(
                    blocks = lr.blocks,
                    sx = lr.playerPos.x,
                    sy = lr.playerPos.y,
                    tx = x,
                    ty = y,
                    allowMine = false,
                    boardSize = lr.boardSize
                )
                if (!miniPath.isNullOrEmpty() && miniPath.size <= maxAutoSteps) {
                    autoStepJob = viewModelScope.launch {
                        for (step in miniPath) {
                            val current = _uiState.value.liveRace ?: break
                            if (current.status != RaceStatus.PLAYING || current.paused) break
                            val sdx = step.x - current.playerPos.x
                            val sdy = step.y - current.playerPos.y
                            if (abs(sdx) + abs(sdy) != 1) break
                            tryMove(sdx, sdy)
                            delay(82L)
                        }
                    }
                    return
                }
            }
            val stepDx = if (abs(dx) >= abs(dy)) dx.coerceIn(-1, 1) else 0
            val stepDy = if (abs(dy) > abs(dx)) dy.coerceIn(-1, 1) else 0
            if (stepDx != 0 || stepDy != 0) {
                tryMove(stepDx, stepDy)
            } else {
                showToast(currentStrings().toastTapAdjacent)
            }
        }
    }

    fun tryMove(dx: Int, dy: Int) {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return
        if (abs(dx) + abs(dy) != 1) return

        val nx = lr.playerPos.x + dx
        val ny = lr.playerPos.y + dy
        if (nx !in 0 until lr.boardSize || ny !in 0 until lr.boardSize) return
        val nextKey = "$nx,$ny"
        val block = lr.blocks[nextKey]

        if (block != null) {
            if (block.special == BlockSpecial.HIDDEN && nextKey !in lr.hiddenRevealed) {
                fx.playStep()
                _uiState.update {
                    it.copy(
                        liveRace = lr.copy(
                            hiddenRevealed = lr.hiddenRevealed + nextKey,
                            targetBlock = GridPos(nx, ny)
                        )
                    )
                }
                showToast(currentStrings().toastHiddenRevealed)
                return
            }
            _uiState.update { it.copy(liveRace = lr.copy(targetBlock = GridPos(nx, ny))) }
            showToast(currentStrings().toastBlocked)
            return
        }

        // Desert Sand Drift clearing (Practice/Campaign): 1 tap sweeps sand drift clear before stepping in!
        if (lr.isPractice && nextKey in lr.sandDrifts) {
            val clearedDrifts = lr.sandDrifts - nextKey
            val clearPts = if (lr.feverRemainingSec > 0) 25 else 5
            fx.playBlockBreak(BlockKind.DIRT, lr.combo)
            hitPulseSeq++
            val pop = nextPopup(nx, ny, "🏜️ +$clearPts", isPositive = true)
            _uiState.update {
                it.copy(
                    liveRace = lr.copy(
                        sandDrifts = clearedDrifts,
                        facingDir = GridPos(dx, dy),
                        hitPulseTick = hitPulseSeq,
                        lastHitPos = GridPos(nx, ny),
                        lastHitKind = BlockKind.DIRT,
                        lastHitBroke = true,
                        lastHitExploded = false,
                        score = lr.score + clearPts,
                        popups = (lr.popups + pop).takeLast(10)
                    )
                )
            }
            schedulePopupCleanup(listOf(pop.id))
            return
        }

        val actionTick = lr.tickCount
        recordedActions.add(ReplayAction.Move(dx = dx, dy = dy, tick = actionTick))

        var px = nx
        var py = ny
        val trail = lr.myTrail.toMutableList()
        trail.add(GridPos(px, py))

        val staminaGain = if (lr.activePower == PowerupType.SPEED) 1.0 else 0.5
        val practiceBonus = if (lr.isPractice) profile.value.staminaLevel * 0.25 else 0.0
        var nextStamina = min(100.0, lr.stamina + staminaGain + practiceBonus)
        var nextScore = lr.score
        var nextTimer = lr.timer
        var nextPower = lr.activePower
        var nextPowerUntil = lr.powerUntilTick
        var nextSonar = lr.sonarCharges
        var nextVision = lr.currentVision
        var nextGemsSet = lr.gems
        var nextPowersMap = lr.powers
        var nextGemsCollected = lr.gemsCollected
        var nextQuestGem = lr.questGemDone
        var nextComboTimer = lr.comboTimerTicks
        val newPopups = ArrayList<FloatingPopup>()
        var movedSoundPlayed = false

        // Ice biome deterministic slide
        if (lr.biome == Biome.ICE && gameplayRng() < lr.biome.rule.slideChance) {
            val sx = px + dx
            val sy = py + dy
            if (sx in 0 until lr.boardSize && sy in 0 until lr.boardSize && !lr.blocks.containsKey("$sx,$sy")) {
                px = sx
                py = sy
                trail.add(GridPos(px, py))
                fx.playIceSlide()
                movedSoundPlayed = true
                showToast(currentStrings().toastIceSlide)
            }
        }

        val curPos = GridPos(px, py)
        val curKey = curPos.key

        // Volcano Lava Puddle hazard (-2s timer unless Shield Lv 2+ or active Shield powerup)
        if (lr.isPractice && curKey in lr.lavaPuddles) {
            val shieldProtects = lr.activePower == PowerupType.SHIELD || profile.value.shieldLevel >= 2
            if (!shieldProtects) {
                nextTimer = max(1, nextTimer - 2)
                fx.playHazard()
                movedSoundPlayed = true
                newPopups.add(nextPopup(px, py, "🌋 -2s", isPositive = false))
                showToast("🌋 Lava Puddle! -2s (Forge Shield Lv.2+ resists)")
            } else {
                newPopups.add(nextPopup(px, py, "🛡️ RESISTED", isPositive = true))
            }
        }

        // Ruins trap check
        if (curKey in lr.traps) {
            var dmg = lr.biome.rule.trapDamage.toDouble()
            if (lr.isPractice && profile.value.shieldLevel > 0) {
                dmg *= (1.0 - profile.value.shieldLevel * 0.30).coerceAtLeast(0.1)
            }
            nextStamina = max(0.0, nextStamina - dmg)
            fx.playHazard()
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "🪤 -${dmg.toInt()} ⚡", isPositive = false))
            showToast(currentStrings().toastRuinsTrap)
        }

        var nextFeverCharge = lr.feverCharge
        var nextFeverSec = lr.feverRemainingSec
        var nextOverdrive = lr.overdriveActive
        val feverMult = if (lr.isPractice && nextFeverSec > 0) 5 else 1

        // Gem collection + Crystal Magnet ability (pulls adjacent gems within 1 tile on Expedition/Custom boards)
        if (curKey in nextGemsSet) {
            nextGemsSet = nextGemsSet - curKey
            val gain = ReplaySimulator.gemGain(lr.biome, lr.modifier) * feverMult
            nextScore += gain
            nextGemsCollected++
            nextQuestGem++
            if (lr.isPractice) {
                nextComboTimer = 3
                if (nextFeverSec == 0) {
                    nextFeverCharge = (nextFeverCharge + 10 + min(lr.combo, 5)).coerceAtMost(100)
                }
            }
            fx.playGem(lr.combo)
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, if (feverMult > 1) "🔥 +$gain" else "💎 +$gain", isPositive = true))
        }
        if (lr.isPractice && MinerAbility.CRYSTAL_MAGNET in lr.unlockedAbilities) {
            val magDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
            for ((mdx, mdy) in magDirs) {
                val mKey = "${px + mdx},${py + mdy}"
                if (mKey in nextGemsSet) {
                    nextGemsSet = nextGemsSet - mKey
                    val gain = ReplaySimulator.gemGain(lr.biome, lr.modifier) * feverMult
                    nextScore += gain
                    nextGemsCollected++
                    nextQuestGem++
                    nextComboTimer = 3
                    if (nextFeverSec == 0) {
                        nextFeverCharge = (nextFeverCharge + 10).coerceAtMost(100)
                    }
                    fx.playGem(lr.combo + 1)
                    movedSoundPlayed = true
                    newPopups.add(nextPopup(px + mdx, py + mdy, "🧲 +$gain", isPositive = true))
                }
            }
        }

        // Powerup pickup
        val picked = nextPowersMap[curKey]
        if (picked != null) {
            nextPowersMap = nextPowersMap - curKey
            nextPower = picked
            nextPowerUntil = actionTick + 15
            when (picked) {
                PowerupType.PICK -> {
                    nextScore += 5
                    if (lr.isPractice && nextFeverSec == 0) {
                        nextFeverCharge = (nextFeverCharge + 20).coerceAtMost(100)
                    }
                }
                PowerupType.CLOCK -> nextTimer += 20
                PowerupType.SONAR -> nextSonar++
                PowerupType.LANTERN -> nextVision = 5
                else -> Unit
            }
            fx.playPowerup()
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "⚡ ${picked.wireName.uppercase()}", isPositive = true))
        }

        // Strategic Relay Beacon Capture (multi-leg labyrinth circuit)
        var nextPlayerBeacons = lr.playerCheckpointsCleared
        val hitUncapturedBeacon = lr.checkpoints.any { it.key == curKey } && curKey !in nextPlayerBeacons
        if (hitUncapturedBeacon) {
            nextPlayerBeacons = nextPlayerBeacons + curKey
            val firstToSector = curKey !in lr.rivalCheckpointsCleared
            val baseGain = (100 + if (firstToSector) 40 else 0) * feverMult
            nextScore += baseGain
            nextTimer += 12
            nextStamina = min(100.0, nextStamina + 25.0)
            if (lr.isPractice && nextFeverSec == 0) {
                nextFeverCharge = (nextFeverCharge + 28).coerceAtMost(100)
            }
            val count = nextPlayerBeacons.size
            val total = lr.checkpoints.size
            fx.playBeaconSync(portalUnlocked = count >= total)
            movedSoundPlayed = true
            if (count >= total) {
                newPopups.add(nextPopup(px, py, "🔓 PORTAL OPEN! +12s", isPositive = true))
                showToast("🔓 All $total Relay Beacons synced! Race to the Finish Portal!")
            } else {
                val tag = if (firstToSector) "FIRST! +12s" else "+12s"
                newPopups.add(nextPopup(px, py, "⚡ BEACON $count/$total • $tag", isPositive = true))
                showToast("⚡ Relay Beacon $count/$total synced! +12s & +25 Stamina")
            }
            if (lr.isGauntlet) {
                gauntletController.onBeaconActivated()
                if (gauntletController.forceGrantSurgeCharge) {
                    nextStamina = 100.0
                    showToast("⚡ FREE SURGE CHARGED! Tap SURGE to blast down the runway!")
                }
            }
        }

        // Ignite Fever 5x if gems/powerup/beacon topped off charge to 100%
        if (lr.isPractice && nextFeverCharge >= 100 && nextFeverSec == 0) {
            nextFeverSec = 10
            nextOverdrive = true
            fx.playFeverIgnite()
            newPopups.add(nextPopup(px, py, "🔥 FEVER 5×!", isPositive = true))
            showToast("🔥 FEVER 5× ACTIVE! 10s of 5× Score & 2× Mining Power!")
        }

        val portalReady = lr.checkpoints.isEmpty() || nextPlayerBeacons.size >= lr.checkpoints.size
        if (curPos == lr.finishPos && !portalReady) {
            val remainingCount = lr.checkpoints.size - nextPlayerBeacons.size
            fx.playHazard()
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "🔒 $remainingCount BEACON(S) LEFT", isPositive = false))
            showToast("🔒 Finish Portal sealed! Capture $remainingCount remaining Relay Beacon(s).")
        }

        if (!movedSoundPlayed) fx.playStep()

        recordedTickInputs.add(ReplayTickInput(direction = MoveDir.fromDelta(dx, dy)))
        val nextSurgeCd = if (lr.isGauntlet && gauntletController.forceGrantSurgeCharge) 0 else lr.surgeCooldownTicks
        val nextGauntletStep = if (lr.isGauntlet) gauntletController.currentStep.value else lr.gauntletStep

        val nextExplored = lr.explored + computeVisibleKeys(curPos, nextVision, lr.boardSize)
        val updatedRace = lr.copy(
            playerPos = curPos,
            facingDir = GridPos(dx, dy),
            myTrail = trail,
            ghostIndex = lr.ghostIndex + 1,
            explored = nextExplored,
            playerCheckpointsCleared = nextPlayerBeacons,
            targetBlock = null,
            score = nextScore,
            timer = nextTimer,
            stamina = nextStamina,
            gems = nextGemsSet,
            powers = nextPowersMap,
            gemsCollected = nextGemsCollected,
            questGemDone = nextQuestGem,
            activePower = nextPower,
            powerUntilTick = nextPowerUntil,
            sonarCharges = nextSonar,
            currentVision = nextVision,
            comboTimerTicks = nextComboTimer,
            surgeCooldownTicks = nextSurgeCd,
            feverCharge = nextFeverCharge,
            feverRemainingSec = nextFeverSec,
            overdriveActive = nextOverdrive,
            gauntletStep = nextGauntletStep,
            popups = (lr.popups + newPopups).takeLast(10)
        )

        syncSoundtrackContext(updatedRace, pausedOverride = false)
        _uiState.update { it.copy(liveRace = updatedRace) }
        if (newPopups.isNotEmpty()) schedulePopupCleanup(newPopups.map { it.id })

        if (curPos == lr.finishPos && portalReady) {
            if (lr.isGauntlet) {
                gauntletController.onExitPortalEntered {
                    viewModelScope.launch {
                        repository.updateProfile { it.copy(gauntletCompleted = true) }
                    }
                }
            }
            finishAsWin(updatedRace.copy(gauntletStep = if (lr.isGauntlet) gauntletController.currentStep.value else updatedRace.gauntletStep))
        }
    }

    fun mineTarget() {
        val lr = _uiState.value.liveRace ?: return
        val target = lr.targetBlock ?: findFirstAdjacentBlock(lr)
        if (target == null) {
            showToast(currentStrings().toastTapAdjacent)
            return
        }
        mineAt(target)
    }

    private fun findFirstAdjacentBlock(lr: LiveRaceState): GridPos? {
        // Prioritize the block directly in front of the miner's facing direction first!
        val dirs = listOf(
            lr.facingDir.x to lr.facingDir.y,
            1 to 0, 0 to 1, -1 to 0, 0 to -1
        ).distinct()
        for ((dx, dy) in dirs) {
            if (abs(dx) + abs(dy) != 1) continue
            val p = GridPos(lr.playerPos.x + dx, lr.playerPos.y + dy)
            val b = lr.blocks[p.key]
            if (b != null && (b.special != BlockSpecial.HIDDEN || p.key in lr.hiddenRevealed)) {
                return p
            }
        }
        return null
    }

    private fun mineAt(target: GridPos) {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return
        if (abs(target.x - lr.playerPos.x) + abs(target.y - lr.playerPos.y) != 1) {
            showToast(currentStrings().toastTapAdjacent)
            return
        }
        val key = target.key
        val block = lr.blocks[key] ?: return
        val s = currentStrings()

        if (block.special == BlockSpecial.LOCKED && !lr.hasKey) {
            fx.playHazard()
            showToast(s.toastLocked)
            return
        }
        if (lr.stamina <= 0.0) {
            fx.playHazard()
            return
        }

        recordedActions.add(ReplayAction.Mine(x = target.x, y = target.y, tick = lr.tickCount))

        var cost = block.kind.staminaCost
        if (lr.activePower == PowerupType.PICK) cost = ceil(cost / 2.0).toInt()
        if (lr.isPractice && profile.value.pickLevel > 0) {
            cost = max(1, ceil(cost * (1.0 - profile.value.pickLevel * 0.14)).toInt())
        }

        var nextStamina = max(0.0, lr.stamina - cost)
        val hasFeverAbility = lr.isPractice && MinerAbility.FEVER_OVERDRIVE in lr.unlockedAbilities
        val feverModeActive = lr.isPractice && lr.feverRemainingSec > 0
        val isOverdrive = feverModeActive || (hasFeverAbility && lr.combo >= 3)
        val feverScoreMult = if (feverModeActive) 5 else 1
        var dmg = if (lr.activePower == PowerupType.PICK) 2 else 1
        if (lr.isPractice && profile.value.pickLevel >= 2) {
            dmg += 1
        }
        if (lr.isPractice && MinerAbility.MASTER_DRILL in lr.unlockedAbilities) {
            dmg += 1
        }
        if (isOverdrive) {
            dmg += 1
        }

        val remainingHp = block.hp - dmg
        val nextCombo = lr.combo + 1
        val nextBestCombo = max(lr.bestCombo, nextCombo)
        var nextScore = lr.score
        if (nextCombo > 2) nextScore += nextCombo
        var nextTimer = lr.timer
        var nextHasKey = lr.hasKey
        var nextBroken = lr.brokenCount
        var nextQuestBreak = lr.questBreakDone
        var nextFeverCharge = lr.feverCharge
        var nextFeverSec = lr.feverRemainingSec
        if (lr.isPractice && nextFeverSec == 0) {
            // Active combo streak accelerates Fever charge!
            val baseStep = if (hasFeverAbility) 12 else 8
            val comboBoost = min(nextCombo, 6)
            nextFeverCharge = (nextFeverCharge + baseStep + comboBoost).coerceAtMost(100)
        }
        val nextBlocks = lr.blocks.toMutableMap()
        val newPopups = ArrayList<FloatingPopup>()
        val blastOrigins = ArrayList<GridPos>()
        var didExplode = false
        var maxChainDepth = 0

        if (remainingHp <= 0) {
            nextBlocks.remove(key)
            nextBroken++
            nextQuestBreak++
            val breakPts = 20 * feverScoreMult
            nextScore += breakPts
            nextStamina = min(100.0, nextStamina + 4.0)
            newPopups.add(
                nextPopup(
                    target.x,
                    target.y,
                    if (feverScoreMult > 1) "🔥 +$breakPts" else "+$breakPts",
                    isPositive = true
                )
            )

            var specialFxPlayed = false
            // Seismic Shockwave Ability: every 5th broken block blasts all 4 adjacent walls!
            if (lr.isPractice && MinerAbility.SEISMIC_BLAST in lr.unlockedAbilities && nextBroken % 5 == 0 && block.special != BlockSpecial.EXPLOSIVE) {
                didExplode = true
                maxChainDepth = max(maxChainDepth, 1)
                blastOrigins.add(target)
                fx.playExplosion()
                specialFxPlayed = true
                newPopups.add(nextPopup(target.x, target.y, "⚡ SEISMIC!", isPositive = true))
                val blastDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                for ((dx, dy) in blastDirs) {
                    val kk = "${target.x + dx},${target.y + dy}"
                    val nb = nextBlocks[kk]
                    if (nb != null && nb.special != BlockSpecial.LOCKED) {
                        if (nb.hp <= 1) nextBlocks.remove(kk)
                        else nextBlocks[kk] = nb.copy(hp = nb.hp - 1)
                    }
                }
            }
            when (block.special) {
                BlockSpecial.EXPLOSIVE -> {
                    didExplode = true
                    specialFxPlayed = true
                    blastOrigins.add(target)
                    val blastDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    if (lr.isPractice) {
                        // Recursive TNT Chain Blasts (up to depth 3) on Expedition & Quick-Play boards!
                        val queue = ArrayDeque<Pair<GridPos, Int>>()
                        val detonated = HashSet<String>()
                        queue.add(target to 1)
                        detonated.add(target.key)
                        while (queue.isNotEmpty()) {
                            val (origin, depth) = queue.removeFirst()
                            if (depth > 3) continue
                            maxChainDepth = max(maxChainDepth, depth)
                            for ((dx, dy) in blastDirs) {
                                val nx = origin.x + dx
                                val ny = origin.y + dy
                                val kk = "$nx,$ny"
                                val nb = nextBlocks[kk] ?: continue
                                if (nb.special == BlockSpecial.LOCKED) continue
                                val wasExplosive = nb.special == BlockSpecial.EXPLOSIVE
                                nextBlocks.remove(kk)
                                nextBroken++
                                nextQuestBreak++
                                val chainPts = 20 * (depth + 1) * feverScoreMult
                                nextScore += chainPts
                                newPopups.add(nextPopup(nx, ny, "💥 +$chainPts", isPositive = true))
                                if (wasExplosive && depth < 3 && detonated.add(kk)) {
                                    val chainPos = GridPos(nx, ny)
                                    blastOrigins.add(chainPos)
                                    queue.add(chainPos to (depth + 1))
                                }
                            }
                        }
                        if (maxChainDepth > 1) {
                            fx.playChainExplosion(maxChainDepth)
                            newPopups.add(nextPopup(target.x, target.y, "💥 CHAIN x$maxChainDepth!", isPositive = true))
                            showToast("💥 CHAIN BLAST x$maxChainDepth! Massive subterranean detonation!")
                        } else {
                            fx.playExplosion()
                            showToast(s.toastExplosive)
                            newPopups.add(nextPopup(target.x, target.y, "💥 BOOM!", isPositive = true))
                        }
                    } else {
                        // Strictly canonical Daily Ranked 21x21 explosive behavior (matches ReplaySimulator bit-for-bit)
                        fx.playExplosion()
                        showToast(s.toastExplosive)
                        newPopups.add(nextPopup(target.x, target.y, "💥 BOOM!", isPositive = true))
                        for ((dx, dy) in blastDirs) {
                            val kk = "${target.x + dx},${target.y + dy}"
                            val nb = nextBlocks[kk]
                            if (nb != null) {
                                if (nb.hp <= 1) nextBlocks.remove(kk)
                                else nextBlocks[kk] = nb.copy(hp = nb.hp - 1)
                            }
                        }
                    }
                }
                BlockSpecial.ICE -> {
                    nextTimer += 5
                    showToast(s.toastIceBlock)
                    newPopups.add(nextPopup(target.x, target.y, "❄️ +5s", isPositive = true))
                }
                BlockSpecial.LAVA -> {
                    if (lr.activePower != PowerupType.SHIELD) {
                        var lavaDmg = 15.0
                        if (lr.isPractice && profile.value.shieldLevel > 0) {
                            lavaDmg *= (1.0 - profile.value.shieldLevel * 0.30).coerceAtLeast(0.1)
                        }
                        nextStamina = max(0.0, nextStamina - lavaDmg)
                        fx.playHazard()
                        specialFxPlayed = true
                        showToast(s.toastLavaHit)
                    } else {
                        showToast(s.toastShieldBlock)
                    }
                }
                else -> Unit
            }

            if (block.kind == BlockKind.CRYSTAL && gameplayRng() < 0.4) {
                nextHasKey = true
                showToast(s.toastKeyFound)
                newPopups.add(nextPopup(target.x, target.y, "🔑 KEY!", isPositive = true))
            }

            if (block.kind == BlockKind.GAMBLE) {
                val r = gameplayRng()
                when {
                    r < 0.4 -> {
                        val jackpot = 50 * feverScoreMult
                        nextScore += jackpot
                        showToast(s.toastJackpot)
                        newPopups.add(nextPopup(target.x, target.y, "🎰 +$jackpot!", isPositive = true))
                    }
                    r < 0.7 -> {
                        nextTimer -= 10
                        showToast(s.toastGambleTrap)
                        newPopups.add(nextPopup(target.x, target.y, "🎰 -10s", isPositive = false))
                    }
                    else -> {
                        val consolation = 10 * feverScoreMult
                        nextScore += consolation
                        newPopups.add(nextPopup(target.x, target.y, "🎰 +$consolation", isPositive = true))
                    }
                }
            }

            if (!specialFxPlayed) fx.playBlockBreak(block.kind, nextCombo)
        } else {
            nextBlocks[key] = block.copy(hp = remainingHp)
            fx.playMineHit(block.kind, nextCombo, remainingHp, block.max)
        }

        // Ignite full 10-second Fever 5x Mode at 100% charge!
        var ignitedFeverNow = false
        if (lr.isPractice && nextFeverCharge >= 100 && nextFeverSec == 0) {
            nextFeverSec = 10
            ignitedFeverNow = true
            fx.playFeverIgnite()
            newPopups.add(nextPopup(lr.playerPos.x, lr.playerPos.y, "🔥 FEVER 5×!", isPositive = true))
            showToast("🔥 FEVER 5× ACTIVE! 10s of 5× Score & 2× Mining Power!")
        }

        val nowOverdrive = (lr.isPractice && nextFeverSec > 0) || (hasFeverAbility && nextCombo >= 3)
        if (nowOverdrive && !lr.overdriveActive && !ignitedFeverNow) {
            fx.playFeverIgnite()
        }
        hitPulseSeq++
        recordedTickInputs.add(
            ReplayTickInput(
                direction = MoveDir.fromDelta(target.x - lr.playerPos.x, target.y - lr.playerPos.y),
                triggerTnt = didExplode
            )
        )
        if (lr.isGauntlet && remainingHp <= 0) {
            gauntletController.onTileDug(target.x, target.y)
            if (didExplode) {
                gauntletController.onTntDetonated()
            }
        }
        val nextGauntletStep = if (lr.isGauntlet) gauntletController.currentStep.value else lr.gauntletStep

        val updatedRace = lr.copy(
            blocks = nextBlocks,
            facingDir = GridPos(target.x - lr.playerPos.x, target.y - lr.playerPos.y),
            targetBlock = if (remainingHp <= 0) null else target,
            hitPulseTick = hitPulseSeq,
            lastHitPos = target,
            lastHitKind = block.kind,
            lastHitBroke = remainingHp <= 0,
            lastHitExploded = didExplode,
            lastBlastOrigins = blastOrigins,
            lastChainDepth = maxChainDepth,
            feverCharge = nextFeverCharge,
            feverRemainingSec = nextFeverSec,
            comboTimerTicks = if (lr.isPractice) 3 else lr.comboTimerTicks,
            overdriveActive = nowOverdrive,
            score = nextScore,
            timer = nextTimer,
            stamina = nextStamina,
            combo = nextCombo,
            bestCombo = nextBestCombo,
            brokenCount = nextBroken,
            questBreakDone = nextQuestBreak,
            hasKey = nextHasKey,
            gauntletStep = nextGauntletStep,
            popups = (lr.popups + newPopups).takeLast(10)
        )
        syncSoundtrackContext(updatedRace, pausedOverride = false)
        _uiState.update { it.copy(liveRace = updatedRace) }
        if (newPopups.isNotEmpty()) schedulePopupCleanup(newPopups.map { it.id })
    }

    private fun finishAsWin(race: LiveRaceState) {
        stopRaceLoops()
        fx.stopSoundtrack()
        fx.playWin()

        val finalTicks = race.tickCount.coerceIn(10, 1000)
        val clearTimeMs = (System.currentTimeMillis() - raceStartElapsedMs).coerceAtLeast(8_000L)
        val bonus = ReplaySimulator.timeBonus(finalTicks)
        val totalScore = race.score + bonus
        val nextAbyssalCumulative = if (race.abyssalFloor != null) race.abyssalCumulativeScore + totalScore else race.abyssalCumulativeScore
        val actionsCopy = recordedActions.toList()
        val tickInputsCopy = if (recordedTickInputs.isNotEmpty()) recordedTickInputs.toList() else Replay16BitCodec.fromTrail(race.myTrail)
        val base64Replay16Bit = Replay16BitCodec.encodeToBase64(tickInputsCopy)
        val abyssalFp = if (race.abyssalFloor != null && race.abyssalRootSeed != null) {
            Replay16BitCodec.computeSha256Fingerprint(
                seed = race.abyssalRootSeed,
                score = nextAbyssalCumulative,
                clearTimeMs = clearTimeMs,
                base64Replay = base64Replay16Bit
            )
        } else {
            null
        }
        val prof = profile.value

        // Harvester upgrade + Founder Pack (+15%) + Abyssal Depth Floor Multiplier multiply wallet gem payout
        val baseWalletGems = 12 + (race.gemsCollected * 2) + (race.brokenCount / 2)
        val harvesterMult = 1.0 + (prof.harvesterLevel * 0.20)
        val founderMult = if (prof.founderPackOwned) 1.15 else 1.0
        val abyssalMult = if (race.abyssalFloor != null) race.abyssalGemMultiplier.toDouble() else 1.0
        val gauntletBonus = if (race.isGauntlet && !prof.gauntletCompleted) 50 else 0
        val walletGemsEarned = ceil(baseWalletGems * harvesterMult * founderMult * abyssalMult).toInt() + gauntletBonus

        val breakdown = ScoreBreakdown(
            totalScore = totalScore,
            baseScore = race.score,
            timeBonus = bonus,
            gemsCollected = race.gemsCollected,
            blocksBroken = race.brokenCount,
            bestCombo = race.bestCombo,
            cellsExplored = race.explored.size,
            elapsedTicks = finalTicks,
            walletGemsEarned = walletGemsEarned
        )

        _uiState.update {
            it.copy(
                liveRace = race.copy(
                    status = RaceStatus.WON,
                    score = totalScore,
                    clearTimeMs = clearTimeMs,
                    compressedReplay16Bit = base64Replay16Bit,
                    abyssalCumulativeScore = nextAbyssalCumulative,
                    abyssalFingerprint = abyssalFp,
                    lastBreakdown = breakdown
                )
            )
        }

        viewModelScope.launch {
            repository.saveGhostTrailIfFaster(race.seed, race.myTrail)

            val verifyResult = repository.verifyAndRecordRun(
                seed = race.seed,
                difficulty = race.difficulty,
                modifier = race.modifier,
                biome = race.biome,
                actions = actionsCopy,
                ticks = finalTicks,
                clientScore = totalScore,
                isPractice = race.isPractice || race.boardSize != MAZE_SIZE,
                remoteServerUrl = prof.verifierServerUrl,
                remoteSession = remoteSession
            )

            // Record Expedition Campaign 1-3 stars, first-clear gem reward & ability unlock detection
            var earnedStars = 0
            var firstClearBonus = 0
            var unlockedNow: MinerAbility? = null
            if (race.campaignLevelId != null) {
                val lvl = CampaignCatalog.byId(race.campaignLevelId)
                if (lvl != null) {
                    val prevCleared = campaignProgress.value.filter { it.stars > 0 }.map { it.levelId }.toSet()
                    val prevAbilities = CampaignCatalog.unlockedAbilities(prevCleared)
                    val (s, bonusGems) = repository.recordCampaignClear(lvl, totalScore, finalTicks)
                    earnedStars = s
                    firstClearBonus = bonusGems
                    val afterAbilities = CampaignCatalog.unlockedAbilities(prevCleared + lvl.id)
                    unlockedNow = (afterAbilities - prevAbilities).firstOrNull()
                }
            }

            var streakMilestoneBonus = 0
            repository.updateProfile { cur ->
                val newStreak = cur.streak + 1
                val milestoneGems = if (newStreak > 0 && newStreak % 7 == 0) 500 else 0
                streakMilestoneBonus = milestoneGems
                val nextHighestAbyssal = max(cur.highestAbyssalFloor, race.abyssalFloor ?: 0)
                var next = cur.copy(
                    gems = cur.gems + walletGemsEarned + firstClearBonus + milestoneGems,
                    totalWins = cur.totalWins + 1,
                    streak = newStreak,
                    bestStreak = max(cur.bestStreak, newStreak),
                    totalBlocksBroken = cur.totalBlocksBroken + race.brokenCount,
                    totalGemsCollected = cur.totalGemsCollected + race.gemsCollected,
                    gauntletCompleted = cur.gauntletCompleted || race.isGauntlet,
                    highestAbyssalFloor = nextHighestAbyssal
                )
                if (!next.hasAchievement(AchievementId.FIRST_WIN)) {
                    next = next.withAchievement(AchievementId.FIRST_WIN)
                        .copy(gems = next.gems + AchievementId.FIRST_WIN.rewardGems)
                }
                if (race.gemsCollected >= 10 && !next.hasAchievement(AchievementId.GEM_HUNTER)) {
                    next = next.withAchievement(AchievementId.GEM_HUNTER)
                        .copy(gems = next.gems + AchievementId.GEM_HUNTER.rewardGems)
                }
                if (race.bestCombo >= 5 && !next.hasAchievement(AchievementId.COMBO_MASTER)) {
                    next = next.withAchievement(AchievementId.COMBO_MASTER)
                        .copy(gems = next.gems + AchievementId.COMBO_MASTER.rewardGems)
                }
                if (race.explored.size >= 150 && !next.hasAchievement(AchievementId.EXPLORER)) {
                    next = next.withAchievement(AchievementId.EXPLORER)
                        .copy(gems = next.gems + AchievementId.EXPLORER.rewardGems)
                }
                if (finalTicks < 90 && !next.hasAchievement(AchievementId.SPEEDSTER)) {
                    next = next.withAchievement(AchievementId.SPEEDSTER)
                        .copy(gems = next.gems + AchievementId.SPEEDSTER.rewardGems)
                }
                next
            }

            if (gauntletBonus > 0) {
                fx.playAbilityUnlock()
                showToast("🎓 GAUNTLET MASTERED! +50 💎 Graduate Bonus!")
            } else if (race.abyssalFloor != null) {
                fx.playAbilityUnlock()
                showToast("🕳️ ABYSSAL FLOOR ${race.abyssalFloor} CLEARED! Tap NEXT RACE to descend to Floor ${race.abyssalFloor + 1}!")
            } else if (streakMilestoneBonus > 0) {
                fx.playAbilityUnlock()
                showToast("🔥 7-WIN STREAK MILESTONE! +$streakMilestoneBonus 💎 Bonus!")
            } else if (unlockedNow != null) {
                fx.playAbilityUnlock()
                showToast("⚡ ABILITY UNLOCKED: ${unlockedNow.displayName}!")
            }

            _uiState.update { cur ->
                val lr = cur.liveRace ?: return@update cur
                cur.copy(
                    liveRace = lr.copy(
                        lastVerifyResult = verifyResult,
                        starsEarned = earnedStars,
                        firstClearGemsEarned = firstClearBonus,
                        newlyUnlockedAbility = unlockedNow
                    ),
                    lastVerifiedReceipt = if (verifyResult.verified) verifyResult else cur.lastVerifiedReceipt
                )
            }
        }
    }

    private fun finishAsLoss(reason: String, tick: Int, stamina: Double) {
        stopRaceLoops()
        fx.stopSoundtrack()
        fx.playLose()
        val lr = _uiState.value.liveRace ?: return
        _uiState.update {
            it.copy(
                liveRace = lr.copy(
                    tickCount = tick,
                    stamina = stamina,
                    status = RaceStatus.LOST,
                    loseReason = reason
                )
            )
        }
        if (!lr.isPractice) {
            viewModelScope.launch {
                repository.updateProfile { it.copy(streak = 0) }
            }
        }
    }

    // --- Real Google Play Billing Monetization Packs ---

    fun launchPackPurchase(activity: Activity, pack: StorePackSpec) {
        billingManager.launchPurchase(activity, pack)
    }

    private fun onVerifiedPackPurchase(pack: StorePackSpec) {
        fx.playWin()
        viewModelScope.launch {
            repository.updateProfile { cur ->
                cur.copy(
                    gems = cur.gems + pack.gemsGrant,
                    founderPackOwned = cur.founderPackOwned || pack.unlocksFounderAura
                )
            }
        }
        showToast("✨ ${pack.title} unlocked (+${pack.gemsGrant} 💎)")
    }

    // --- Forge, Quests, Daily Chest, Skill Vault ---

    fun buyUpgrade(upgrade: UpgradeType) {
        viewModelScope.launch {
            val totalStars = campaignProgress.value.sumOf { it.stars }
            val (outcome, updatedProfile) = repository.purchaseUpgrade(upgrade, totalStars)
            val s = currentStrings()
            when (outcome) {
                GameRepository.UpgradePurchaseOutcome.SUCCESS -> {
                    val newLvl = updatedProfile.levelFor(upgrade)
                    val tierName = upgrade.currentTierLabel(newLvl)
                    fx.playForgeUpgrade(newLvl)
                    showToast("⚒️ Forged ${AppStrings.upgradeName(s, upgrade)} • $tierName Lv.$newLvl!")
                }
                GameRepository.UpgradePurchaseOutcome.MASTERY_LOCKED -> {
                    val curLvl = updatedProfile.levelFor(upgrade)
                    val req = upgrade.requirementForNextLevel(curLvl)
                    val missing = req.missingSummary(
                        updatedProfile.totalWins,
                        updatedProfile.totalBlocksBroken,
                        totalStars
                    )
                    fx.playHazard()
                    showToast("🔒 Earn ${req.tierName} Tier first: $missing")
                }
                GameRepository.UpgradePurchaseOutcome.INSUFFICIENT_GEMS -> {
                    fx.playHazard()
                    showToast(s.toastNeedGems)
                }
                GameRepository.UpgradePurchaseOutcome.MAX_LEVEL -> {
                    showToast(s.maxLevel)
                }
            }
        }
    }

    fun claimDailyQuests() {
        val lr = _uiState.value.liveRace
        val s = currentStrings()
        if (lr == null) {
            showToast(s.toastFinishQuestFirst)
            return
        }
        var reward = 0
        var q1 = lr.questBreakDone
        var q2 = lr.questGemDone
        if (q1 >= 5) {
            reward += 20
            q1 -= 5
        }
        if (q2 >= 3) {
            reward += 25
            q2 -= 3
        }
        if (reward == 0) {
            showToast(s.toastFinishQuestFirst)
            return
        }
        fx.playGem()
        _uiState.update { it.copy(liveRace = lr.copy(questBreakDone = q1, questGemDone = q2)) }
        viewModelScope.launch {
            repository.updateProfile { it.copy(gems = it.gems + reward) }
        }
        showToast("${s.toastQuestClaimed} +$reward 💎")
    }

    fun openDailyChest() {
        val today = todayUtcSeed()
        val prof = profile.value
        val s = currentStrings()
        if (prof.chestOpenedDay == today) {
            showToast(s.chestOpenedToday)
            return
        }
        val rng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$today|chest|${prof.streak}"))
        val baseReward = 20 + floor(rng() * 60.0).toInt()
        val reward = baseReward + (prof.harvesterLevel * 5)
        fx.playWin()
        viewModelScope.launch {
            repository.updateProfile { it.copy(gems = it.gems + reward, chestOpenedDay = today) }
        }
        showToast("🎁 +$reward 💎")
    }

    fun claimDailySkillVault() {
        val today = todayUtcSeed()
        val prof = profile.value
        val s = currentStrings()
        if (prof.vaultChallengeDay == today) {
            showToast(s.vaultClaimedToday)
            return
        }
        fx.playPowerup()
        viewModelScope.launch {
            repository.updateProfile { it.copy(gems = it.gems + 35, vaultChallengeDay = today) }
        }
        showToast("⚡ +35 💎")
    }

    // --- Ranks, Self-Tests & Settings ---

    fun runEngineSelfTests() {
        val results = ReplaySimulator.runSelfTests()
        _uiState.update { it.copy(selfTestResults = results) }
        val passed = results.count { it.passed }
        if (passed == results.size) fx.playPowerup() else fx.playHazard()
        showToast("🧪 $passed/${results.size} PASSED")
    }

    fun clearLeaderboard() {
        viewModelScope.launch { repository.clearLeaderboard() }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            repository.updateProfile { it.copy(languageCode = lang.code) }
        }
        _uiState.update { it.copy(showLanguageSheet = false) }
    }

    fun toggleSound() {
        viewModelScope.launch {
            repository.updateProfile { it.copy(soundEnabled = !it.soundEnabled) }
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            val updated = repository.updateProfile { it.copy(musicEnabled = !it.musicEnabled) }
            fx.musicEnabled = updated.musicEnabled
            val lr = _uiState.value.liveRace
            if (updated.musicEnabled && _uiState.value.screen == AppScreen.PLAYING && lr?.status == RaceStatus.PLAYING && !lr.paused) {
                fx.startSoundtrack(lr.biome)
            } else if (!updated.musicEnabled) {
                fx.stopSoundtrack()
            }
        }
    }

    fun toggleHaptics() {
        viewModelScope.launch {
            repository.updateProfile { it.copy(hapticsEnabled = !it.hapticsEnabled) }
        }
    }

    fun toggleShowDpad() {
        viewModelScope.launch {
            repository.updateProfile { it.copy(showDpad = !it.showDpad) }
        }
    }

    fun checkRemoteServerHealth(url: String) {
        viewModelScope.launch {
            repository.updateProfile { it.copy(verifierServerUrl = url.trim()) }
            val health = repository.checkRemoteServerHealth(url)
            val status = if (health != null && health.ok) {
                "✅ v${health.version ?: "?"} • db=${health.db ?: "?"} • runs=${health.leaderboard ?: 0}"
            } else {
                "⚠️ ${currentStrings().offlineLocalSqlite}"
            }
            _uiState.update { it.copy(remoteServerStatus = status) }
            showToast(status)
        }
    }

    // --- Helpers ---

    private fun computeVisibleKeys(center: GridPos, radius: Int, boardSize: Int): Set<String> {
        val set = HashSet<String>()
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (abs(dx) + abs(dy) <= radius) {
                    val x = center.x + dx
                    val y = center.y + dy
                    if (x in 0 until boardSize && y in 0 until boardSize) {
                        set.add("$x,$y")
                    }
                }
            }
        }
        return set
    }

    private fun nextPopup(x: Int, y: Int, text: String, isPositive: Boolean): FloatingPopup {
        popupSeq++
        return FloatingPopup(id = popupSeq, gridX = x, gridY = y, text = text, isPositive = isPositive)
    }

    private fun schedulePopupCleanup(ids: List<Long>) {
        val idSet = ids.toSet()
        viewModelScope.launch {
            delay(950L)
            _uiState.update { cur ->
                val lr = cur.liveRace ?: return@update cur
                cur.copy(liveRace = lr.copy(popups = lr.popups.filterNot { it.id in idSet }))
            }
        }
    }

    private fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2600L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    override fun onCleared() {
        stopRaceLoops()
        fx.stopSoundtrack()
        billingManager.destroy()
        super.onCleared()
    }
}
