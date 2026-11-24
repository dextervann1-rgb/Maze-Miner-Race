package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlayerProfileEntity
import com.example.engine.CampaignCatalog
import com.example.engine.ENGINE_VERSION
import com.example.engine.PowerupType
import com.example.engine.VerificationSource
import com.example.i18n.AppStrings
import com.example.i18n.LocalizedStrings
import com.example.ui.components.MazeCanvas
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LiveRaceState
import com.example.viewmodel.RaceStatus
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GamePlayScreen(
    race: LiveRaceState,
    profile: PlayerProfileEntity,
    strings: LocalizedStrings,
    onCellTapped: (Int, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onMineTarget: () -> Unit,
    onSonar: () -> Unit,
    onCycleZoom: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onPlayAgain: () -> Unit,
    onNextCampaignLevel: () -> Unit,
    onExitToHub: () -> Unit
) {
    BackHandler {
        if (race.status == RaceStatus.PLAYING && !race.paused) {
            onTogglePause()
        } else {
            onExitToHub()
        }
    }

    val context = LocalContext.current
    val distToFinish = abs(race.finishPos.x - race.playerPos.x) + abs(race.finishPos.y - race.playerPos.y)
    val rivalDist = abs(race.finishPos.x - race.rivalPos.x) + abs(race.finishPos.y - race.rivalPos.y)

    // Full-screen immersive game surface — MazeCanvas fills 100% edge-to-edge!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
    ) {
        MazeCanvas(
            race = race,
            founderAura = profile.founderPackOwned,
            onCellTapped = onCellTapped,
            onSwipeDirection = onMove,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Translucent Top HUD Overlay (Cutout-safe on notched/foldable displays)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Exit, Stage/Biome Title, Music/SFX/Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ObsidianCard.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        IconButton(
                            onClick = onExitToHub,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gameplay_exit_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.exitRun,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            val titleText = race.campaignTitle?.let {
                                "Lv.${race.campaignLevelId} $it (${race.boardSize}×${race.boardSize})"
                            } ?: "${race.biome.wireName} • ${race.seed}"
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "🏁 $distToFinish  •  🤖 $rivalDist  •  ${if (race.isPractice) strings.practiceBadge else strings.verifiedBadge}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (distToFinish <= rivalDist) EmeraldGem else RivalCrimson,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = ObsidianCard.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        IconButton(
                            onClick = onToggleMusic,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gameplay_music_button")
                        ) {
                            Icon(
                                if (profile.musicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                                contentDescription = "Toggle Music",
                                tint = if (profile.musicEnabled) CrystalCyan else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = ObsidianCard.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        IconButton(
                            onClick = onToggleSound,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gameplay_sound_button")
                        ) {
                            Icon(
                                if (profile.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = strings.soundLabel,
                                tint = if (profile.soundEnabled) ForgedGold else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = ObsidianCard.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gameplay_pause_button")
                        ) {
                            Icon(
                                if (race.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (race.paused) strings.resume else strings.pause,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Translucent 4-Stat Telemetry HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudMetricCard(
                    label = strings.score,
                    value = "${race.score}",
                    accent = ForgedGold,
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    label = if (race.activePower == PowerupType.SPEED) strings.speedHalfTag else strings.time,
                    value = "${race.timer}s",
                    accent = if (race.timer <= 25) RivalCrimson else CrystalCyan,
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    label = strings.gems,
                    value = "${profile.gems}",
                    accent = EmeraldGem,
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    label = strings.combo,
                    value = "x${race.combo}",
                    accent = ForgedGold,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 3: Translucent Stamina & Powerup Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = strings.stamina,
                        tint = if (race.stamina < 25) RivalCrimson else EmeraldGem,
                        modifier = Modifier.size(16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (race.stamina / 100.0).toFloat().coerceIn(0f, 1f) },
                        color = if (race.stamina < 25) RivalCrimson else EmeraldGem,
                        trackColor = ObsidianDeep,
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = "${race.stamina.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )

                    if (race.hasKey) {
                        Icon(
                            Icons.Filled.VpnKey,
                            contentDescription = "Key",
                            tint = ForgedGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val powerText = race.activePower?.let { p ->
                        val rem = (race.powerUntilTick - race.tickCount).coerceAtLeast(0)
                        "${AppStrings.powerupBadge(p)} ${rem}s"
                    } ?: strings.noPowerup
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (race.activePower != null) CrystalCyan.copy(alpha = 0.22f) else ObsidianDeep.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = powerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (race.activePower != null) CrystalCyan else TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Floating Bottom Tactile Controls Deck (Cutout-safe on landscape/curved/foldable edges)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zero-Modal Level 1 FTUE Contextual Coach Pill
            if (race.campaignLevelId == 1 && race.status == RaceStatus.PLAYING && !race.paused) {
                val ftueHint = when {
                    race.myTrail.size <= 2 ->
                        "Swipe or tap an open corridor to move • Follow the green compass arrow"
                    race.brokenCount == 0 ->
                        "Tap an adjacent rock or press MINE to carve a faster shortcut"
                    !race.sonarActive && race.sonarCharges > 0 ->
                        "Tap SONAR to scan fog & illuminate the cyan BFS Pathfinder route"
                    else -> null
                }
                if (ftueHint != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("level1_ftue_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TipsAndUpdates,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = ftueHint,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMineTarget,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgedGold,
                        contentColor = ObsidianDeep
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("mine_button")
                ) {
                    Icon(
                        Icons.Filled.Construction,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.mineAction,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                OutlinedButton(
                    onClick = onSonar,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (race.sonarCharges > 0) CrystalCyan else ObsidianBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.86f),
                        contentColor = if (race.sonarCharges > 0) CrystalCyan else TextMuted
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("sonar_button")
                ) {
                    Icon(
                        Icons.Filled.Radar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${strings.sonarAction} (${race.sonarCharges})",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onCycleZoom,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.86f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 10.dp),
                    modifier = Modifier.testTag("zoom_button")
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = strings.zoom,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${race.viewSize}×${race.viewSize}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (profile.showDpad) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DpadButton(
                        icon = Icons.Filled.KeyboardArrowLeft,
                        desc = "Move Left",
                        tag = "dpad_left",
                        onClick = { onMove(-1, 0) },
                        modifier = Modifier.weight(1f)
                    )
                    DpadButton(
                        icon = Icons.Filled.KeyboardArrowUp,
                        desc = "Move Up",
                        tag = "dpad_up",
                        onClick = { onMove(0, -1) },
                        modifier = Modifier.weight(1f)
                    )
                    DpadButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        desc = "Move Down",
                        tag = "dpad_down",
                        onClick = { onMove(0, 1) },
                        modifier = Modifier.weight(1f)
                    )
                    DpadButton(
                        icon = Icons.Filled.KeyboardArrowRight,
                        desc = "Move Right",
                        tag = "dpad_right",
                        onClick = { onMove(1, 0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Pause Modal Overlay
        if (race.paused && race.status == RaceStatus.PLAYING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianDeep.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.pause,
                            style = MaterialTheme.typography.headlineMedium,
                            color = ForgedGold
                        )
                        Button(
                            onClick = onTogglePause,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForgedGold,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_button")
                        ) {
                            Text(strings.resume, style = MaterialTheme.typography.titleSmall)
                        }
                        OutlinedButton(
                            onClick = onPlayAgain,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.playAgain, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onExitToHub,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.exitRun, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Victory / Loss Result & Verified Receipt Overlay
        if (race.status != RaceStatus.PLAYING) {
            PostRunModal(
                race = race,
                strings = strings,
                onPlayAgain = onPlayAgain,
                onNextCampaignLevel = onNextCampaignLevel,
                onShare = {
                    val v = race.lastVerifyResult
                    val shareText = "Maze Miner Race v$ENGINE_VERSION | Seed ${race.seed} | Score ${race.score} | " +
                        "Status: ${if (v?.verified == true) "VERIFIED #${v.rank ?: 1}" else "Expedition"} | " +
                        "FP: ${v?.fingerprint?.take(12) ?: "-"}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, strings.shareResult))
                },
                onExitToHub = onExitToHub
            )
        }
    }
}

@Composable
private fun HudMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DpadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = ObsidianElevated.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
            .height(50.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PostRunModal(
    race: LiveRaceState,
    strings: LocalizedStrings,
    onPlayAgain: () -> Unit,
    onNextCampaignLevel: () -> Unit,
    onShare: () -> Unit,
    onExitToHub: () -> Unit
) {
    val isWin = race.status == RaceStatus.WON
    val verify = race.lastVerifyResult
    val breakdown = race.lastBreakdown
    val hasNextCampaign = isWin && race.campaignLevelId != null && CampaignCatalog.byId(race.campaignLevelId + 1) != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(
                1.5.dp,
                if (isWin) ForgedGold.copy(alpha = 0.6f) else RivalCrimson.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isWin) Icons.Filled.EmojiEvents else Icons.Filled.Timer,
                    contentDescription = null,
                    tint = if (isWin) ForgedGold else RivalCrimson,
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = if (isWin) strings.winTitle else strings.loseTitle,
                    style = MaterialTheme.typography.displaySmall,
                    color = if (isWin) ForgedGold else RivalCrimson
                )

                // Campaign 1-3 Stars Showcase
                if (isWin && race.campaignLevelId != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = if (i <= race.starsEarned) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= race.starsEarned) ForgedGold else TextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    if (race.firstClearGemsEarned > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = EmeraldGem.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "FIRST CLEAR BONUS +${race.firstClearGemsEarned} 💎",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldGem,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (!isWin && race.loseReason != null) {
                    Text(
                        text = race.loseReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Cryptographic Verification Receipt Banner
                if (isWin && verify != null) {
                    val badgeColor = when (verify.source) {
                        VerificationSource.REMOTE_SERVER, VerificationSource.CANONICAL_LOCAL_SQLITE -> EmeraldGem
                        VerificationSource.PRACTICE_UNRANKED -> CrystalCyan
                        VerificationSource.REJECTED -> RivalCrimson
                    }
                    val badgeText = when (verify.source) {
                        VerificationSource.REMOTE_SERVER -> strings.serverVerifiedBadge
                        VerificationSource.CANONICAL_LOCAL_SQLITE -> strings.verifiedBadge
                        VerificationSource.PRACTICE_UNRANKED ->
                            race.campaignTitle?.let { "EXPEDITION • Lv.${race.campaignLevelId} $it" }
                                ?: strings.practiceBadge
                        VerificationSource.REJECTED -> "${strings.rejectedBadge}: ${verify.reason ?: ""}"
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = badgeColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_receipt_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = badgeText + (verify.rank?.let { " • #$it" } ?: ""),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = badgeColor
                                )
                            }
                            if (verify.fingerprint != null) {
                                Text(
                                    text = "SHA-256: ${verify.fingerprint.take(24)}…",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                if (breakdown != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianDeep,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.scoreBreakdown,
                                style = MaterialTheme.typography.titleSmall,
                                color = ForgedGold
                            )
                            BreakdownRow(strings.score, "${breakdown.totalScore}")
                            BreakdownRow("Base + Time Bonus", "${breakdown.baseScore} + ${breakdown.timeBonus}")
                            BreakdownRow(strings.gems, "${breakdown.gemsCollected} (+${breakdown.walletGemsEarned} 💎)")
                            BreakdownRow("Blocks / Best Combo", "${breakdown.blocksBroken} / x${breakdown.bestCombo}")
                            BreakdownRow("Time / Explored", "${breakdown.elapsedTicks}s / ${breakdown.cellsExplored}")
                        }
                    }
                }

                if (hasNextCampaign) {
                    Button(
                        onClick = onNextCampaignLevel,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGem,
                            contentColor = ObsidianDeep
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("postrun_next_level_button")
                    ) {
                        Icon(Icons.Filled.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEXT LEVEL (Lv.${race.campaignLevelId!! + 1})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }

                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgedGold,
                        contentColor = ObsidianDeep
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("postrun_play_again_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.playAgain, style = MaterialTheme.typography.titleSmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isWin) {
                        OutlinedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.shareResult, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    OutlinedButton(
                        onClick = onExitToHub,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.openHub, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}
