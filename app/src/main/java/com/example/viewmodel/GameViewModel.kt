package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundFxEngine
import com.example.billing.BillingManager
import com.example.billing.BillingUiState
import com.example.billing.StorePackSpec
import com.example.data.CampaignProgressEntity
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.LeaderboardEntity
import com.example.data.PlayerProfileEntity
import com.example.data.SessionResponseDto
import com.example.data.hasAchievement
import com.example.data.withAchievement
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
import com.example.engine.GridPos
import com.example.engine.MAZE_SIZE
import com.example.engine.MazeGenerator
import com.example.engine.Modifier
import com.example.engine.PowerupType
import com.example.engine.ReplayAction
import com.example.engine.ReplaySimulator
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
    val overdriveActive: Boolean = false,
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
    val lastBreakdown: ScoreBreakdown? = null
)

data class GameUiState(
    val screen: AppScreen = AppScreen.LAUNCH,
    val activeTab: HubTab = HubTab.RACE,
    val seedInput: String = todayUtcSeed(),
    val selectedDifficulty: Difficulty = Difficulty.NORMAL,
    val selectedRival: RivalPersonality = RivalPersonality.RUBBLE,
    val selectedArchetype: BoardArchetype = BoardArchetype.CLASSIC_21,
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
    private var remoteSession: SessionResponseDto? = null
    private var timerJob: Job? = null
    private var rivalJob: Job? = null
    private var sonarJob: Job? = null
    private var autoStepJob: Job? = null
    private var toastJob: Job? = null
    private var popupSeq = 0L
    private var hitPulseSeq = 0L

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

    fun startRace(practice: Boolean = false, seedOverride: String? = null) {
        val state = _uiState.value
        val seed = (seedOverride ?: state.seedInput.trim()).ifEmpty { todayUtcSeed() }
        val biome = MazeGenerator.biomeForSeed(seed)
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
            isPractice = true, // Enables Forge upgrades & multi-board sizing while awarding campaign stars
            boardSize = level.archetype.size,
            campaignLevel = level
        )
    }

    fun replayCurrentRace() {
        val lr = _uiState.value.liveRace
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
        val currentId = _uiState.value.liveRace?.campaignLevelId ?: return
        val next = CampaignCatalog.byId(currentId + 1)
        if (next != null) {
            startCampaignLevel(next)
        } else {
            openHub(HubTab.RACE)
            showToast("🏆 All 15 Expedition Levels Completed!")
        }
    }

    private fun launchBoardSession(
        seed: String,
        difficulty: Difficulty,
        modifier: Modifier,
        biome: Biome,
        rival: RivalPersonality,
        isPractice: Boolean,
        boardSize: Int,
        campaignLevel: CampaignLevel?
    ) {
        stopRaceLoops()
        val prof = profile.value
        val generated = MazeGenerator.genMaze(seed, boardSize)
        val finishPos = GridPos(generated.finishX, generated.finishY)

        // Separate RNG streams: gameplayRng matches ReplaySimulator bit-for-bit on 21x21 Ranked!
        gameplayRng = DeterministicRng.mulberry(
            DeterministicRng.fnv1aHash("$seed|gameplay|${difficulty.wireName}|${modifier.wireName}")
        )
        rivalRng = DeterministicRng.mulberry(
            DeterministicRng.fnv1aHash("$seed|rival|${rival.wireName}")
        )
        recordedActions.clear()
        remoteSession = null

        val traps = if (biome == Biome.RUINS) {
            MazeGenerator.generateRuinsTraps(seed, generated.maze, generated.boardSize)
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

        val startingSonar = if (modifier == Modifier.NO_SONAR) {
            if (isPractice && prof.sonarLevel > 0) 1 else 0
        } else {
            1 + if (isPractice) prof.sonarLevel.coerceAtMost(3) else 0
        }

        val initialExplored = computeVisibleKeys(GridPos(START_X, START_Y), baseVision, generated.boardSize)

        val live = LiveRaceState(
            seed = seed,
            difficulty = difficulty,
            modifier = modifier,
            biome = biome,
            rivalPersonality = rival,
            isPractice = isPractice,
            boardSize = generated.boardSize,
            finishPos = finishPos,
            campaignLevelId = campaignLevel?.id,
            campaignTitle = campaignLevel?.title,
            blocks = generated.blocks,
            gems = generated.gems,
            powers = generated.powers,
            traps = traps,
            explored = initialExplored,
            timer = difficulty.startTimer,
            sonarCharges = startingSonar,
            baseVision = baseVision,
            currentVision = baseVision
        )

        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                seedInput = if (campaignLevel == null) seed else it.seedInput,
                liveRace = live
            )
        }

        // Load saved ghost trail & optional remote session asynchronously
        viewModelScope.launch {
            val ghost = repository.getGhostTrail(seed)
            _uiState.update { cur ->
                val lr = cur.liveRace ?: return@update cur
                cur.copy(liveRace = lr.copy(ghostTrail = ghost))
            }
        }
        if (!isPractice && prof.verifierServerUrl.isNotBlank()) {
            viewModelScope.launch {
                remoteSession = repository.requestRemoteSession(prof.verifierServerUrl)
            }
        }

        // Start adaptive biome soundtrack
        fx.updateSoundtrackContext(biome, difficulty.startTimer, combo = 0, paused = false)
        fx.startSoundtrack(biome)

        val banner = campaignLevel?.let { "Lv.${it.id} ${it.title} • ${it.archetype.label}" }
            ?: AppStrings.biomeDesc(currentStrings(), biome)
        showToast(banner)
        startRaceLoops(rival)
    }

    private fun startRaceLoops(rival: RivalPersonality) {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                onGameTick()
            }
        }
        rivalJob = viewModelScope.launch {
            while (true) {
                delay(rival.stepIntervalMs)
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
        fx.updateSoundtrackContext(lr.biome, lr.timer, lr.combo, paused = true)
        if (_uiState.value.screen == AppScreen.PLAYING && lr.status == RaceStatus.PLAYING && !lr.paused) {
            _uiState.update { it.copy(liveRace = lr.copy(paused = true)) }
            showToast(currentStrings().toastAutoPaused)
        }
    }

    fun onLifecycleResume() {
        val lr = _uiState.value.liveRace ?: return
        if (_uiState.value.screen == AppScreen.PLAYING && lr.status == RaceStatus.PLAYING && !lr.paused) {
            fx.updateSoundtrackContext(lr.biome, lr.timer, lr.combo, paused = false)
            if (profile.value.musicEnabled) fx.startSoundtrack(lr.biome)
        }
    }

    fun togglePause() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING) return
        val nextPaused = !lr.paused
        fx.updateSoundtrackContext(lr.biome, lr.timer, lr.combo, paused = nextPaused)
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
        val newExplored = lr.explored + computeVisibleKeys(lr.playerPos, expandedVision, lr.boardSize)

        // Compute optimal BFS pathfinder trail toward the finish gate
        val bestPath = MazeGenerator.bfsPath(
            blocks = lr.blocks,
            sx = lr.playerPos.x,
            sy = lr.playerPos.y,
            tx = lr.finishPos.x,
            ty = lr.finishPos.y,
            allowMine = false,
            boardSize = lr.boardSize
        ) ?: MazeGenerator.bfsPath(
            blocks = lr.blocks,
            sx = lr.playerPos.x,
            sy = lr.playerPos.y,
            tx = lr.finishPos.x,
            ty = lr.finishPos.y,
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
                    hintPath = bestPath.take(16)
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

        fx.updateSoundtrackContext(lr.biome, nextTimer, lr.combo, paused = false)

        _uiState.update {
            it.copy(
                liveRace = lr.copy(
                    tickCount = nextTick,
                    timer = nextTimer,
                    stamina = nextStamina,
                    activePower = nextPower,
                    currentVision = nextVision
                )
            )
        }
    }

    private fun onRivalTick() {
        val lr = _uiState.value.liveRace ?: return
        if (lr.status != RaceStatus.PLAYING || lr.paused) return

        val personality = lr.rivalPersonality
        var target = lr.finishPos
        if (personality.seeksGems && lr.gems.isNotEmpty()) {
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
            if (bestGem != null && bestDist < 8) target = bestGem
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

        // Smart Tactical Shortcut Mining in Campaign/Practice: Rival can break adjacent weak rock if it cuts a long detour
        if (lr.isPractice && (path == null || path.size > 14) && rivalRng() < personality.mineBias * 0.65) {
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
                if (b != null && b.special != BlockSpecial.LOCKED && b.hp <= 2) {
                    val updatedBlocks = lr.blocks.toMutableMap()
                    if (b.hp <= 1) updatedBlocks.remove(k)
                    else updatedBlocks[k] = b.copy(hp = b.hp - 1)
                    _uiState.update { it.copy(liveRace = lr.copy(blocks = updatedBlocks)) }
                    return
                }
            }
        }

        if (path.isNullOrEmpty()) return

        val nextStep = path.first()
        if (lr.blocks.containsKey(nextStep.key)) return

        if (nextStep == lr.finishPos) {
            _uiState.update { it.copy(liveRace = lr.copy(rivalPos = nextStep)) }
            finishAsLoss(currentStrings().rivalWonReason, lr.tickCount, lr.stamina)
        } else {
            _uiState.update { it.copy(liveRace = lr.copy(rivalPos = nextStep)) }
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
            // Intelligent multi-tile corridor pathfinder (up to 4 smooth canonical steps along explored open path)
            val tapKey = "$x,$y"
            if (tapKey in lr.explored && !lr.blocks.containsKey(tapKey) && dist <= 5) {
                val miniPath = MazeGenerator.bfsPath(
                    blocks = lr.blocks,
                    sx = lr.playerPos.x,
                    sy = lr.playerPos.y,
                    tx = x,
                    ty = y,
                    allowMine = false,
                    boardSize = lr.boardSize
                )
                if (!miniPath.isNullOrEmpty() && miniPath.size <= 5) {
                    autoStepJob = viewModelScope.launch {
                        for (step in miniPath) {
                            val current = _uiState.value.liveRace ?: break
                            if (current.status != RaceStatus.PLAYING || current.paused) break
                            val sdx = step.x - current.playerPos.x
                            val sdy = step.y - current.playerPos.y
                            if (abs(sdx) + abs(sdy) != 1) break
                            tryMove(sdx, sdy)
                            delay(95L)
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

        // Ruins trap check
        if (curKey in lr.traps) {
            var dmg = lr.biome.rule.trapDamage.toDouble()
            if (lr.isPractice && profile.value.shieldLevel > 0) {
                dmg *= (1.0 - profile.value.shieldLevel * 0.30).coerceAtLeast(0.1)
            }
            nextStamina = max(0.0, nextStamina - dmg)
            fx.playHazard()
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "-${dmg.toInt()} ⚡", isPositive = false))
            showToast(currentStrings().toastRuinsTrap)
        }

        // Gem collection
        if (curKey in nextGemsSet) {
            nextGemsSet = nextGemsSet - curKey
            val gain = ReplaySimulator.gemGain(lr.biome, lr.modifier)
            nextScore += gain
            nextGemsCollected++
            nextQuestGem++
            fx.playGem(lr.combo)
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "💎 +$gain", isPositive = true))
        }

        // Powerup pickup
        val picked = nextPowersMap[curKey]
        if (picked != null) {
            nextPowersMap = nextPowersMap - curKey
            nextPower = picked
            nextPowerUntil = actionTick + 15
            when (picked) {
                PowerupType.PICK -> nextScore += 5
                PowerupType.CLOCK -> nextTimer += 20
                PowerupType.SONAR -> nextSonar++
                PowerupType.LANTERN -> nextVision = 5
                else -> Unit
            }
            fx.playPowerup()
            movedSoundPlayed = true
            newPopups.add(nextPopup(px, py, "⚡ ${picked.wireName.uppercase()}", isPositive = true))
        }

        if (!movedSoundPlayed) fx.playStep()

        val nextExplored = lr.explored + computeVisibleKeys(curPos, nextVision, lr.boardSize)
        val updatedRace = lr.copy(
            playerPos = curPos,
            facingDir = GridPos(dx, dy),
            myTrail = trail,
            ghostIndex = lr.ghostIndex + 1,
            explored = nextExplored,
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
            popups = (lr.popups + newPopups).takeLast(6)
        )

        _uiState.update { it.copy(liveRace = updatedRace) }
        if (newPopups.isNotEmpty()) schedulePopupCleanup(newPopups.map { it.id })

        if (curPos == lr.finishPos) {
            finishAsWin(updatedRace)
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
        val isOverdrive = lr.isPractice && lr.combo >= 4
        var dmg = if (lr.activePower == PowerupType.PICK) 2 else 1
        if (lr.isPractice && profile.value.pickLevel >= 2) {
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
        val nextBlocks = lr.blocks.toMutableMap()
        val newPopups = ArrayList<FloatingPopup>()
        var didExplode = false

        if (remainingHp <= 0) {
            nextBlocks.remove(key)
            nextBroken++
            nextQuestBreak++
            nextScore += 20
            nextStamina = min(100.0, nextStamina + 4.0)
            newPopups.add(nextPopup(target.x, target.y, if (isOverdrive) "🔥 +20" else "+20", isPositive = true))

            var specialFxPlayed = false
            when (block.special) {
                BlockSpecial.EXPLOSIVE -> {
                    didExplode = true
                    fx.playExplosion()
                    specialFxPlayed = true
                    showToast(s.toastExplosive)
                    newPopups.add(nextPopup(target.x, target.y, "💥 BOOM!", isPositive = true))
                    val blastDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    for ((dx, dy) in blastDirs) {
                        val kk = "${target.x + dx},${target.y + dy}"
                        val nb = nextBlocks[kk]
                        if (nb != null) {
                            if (nb.hp <= 1) nextBlocks.remove(kk)
                            else nextBlocks[kk] = nb.copy(hp = nb.hp - 1)
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
                        nextScore += 50
                        showToast(s.toastJackpot)
                        newPopups.add(nextPopup(target.x, target.y, "🎰 +50!", isPositive = true))
                    }
                    r < 0.7 -> {
                        nextTimer -= 10
                        showToast(s.toastGambleTrap)
                        newPopups.add(nextPopup(target.x, target.y, "🎰 -10s", isPositive = false))
                    }
                    else -> {
                        nextScore += 10
                        newPopups.add(nextPopup(target.x, target.y, "🎰 +10", isPositive = true))
                    }
                }
            }

            if (!specialFxPlayed) fx.playBlockBreak(block.kind, nextCombo)
        } else {
            nextBlocks[key] = block.copy(hp = remainingHp)
            fx.playMineHit(block.kind, nextCombo)
        }

        fx.updateSoundtrackContext(lr.biome, nextTimer, nextCombo, paused = false)
        hitPulseSeq++

        val updatedRace = lr.copy(
            blocks = nextBlocks,
            facingDir = GridPos(target.x - lr.playerPos.x, target.y - lr.playerPos.y),
            targetBlock = if (remainingHp <= 0) null else target,
            hitPulseTick = hitPulseSeq,
            lastHitPos = target,
            lastHitKind = block.kind,
            lastHitBroke = remainingHp <= 0,
            lastHitExploded = didExplode,
            overdriveActive = lr.isPractice && nextCombo >= 4,
            score = nextScore,
            timer = nextTimer,
            stamina = nextStamina,
            combo = nextCombo,
            bestCombo = nextBestCombo,
            brokenCount = nextBroken,
            questBreakDone = nextQuestBreak,
            hasKey = nextHasKey,
            popups = (lr.popups + newPopups).takeLast(6)
        )
        _uiState.update { it.copy(liveRace = updatedRace) }
        if (newPopups.isNotEmpty()) schedulePopupCleanup(newPopups.map { it.id })
    }

    private fun finishAsWin(race: LiveRaceState) {
        stopRaceLoops()
        fx.stopSoundtrack()
        fx.playWin()

        val finalTicks = race.tickCount.coerceIn(10, 1000)
        val bonus = ReplaySimulator.timeBonus(finalTicks)
        val totalScore = race.score + bonus
        val actionsCopy = recordedActions.toList()
        val prof = profile.value

        // Harvester upgrade + Founder Pack (+15%) multiply wallet gem payout
        val baseWalletGems = 12 + (race.gemsCollected * 2) + (race.brokenCount / 2)
        val harvesterMult = 1.0 + (prof.harvesterLevel * 0.20)
        val founderMult = if (prof.founderPackOwned) 1.15 else 1.0
        val walletGemsEarned = ceil(baseWalletGems * harvesterMult * founderMult).toInt()

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

            // Record Expedition Campaign 1-3 stars & first-clear gem reward if applicable
            var earnedStars = 0
            var firstClearBonus = 0
            if (race.campaignLevelId != null) {
                val lvl = CampaignCatalog.byId(race.campaignLevelId)
                if (lvl != null) {
                    val (s, bonusGems) = repository.recordCampaignClear(lvl, totalScore, finalTicks)
                    earnedStars = s
                    firstClearBonus = bonusGems
                }
            }

            repository.updateProfile { cur ->
                var next = cur.copy(
                    gems = cur.gems + walletGemsEarned + firstClearBonus,
                    totalWins = cur.totalWins + 1,
                    totalBlocksBroken = cur.totalBlocksBroken + race.brokenCount,
                    totalGemsCollected = cur.totalGemsCollected + race.gemsCollected
                )
                if (!race.isPractice) {
                    val newStreak = cur.streak + 1
                    next = next.copy(streak = newStreak, bestStreak = max(cur.bestStreak, newStreak))
                }
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

            _uiState.update { cur ->
                val lr = cur.liveRace ?: return@update cur
                cur.copy(
                    liveRace = lr.copy(
                        lastVerifyResult = verifyResult,
                        starsEarned = earnedStars,
                        firstClearGemsEarned = firstClearBonus
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
            val (ok, _) = repository.purchaseUpgrade(upgrade)
            val s = currentStrings()
            if (ok) {
                fx.playPowerup()
                showToast(s.toastUpgraded)
            } else {
                fx.playHazard()
                showToast(s.toastNeedGems)
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
