package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cloud.AuthScreen
import com.example.cloud.CloudRepository
import com.example.cloud.PvpRoomStatus
import com.example.cloud.authStateFlow
import com.example.cloud.signOutFromGoogle
import com.example.engine.DeterministicRng
import com.example.engine.Replay16BitCodec
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HowToPlayGuideOverlay
import com.example.ui.screens.HubScreen
import com.example.ui.screens.LanguageSelectorOverlay
import com.example.ui.screens.LaunchScreen
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MazeMinerTheme
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CloudArenaViewModel
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.HubTab
import com.example.viewmodel.RaceStatus
import com.example.viewmodel.todayUtcSeed
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        configureFullScreenImmersiveWindow()

        setContent {
            MazeMinerTheme {
                MazeMinerAuthGate(
                    activity = this,
                    gameViewModel = viewModel
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBarsImmersively()
        }
    }

    private fun configureFullScreenImmersiveWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBarsImmersively()
    }

    private fun hideSystemBarsImmersively() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Navigation-Level Auth Gate:
 * Ensures zero Firestore queries or listeners attach before Google Sign-In resolves,
 * and keys all cloud ViewModels & state cleanly by authenticated UID.
 */
@Composable
private fun MazeMinerAuthGate(
    activity: Activity,
    gameViewModel: GameViewModel
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName?.trim()?.take(40)?.ifBlank { "Miner" } ?: "Miner"
        key(uid) {
            val cloudViewModel: CloudArenaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        CloudArenaViewModel(
                            repository = CloudRepository(db),
                            currentUserId = uid,
                            displayName = displayName
                        )
                    }
                }
            )
            MazeMinerRoot(
                activity = activity,
                currentUserId = uid,
                currentUserDisplayName = displayName,
                viewModel = gameViewModel,
                cloudViewModel = cloudViewModel
            )
        }
    }
}

@Composable
private fun MazeMinerRoot(
    activity: Activity,
    currentUserId: String,
    currentUserDisplayName: String,
    viewModel: GameViewModel,
    cloudViewModel: CloudArenaViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val campaignProgress by viewModel.campaignProgress.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()

    val cloudProfileState by cloudViewModel.cloudProfileState.collectAsStateWithLifecycle()
    val openPvpRoomsState by cloudViewModel.openPvpRoomsState.collectAsStateWithLifecycle()
    val communitySeedsState by cloudViewModel.communitySeedsState.collectAsStateWithLifecycle()
    val dailyGhostsState by cloudViewModel.dailyGhostsState.collectAsStateWithLifecycle()
    val abyssalLeaderboardState by cloudViewModel.abyssalLeaderboardState.collectAsStateWithLifecycle()
    val liveBalanceConfig by cloudViewModel.liveBalanceConfig.collectAsStateWithLifecycle()
    val activePvpRoom by cloudViewModel.activePvpRoom.collectAsStateWithLifecycle()
    val isCloudSyncing by cloudViewModel.isSyncing.collectAsStateWithLifecycle()
    val cloudStatusMsg by cloudViewModel.statusMessage.collectAsStateWithLifecycle()

    var initialCloudSyncDone by remember(currentUserId) { mutableStateOf(false) }
    var launchedPvpRoomId by remember(currentUserId) { mutableStateOf<String?>(null) }
    var finalizedPvpRoomId by remember(currentUserId) { mutableStateOf<String?>(null) }
    var lastSubmittedGhostKey by remember(currentUserId) { mutableStateOf<String?>(null) }
    var lastSubmittedAbyssKey by remember(currentUserId) { mutableStateOf<String?>(null) }

    // Sync Live-Ops remote balance tuning into GameViewModel in real time
    LaunchedEffect(liveBalanceConfig) {
        viewModel.updateLiveBalanceConfig(liveBalanceConfig)
    }

    // Initial silent cross-device cloud save merge on sign-in
    LaunchedEffect(currentUserId, profile.id) {
        if (!initialCloudSyncDone) {
            initialCloudSyncDone = true
            cloudViewModel.syncCloudProfile(
                localProfile = profile,
                localCampaign = campaignProgress,
                silent = true
            ) { merged ->
                viewModel.applyCloudMergedSave(merged)
            }
        }
    }

    // Auto-launch Live PvP match when hosted/joined room turns ACTIVE and sync opponent telemetry
    LaunchedEffect(activePvpRoom) {
        val room = activePvpRoom ?: return@LaunchedEffect
        if (room.parsedStatus() == PvpRoomStatus.ACTIVE) {
            if (launchedPvpRoomId != room.id && uiState.liveRace?.pvpRoomId != room.id) {
                launchedPvpRoomId = room.id
                viewModel.startPvpRace(room, currentUserId)
            } else {
                viewModel.syncLivePvpOpponentState(room, currentUserId)
            }
        } else if (room.parsedStatus() == PvpRoomStatus.FINISHED) {
            viewModel.syncLivePvpOpponentState(room, currentUserId)
        }
    }

    // Push live player coordinates/score/beacons & match completion to Firestore during Live PvP
    val liveRace = uiState.liveRace
    LaunchedEffect(
        liveRace?.pvpRoomId,
        liveRace?.playerPos,
        liveRace?.score,
        liveRace?.playerCheckpointsCleared?.size,
        liveRace?.status
    ) {
        val lr = liveRace ?: return@LaunchedEffect
        val roomId = lr.pvpRoomId ?: return@LaunchedEffect
        if (lr.status == RaceStatus.PLAYING) {
            cloudViewModel.pushLivePvpTelemetry(
                roomId = roomId,
                asHost = lr.pvpAsHost,
                x = lr.playerPos.x,
                y = lr.playerPos.y,
                score = lr.score,
                beacons = lr.playerCheckpointsCleared.size
            )
        } else if (finalizedPvpRoomId != roomId) {
            finalizedPvpRoomId = roomId
            val wonMatch = lr.status == RaceStatus.WON
            if (wonMatch) {
                cloudViewModel.finishLivePvpMatch(
                    roomId = roomId,
                    asHost = lr.pvpAsHost,
                    x = lr.playerPos.x,
                    y = lr.playerPos.y,
                    score = lr.score,
                    beacons = lr.playerCheckpointsCleared.size
                )
            }
            cloudViewModel.syncCloudProfile(
                localProfile = profile,
                localCampaign = campaignProgress,
                pvpWinDelta = if (wonMatch) 1 else 0,
                pvpMatchDelta = 1,
                silent = true
            ) { merged ->
                viewModel.applyCloudMergedSave(merged)
            }
        }
    }

    // Auto-backup Cloud Save & upload Daily Ranked PB Ghosts / Abyssal Depth runs on finish
    LaunchedEffect(liveRace?.status, liveRace?.compressedReplay16Bit, liveRace?.abyssalFingerprint) {
        val lr = liveRace ?: return@LaunchedEffect
        if (lr.status != RaceStatus.PLAYING && lr.pvpRoomId == null) {
            cloudViewModel.syncCloudProfile(
                localProfile = profile,
                localCampaign = campaignProgress,
                silent = true
            ) { merged ->
                viewModel.applyCloudMergedSave(merged)
            }

            if (lr.status == RaceStatus.WON) {
                val replay16 = lr.compressedReplay16Bit
                if (!lr.isPractice && !lr.isGauntlet && lr.campaignLevelId == null && lr.abyssalFloor == null && !replay16.isNullOrBlank()) {
                    val dateKey = if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(lr.seed)) lr.seed else todayUtcSeed()
                    val ghostSubmissionKey = "$dateKey|${lr.score}|${lr.clearTimeMs}|${replay16.hashCode()}"
                    if (lastSubmittedGhostKey != ghostSubmissionKey) {
                        lastSubmittedGhostKey = ghostSubmissionKey
                        val safeClearMs = lr.clearTimeMs.coerceAtLeast(1000L)
                        val fingerprint = Replay16BitCodec.computeSha256Fingerprint(
                            seed = DeterministicRng.fnv1aHash(lr.seed).toLong(),
                            score = lr.score,
                            clearTimeMs = safeClearMs,
                            base64Replay = replay16
                        )
                        cloudViewModel.submitDailyRankedGhost(
                            dateKey = dateKey,
                            score = lr.score,
                            clearTimeMs = safeClearMs,
                            sha256Fingerprint = fingerprint,
                            compressedInputs = replay16
                        )
                    }
                }

                val floor = lr.abyssalFloor
                val rootSeed = lr.abyssalRootSeed
                val abyssFp = lr.abyssalFingerprint
                if (floor != null && rootSeed != null && !abyssFp.isNullOrBlank()) {
                    val abyssKey = "$rootSeed|$floor|${lr.abyssalCumulativeScore}"
                    if (lastSubmittedAbyssKey != abyssKey) {
                        lastSubmittedAbyssKey = abyssKey
                        cloudViewModel.submitAbyssalRun(
                            highestFloor = floor,
                            totalScore = lr.abyssalCumulativeScore,
                            runRootSeed = rootSeed,
                            runFingerprint = abyssFp
                        )
                    }
                }
            }
        }
    }

    // Surface CloudArenaViewModel status messages inside the unified toast pill
    LaunchedEffect(cloudStatusMsg) {
        val msg = cloudStatusMsg ?: return@LaunchedEffect
        kotlinx.coroutines.delay(3200L)
        cloudViewModel.consumeStatusMessage()
    }

    val currentLang = AppLanguage.fromCode(profile.languageCode)
    val strings = AppStrings.forLanguage(currentLang)
    val layoutDir = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Keep screen awake during active full-screen gameplay
    DisposableEffect(uiState.screen) {
        if (uiState.screen == AppScreen.PLAYING) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-pause active race & soundtrack on background, resume cleanly on foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onLifecyclePause()
                Lifecycle.Event.ON_RESUME -> viewModel.onLifecycleResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.screen) {
                AppScreen.LAUNCH -> LaunchScreen(
                    strings = strings,
                    currentLanguage = currentLang,
                    profile = profile,
                    campaignProgress = campaignProgress,
                    dailySeed = uiState.seedInput,
                    onPlayExpedition = viewModel::startNextPlayableCampaignLevel,
                    onQuickPlayBoard = viewModel::startBoardQuickPlay,
                    onPlayDaily = { viewModel.startRace(practice = false) },
                    onPlayGauntlet = viewModel::startFirstRunGauntlet,
                    onPlayAbyssal = { viewModel.startAbyssalDescent() },
                    onOpenHub = { viewModel.openHub(HubTab.RACE) },
                    onOpenLanguages = { viewModel.setLanguageSheetVisible(true) },
                    onOpenTutorial = { viewModel.setTutorialVisible(true) }
                )

                AppScreen.PLAYING -> {
                    val live = uiState.liveRace
                    if (live != null) {
                        GamePlayScreen(
                            race = live,
                            profile = profile,
                            strings = strings,
                            onCellTapped = viewModel::onBoardCellTapped,
                            onMove = viewModel::tryMove,
                            onMineTarget = viewModel::mineTarget,
                            onMinerSurge = viewModel::triggerMinerSurge,
                            onSonar = viewModel::triggerSonar,
                            onCycleZoom = viewModel::cycleZoom,
                            onTogglePause = viewModel::togglePause,
                            onToggleSound = viewModel::toggleSound,
                            onToggleMusic = viewModel::toggleMusic,
                            onPlayAgain = viewModel::replayCurrentRace,
                            onNextRace = viewModel::startNextRace,
                            onOpenTutorial = { viewModel.setTutorialVisible(true) },
                            onExitToHub = {
                                if (live.pvpRoomId != null) {
                                    cloudViewModel.leaveActivePvpRoom()
                                    launchedPvpRoomId = null
                                    viewModel.openHub(HubTab.CLOUD)
                                } else {
                                    viewModel.openHub(HubTab.RACE)
                                }
                            }
                        )
                    } else {
                        viewModel.returnToLaunch()
                    }
                }

                AppScreen.HUB -> HubScreen(
                    uiState = uiState,
                    profile = profile,
                    leaderboard = leaderboard,
                    campaignProgress = campaignProgress,
                    billingState = billingState,
                    strings = strings,
                    currentLanguage = currentLang,
                    onSelectTab = viewModel::selectHubTab,
                    onBackToLaunch = viewModel::returnToLaunch,
                    onSeedChange = viewModel::setSeedInput,
                    onTodaySeed = viewModel::useTodaySeed,
                    onSelectDifficulty = viewModel::setDifficulty,
                    onSelectRival = viewModel::setRival,
                    onSelectArchetype = viewModel::setBoardArchetype,
                    onSelectBiomeOverride = viewModel::setBiomeOverride,
                    onQuickPlayBoard = viewModel::startBoardQuickPlay,
                    onPlayRanked = { viewModel.startRace(practice = false) },
                    onPlayPractice = { viewModel.startRace(practice = true) },
                    onStartCampaignLevel = viewModel::startCampaignLevel,
                    onBuyUpgrade = viewModel::buyUpgrade,
                    onBuyStorePack = { pack -> viewModel.launchPackPurchase(activity, pack) },
                    onOpenDailyChest = viewModel::openDailyChest,
                    onClaimSkillVault = viewModel::claimDailySkillVault,
                    onClaimQuests = viewModel::claimDailyQuests,
                    onRunSelfTests = viewModel::runEngineSelfTests,
                    onClearBoard = viewModel::clearLeaderboard,
                    onSelectLanguage = viewModel::setLanguage,
                    onToggleSound = viewModel::toggleSound,
                    onToggleMusic = viewModel::toggleMusic,
                    onToggleHaptics = viewModel::toggleHaptics,
                    onToggleDpad = viewModel::toggleShowDpad,
                    onTestServer = viewModel::checkRemoteServerHealth,
                    onOpenTutorial = { viewModel.setTutorialVisible(true) },
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName,
                    cloudProfileState = cloudProfileState,
                    openPvpRoomsState = openPvpRoomsState,
                    communitySeedsState = communitySeedsState,
                    dailyGhostsState = dailyGhostsState,
                    abyssalLeaderboardState = abyssalLeaderboardState,
                    liveBalanceConfig = liveBalanceConfig,
                    activePvpRoom = activePvpRoom,
                    isCloudSyncing = isCloudSyncing,
                    onSyncCloudNow = {
                        cloudViewModel.syncCloudProfile(
                            localProfile = profile,
                            localCampaign = campaignProgress,
                            silent = false
                        ) { merged ->
                            viewModel.applyCloudMergedSave(merged)
                        }
                    },
                    onSignOut = {
                        cloudViewModel.leaveActivePvpRoom()
                        signOutFromGoogle(context = context, scope = scope)
                    },
                    onHostPvpRoom = cloudViewModel::hostPvpRoom,
                    onJoinPvpRoom = cloudViewModel::joinPvpRoom,
                    onJoinPvpByCode = cloudViewModel::joinPvpRoomByCode,
                    onCancelWaitingRoom = cloudViewModel::cancelActiveWaitingRoom,
                    onLaunchActivePvpMatch = { room ->
                        launchedPvpRoomId = room.id
                        viewModel.startPvpRace(room, currentUserId)
                    },
                    onStartAbyssalDescent = { viewModel.startAbyssalDescent() },
                    onPlayDailyGhost = { ghost -> viewModel.startDailyWithGhost(ghost) },
                    onPublishCommunitySeed = { title, seed, biome, size, diff, strategic, onDone ->
                        cloudViewModel.publishCommunitySeed(
                            title = title,
                            seed = seed,
                            biome = biome,
                            boardSize = size,
                            difficulty = diff,
                            strategicMaze = strategic,
                            bestScore = uiState.liveRace?.score ?: 0,
                            onPublished = onDone
                        )
                    },
                    onPlayCommunitySeed = { seed ->
                        cloudViewModel.onPlayCommunitySeed(seed)
                        viewModel.startCommunitySeedRace(seed)
                    },
                    onUpvoteCommunitySeed = cloudViewModel::upvoteCommunitySeed,
                    onDeleteCommunitySeed = cloudViewModel::deleteCommunitySeed
                )
            }

            if (uiState.showLanguageSheet) {
                LanguageSelectorOverlay(
                    strings = strings,
                    currentLanguage = currentLang,
                    onSelect = viewModel::setLanguage,
                    onDismiss = { viewModel.setLanguageSheetVisible(false) }
                )
            }

            if (uiState.showTutorialModal) {
                HowToPlayGuideOverlay(
                    strings = strings,
                    onDismiss = { viewModel.setTutorialVisible(false) }
                )
            }

            // Floating Toast Feedback Pill (Unified local + cloud notifications)
            val activeToast = uiState.toastMessage ?: cloudStatusMsg
            AnimatedVisibility(
                visible = activeToast != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ForgedGold),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = activeToast.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
