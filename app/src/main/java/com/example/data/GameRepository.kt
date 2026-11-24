package com.example.data

import android.database.sqlite.SQLiteConstraintException
import com.example.engine.AchievementId
import com.example.engine.Biome
import com.example.engine.CampaignLevel
import com.example.engine.DeterministicRng
import com.example.engine.Difficulty
import com.example.engine.ENGINE_VERSION
import com.example.engine.GridPos
import com.example.engine.Modifier
import com.example.engine.ReplayAction
import com.example.engine.ReplaySimulator
import com.example.engine.UpgradeType
import com.example.engine.VerificationSource
import com.example.engine.VerifyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Repository coordinating Room SQLite storage, canonical 0-tolerance anti-cheat verification,
 * UNIQUE replay fingerprint enforcement, 15-level expedition campaign progress, ghost trails,
 * forge upgrades, and optional remote server sync.
 */
class GameRepository(private val dao: GameDao) {

    val leaderboard: Flow<List<LeaderboardEntity>> = dao.observeLeaderboard()

    val profile: Flow<PlayerProfileEntity> = dao.observeProfile().map { it ?: PlayerProfileEntity() }

    val campaignProgress: Flow<List<CampaignProgressEntity>> = dao.observeCampaignProgress()

    suspend fun ensureProfileInitialized(): PlayerProfileEntity = withContext(Dispatchers.IO) {
        val existing = dao.getProfileOnce()
        if (existing != null) existing
        else PlayerProfileEntity().also { dao.upsertProfile(it) }
    }

    suspend fun updateProfile(transform: (PlayerProfileEntity) -> PlayerProfileEntity): PlayerProfileEntity =
        withContext(Dispatchers.IO) {
            val current = dao.getProfileOnce() ?: PlayerProfileEntity()
            val updated = transform(current)
            dao.upsertProfile(updated)
            updated
        }

    /**
     * Records a completed Expedition Campaign level clear, computing 1-3 stars and granting
     * the one-time first-clear gem reward. Returns (starsEarned, firstClearGemsAwarded).
     */
    suspend fun recordCampaignClear(
        level: CampaignLevel,
        score: Int,
        elapsedTicks: Int
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val stars = when {
            score >= level.threeStarScore -> 3
            score >= level.twoStarScore -> 2
            else -> 1
        }
        val existing = dao.getCampaignProgress(level.id)
        val firstClearBonus = if (existing == null || !existing.firstClearClaimed) {
            level.firstClearGems
        } else {
            0
        }
        val merged = CampaignProgressEntity(
            levelId = level.id,
            stars = max(existing?.stars ?: 0, stars),
            bestScore = max(existing?.bestScore ?: 0, score),
            bestTimeSeconds = min(existing?.bestTimeSeconds ?: 999, elapsedTicks),
            firstClearClaimed = true
        )
        dao.upsertCampaignProgress(merged)
        stars to firstClearBonus
    }

    suspend fun getGhostTrail(seed: String): List<GridPos> = withContext(Dispatchers.IO) {
        val entity = dao.getGhostTrail(seed) ?: return@withContext emptyList()
        decodeTrail(entity.trailEncoded)
    }

    suspend fun saveGhostTrailIfFaster(seed: String, trail: List<GridPos>) = withContext(Dispatchers.IO) {
        if (trail.size < 2) return@withContext
        val existing = dao.getGhostTrail(seed)
        if (existing == null || trail.size < existing.stepCount) {
            dao.upsertGhostTrail(
                GhostTrailEntity(
                    seed = seed,
                    trailEncoded = encodeTrail(trail),
                    stepCount = trail.size
                )
            )
        }
    }

    suspend fun clearLeaderboard() = withContext(Dispatchers.IO) {
        dao.clearLeaderboard()
    }

    suspend fun checkRemoteServerHealth(baseUrl: String): HealthResponseDto? = withContext(Dispatchers.IO) {
        val api = VerifierApiFactory.create(baseUrl) ?: return@withContext null
        runCatching { api.health() }.getOrNull()
    }

    suspend fun requestRemoteSession(baseUrl: String): SessionResponseDto? = withContext(Dispatchers.IO) {
        val api = VerifierApiFactory.create(baseUrl) ?: return@withContext null
        runCatching { api.createSession() }.getOrNull()
    }

    /**
     * Validates a Google Play Billing purchase token against the configured backend verifier
     * (which invokes Google Play Developer API `purchases.products.get`). When no remote server
     * URL is configured, returns true so local Play Store test flows remain operable.
     */
    suspend fun verifyPlayPurchaseToken(
        baseUrl: String,
        packageName: String,
        productId: String,
        purchaseToken: String,
        orderId: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext true
        val api = VerifierApiFactory.create(baseUrl) ?: return@withContext false
        val response = runCatching {
            api.verifyPurchase(
                PurchaseVerifyRequestDto(
                    productId = productId,
                    purchaseToken = purchaseToken,
                    packageName = packageName,
                    orderId = orderId
                )
            )
        }.getOrNull()
        response != null && response.ok && response.verified
    }

    /**
     * Verifies a completed run using the canonical ReplaySimulator (0-tolerance) and optional
     * remote v12 Node verifier, then persists transactionally into Room SQLite with UNIQUE
     * SHA-256 fingerprint enforcement.
     */
    suspend fun verifyAndRecordRun(
        seed: String,
        difficulty: Difficulty,
        modifier: Modifier,
        biome: Biome,
        actions: List<ReplayAction>,
        ticks: Int,
        clientScore: Int,
        isPractice: Boolean,
        remoteServerUrl: String,
        remoteSession: SessionResponseDto?
    ): VerifyResult = withContext(Dispatchers.IO) {
        if (isPractice) {
            val fp = DeterministicRng.fingerprintActions(actions)
            return@withContext VerifyResult(
                ok = true,
                verified = false,
                serverScore = clientScore,
                fingerprint = fp,
                reason = "practice",
                source = VerificationSource.PRACTICE_UNRANKED
            )
        }

        // Step 1: Canonical 0-tolerance simulation check
        val localSim = ReplaySimulator.simulateRun(
            seed = seed,
            difficulty = difficulty,
            modifier = modifier,
            biome = biome,
            actions = actions,
            ticks = ticks,
            clientScore = clientScore
        )
        if (!localSim.ok || localSim.serverScore == null || localSim.fingerprint == null) {
            return@withContext localSim.copy(source = VerificationSource.REJECTED)
        }

        // Step 2: Optional remote server authoritative verification if configured & reachable
        var verifiedSource = VerificationSource.CANONICAL_LOCAL_SQLITE
        var finalScore = localSim.serverScore
        var finalFp = localSim.fingerprint
        var remoteRank: Int? = null

        if (remoteServerUrl.isNotBlank() && remoteSession != null && remoteSession.seed == seed) {
            val api = VerifierApiFactory.create(remoteServerUrl)
            if (api != null) {
                val wireActions = actions.map { a ->
                    when (a) {
                        is ReplayAction.Move -> ActionWireDto(t = "move", dx = a.dx, dy = a.dy, tick = a.tick)
                        is ReplayAction.Mine -> ActionWireDto(t = "mine", x = a.x, y = a.y, tick = a.tick)
                    }
                }
                val dto = RunSubmissionDto(
                    sessionId = remoteSession.sessionId,
                    token = remoteSession.token,
                    seed = seed,
                    version = ENGINE_VERSION,
                    difficulty = difficulty.wireName,
                    modifier = modifier.wireName,
                    biome = biome.wireName,
                    actions = wireActions,
                    ticks = ticks,
                    clientScore = clientScore
                )
                val remoteRes = runCatching { api.submitRun(dto) }.getOrNull()
                if (remoteRes != null) {
                    if (remoteRes.ok && remoteRes.verified) {
                        verifiedSource = VerificationSource.REMOTE_SERVER
                        finalScore = remoteRes.serverScore ?: finalScore
                        finalFp = remoteRes.fingerprint ?: finalFp
                        remoteRank = remoteRes.rank
                    } else if (!remoteRes.ok && remoteRes.reason != null) {
                        return@withContext VerifyResult(
                            ok = false,
                            verified = false,
                            reason = "remote rejected: ${remoteRes.reason}",
                            source = VerificationSource.REJECTED
                        )
                    }
                }
            }
        }

        // Step 3: Transactional SQLite INSERT with UNIQUE fingerprint enforcement
        val entry = LeaderboardEntity(
            fingerprint = finalFp,
            seed = seed,
            serverScore = finalScore,
            timeSeconds = ticks,
            difficulty = difficulty.wireName,
            biome = biome.wireName,
            season = "season-1",
            verifiedSource = if (verifiedSource == VerificationSource.REMOTE_SERVER) "SERVER" else "SQLITE"
        )

        return@withContext try {
            val localRank = dao.insertLeaderboardTransactional(entry)
            VerifyResult(
                ok = true,
                verified = true,
                serverScore = finalScore,
                rank = remoteRank ?: localRank,
                fingerprint = finalFp,
                source = verifiedSource
            )
        } catch (_: SQLiteConstraintException) {
            VerifyResult(
                ok = false,
                verified = false,
                serverScore = finalScore,
                fingerprint = finalFp,
                reason = "duplicate replay",
                source = VerificationSource.REJECTED
            )
        }
    }

    suspend fun purchaseUpgrade(upgrade: UpgradeType): Pair<Boolean, PlayerProfileEntity> =
        withContext(Dispatchers.IO) {
            val current = dao.getProfileOnce() ?: PlayerProfileEntity()
            val curLvl = current.levelFor(upgrade)
            if (curLvl >= upgrade.maxLevel) return@withContext (false to current)
            val cost = upgrade.costForNextLevel(curLvl)
            if (current.gems < cost) return@withContext (false to current)

            var updated = current.copy(gems = current.gems - cost).withUpgradeLevel(upgrade, curLvl + 1)
            val totalLevels = updated.totalUpgradeLevels()
            if (totalLevels >= 5 && !updated.hasAchievement(AchievementId.UPGRADE_MASTER)) {
                updated = updated
                    .withAchievement(AchievementId.UPGRADE_MASTER)
                    .copy(gems = updated.gems + AchievementId.UPGRADE_MASTER.rewardGems)
            }
            dao.upsertProfile(updated)
            true to updated
        }

    companion object {
        fun encodeTrail(trail: List<GridPos>): String =
            trail.joinToString(";") { "${it.x},${it.y}" }

        fun decodeTrail(encoded: String): List<GridPos> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toIntOrNull()
                    val y = parts[1].toIntOrNull()
                    if (x != null && y != null) GridPos(x, y) else null
                } else null
            }
        }
    }
}

fun PlayerProfileEntity.levelFor(upgrade: UpgradeType): Int = when (upgrade) {
    UpgradeType.TITANIUM_PICK -> pickLevel
    UpgradeType.LANTERN_OPTICS -> lanternLevel
    UpgradeType.STAMINA_FLASK -> staminaLevel
    UpgradeType.SONAR_RIG -> sonarLevel
    UpgradeType.GEM_HARVESTER -> harvesterLevel
    UpgradeType.BLAST_SHIELD -> shieldLevel
}

fun PlayerProfileEntity.withUpgradeLevel(upgrade: UpgradeType, newLevel: Int): PlayerProfileEntity =
    when (upgrade) {
        UpgradeType.TITANIUM_PICK -> copy(pickLevel = newLevel)
        UpgradeType.LANTERN_OPTICS -> copy(lanternLevel = newLevel)
        UpgradeType.STAMINA_FLASK -> copy(staminaLevel = newLevel)
        UpgradeType.SONAR_RIG -> copy(sonarLevel = newLevel)
        UpgradeType.GEM_HARVESTER -> copy(harvesterLevel = newLevel)
        UpgradeType.BLAST_SHIELD -> copy(shieldLevel = newLevel)
    }

fun PlayerProfileEntity.totalUpgradeLevels(): Int =
    pickLevel + lanternLevel + staminaLevel + sonarLevel + harvesterLevel + shieldLevel

fun PlayerProfileEntity.unlockedAchievementSet(): Set<AchievementId> {
    if (unlockedAchievementsCsv.isBlank()) return emptySet()
    return unlockedAchievementsCsv.split(",")
        .mapNotNull { name -> AchievementId.entries.find { it.name == name } }
        .toSet()
}

fun PlayerProfileEntity.hasAchievement(id: AchievementId): Boolean = id in unlockedAchievementSet()

fun PlayerProfileEntity.withAchievement(id: AchievementId): PlayerProfileEntity {
    val set = unlockedAchievementSet().toMutableSet()
    set.add(id)
    return copy(unlockedAchievementsCsv = set.joinToString(",") { it.name })
}
