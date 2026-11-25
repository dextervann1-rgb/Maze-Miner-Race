package com.example.cloud

import androidx.annotation.Keep
import com.example.data.CampaignProgressEntity
import com.example.engine.Biome
import com.example.engine.BoardArchetype
import com.example.engine.Difficulty
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudProfileSave(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Miner",
    val gems: Long = 35L,
    val streak: Long = 0L,
    val bestStreak: Long = 0L,
    val totalWins: Long = 0L,
    val totalBlocksBroken: Long = 0L,
    val totalGemsCollected: Long = 0L,
    val pickLevel: Long = 0L,
    val lanternLevel: Long = 0L,
    val staminaLevel: Long = 0L,
    val sonarLevel: Long = 0L,
    val harvesterLevel: Long = 0L,
    val shieldLevel: Long = 0L,
    val founderPackOwned: Boolean = false,
    val pvpWins: Long = 0L,
    val pvpMatches: Long = 0L,
    val campaignSummary: String = "",
    val gauntletCompleted: Boolean = false,
    val highestAbyssalFloor: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun encodeCampaignSummary(progress: List<CampaignProgressEntity>): String {
            return progress
                .filter { it.stars > 0 || it.bestScore > 0 }
                .sortedBy { it.levelId }
                .joinToString(",") { "${it.levelId}:${it.stars}:${it.bestScore}:${it.bestTimeSeconds}" }
                .take(1200)
        }

        fun decodeCampaignSummary(summary: String): List<CampaignProgressEntity> {
            if (summary.isBlank()) return emptyList()
            return summary.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size < 3) return@mapNotNull null
                val levelId = parts[0].toIntOrNull() ?: return@mapNotNull null
                val stars = parts[1].toIntOrNull()?.coerceIn(0, 3) ?: return@mapNotNull null
                val bestScore = parts[2].toIntOrNull()?.coerceAtLeast(0) ?: 0
                val bestTime = parts.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(1) ?: 999
                CampaignProgressEntity(
                    levelId = levelId,
                    stars = stars,
                    bestScore = bestScore,
                    bestTimeSeconds = bestTime,
                    firstClearClaimed = stars > 0
                )
            }
        }
    }
}

enum class PvpRoomStatus {
    WAITING,
    ACTIVE,
    FINISHED;

    companion object {
        fun fromRaw(raw: String?): PvpRoomStatus =
            entries.firstOrNull { it.name == raw } ?: WAITING
    }
}

@Keep
@IgnoreExtraProperties
data class PvpMatchRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val seed: String = "",
    val biome: String = "VOLCANO",
    val boardSize: Long = 21L,
    val difficulty: String = "NORMAL",
    val status: String = "WAITING",
    val hostUid: String = "",
    val hostName: String = "Host",
    val hostX: Long = 1L,
    val hostY: Long = 1L,
    val hostScore: Long = 0L,
    val hostBeacons: Long = 0L,
    val hostFinished: Boolean = false,
    val guestUid: String = "",
    val guestName: String = "",
    val guestX: Long = 1L,
    val guestY: Long = 1L,
    val guestScore: Long = 0L,
    val guestBeacons: Long = 0L,
    val guestFinished: Boolean = false,
    val winnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): PvpRoomStatus = PvpRoomStatus.fromRaw(status)

    fun parsedBiome(): Biome = Biome.entries.firstOrNull { it.name == biome } ?: Biome.VOLCANO

    fun parsedDifficulty(): Difficulty =
        Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.NORMAL

    fun parsedArchetype(): BoardArchetype =
        BoardArchetype.entries.firstOrNull { it.size == boardSize.toInt() } ?: BoardArchetype.CLASSIC_21
}

@Keep
@IgnoreExtraProperties
data class CommunityMazeSeed(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "Miner",
    val title: String = "",
    val seed: String = "",
    val biome: String = "DESERT",
    val boardSize: Long = 21L,
    val difficulty: String = "NORMAL",
    val strategicMaze: Boolean = true,
    val visibility: String = "PUBLIC",
    val bestScore: Long = 0L,
    val playsCount: Long = 0L,
    val upvotes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedBiome(): Biome = Biome.entries.firstOrNull { it.name == biome } ?: Biome.DESERT

    fun parsedDifficulty(): Difficulty =
        Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.NORMAL

    fun parsedArchetype(): BoardArchetype =
        BoardArchetype.entries.firstOrNull { it.size == boardSize.toInt() } ?: BoardArchetype.CLASSIC_21
}

@Keep
@IgnoreExtraProperties
data class DailyRankedGhostEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "Miner",
    val score: Long = 0L,
    val clearTimeMs: Long = 0L,
    val sha256Fingerprint: String = "",
    val compressedInputs: String = "",
    @ServerTimestamp val submittedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AbyssalLeaderboardEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "Miner",
    val highestFloor: Long = 1L,
    val totalScore: Long = 0L,
    val runRootSeed: Long = 0L,
    val runFingerprint: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveBalanceConfigDto(
    val goldieSpeedMultiplier: Double? = null,
    val vexDrillCooldownMs: Long? = null,
    val ruinsTrapCycleMs: Long? = null,
    val feverDecayRatePerSec: Double? = null,
    val endlessModeUnlockStars: Long? = null
) {
    fun toConfig(): com.example.engine.LiveBalanceConfig {
        val defaults = com.example.engine.LiveBalanceConfig()
        return com.example.engine.LiveBalanceConfig(
            goldieSpeedMultiplier = goldieSpeedMultiplier?.toFloat()?.coerceIn(0.6f, 2.0f) ?: defaults.goldieSpeedMultiplier,
            vexDrillCooldownMs = vexDrillCooldownMs?.coerceIn(400L, 4000L) ?: defaults.vexDrillCooldownMs,
            ruinsTrapCycleMs = ruinsTrapCycleMs?.coerceIn(800L, 6000L) ?: defaults.ruinsTrapCycleMs,
            feverDecayRatePerSec = feverDecayRatePerSec?.toFloat()?.coerceIn(0.05f, 1.0f) ?: defaults.feverDecayRatePerSec,
            endlessModeUnlockStars = endlessModeUnlockStars?.toInt()?.coerceIn(0, 60) ?: defaults.endlessModeUnlockStars
        )
    }
}
