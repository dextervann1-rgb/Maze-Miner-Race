package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CampaignProgressEntity
import com.example.engine.BoardArchetype
import com.example.engine.CampaignCatalog
import com.example.engine.CampaignLevel
import com.example.engine.Difficulty
import com.example.engine.MazeGenerator
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
    campaignProgress: List<CampaignProgressEntity>,
    onSeedChange: (String) -> Unit,
    onTodaySeed: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onSelectRival: (RivalPersonality) -> Unit,
    onSelectArchetype: (BoardArchetype) -> Unit,
    onPlayRanked: () -> Unit,
    onPlayPractice: () -> Unit,
    onStartCampaignLevel: (CampaignLevel) -> Unit
) {
    val activeSeed = seedInput.trim().ifEmpty { "2026-11-18" }
    val biome = MazeGenerator.biomeForSeed(activeSeed)
    val modifier = MazeGenerator.modifierForSeed(activeSeed)

    val progressByLevel = remember(campaignProgress) { campaignProgress.associateBy { it.levelId } }
    val totalStars = remember(campaignProgress) { campaignProgress.sumOf { it.stars } }
    var selectedAct by remember { mutableStateOf(BoardArchetype.SPRINT_15) }
    val actLevels = remember(selectedAct) {
        CampaignCatalog.levels.filter { it.archetype == selectedAct }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 15-Level Multi-Board Expedition Campaign Section
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
                                    text = "15 Handcrafted Levels • 3 Board Sizes",
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
                                    text = "$totalStars / 45",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ForgedGold
                                )
                            }
                        }
                    }

                    // Act / Board Size Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BoardArchetype.entries.forEachIndexed { idx, arch ->
                            val selected = arch == selectedAct
                            val shortLabel = when (arch) {
                                BoardArchetype.SPRINT_15 -> "Act I • 15×15"
                                BoardArchetype.CLASSIC_21 -> "Act II • 21×21"
                                BoardArchetype.ABYSSAL_25 -> "Act III • 25×25"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { selectedAct = arch },
                                label = {
                                    Text(
                                        text = shortLabel,
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
                                    .testTag("campaign_act_${idx + 1}")
                            )
                        }
                    }
                }
            }
        }

        // Campaign Level Cards for Selected Act
        items(actLevels, key = { it.id }) { level ->
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
