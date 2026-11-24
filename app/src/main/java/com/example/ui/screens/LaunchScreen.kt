package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.data.PlayerProfileEntity
import com.example.engine.ENGINE_VERSION
import com.example.engine.MazeGenerator
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
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
    dailySeed: String,
    onPlayDaily: () -> Unit,
    onPlayPractice: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    val biome = MazeGenerator.biomeForSeed(dailySeed)
    val modifier = MazeGenerator.modifierForSeed(dailySeed)

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

            // Daily Seed & Biome Telemetry Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard.copy(alpha = 0.86f),
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                text = "${strings.todayDaily}: $dailySeed",
                                style = MaterialTheme.typography.titleSmall,
                                color = ForgedGold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CrystalCyan.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = AppStrings.modifierBadge(modifier),
                                style = MaterialTheme.typography.labelSmall,
                                color = CrystalCyan,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = AppStrings.biomeDesc(strings, biome),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Launch CTA
            Button(
                onClick = onPlayDaily,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForgedGold,
                    contentColor = ObsidianDeep
                ),
                contentPadding = PaddingValues(vertical = 17.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(ctaScale)
                    .testTag("launch_play_daily_button")
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.playDaily,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action Row: Practice + Hub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPlayPractice,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.78f),
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_practice_button")
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = CrystalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.practiceMode,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenHub,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.78f),
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
                        tint = ForgedGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.openHub,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
