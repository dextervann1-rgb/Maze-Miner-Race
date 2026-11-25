package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CampaignProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.engine.BoardArchetype
import com.example.engine.CampaignCatalog
import com.example.engine.ENGINE_VERSION
import com.example.engine.MazeGenerator
import com.example.engine.MinerAbility
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MysticAmethyst
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LaunchScreen(
    strings: LocalizedStrings,
    currentLanguage: AppLanguage,
    profile: PlayerProfileEntity,
    campaignProgress: List<CampaignProgressEntity>,
    dailySeed: String,
    onPlayExpedition: () -> Unit,
    onQuickPlayBoard: (BoardArchetype) -> Unit,
    onPlayDaily: () -> Unit,
    onPlayGauntlet: () -> Unit,
    onPlayAbyssal: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    val biome = MazeGenerator.biomeForSeed(dailySeed)
    val modifier = MazeGenerator.modifierForSeed(dailySeed)
    val clearedIds = campaignProgress.filter { it.stars > 0 }.map { it.levelId }.toSet()
    val nextLevel = CampaignCatalog.nextRecommendedLevel(clearedIds)
    val unlockedAbilities = CampaignCatalog.unlockedAbilities(clearedIds)
    val totalStars = campaignProgress.sumOf { it.stars }

    val pulse = rememberInfiniteTransition(label = "launch_pulse")
    val ctaScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )
    val emberPhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "launch_embers"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed subterranean cavern hero artwork
        Image(
            painter = painterResource(id = R.drawable.img_launch_hero_1795479090871),
            contentDescription = strings.appTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Rich atmospheric vignette for effortless readability across every viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to ObsidianDeep.copy(alpha = 0.88f),
                        0.28f to ObsidianDeep.copy(alpha = 0.35f),
                        0.55f to ObsidianDeep.copy(alpha = 0.50f),
                        0.82f to ObsidianDeep.copy(alpha = 0.92f),
                        1.0f to ObsidianDeep.copy(alpha = 0.98f)
                    )
                )
        )

        // Live drifting subterranean embers & crystal motes for studio-grade motion polish
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 20
            for (i in 0 until count) {
                val sx = ((i * 67 + 13) % 100) / 100f
                val sy = ((i * 37 + 11) % 100) / 100f
                val t = (emberPhase * (0.55f + (i % 3) * 0.25f) + sy) % 1f
                val px = (sx + sin((t * 3.0 * PI + i).toDouble()).toFloat() * 0.03f) * size.width
                val py = (1f - t) * size.height
                val alpha = (sin(t * PI).toFloat().coerceIn(0f, 1f)) * 0.65f
                val col = if (i % 3 == 0) CrystalCyan else ForgedGold
                val r = 2.0f + (i % 3) * 1.1f
                drawCircle(color = col.copy(alpha = alpha * 0.35f), radius = r * 2.4f, center = Offset(px, py))
                drawCircle(color = col.copy(alpha = alpha), radius = r, center = Offset(px, py))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Utility Row: Wallet Pills + 11-Language Quick Switcher + Help
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        icon = {
                            Icon(
                                Icons.Filled.Diamond,
                                contentDescription = strings.gems,
                                tint = CrystalCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        text = "${profile.gems}"
                    )
                    StatPill(
                        icon = {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = strings.streak,
                                tint = ForgedGold,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        text = "${profile.streak}"
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenLanguages,
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianCard.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("launch_language_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Translate,
                                contentDescription = strings.languagesTitle,
                                tint = CrystalCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentLanguage.regionTag,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = ObsidianCard.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        IconButton(
                            onClick = onOpenTutorial,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("launch_tutorial_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = strings.tutorial,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))

            // Hero Title Block
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = EmeraldGem.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = EmeraldGem,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "v$ENGINE_VERSION • SHA-256 SQLITE VERIFIED",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGem
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.launchTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1.1f))

            // Progressive Expedition & All-Boards Telemetry Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Explore,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Next: Lv.${nextLevel.id} ${nextLevel.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ForgedGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CrystalCyan.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "★ $totalStars/${CampaignCatalog.MAX_STARS} • ⚡ ${unlockedAbilities.size}/${MinerAbility.entries.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrystalCyan,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "${nextLevel.archetype.label} • ${nextLevel.biome.wireName} • ${AppStrings.biomeDesc(strings, biome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Instant Play Any Board Strip (All 4 Boards Unlocked & Playable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BoardArchetype.entries.forEach { arch ->
                            Surface(
                                onClick = { onQuickPlayBoard(arch) },
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianDeep.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("launch_board_${arch.size}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${arch.size}×${arch.size}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = CrystalCyan
                                    )
                                    Text(
                                        text = arch.actLabel.substringBefore(" •"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Launch CTA — Jump Straight Into Next Progressive Level!
            Button(
                onClick = onPlayExpedition,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForgedGold,
                    contentColor = ObsidianDeep
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(ctaScale)
                    .testTag("launch_play_expedition_button")
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY EXPEDITION • LV.${nextLevel.id}/${CampaignCatalog.TOTAL_LEVELS}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action Row: All 20 Levels & Abilities Hub + Daily Verified 21x21 Race
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenHub,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.82f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_open_hub_button")
                ) {
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = EmeraldGem,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "20 Levels & Boards",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPlayDaily,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.82f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_play_daily_button")
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = CrystalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Daily 21×21",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Benchmark 10.0 Quick-Launch Row: 45s First-Run Gauntlet + Endless Abyssal Depth
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPlayGauntlet,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (!profile.gauntletCompleted) 1.5.dp else 1.dp,
                        color = if (!profile.gauntletCompleted) ForgedGold else ForgedGold.copy(alpha = 0.45f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!profile.gauntletCompleted) ForgedGold.copy(alpha = 0.16f) else ObsidianCard.copy(alpha = 0.78f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_play_gauntlet_button")
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = ForgedGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (!profile.gauntletCompleted) "45s Gauntlet" else "Gauntlet",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPlayAbyssal,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MysticAmethyst.copy(alpha = 0.75f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MysticAmethyst.copy(alpha = 0.16f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_play_abyssal_button")
                ) {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MysticAmethyst,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (profile.highestAbyssalFloor > 0) "Abyss • F${profile.highestAbyssalFloor}" else "Abyssal Depth",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Created by Dexter Lamar Vann • Vann Family Ventures LLC",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = ForgedGold.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_creator_credit")
            )

            Text(
                text = stringResource(id = R.string.legal_footer),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_legal_footer")
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatPill(
    icon: @Composable () -> Unit,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = ObsidianCard.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}
