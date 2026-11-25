package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.billing.BillingUiState
import com.example.billing.StorePackSpec
import com.example.cloud.AbyssalLeaderboardEntry
import com.example.cloud.CloudProfileSave
import com.example.cloud.CloudUiState
import com.example.cloud.CommunityMazeSeed
import com.example.cloud.DailyRankedGhostEntry
import com.example.cloud.PvpMatchRoom
import com.example.data.CampaignProgressEntity
import com.example.data.LeaderboardEntity
import com.example.data.PlayerProfileEntity
import com.example.engine.Biome
import com.example.engine.BoardArchetype
import com.example.engine.CampaignLevel
import com.example.engine.Difficulty
import com.example.engine.LiveBalanceConfig
import com.example.engine.RivalPersonality
import com.example.engine.UpgradeType
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GameUiState
import com.example.viewmodel.HubTab

@Composable
fun HubScreen(
    uiState: GameUiState,
    profile: PlayerProfileEntity,
    leaderboard: List<LeaderboardEntity>,
    campaignProgress: List<CampaignProgressEntity>,
    billingState: BillingUiState,
    strings: LocalizedStrings,
    currentLanguage: AppLanguage,
    onSelectTab: (HubTab) -> Unit,
    onBackToLaunch: () -> Unit,
    onSeedChange: (String) -> Unit,
    onTodaySeed: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onSelectRival: (RivalPersonality) -> Unit,
    onSelectArchetype: (BoardArchetype) -> Unit,
    onSelectBiomeOverride: (Biome?) -> Unit,
    onQuickPlayBoard: (BoardArchetype) -> Unit,
    onPlayRanked: () -> Unit,
    onPlayPractice: () -> Unit,
    onStartCampaignLevel: (CampaignLevel) -> Unit,
    onBuyUpgrade: (UpgradeType) -> Unit,
    onBuyStorePack: (StorePackSpec) -> Unit,
    onOpenDailyChest: () -> Unit,
    onClaimSkillVault: () -> Unit,
    onClaimQuests: () -> Unit,
    onRunSelfTests: () -> Unit,
    onClearBoard: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleDpad: () -> Unit,
    onTestServer: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    currentUserId: String,
    currentUserDisplayName: String,
    cloudProfileState: CloudUiState<CloudProfileSave?>,
    openPvpRoomsState: CloudUiState<List<PvpMatchRoom>>,
    communitySeedsState: CloudUiState<List<CommunityMazeSeed>>,
    dailyGhostsState: CloudUiState<List<DailyRankedGhostEntry>>,
    abyssalLeaderboardState: CloudUiState<List<AbyssalLeaderboardEntry>>,
    liveBalanceConfig: LiveBalanceConfig,
    activePvpRoom: PvpMatchRoom?,
    isCloudSyncing: Boolean,
    onSyncCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onHostPvpRoom: (String, Biome, Int, Difficulty) -> Unit,
    onJoinPvpRoom: (PvpMatchRoom) -> Unit,
    onJoinPvpByCode: (String) -> Unit,
    onCancelWaitingRoom: () -> Unit,
    onLaunchActivePvpMatch: (PvpMatchRoom) -> Unit,
    onStartAbyssalDescent: () -> Unit,
    onPlayDailyGhost: (DailyRankedGhostEntry) -> Unit,
    onPublishCommunitySeed: (String, String, Biome, Int, Difficulty, Boolean, () -> Unit) -> Unit,
    onPlayCommunitySeed: (CommunityMazeSeed) -> Unit,
    onUpvoteCommunitySeed: (CommunityMazeSeed) -> Unit,
    onDeleteCommunitySeed: (CommunityMazeSeed) -> Unit
) {
    BackHandler {
        onBackToLaunch()
    }

    val navItems = listOf(
        HubNavItem(HubTab.RACE, strings.tabRace, Icons.Filled.Flag, Icons.Outlined.Flag),
        HubNavItem(HubTab.CLOUD, "Cloud", Icons.Filled.Public, Icons.Outlined.Public),
        HubNavItem(HubTab.FORGE, strings.tabForge, Icons.Filled.Build, Icons.Outlined.Build),
        HubNavItem(HubTab.RANKS, strings.tabRanks, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
        HubNavItem(HubTab.SETTINGS, strings.tabSettings, Icons.Filled.Tune, Icons.Outlined.Tune)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianDeep,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                HubTopHeader(
                    title = strings.appTitle,
                    profile = profile,
                    onBackToLaunch = onBackToLaunch,
                    onOpenTutorial = onOpenTutorial
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianDeep,
                                    selectedTextColor = ForgedGold,
                                    indicatorColor = ForgedGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianDeep,
                                    selectedTextColor = ForgedGold,
                                    indicatorColor = ForgedGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                        when (uiState.activeTab) {
                            HubTab.RACE -> RaceHubTab(
                                strings = strings,
                                seedInput = uiState.seedInput,
                                selectedDifficulty = uiState.selectedDifficulty,
                                selectedRival = uiState.selectedRival,
                                selectedArchetype = uiState.selectedArchetype,
                                selectedBiomeOverride = uiState.selectedBiomeOverride,
                                campaignProgress = campaignProgress,
                                onSeedChange = onSeedChange,
                                onTodaySeed = onTodaySeed,
                                onSelectDifficulty = onSelectDifficulty,
                                onSelectRival = onSelectRival,
                                onSelectArchetype = onSelectArchetype,
                                onSelectBiomeOverride = onSelectBiomeOverride,
                                onQuickPlayBoard = onQuickPlayBoard,
                                onPlayRanked = onPlayRanked,
                                onPlayPractice = onPlayPractice,
                                onStartCampaignLevel = onStartCampaignLevel
                            )
                            HubTab.CLOUD -> CloudHubTab(
                                currentUserId = currentUserId,
                                displayName = currentUserDisplayName,
                                selectedBiome = uiState.selectedBiomeOverride ?: Biome.VOLCANO,
                                selectedArchetype = uiState.selectedArchetype,
                                selectedDifficulty = uiState.selectedDifficulty,
                                currentSeedInput = uiState.seedInput,
                                totalCampaignStars = campaignProgress.sumOf { it.stars },
                                highestAbyssalFloor = profile.highestAbyssalFloor,
                                cloudProfileState = cloudProfileState,
                                openPvpRoomsState = openPvpRoomsState,
                                communitySeedsState = communitySeedsState,
                                dailyGhostsState = dailyGhostsState,
                                abyssalLeaderboardState = abyssalLeaderboardState,
                                liveBalanceConfig = liveBalanceConfig,
                                activePvpRoom = activePvpRoom,
                                isSyncing = isCloudSyncing,
                                onSyncNow = onSyncCloudNow,
                                onSignOut = onSignOut,
                                onHostPvpRoom = onHostPvpRoom,
                                onJoinPvpRoom = onJoinPvpRoom,
                                onJoinPvpByCode = onJoinPvpByCode,
                                onCancelWaitingRoom = onCancelWaitingRoom,
                                onLaunchActivePvpMatch = onLaunchActivePvpMatch,
                                onStartAbyssalDescent = onStartAbyssalDescent,
                                onPlayDailyGhost = onPlayDailyGhost,
                                onPublishSeed = onPublishCommunitySeed,
                                onPlayCommunitySeed = onPlayCommunitySeed,
                                onUpvoteCommunitySeed = onUpvoteCommunitySeed,
                                onDeleteCommunitySeed = onDeleteCommunitySeed
                            )
                            HubTab.FORGE -> ForgeHubTab(
                                strings = strings,
                                profile = profile,
                                campaignProgress = campaignProgress,
                                liveRace = uiState.liveRace,
                                billingState = billingState,
                                onBuyUpgrade = onBuyUpgrade,
                                onBuyStorePack = onBuyStorePack,
                                onOpenDailyChest = onOpenDailyChest,
                                onClaimSkillVault = onClaimSkillVault,
                                onClaimQuests = onClaimQuests
                            )
                            HubTab.RANKS -> LeaderboardHubTab(
                                strings = strings,
                                entries = leaderboard,
                                lastReceipt = uiState.lastVerifiedReceipt,
                                selfTests = uiState.selfTestResults,
                                onRunSelfTests = onRunSelfTests,
                                onClearBoard = onClearBoard
                            )
                            HubTab.SETTINGS -> SettingsHubTab(
                                strings = strings,
                                currentLanguage = currentLanguage,
                                profile = profile,
                                campaignProgress = campaignProgress,
                                remoteServerStatus = uiState.remoteServerStatus,
                                onSelectLanguage = onSelectLanguage,
                                onToggleSound = onToggleSound,
                                onToggleMusic = onToggleMusic,
                                onToggleHaptics = onToggleHaptics,
                                onToggleDpad = onToggleDpad,
                                onTestServer = onTestServer,
                                onOpenTutorial = onOpenTutorial
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HubNavItem(
    val tab: HubTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
private fun HubTopHeader(
    title: String,
    profile: PlayerProfileEntity,
    onBackToLaunch: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(0.5.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToLaunch,
                    modifier = Modifier.testTag("hub_back_to_launch")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenTutorial,
                    modifier = Modifier.testTag("hub_open_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "How to Play Guide",
                        tint = ForgedGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = CrystalCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("${profile.gems}", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = ForgedGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("${profile.streak}", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorOverlay(
    strings: LocalizedStrings,
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.languagesTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = CrystalCyan
                )
                AppLanguage.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { lang ->
                            val selected = lang == currentLanguage
                            Surface(
                                onClick = { onSelect(lang) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) ForgedGold else ObsidianDeep,
                                border = BorderStroke(1.dp, if (selected) ForgedGold else ObsidianBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("modal_lang_${lang.code}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) ObsidianDeep else TextPrimary
                                    )
                                    Text(
                                        text = lang.regionTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) ObsidianDeep else TextMuted
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianDeep,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.gotIt)
                }
            }
        }
    }
}

@Composable
fun TutorialOverlay(
    strings: LocalizedStrings,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.tutorial,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ForgedGold
                )
                Text(
                    text = strings.tutorialBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgedGold,
                        contentColor = ObsidianDeep
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tutorial_close_button")
                ) {
                    Text(strings.gotIt, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
