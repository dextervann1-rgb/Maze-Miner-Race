package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CampaignProgressEntity
import com.example.engine.Biome
import com.example.engine.BoardArchetype
import com.example.engine.CampaignCatalog
import com.example.engine.CampaignLevel
import com.example.engine.Difficulty
import com.example.engine.MazeGenerator
import com.example.engine.MinerAbility
import com.example.engine.RivalPersonality
import com.example.i18n.AppStrings
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RaceHubTab(
    strings: LocalizedStrings,
    seedInput: String,
    selectedDifficulty: Difficulty,
    selectedRival: RivalPersonality,
    selectedArchetype: BoardArchetype,
    selectedBiomeOverride: Biome?,
    campaignProgress: List<CampaignProgressEntity>,
    onSeedChange: (String) -> Unit,
    onTodaySeed: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onSelectRival: (RivalPersonality) -> Unit,
    onSelectArchetype: (BoardArchetype) -> Unit,
    onSelectBiomeOverride: (Biome?) -> Unit,
    onQuickPlayBoard: (BoardArchetype) -> Unit,
    onPlayRanked: () -> Unit,
    onPlayPractice: () -> Unit,
    onStartCampaignLevel: (CampaignLevel) -> Unit
) {
    val activeSeed = seedInput.trim().ifEmpty { "2026-11-18" }
    val biome = MazeGenerator.biomeForSeed(activeSeed)
    val modifier = MazeGenerator.modifierForSeed(activeSeed)

    val progressByLevel = remember(campaignProgress) { campaignProgress.associateBy { it.levelId } }
    val clearedIds = remember(campaignProgress) {
        campaignProgress.filter { it.stars > 0 }.map { it.levelId }.toSet()
    }
    val unlockedAbilities = remember(clearedIds) { CampaignCatalog.unlockedAbilities(clearedIds) }
    val totalStars = remember(campaignProgress) { campaignProgress.sumOf { it.stars } }
    // null = show ALL 20 progressive levels across all 4 board sizes
    var selectedBoardFilter by remember { mutableStateOf<BoardArchetype?>(null) }
    val visibleLevels = remember(selectedBoardFilter) {
        if (selectedBoardFilter == null) CampaignCatalog.levels
        else CampaignCatalog.levels.filter { it.archetype == selectedBoardFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stylized Feature Hero Graphic Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("race_hub_feature_banner")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_feature_banner_1795484936139),
                        contentDescription = strings.appTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // All 4 Playable Boards — 1-Tap Instant Launch Grid
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.45f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.GridView,
                                contentDescription = null,
                                tint = CrystalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "All 5 Boards — Strategic Instant Play",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Braided mazes, Relay Beacons & any biome from 15×15 up to 29×29",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // 5-Biome Atmosphere Quick Selector (Auto + Desert/Ice/Volcano/Cavern/Ruins)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_biome_selector")
                    ) {
                        item {
                            FilterChip(
                                selected = selectedBiomeOverride == null,
                                onClick = { onSelectBiomeOverride(null) },
                                label = { Text("🎲 Auto", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForgedGold,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianDeep,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("biome_chip_auto")
                            )
                        }
                        items(Biome.entries) { b ->
                            val emoji = when (b) {
                                Biome.DESERT -> "🏜️"
                                Biome.ICE -> "🧊"
                                Biome.VOLCANO -> "🌋"
                                Biome.CAVERN -> "💎"
                                Biome.RUINS -> "🏛️"
                            }
                            FilterChip(
                                selected = selectedBiomeOverride == b,
                                onClick = { onSelectBiomeOverride(b) },
                                label = { Text("$emoji ${b.wireName}", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrystalCyan,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianDeep,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("biome_chip_${b.wireName.lowercase()}")
                            )
                        }
                    }

                    BoardArchetype.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { arch ->
                                Surface(
                                    onClick = { onQuickPlayBoard(arch) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianDeep,
                                    border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_board_${arch.size}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${arch.size}×${arch.size} • ⚡${arch.strategicBeaconCount}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (arch == BoardArchetype.GRAND_29) ForgedGold else CrystalCyan
                                            )
                                            Text(
                                                text = "${arch.label.substringAfter(" ")} (${arch.strategicBeaconCount} Beacons)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play ${arch.label}",
                                            tint = ForgedGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Progressive Unlockable Miner Abilities Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.45f)),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Unlockable Miner Abilities",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ForgedGold
                                )
                                Text(
                                    text = "Clear progressive levels to unlock permanent board powers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CrystalCyan.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${unlockedAbilities.size}/${MinerAbility.entries.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrystalCyan,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    MinerAbility.entries.forEach { ability ->
                        val isUnlocked = ability in unlockedAbilities
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUnlocked) EmeraldGem.copy(alpha = 0.10f) else ObsidianDeep,
                            border = BorderStroke(
                                1.dp,
                                if (isUnlocked) EmeraldGem.copy(alpha = 0.45f) else ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) EmeraldGem else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ability.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isUnlocked) TextPrimary else TextSecondary
                                        )
                                        Text(
                                            text = when {
                                                isUnlocked -> "ACTIVE"
                                                else -> "Clear Lv.${ability.unlockAfterLevel}"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isUnlocked) EmeraldGem else ForgedGold
                                        )
                                    }
                                    Text(
                                        text = ability.shortEffect,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 20-Level Multi-Board Progressive Expedition Campaign Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.45f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = null,
                                tint = EmeraldGem,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Expedition Campaign",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${CampaignCatalog.TOTAL_LEVELS} Progressive Levels • 4 Board Sizes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ForgedGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = ForgedGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$totalStars / ${CampaignCatalog.MAX_STARS}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ForgedGold
                                )
                            }
                        }
                    }

                    // Board Filter Chips: All (20) + 15x15 / 19x19 / 21x21 / 25x25
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedBoardFilter == null,
                            onClick = { selectedBoardFilter = null },
                            label = { Text("All (20)", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGem,
                                selectedLabelColor = ObsidianDeep,
                                containerColor = ObsidianDeep,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("campaign_filter_all")
                        )
                        BoardArchetype.entries.forEach { arch ->
                            val selected = arch == selectedBoardFilter
                            FilterChip(
                                selected = selected,
                                onClick = { selectedBoardFilter = arch },
                                label = {
                                    Text(
                                        text = "${arch.size}×${arch.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGem,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianDeep,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("campaign_act_${arch.size}")
                            )
                        }
                    }
                }
            }
        }

        // Progressive Campaign Level Cards (All Unlocked & Playable)
        items(visibleLevels, key = { it.id }) { level ->
            val prog = progressByLevel[level.id]
            val stars = prog?.stars ?: 0
            val claimedFirst = prog?.firstClearClaimed == true

            Surface(
                onClick = { onStartCampaignLevel(level) },
                shape = RoundedCornerShape(16.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (stars == 3) ForgedGold.copy(alpha = 0.6f)
                    else if (stars > 0) EmeraldGem.copy(alpha = 0.45f)
                    else if (level.unlocksAbility != null) CrystalCyan.copy(alpha = 0.55f)
                    else ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campaign_level_${level.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ForgedGold.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "LV.${level.id}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ForgedGold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CrystalCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${level.archetype.size}×${level.archetype.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrystalCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = level.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${level.biome.wireName} • ${AppStrings.modifierBadge(level.modifier)} • ${level.difficulty.wireName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrystalCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (level.unlocksAbility != null) {
                            val unlocked = level.unlocksAbility in unlockedAbilities
                            Text(
                                text = if (unlocked) "✓ UNLOCKED: ${level.unlocksAbility.displayName}"
                                else "⚡ UNLOCKS: ${level.unlocksAbility.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (unlocked) EmeraldGem else ForgedGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (i in 1..3) {
                                Icon(
                                    imageVector = if (i <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = null,
                                    tint = if (i <= stars) ForgedGold else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (claimedFirst) "Best ${prog?.bestScore ?: 0}" else "+${level.firstClearGems} 💎",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (claimedFirst) TextSecondary else EmeraldGem
                        )
                    }
                }
            }
        }

        // Daily Verified Race & Custom Seed Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.4f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Explore,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "${strings.biomeLabel}: ${biome.wireName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = AppStrings.biomeDesc(strings, biome),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = seedInput,
                            onValueChange = onSeedChange,
                            label = { Text(strings.seedLabel) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForgedGold,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_seed_input")
                        )
                        OutlinedButton(
                            onClick = onTodaySeed,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("hub_today_seed_button")
                        ) {
                            Icon(
                                Icons.Filled.Today,
                                contentDescription = strings.todayDaily,
                                tint = CrystalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Practice Board Size Selector (15x15 / 21x21 / 25x25)
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
                            Icons.Filled.GridView,
                            contentDescription = null,
                            tint = CrystalCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Practice Board Size (Ranked is always 21×21)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardArchetype.entries.forEach { arch ->
                            val selected = arch == selectedArchetype
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectArchetype(arch) },
                                label = {
                                    Text("${arch.size}×${arch.size}", style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrystalCyan,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianDeep,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("board_size_${arch.size}")
                            )
                        }
                    }
                }
            }
        }

        // Difficulty Selector
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
                    Text(
                        text = strings.difficultyLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Difficulty.entries.forEach { diff ->
                            val selected = diff == selectedDifficulty
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDifficulty(diff) },
                                label = {
                                    Text(
                                        "${AppStrings.difficultyName(strings, diff)} (${diff.startTimer}s)",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForgedGold,
                                    selectedLabelColor = ObsidianDeep,
                                    containerColor = ObsidianDeep,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("diff_${diff.wireName}")
                            )
                        }
                    }
                }
            }
        }

        // Rival AI Selector
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
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = RivalCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.rivalLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RivalPersonality.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { rival ->
                                    val selected = rival == selectedRival
                                    Surface(
                                        onClick = { onSelectRival(rival) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) RivalCrimson.copy(alpha = 0.18f) else ObsidianDeep,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) RivalCrimson else ObsidianBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("rival_${rival.wireName.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = AppStrings.rivalName(strings, rival),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) ForgedGold else TextPrimary
                                            )
                                            Text(
                                                text = "${rival.stepIntervalMs}ms • BFS AI",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Launch CTAs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayRanked,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgedGold,
                        contentColor = ObsidianDeep
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_play_ranked_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strings.playDaily} (21×21 Verified)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                OutlinedButton(
                    onClick = onPlayPractice,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_play_practice_button")
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = EmeraldGem,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${strings.practiceMode} (${selectedArchetype.size}×${selectedArchetype.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldGem
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
