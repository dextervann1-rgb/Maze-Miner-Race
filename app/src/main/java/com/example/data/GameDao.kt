package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM leaderboard ORDER BY server_score DESC LIMIT 100")
    fun observeLeaderboard(): Flow<List<LeaderboardEntity>>

    @Query("SELECT COUNT(*) FROM leaderboard")
    suspend fun leaderboardCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM leaderboard WHERE fingerprint = :fingerprint)")
    suspend fun hasFingerprint(fingerprint: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLeaderboardStrict(entry: LeaderboardEntity)

    @Query(
        "SELECT COUNT(*) + 1 FROM leaderboard WHERE server_score > " +
            "(SELECT server_score FROM leaderboard WHERE fingerprint = :fingerprint)"
    )
    suspend fun rankForFingerprint(fingerprint: String): Int

    @Transaction
    suspend fun insertLeaderboardTransactional(entry: LeaderboardEntity): Int {
        insertLeaderboardStrict(entry)
        return rankForFingerprint(entry.fingerprint)
    }

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()

    // --- Ghost Trails ---

    @Query("SELECT * FROM ghost_trails WHERE seed = :seed LIMIT 1")
    suspend fun getGhostTrail(seed: String): GhostTrailEntity?

    @Upsert
    suspend fun upsertGhostTrail(trail: GhostTrailEntity)

    // --- Campaign Progress ---

    @Query("SELECT * FROM campaign_progress ORDER BY level_id ASC")
    fun observeCampaignProgress(): Flow<List<CampaignProgressEntity>>

    @Query("SELECT * FROM campaign_progress WHERE level_id = :levelId LIMIT 1")
    suspend fun getCampaignProgress(levelId: Int): CampaignProgressEntity?

    @Upsert
    suspend fun upsertCampaignProgress(progress: CampaignProgressEntity)

    // --- Player Profile ---

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: PlayerProfileEntity)
}
