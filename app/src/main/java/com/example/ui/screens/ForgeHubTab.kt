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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.billing.BillingManager
import com.example.billing.BillingUiState
import com.example.billing.StorePackSpec
import com.example.data.PlayerProfileEntity
import com.example.data.hasAchievement
import com.example.data.levelFor
import com.example.data.totalUpgradeLevels
import com.example.engine.AchievementId
import com.example.engine.UpgradeType
import com.example.i18n.AppStrings
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
import com.example.viewmodel.LiveRaceState
import com.example.viewmodel.todayUtcSeed

@Composable
fun ForgeHubTab(
    strings: LocalizedStrings,
    profile: PlayerProfileEntity,
    liveRace: LiveRaceState?,
    billingState: BillingUiState,
    onBuyUpgrade: (UpgradeType) -> Unit,
    onBuyStorePack: (StorePackSpec) -> Unit,
    onOpenDailyChest: () -> Unit,
    onClaimSkillVault: () -> Unit,
    onClaimQuests: () -> Unit
) {
    val today = todayUtcSeed()
    val chestOpened = profile.chestOpenedDay == today
    val vaultClaimed = profile.vaultChallengeDay == today
    val q1Done = liveRace?.questBreakDone ?: 0
    val q2Done = liveRace?.questGemDone ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Real Google Play Billing Monetization Packs Store (Creator Program Compliant — Zero Ad SDKs)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.55f)),
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
                                Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = ForgedGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Miner's Treasury Store",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ForgedGold
                                )
                                Text(
                                    text = billingState.statusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (billingState.isBillingConnected) EmeraldGem else TextMuted
                                )
                            }
                        }

                        if (profile.founderPackOwned) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = ForgedGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, ForgedGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = ForgedGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("FOUNDER +15%", style = MaterialTheme.typography.labelSmall, color = ForgedGold)
                                }
                            }
                        }
                    }

                    BillingManager.PACKS.forEach { pack ->
                        val liveDetail = billingState.liveProducts[pack.productId]
                        val priceText = liveDetail?.oneTimePurchaseOfferDetails?.formattedPrice
                            ?: pack.fallbackDisplayPrice
                        val ownedNonConsumable = !pack.isConsumable && pack.unlocksFounderAura && profile.founderPackOwned

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianDeep,
                            border = BorderStroke(
                                1.dp,
                                if (pack.badge != null) ForgedGold.copy(alpha = 0.5f) else ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (pack.unlocksFounderAura) Icons.Filled.WorkspacePremium else Icons.Filled.Diamond,
                                            contentDescription = null,
                                            tint = if (pack.unlocksFounderAura) ForgedGold else CrystalCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = pack.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        if (pack.badge != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ForgedGold.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = pack.badge,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = ForgedGold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = pack.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { onBuyStorePack(pack) },
                                    enabled = !ownedNonConsumable,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (ownedNonConsumable) EmeraldGem else ForgedGold,
                                        contentColor = ObsidianDeep
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("store_pack_${pack.productId}")
                                ) {
                                    Text(
                                        text = if (ownedNonConsumable) "OWNED" else priceText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Miner's Chest Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ForgedGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_chest_1795479090485),
                        contentDescription = strings.dailyChestTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.dailyChestTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = ForgedGold
                        )
                        Text(
                            text = if (chestOpened) strings.chestOpenedToday else "+20–80 💎 (${strings.streak} bonus)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Button(
                            onClick = onOpenDailyChest,
                            enabled = !chestOpened,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForgedGold,
                                contentColor = ObsidianDeep
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_daily_chest_button")
                        ) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.openChest, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Creator-Program-Compliant Daily Skill Vault Bonus Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = CrystalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.bonusVaultTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = CrystalCyan
                            )
                        }
                        Text(
                            text = if (vaultClaimed) strings.vaultClaimedToday else strings.bonusVaultDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onClaimSkillVault,
                        enabled = !vaultClaimed,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (vaultClaimed) ObsidianBorder else CrystalCyan),
                        modifier = Modifier.testTag("claim_skill_vault_button")
                    ) {
                        Text(
                            text = if (vaultClaimed) "✓" else strings.claimBonusVault,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (vaultClaimed) TextMuted else CrystalCyan
                        )
                    }
                }
            }
        }

        // Upgrade Forge Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.upgradeForgeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = strings.upgradesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ForgedGold.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Lv.${profile.totalUpgradeLevels()}/28",
                        style = MaterialTheme.typography.labelMedium,
                        color = ForgedGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 6 Permanent Upgrade Cards
        items(UpgradeType.entries) { upgrade ->
            val currentLvl = profile.levelFor(upgrade)
            val isMax = currentLvl >= upgrade.maxLevel
            val cost = upgrade.costForNextLevel(currentLvl)
            val canAfford = profile.gems >= cost

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (isMax) EmeraldGem.copy(alpha = 0.5f) else ObsidianBorder
                ),
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
                                text = AppStrings.upgradeName(strings, upgrade),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = AppStrings.upgradeDesc(strings, upgrade),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onBuyUpgrade(upgrade) },
                            enabled = !isMax && canAfford,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMax) EmeraldGem else ForgedGold,
                                contentColor = ObsidianDeep
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("upgrade_${upgrade.name.lowercase()}")
                        ) {
                            if (isMax) {
                                Text(strings.maxLevel, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$cost 💎", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { currentLvl.toFloat() / upgrade.maxLevel.toFloat() },
                            color = if (isMax) EmeraldGem else CrystalCyan,
                            trackColor = ObsidianDeep,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                        Text(
                            text = "$currentLvl/${upgrade.maxLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Daily Quests Card
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
                        text = strings.dailyQuestsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = ForgedGold
                    )
                    QuestRow(strings.questBreakBlocks, q1Done, 5, "+20 💎")
                    QuestRow(strings.questCollectGems, q2Done, 3, "+25 💎")
                    Button(
                        onClick = onClaimQuests,
                        enabled = q1Done >= 5 || q2Done >= 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGem,
                            contentColor = ObsidianDeep
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_quests_button")
                    ) {
                        Text(strings.claimRewards, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Achievements Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.MilitaryTech,
                            contentDescription = null,
                            tint = ForgedGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.achievementsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    AchievementId.entries.forEach { ach ->
                        val unlocked = profile.hasAchievement(ach)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (unlocked) EmeraldGem else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = AppStrings.achievementLabel(strings, ach),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (unlocked) TextPrimary else TextMuted
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuestRow(label: String, done: Int, goal: Int, reward: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "$label ($done/$goal)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = reward, style = MaterialTheme.typography.labelMedium, color = CrystalCyan)
        }
        LinearProgressIndicator(
            progress = { (done.toFloat() / goal.toFloat()).coerceIn(0f, 1f) },
            color = ForgedGold,
            trackColor = ObsidianDeep,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
        )
    }
}
