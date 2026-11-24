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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LeaderboardEntity
import com.example.engine.EngineSelfTestResult
import com.example.engine.VerifyResult
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LeaderboardHubTab(
    strings: LocalizedStrings,
    entries: List<LeaderboardEntity>,
    lastReceipt: VerifyResult?,
    selfTests: List<EngineSelfTestResult>,
    onRunSelfTests: () -> Unit,
    onClearBoard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Verified Leaderboard Header
        item {
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
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.leaderboardTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
                if (entries.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearBoard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("clear_leaderboard_button")
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = strings.clearBoard,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = CrystalCyan,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = strings.noVerifiedRuns,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            itemsIndexed(entries.take(25)) { index, row ->
                val rankColor = when (index) {
                    0 -> ForgedGold
                    1 -> CrystalCyan
                    2 -> EmeraldGem
                    else -> TextSecondary
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(
                        1.dp,
                        if (index == 0) ForgedGold.copy(alpha = 0.5f) else ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leaderboard_row_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = rankColor
                            )
                            Column {
                                Text(
                                    text = "${row.serverScore} pts • ${row.timeSeconds}s",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${row.seed} • ${row.biome} • ${row.difficulty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "fp:${row.fingerprint.take(16)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = TextMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = EmeraldGem.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = row.verifiedSource,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGem,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Anti-Cheat Receipt Inspector
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, EmeraldGem.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
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
                            Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldGem,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.receiptTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldGem
                        )
                    }
                    if (lastReceipt == null) {
                        Text(
                            text = strings.noReceiptYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Score: ${lastReceipt.serverScore ?: 0} • Rank #${lastReceipt.rank ?: "-"} • ${lastReceipt.source.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "SHA-256: ${lastReceipt.fingerprint ?: "-"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = CrystalCyan
                        )
                    }
                }
            }
        }

        // Built-In Engine Self-Test Runner (Developer Verification Suite)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, CrystalCyan.copy(alpha = 0.35f)),
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
                                Icons.Filled.Science,
                                contentDescription = null,
                                tint = CrystalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.selfTestsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Button(
                        onClick = onRunSelfTests,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrystalCyan,
                            contentColor = ObsidianDeep
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_self_tests_button")
                    ) {
                        Text(strings.runSelfTests, style = MaterialTheme.typography.labelLarge)
                    }

                    if (selfTests.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            selfTests.forEach { res ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (res.passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                        contentDescription = null,
                                        tint = if (res.passed) EmeraldGem else RivalCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = res.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = res.detail,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
