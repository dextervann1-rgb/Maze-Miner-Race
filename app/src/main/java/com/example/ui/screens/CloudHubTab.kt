package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloud.AbyssalLeaderboardEntry
import com.example.cloud.CloudProfileSave
import com.example.cloud.CloudUiState
import com.example.cloud.CommunityMazeSeed
import com.example.cloud.DailyRankedGhostEntry
import com.example.cloud.PvpMatchRoom
import com.example.cloud.PvpRoomStatus
import com.example.engine.Biome
import com.example.engine.BoardArchetype
import com.example.engine.Difficulty
import com.example.engine.LiveBalanceConfig
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MysticAmethyst
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VolcanicOrange

private enum class CloudSection(val label: String) {
    PVP_ARENA("PvP Arena"),
    ABYSSAL_GHOSTS("Abyss & Ghosts"),
    COMMUNITY_SEEDS("Seeds")
}

@Composable
fun CloudHubTab(
    currentUserId: String,
    displayName: String,
    selectedBiome: Biome,
    selectedArchetype: BoardArchetype,
    selectedDifficulty: Difficulty,
    currentSeedInput: String,
    totalCampaignStars: Int,
    highestAbyssalFloor: Int,
    cloudProfileState: CloudUiState<CloudProfileSave?>,
    openPvpRoomsState: CloudUiState<List<PvpMatchRoom>>,
    communitySeedsState: CloudUiState<List<CommunityMazeSeed>>,
    dailyGhostsState: CloudUiState<List<DailyRankedGhostEntry>>,
    abyssalLeaderboardState: CloudUiState<List<AbyssalLeaderboardEntry>>,
    liveBalanceConfig: LiveBalanceConfig,
    activePvpRoom: PvpMatchRoom?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onHostPvpRoom: (String, Biome, Int, Difficulty) -> Unit,
    onJoinPvpRoom: (PvpMatchRoom) -> Unit,
    onJoinPvpByCode: (String) -> Unit,
    onCancelWaitingRoom: () -> Unit,
    onLaunchActivePvpMatch: (PvpMatchRoom) -> Unit,
    onStartAbyssalDescent: () -> Unit,
    onPlayDailyGhost: (DailyRankedGhostEntry) -> Unit,
    onPublishSeed: (String, String, Biome, Int, Difficulty, Boolean, () -> Unit) -> Unit,
    onPlayCommunitySeed: (CommunityMazeSeed) -> Unit,
    onUpvoteCommunitySeed: (CommunityMazeSeed) -> Unit,
    onDeleteCommunitySeed: (CommunityMazeSeed) -> Unit
) {
    val context = LocalContext.current
    var activeSection by remember { mutableStateOf(CloudSection.PVP_ARENA) }
    var joinCodeInput by remember { mutableStateOf("") }
    var publishTitle by remember { mutableStateOf("") }
    var publishSeed by remember(currentSeedInput) { mutableStateOf(currentSeedInput) }
    var publishBiome by remember(selectedBiome) { mutableStateOf(selectedBiome) }
    var publishArchetype by remember(selectedArchetype) { mutableStateOf(selectedArchetype) }
    var publishStrategic by remember { mutableStateOf(true) }

    val cloudProfile = (cloudProfileState as? CloudUiState.Success)?.data

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("cloud_hub_tab")
    ) {
        // 1. Cross-Device Cloud Save Identity & Sync Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.2.dp, EmeraldGem.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGem.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = EmeraldGem,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Google Cloud Save Active • PvP ${cloudProfile?.pvpWins ?: 0}W / ${cloudProfile?.pvpMatches ?: 0}M",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGem
                                )
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("cloud_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGem,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cloud_sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = ObsidianDeep,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "SYNCING..." else "SYNC CLOUD SAVE",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }

        // 2. Active PvP Waiting/Live Room Banner
        if (activePvpRoom != null && activePvpRoom.parsedStatus() != PvpRoomStatus.FINISHED) {
            item {
                val isWaiting = activePvpRoom.parsedStatus() == PvpRoomStatus.WAITING
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.5.dp, if (isWaiting) ForgedGold else CrystalCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isWaiting) "📡 WAITING FOR CHALLENGER" else "⚡ LIVE PVP MATCH READY!",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isWaiting) ForgedGold else CrystalCyan
                            )
                            Text(
                                text = activePvpRoom.roomCode,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black
                                ),
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${activePvpRoom.parsedBiome().wireName} • ${activePvpRoom.boardSize}×${activePvpRoom.boardSize} • Seed: ${activePvpRoom.seed}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isWaiting) {
                                OutlinedButton(
                                    onClick = {
                                        val shareText = "Join my Live PvP Maze Miner Race! Room Code: ${activePvpRoom.roomCode} (${activePvpRoom.parsedBiome().wireName} ${activePvpRoom.boardSize}x${activePvpRoom.boardSize})"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share PvP Room Code"))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, tint = ForgedGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SHARE CODE", color = ForgedGold)
                                }
                                OutlinedButton(
                                    onClick = onCancelWaitingRoom,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, RivalCrimson.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CANCEL ROOM", color = RivalCrimson)
                                }
                            } else {
                                Button(
                                    onClick = { onLaunchActivePvpMatch(activePvpRoom) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CrystalCyan,
                                        contentColor = ObsidianDeep
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("pvp_enter_active_race_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ENTER LIVE PVP RACE",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Mode Switcher Chips (Live PvP Arena vs Abyss & Ghosts vs Community Seeds)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CloudSection.entries.forEach { sec ->
                    val selected = activeSection == sec
                    val chipColor = when (sec) {
                        CloudSection.PVP_ARENA -> CrystalCyan
                        CloudSection.ABYSSAL_GHOSTS -> MysticAmethyst
                        CloudSection.COMMUNITY_SEEDS -> ForgedGold
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { activeSection = sec },
                        leadingIcon = {
                            Icon(
                                imageVector = when (sec) {
                                    CloudSection.PVP_ARENA -> Icons.Filled.Groups
                                    CloudSection.ABYSSAL_GHOSTS -> Icons.Filled.Layers
                                    CloudSection.COMMUNITY_SEEDS -> Icons.Filled.Public
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = sec.label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor,
                            selectedLabelColor = ObsidianDeep,
                            selectedLeadingIconColor = ObsidianDeep,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cloud_section_${sec.name.lowercase()}")
                    )
                }
            }
        }

        if (activeSection == CloudSection.ABYSSAL_GHOSTS) {
            // Endless Abyssal Depth Descent Card + Live-Ops Balance Telemetry
            item {
                val prestigeReady = totalCampaignStars >= liveBalanceConfig.endlessModeUnlockStars
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.2.dp, MysticAmethyst.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🕳️ ENDLESS ABYSSAL DEPTH",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MysticAmethyst
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (prestigeReady) ForgedGold.copy(alpha = 0.2f) else CrystalCyan.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (highestAbyssalFloor > 0) "BEST FLOOR $highestAbyssalFloor" else "FLOOR 1..∞",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (prestigeReady) ForgedGold else CrystalCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Deterministic SplitMix + Mulberry32 procedural floors scaling from 15×15 to 29×29 with escalating beacons, hazards, and gem multipliers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Live-Ops Tuning: Goldie ${"%.2f".format(liveBalanceConfig.goldieSpeedMultiplier)}× • Vex ${liveBalanceConfig.vexDrillCooldownMs}ms • Mastery ★ $totalCampaignStars/${liveBalanceConfig.endlessModeUnlockStars}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Button(
                            onClick = onStartAbyssalDescent,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MysticAmethyst,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("cloud_start_abyssal_button")
                        ) {
                            Icon(Icons.Filled.Layers, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESCEND ABYSSAL DEPTH (FLOOR 1)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }

            // Daily Ranked Holographic Ghost Racers Leaderboard
            item {
                Text(
                    text = "👻 DAILY RANKED GHOST RACERS (16-BIT RLE VERIFIED)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = CrystalCyan
                )
            }
            when (dailyGhostsState) {
                is CloudUiState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CrystalCyan)
                    }
                }
                is CloudUiState.Error -> item {
                    CloudErrorCard(message = dailyGhostsState.message)
                }
                is CloudUiState.Success -> {
                    val ghosts = dailyGhostsState.data
                    if (ghosts.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No Daily Ranked ghosts posted yet today. Finish a Daily 21×21 race to publish the #1 holographic ghost!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(ghosts, key = { it.id.ifBlank { it.uid } }) { ghost ->
                            val rank = ghosts.indexOf(ghost) + 1
                            val clearSec = ghost.clearTimeMs / 1000.0
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "#$rank ${ghost.displayName}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "SHA-256 Verified",
                                                tint = EmeraldGem,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = "${ghost.score} pts • ${"%.1f".format(clearSec)}s • ${ghost.compressedInputs.length}B RLE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = CrystalCyan
                                        )
                                    }
                                    Button(
                                        onClick = { onPlayDailyGhost(ghost) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CrystalCyan,
                                            contentColor = ObsidianDeep
                                        ),
                                        modifier = Modifier.testTag("race_ghost_${ghost.uid}")
                                    ) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("RACE GHOST", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Abyssal Depth Cloud Leaderboard
            item {
                Text(
                    text = "🏆 ABYSSAL DEPTH CLOUD LEADERBOARD",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = ForgedGold
                )
            }
            when (abyssalLeaderboardState) {
                is CloudUiState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ForgedGold)
                    }
                }
                is CloudUiState.Error -> item {
                    CloudErrorCard(message = abyssalLeaderboardState.message)
                }
                is CloudUiState.Success -> {
                    val entries = abyssalLeaderboardState.data
                    if (entries.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No Abyssal Depth descents recorded yet. Clear Floor 1 above to claim the deepest run!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(entries, key = { it.id.ifBlank { it.uid } }) { entry ->
                            val rank = entries.indexOf(entry) + 1
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.42f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "#$rank ${entry.displayName}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Root Seed #${entry.runRootSeed} • ${entry.totalScore} pts",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MysticAmethyst.copy(alpha = 0.24f),
                                        border = BorderStroke(1.dp, MysticAmethyst)
                                    ) {
                                        Text(
                                            text = "FLOOR ${entry.highestFloor}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = ForgedGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeSection == CloudSection.PVP_ARENA) {
            // Host & Join PvP Controls
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "LIVE PVP CLOUD MATCHMAKING",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = CrystalCyan
                        )
                        Text(
                            text = "Host a real-time head-to-head braided labyrinth room (${selectedBiome.wireName} • ${selectedArchetype.size}×${selectedArchetype.size}) or enter a friend's room code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Button(
                            onClick = {
                                val roomSeed = "pvp-${selectedBiome.wireName.lowercase()}-${System.currentTimeMillis() % 9000 + 1000}"
                                onHostPvpRoom(roomSeed, selectedBiome, selectedArchetype.size, selectedDifficulty)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrystalCyan,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("pvp_host_room_button")
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOST LIVE PVP ROOM (${selectedArchetype.size}×${selectedArchetype.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(12) },
                                label = { Text("Room Code (e.g. MZ-4821)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForgedGold,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pvp_room_code_input")
                            )
                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        onJoinPvpByCode(joinCodeInput)
                                        joinCodeInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForgedGold,
                                    contentColor = ObsidianDeep
                                ),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("pvp_join_code_button")
                            ) {
                                Text("JOIN", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Open PvP Rooms Feed
            item {
                Text(
                    text = "OPEN MATCHMAKING LOBBY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextSecondary
                )
            }
            when (openPvpRoomsState) {
                is CloudUiState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CrystalCyan)
                    }
                }
                is CloudUiState.Error -> item {
                    CloudErrorCard(message = openPvpRoomsState.message)
                }
                is CloudUiState.Success -> {
                    val rooms = openPvpRoomsState.data
                    if (rooms.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No open PvP rooms right now. Tap HOST LIVE PVP ROOM above to open the first lobby!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(rooms, key = { it.id }) { room ->
                            val isMine = room.hostUid == currentUserId
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, if (isMine) ForgedGold.copy(alpha = 0.5f) else ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${room.hostName} • ${room.roomCode}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${room.parsedBiome().wireName} • ${room.boardSize}×${room.boardSize} • ${room.difficulty}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrystalCyan
                                        )
                                    }
                                    Button(
                                        onClick = { onJoinPvpRoom(room) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isMine) ForgedGold else EmeraldGem,
                                            contentColor = ObsidianDeep
                                        ),
                                        modifier = Modifier.testTag("pvp_join_room_${room.roomCode}")
                                    ) {
                                        Text(
                                            text = if (isMine) "MY ROOM" else "RACE",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // COMMUNITY MAZE SEEDS PUBLISHER
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PUBLISH SHAREABLE MAZE SEED",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = ForgedGold
                        )
                        OutlinedTextField(
                            value = publishTitle,
                            onValueChange = { publishTitle = it.take(60) },
                            label = { Text("Challenge Title (e.g. Obsidian Gauntlet)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForgedGold,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("community_seed_title_input")
                        )
                        OutlinedTextField(
                            value = publishSeed,
                            onValueChange = { publishSeed = it.take(64) },
                            label = { Text("Deterministic Seed String") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrystalCyan,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("community_seed_code_input")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(Biome.entries) { b ->
                                FilterChip(
                                    selected = publishBiome == b,
                                    onClick = { publishBiome = b },
                                    label = { Text(b.wireName) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(BoardArchetype.entries) { arch ->
                                FilterChip(
                                    selected = publishArchetype == arch,
                                    onClick = { publishArchetype = arch },
                                    label = { Text("${arch.size}×${arch.size}") }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strategic Multi-Beacon Labyrinth",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Switch(
                                checked = publishStrategic,
                                onCheckedChange = { publishStrategic = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ObsidianDeep, checkedTrackColor = CrystalCyan)
                            )
                        }
                        Button(
                            onClick = {
                                onPublishSeed(
                                    publishTitle,
                                    publishSeed,
                                    publishBiome,
                                    publishArchetype.size,
                                    selectedDifficulty,
                                    publishStrategic
                                ) {
                                    publishTitle = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForgedGold,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("publish_community_seed_button")
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PUBLISH TO COMMUNITY", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Community Seeds Feed
            item {
                Text(
                    text = "TRENDING COMMUNITY MAZE SEEDS",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextSecondary
                )
            }
            when (communitySeedsState) {
                is CloudUiState.Loading -> item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ForgedGold)
                    }
                }
                is CloudUiState.Error -> item {
                    CloudErrorCard(message = communitySeedsState.message)
                }
                is CloudUiState.Success -> {
                    val seeds = communitySeedsState.data
                    if (seeds.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No community seeds published yet. Share your best labyrinth seed above!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(seeds, key = { it.id }) { seed ->
                            CommunitySeedCard(
                                seed = seed,
                                isAuthor = seed.authorUid == currentUserId,
                                onPlay = { onPlayCommunitySeed(seed) },
                                onUpvote = { onUpvoteCommunitySeed(seed) },
                                onShare = {
                                    val shareText = "Try '${seed.title}' in Maze Miner Race! Seed: ${seed.seed} • ${seed.parsedBiome().wireName} ${seed.boardSize}x${seed.boardSize} (${seed.difficulty})"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Maze Seed"))
                                },
                                onDelete = { onDeleteCommunitySeed(seed) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySeedCard(
    seed: CommunityMazeSeed,
    isAuthor: Boolean,
    onPlay: () -> Unit,
    onUpvote: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val biomeAccent = when (seed.parsedBiome()) {
        Biome.DESERT -> ForgedGold
        Biome.ICE -> CrystalCyan
        Biome.VOLCANO -> VolcanicOrange
        Biome.CAVERN -> EmeraldGem
        Biome.RUINS -> MysticAmethyst
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, biomeAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seed.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "by ${seed.authorName} • Seed: ${seed.seed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isAuthor) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Seed",
                            tint = RivalCrimson
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${seed.parsedBiome().wireName} • ${seed.boardSize}×${seed.boardSize} • ${seed.difficulty}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = biomeAccent
                )
                Text(
                    text = "• ${seed.playsCount} Plays • 👍 ${seed.upvotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGem,
                        contentColor = ObsidianDeep
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY SEED", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onUpvote,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "Upvote", tint = ForgedGold, modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share Seed", tint = CrystalCyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CloudErrorCard(message: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RivalCrimson.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, RivalCrimson.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = RivalCrimson,
            modifier = Modifier.padding(14.dp)
        )
    }
}
