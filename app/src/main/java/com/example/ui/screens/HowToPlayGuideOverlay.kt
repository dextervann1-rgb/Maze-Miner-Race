package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.MinerAbility
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MysticAmethyst
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VolcanicOrange

private enum class GuideSection(val label: String) {
    CONTROLS("Controls & Surge"),
    STRATEGY("Maze & Beacons"),
    BLOCKS("Blocks & Fever"),
    BIOMES("Biomes & Rivals"),
    PROGRESSION("Forge & Ranked")
}

/**
 * Comprehensive interactive Gameplay & How-To-Play Field Manual UI.
 * Accessible from both the Hub header and the in-race Pause menu.
 */
@Composable
fun HowToPlayGuideOverlay(
    strings: LocalizedStrings,
    onDismiss: () -> Unit
) {
    var activeSection by remember { mutableStateOf(GuideSection.CONTROLS) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.90f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.5.dp, ForgedGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("howtoplay_guide_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ForgedGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Miner's Tactical Field Manual",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = ForgedGold
                            )
                            Text(
                                text = "Complete Gameplay, Strategy & Controls Guide",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("guide_close_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Guide",
                            tint = TextSecondary
                        )
                    }
                }

                // 5-Section Category Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GuideSection.entries) { sec ->
                        val selected = sec == activeSection
                        FilterChip(
                            selected = selected,
                            onClick = { activeSection = sec },
                            label = {
                                Text(
                                    text = sec.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForgedGold,
                                selectedLabelColor = ObsidianDeep,
                                containerColor = ObsidianDeep,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("guide_tab_${sec.name.lowercase()}")
                        )
                    }
                }

                // Scrollable Section Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeSection) {
                        GuideSection.CONTROLS -> {
                            GuideCard(
                                icon = Icons.Filled.Explore,
                                accent = CrystalCyan,
                                title = "Fluid Navigation & Quick-Step Pathing",
                                bullets = listOf(
                                    "Tap adjacent open tiles or swipe in any direction to step through corridors.",
                                    "Quick-Step: Tap any explored open floor up to 12 tiles away to auto-cruise smoothly along the optimal corridor path.",
                                    "Optional on-screen D-Pad can be toggled anytime in Settings.",
                                    "Use ZOOM (5×5 → 11×11) to switch between close-up tactical mining and wide maze-structure visibility."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.Bolt,
                                accent = ForgedGold,
                                title = "Active Kinetic SURGE Capability (6s Cooldown)",
                                bullets = listOf(
                                    "Spend 18 Stamina on Expedition & Instant Play boards to trigger SURGE.",
                                    "Facing open floor: Instantly dashes 2 tiles ahead (sweeps Desert Sand Drifts automatically!).",
                                    "Facing rock: Delivers a Heavy Power Strike (+2 bonus damage) to shatter fortified shortcut walls in a single blow."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.Radar,
                                accent = EmeraldGem,
                                title = "SONAR Pulse & BFS Pathfinder",
                                bullets = listOf(
                                    "Expands vision through fog, pings all uncaptured Relay Beacons & the Rival on radar, and paints glowing cyan BFS breadcrumbs to your next objective.",
                                    "Earn extra Sonar charges from cyan powerup orbs, Forge upgrades, and the Crystal Magnet ability."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.AutoAwesome,
                                accent = MysticAmethyst,
                                title = "6 Unlockable Expedition Abilities",
                                bullets = MinerAbility.entries.map { ab ->
                                    val unlockNote = if (ab.unlockAfterLevel == 0) "Starter" else "Lv.${ab.unlockAfterLevel}"
                                    "${ab.displayName} ($unlockNote): ${ab.shortEffect}"
                                }
                            )
                        }

                        GuideSection.STRATEGY -> {
                            GuideCard(
                                icon = Icons.Filled.Explore,
                                accent = ForgedGold,
                                title = "Multi-Leg Relay Beacon Circuits",
                                bullets = listOf(
                                    "Expedition & Instant Play boards spawn 2–4 Quadrant Relay Beacons (2 on 15×15, 3 on 19×19–21×21, 4 on 25×25–29×29).",
                                    "The Finish Portal begins SEALED (amber crossed rings). Sync every Relay Beacon in ANY order you choose to unseal it!",
                                    "Each Beacon grants +12s Split Time, +25 Stamina Surge, +28% Fever Charge, and +40 First-to-Sector bonus if you beat the Rival there."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.Construction,
                                accent = CrystalCyan,
                                title = "Racing the Braided Maze Structure",
                                bullets = listOf(
                                    "Braided corridors create real route forks: take a longer safe corridor loop to conserve stamina, or breach thin Iron/Crystal/TNT shortcut walls to cut 15+ steps off the labyrinth!",
                                    "Follow the directional compass chevron around your miner: Gold points to your nearest uncaptured Beacon; Emerald points straight to the unlocked Finish Portal.",
                                    "Read the top-right Tactical Mini-Map to spot Beacon diamonds (gold), Rival position (crimson), and unexplored sectors."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.Verified,
                                accent = EmeraldGem,
                                title = "S+ Efficiency Rank & Lead 2.0",
                                bullets = listOf(
                                    "LEAD 2.0 tracks full remaining multi-leg distance plus score momentum—green when ahead, crimson when trailing.",
                                    "Finish races with high combo mastery, strong rival margin, and remaining time to earn S+ MASTER efficiency ratings."
                                )
                            )
                        }

                        GuideSection.BLOCKS -> {
                            GuideCard(
                                icon = Icons.Filled.Construction,
                                accent = ForgedGold,
                                title = "Block Durability & Special Seams",
                                bullets = listOf(
                                    "Dirt (1 HP / 5 ⚡) • Stone (2 HP / 8 ⚡) • Iron (3 HP / 12 ⚡) • Crystal (4 HP / 16 ⚡, high score) • Gamble (2 HP, mystery reward or hazard).",
                                    "Explosive TNT: Detonates adjacent rocks and triggers recursive Chain Blasts (up to 3 blasts deep!) for massive shortcuts and bonus points.",
                                    "Hidden Blocks: Shimmer faintly when adjacent—bump once to reveal, then mine. Locked Blocks require a golden Key dropped from broken rocks."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.LocalFireDepartment,
                                accent = VolcanicOrange,
                                title = "Combo Decay Bar & FEVER 5× Overdrive",
                                bullets = listOf(
                                    "Chain mining strikes within 3 seconds to sustain your Combo multiplier—higher combos fill the Fever meter significantly faster!",
                                    "At 100% charge, FEVER 5× ignites for 10 seconds with a golden vignette: 5× score on all blocks/gems and +1 bonus pickaxe power.",
                                    "Pitch rises dynamically with every block hit and combo tier so you can feel your mining rhythm by ear."
                                )
                            )
                        }

                        GuideSection.BIOMES -> {
                            GuideCard(
                                icon = Icons.Filled.Explore,
                                accent = VolcanicOrange,
                                title = "5 Atmospheric Biomes & Counterplay",
                                bullets = listOf(
                                    "🏜️ Desert: Reduced vision, gentle stamina drain & wind-rippled Sand Drifts (1 tap/surge sweeps clear for bonus points).",
                                    "🧊 Ice: +0.5 stamina recovery per second, but 18% momentum ice-slide risk on open corridors.",
                                    "🌋 Volcano: Heat stamina drain & glowing Lava Puddles (-2s clock unless shielded or Blast Shield Lv.2+).",
                                    "💎 Cavern: Deep darkness radius (upgrade Lantern Optics!) with 2× Gem payout.",
                                    "🏛️ Ruins: Hidden floor pressure traps that sap stamina—spot their violet glyph and mitigate with Blast Shield."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.SmartToy,
                                accent = RivalCrimson,
                                title = "4 Adaptive Rival AI Personalities",
                                bullets = listOf(
                                    "⚡ Goldie: Fast corridor sprinter (45% 2-tile dash) who hunts gems when leading—beat her by cutting wall shortcuts!",
                                    "⛏️ Vex: Relentless wall breacher (smashes up to 3-HP walls)—counter him with Chain TNT and Speed/Surge bursts.",
                                    "🌿 Moss: Disciplined maze tactician who paces smoothly across beacons.",
                                    "🎲 Rubble: Chaotic wildcard who mixes surprise feints with opportunistic breaches."
                                )
                            )
                        }

                        GuideSection.PROGRESSION -> {
                            GuideCard(
                                icon = Icons.Filled.Build,
                                accent = ForgedGold,
                                title = "Earned Mastery Forge Tiers",
                                bullets = listOf(
                                    "Upgrades are genuinely earned—each tier (Tempered → Forged → Obsidian → Mythril → Apex) requires both Gems AND career milestones (Race Wins, Campaign Stars & Blocks Broken).",
                                    "Forge boosts apply across all 20 Expedition Levels & 5 Instant Play Boards (15×15 to 29×29).",
                                    "Build a 7-Win Streak to trigger the +500 💎 Milestone Jackpot, plus claim your Daily Chest, Skill Vault, and Quests."
                                )
                            )
                            GuideCard(
                                icon = Icons.Filled.Verified,
                                accent = EmeraldGem,
                                title = "Daily Ranked 21×21 (Zero-Tolerance SHA-256)",
                                bullets = listOf(
                                    "Daily Ranked runs on a canonical 21×21 seed with identical stats for all players—no Forge bonuses or abilities—ensuring pure skill fairness.",
                                    "Every Ranked finish is replayed deterministically and sealed with a 64-character SHA-256 fingerprint receipt."
                                )
                            )
                        }
                    }
                }

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
                    Text(
                        text = strings.gotIt,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    bullets: List<String>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianDeep,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent
                )
            }
            bullets.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
