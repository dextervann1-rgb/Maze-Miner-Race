package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "leaderboard",
    indices = [
        Index(value = ["server_score"], name = "idx_leaderboard_score", orders = [Index.Order.DESC]),
        Index(
            value = ["season", "server_score"],
            name = "idx_leaderboard_season",
            orders = [Index.Order.ASC, Index.Order.DESC]
        )
    ]
)
data class LeaderboardEntity(
    @PrimaryKey
    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,
    @ColumnInfo(name = "seed")
    val seed: String,
    @ColumnInfo(name = "server_score")
    val serverScore: Int,
    @ColumnInfo(name = "time_seconds")
    val timeSeconds: Int,
    @ColumnInfo(name = "difficulty")
    val difficulty: String,
    @ColumnInfo(name = "biome")
    val biome: String,
    @ColumnInfo(name = "season")
    val season: String = "season-1",
    @ColumnInfo(name = "verified_source")
    val verifiedSource: String = "SQLITE",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ghost_trails")
data class GhostTrailEntity(
    @PrimaryKey
    @ColumnInfo(name = "seed")
    val seed: String,
    @ColumnInfo(name = "trail_encoded")
    val trailEncoded: String,
    @ColumnInfo(name = "step_count")
    val stepCount: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "campaign_progress")
data class CampaignProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "level_id")
    val levelId: Int,
    @ColumnInfo(name = "stars")
    val stars: Int = 0,
    @ColumnInfo(name = "best_score")
    val bestScore: Int = 0,
    @ColumnInfo(name = "best_time_seconds")
    val bestTimeSeconds: Int = 999,
    @ColumnInfo(name = "first_clear_claimed")
    val firstClearClaimed: Boolean = false
)

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val gems: Int = 60,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalWins: Int = 0,
    val totalBlocksBroken: Int = 0,
    val totalGemsCollected: Int = 0,
    val chestOpenedDay: String = "",
    val vaultChallengeDay: String = "",
    val unlockedAchievementsCsv: String = "",
    val pickLevel: Int = 0,
    val lanternLevel: Int = 0,
    val staminaLevel: Int = 0,
    val sonarLevel: Int = 0,
    val harvesterLevel: Int = 0,
    val shieldLevel: Int = 0,
    val founderPackOwned: Boolean = false,
    val languageCode: String = "en",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showDpad: Boolean = true,
    val verifierServerUrl: String = ""
)
