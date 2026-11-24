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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.LanguageSelectorOverlay
import com.example.ui.screens.LaunchScreen
import com.example.ui.screens.TutorialOverlay
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MazeMinerTheme
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.HubTab

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
                MazeMinerRoot(
                    activity = this,
                    viewModel = viewModel
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

@Composable
private fun MazeMinerRoot(
    activity: Activity,
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val campaignProgress by viewModel.campaignProgress.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()

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
                    dailySeed = uiState.seedInput,
                    onPlayDaily = { viewModel.startRace(practice = false) },
                    onPlayPractice = { viewModel.startRace(practice = true) },
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
                            onSonar = viewModel::triggerSonar,
                            onCycleZoom = viewModel::cycleZoom,
                            onTogglePause = viewModel::togglePause,
                            onToggleSound = viewModel::toggleSound,
                            onToggleMusic = viewModel::toggleMusic,
                            onPlayAgain = viewModel::replayCurrentRace,
                            onNextCampaignLevel = viewModel::startNextCampaignLevel,
                            onExitToHub = { viewModel.openHub(HubTab.RACE) }
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
                    onOpenTutorial = { viewModel.setTutorialVisible(true) }
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
                TutorialOverlay(
                    strings = strings,
                    onDismiss = { viewModel.setTutorialVisible(false) }
                )
            }

            // Floating Toast Feedback Pill
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
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
                        text = uiState.toastMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
