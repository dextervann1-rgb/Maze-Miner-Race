package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CampaignProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.data.totalUpgradeLevels
import com.example.engine.CampaignCatalog
import com.example.engine.DeveloperRatingAudit
import com.example.engine.ENGINE_VERSION
import com.example.engine.MinerAbility
import com.example.engine.MinerRank
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsHubTab(
    strings: LocalizedStrings,
    currentLanguage: AppLanguage,
    profile: PlayerProfileEntity,
    campaignProgress: List<CampaignProgressEntity>,
    remoteServerStatus: String?,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleDpad: () -> Unit,
    onTestServer: (String) -> Unit,
    onOpenTutorial: () -> Unit
) {
    val totalStars = remember(campaignProgress) { campaignProgress.sumOf { it.stars } }
    val clearedIds = remember(campaignProgress) {
        campaignProgress.filter { it.stars > 0 }.map { it.levelId }.toSet()
    }
    val unlockedAbilities = remember(clearedIds) { CampaignCatalog.unlockedAbilities(clearedIds) }
    val forgeLevels = profile.totalUpgradeLevels()
    val minerRank = remember(profile.totalWins, totalStars, forgeLevels) {
        MinerRank.forProgress(profile.totalWins, totalStars, forgeLevels)
    }
    var serverUrlDraft by remember(profile.verifierServerUrl) {
        mutableStateOf(profile.verifierServerUrl)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Flagship Game Bio, Feature Matrix & Dexter Lamar Vann Studio Showcase
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.2.dp, ForgedGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_game_bio_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_official_app_icon),
                            contentDescription = "Maze Miner Race Official Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Maze Miner Race",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ForgedGold
                                )
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = EmeraldGem,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Creator & Lead Developer: Dexter Lamar Vann",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrystalCyan
                            )
                            Text(
                                text = "Vann Family Ventures LLC • Engine v$ENGINE_VERSION",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Text(
                        text = "Carve tactical shortcuts through braided subterranean labyrinths in multi-leg strategic circuit races against adaptive BFS Cyber-Driller rivals. Sync quadrant Relay Beacons to unseal deep randomized Finish Portals across 5 atmospheric biomes—Volcanic lava puddles & embers, Arctic ice slides, Desert sand drifts, Bioluminescent caverns, and Arcane ruins—spanning 20 progressive levels, 5 unlocked boards (15×15 up to 29×29 Grand Labyrinth), 6 unlockable abilities, Fever 5× & Chain Blasts, live tactical radar, adaptive synthwave audio, and 0-tolerance SHA-256 verified daily competition.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BioFeatureChip("Braided Maze + Beacons", ForgedGold, Modifier.weight(1f))
                        BioFeatureChip("20 Levels • 5 Boards", CrystalCyan, Modifier.weight(1f))
                        BioFeatureChip("SHA-256 Anti-Cheat", EmeraldGem, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BioFeatureChip("Adaptive 4-Rival AI", CrystalCyan, Modifier.weight(1f))
                        BioFeatureChip("Fever 5× + Chain TNT", ForgedGold, Modifier.weight(1f))
                        BioFeatureChip("11 Languages + RTL", EmeraldGem, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BioFeatureChip("60fps 2.5D Canvas", EmeraldGem, Modifier.weight(1f))
                        BioFeatureChip("Adaptive Synth Audio", ForgedGold, Modifier.weight(1f))
                        BioFeatureChip("Play Policy Verified", CrystalCyan, Modifier.weight(1f))
                    }
                }
            }
        }

        // Accurate Evidence-Backed Developer Level Rating & Engineering Audit Card
        item {
            val composite = DeveloperRatingAudit.compositeScore
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.2.dp, CrystalCyan.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_developer_rating_card")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = CrystalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Developer Level & Build Audit",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CrystalCyan
                                )
                                Text(
                                    text = "Dexter Lamar Vann • ${DeveloperRatingAudit.DEVELOPER_TITLE}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForgedGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, ForgedGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "%.2f / 10".format(composite),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ForgedGold
                                )
                                Text(
                                    text = "DEV LV.${DeveloperRatingAudit.developerLevel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGem
                                )
                            }
                        }
                    }

                    DeveloperRatingAudit.pillars.forEach { pillar ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pillar.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "%.1f / 10".format(pillar.scoreOutOfTen),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (pillar.scoreOutOfTen >= 9.5) EmeraldGem else ForgedGold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (pillar.scoreOutOfTen / 10.0).toFloat().coerceIn(0f, 1f) },
                                color = if (pillar.scoreOutOfTen >= 9.5) EmeraldGem else CrystalCyan,
                                trackColor = ObsidianDeep,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = pillar.evidence,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianDeep,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = DeveloperRatingAudit.NEXT_MILESTONE_NOTE,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Live Career Mastery & Miner Rank Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_career_mastery_card")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = minerRank.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ForgedGold
                                )
                                Text(
                                    text = "Career Mastery Telemetry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = EmeraldGem.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = minerRank.badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldGem,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CareerStatBox("Wins", "${profile.totalWins}", ForgedGold, Modifier.weight(1f))
                        CareerStatBox("Stars", "$totalStars/${CampaignCatalog.MAX_STARS}", CrystalCyan, Modifier.weight(1f))
                        CareerStatBox("Abilities", "${unlockedAbilities.size}/${MinerAbility.entries.size}", EmeraldGem, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CareerStatBox("Blocks", "${profile.totalBlocksBroken}", TextPrimary, Modifier.weight(1f))
                        CareerStatBox("Gems Mined", "${profile.totalGemsCollected}", CrystalCyan, Modifier.weight(1f))
                        CareerStatBox("Forge", "Lv.$forgeLevels/28", ForgedGold, Modifier.weight(1f))
                    }
                }
            }
        }

        // 11-Language Selector Menu
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Translate,
                            contentDescription = null,
                            tint = CrystalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.languagesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    AppLanguage.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { lang ->
                                val selected = lang == currentLanguage
                                Surface(
                                    onClick = { onSelectLanguage(lang) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) ForgedGold else ObsidianDeep,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) ForgedGold else ObsidianBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lang_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
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
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Audio & Controls Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = null,
                            tint = ForgedGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.audioHapticsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    ToggleRow("Adaptive Biome Soundtrack", profile.musicEnabled, onToggleMusic, "toggle_music")
                    ToggleRow(strings.soundLabel, profile.soundEnabled, onToggleSound, "toggle_sound")
                    ToggleRow(strings.hapticsLabel, profile.hapticsEnabled, onToggleHaptics, "toggle_haptics")
                    ToggleRow(strings.dpadLabel, profile.showDpad, onToggleDpad, "toggle_dpad")
                }
            }
        }

        // Optional Remote v12 Server Verifier Endpoint
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = EmeraldGem,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.remoteVerifierTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = serverUrlDraft,
                        onValueChange = { serverUrlDraft = it },
                        placeholder = { Text(strings.remoteVerifierHint, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrystalCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = remoteServerStatus ?: strings.offlineLocalSqlite,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGem,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onTestServer(serverUrlDraft) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrystalCyan,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier.testTag("test_server_health_button")
                        ) {
                            Text(strings.testConnection, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Tutorial CTA
        item {
            OutlinedButton(
                onClick = onOpenTutorial,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_tutorial_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = ForgedGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.tutorial, color = ForgedGold, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Play Console Data Safety & Privacy Transparency Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_data_safety_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = CrystalCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Privacy & Data Safety",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "• Zero third-party advertising SDKs or cross-app tracking.\n" +
                            "• Campaign progress, Forge gear, and rankings stay on-device in encrypted app storage (SQLite).\n" +
                            "• Optional purchases are processed securely via Google Play Billing; purchase tokens and SHA-256 replay proofs are transmitted solely for anti-cheat & entitlement verification when a verifier server is configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Legal & Ownership Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Maze Miner Race v$ENGINE_VERSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.legal_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("settings_legal_footer")
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianDeep,
                checkedTrackColor = ForgedGold,
                uncheckedTrackColor = ObsidianDeep
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun BioFeatureChip(
    text: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianDeep,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CareerStatBox(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianDeep,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accent,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}
