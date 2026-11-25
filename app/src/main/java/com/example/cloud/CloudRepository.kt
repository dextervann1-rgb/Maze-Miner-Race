package com.example.cloud

import android.content.Context
import com.example.R
import com.example.data.CampaignProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.engine.Biome
import com.example.engine.Difficulty
import com.example.engine.LiveBalanceConfig
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Production Firestore repository for Maze Miner Race Path 9.8+ Cloud Architecture:
 * 1. Cross-Device Cloud Saves (/users/{userId})
 * 2. Live PvP Cloud Matchmaking & Telemetry (/pvp_rooms/{roomId})
 * 3. Shareable Community Maze Seeds (/community_seeds/{seedId})
 */
class CloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing cloud services.")
        }
        return uid
    }

    private fun sanitizeName(raw: String?): String {
        val cleaned = raw?.trim()?.take(40).orEmpty()
        return if (cleaned.isNotEmpty()) cleaned else "Miner"
    }

    // =========================================================================
    // 1. CROSS-DEVICE CLOUD SAVE SYNC (/users/{userId})
    // =========================================================================

    fun observeCloudProfile(userId: String): Flow<CloudProfileSave?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CloudProfileSave::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Performs a smart, lossless bidirectional merge between local Room profile/campaign and
     * Firestore cloud backup so switching devices preserves maximum career progression.
     */
    suspend fun syncAndMergeCloudProfile(
        localProfile: PlayerProfileEntity,
        localCampaign: List<CampaignProgressEntity>,
        displayName: String,
        pvpWinDelta: Int = 0,
        pvpMatchDelta: Int = 0
    ): Result<CloudProfileSave> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Sign in with Google to sync cloud saves."))
        }
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val mergedResult = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val remote = if (snap.exists()) {
                    snap.toObject(CloudProfileSave::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }

                val remoteCampaign = CloudProfileSave.decodeCampaignSummary(remote?.campaignSummary.orEmpty())
                val mergedCampaignMap = LinkedHashMap<Int, CampaignProgressEntity>()
                (localCampaign + remoteCampaign).forEach { entry ->
                    val cur = mergedCampaignMap[entry.levelId]
                    mergedCampaignMap[entry.levelId] = if (cur == null) {
                        entry
                    } else {
                        CampaignProgressEntity(
                            levelId = entry.levelId,
                            stars = max(cur.stars, entry.stars),
                            bestScore = max(cur.bestScore, entry.bestScore),
                            bestTimeSeconds = min(cur.bestTimeSeconds, entry.bestTimeSeconds),
                            firstClearClaimed = cur.firstClearClaimed || entry.firstClearClaimed
                        )
                    }
                }
                val mergedCampaignSummary = CloudProfileSave.encodeCampaignSummary(mergedCampaignMap.values.toList())

                val mergedGems = max(localProfile.gems.toLong(), remote?.gems ?: 0L).coerceIn(0L, 9_999_999L)
                val mergedStreak = max(localProfile.streak.toLong(), remote?.streak ?: 0L).coerceIn(0L, 9_999L)
                val mergedBestStreak = max(localProfile.bestStreak.toLong(), remote?.bestStreak ?: 0L).coerceIn(0L, 9_999L)
                val mergedWins = max(localProfile.totalWins.toLong(), remote?.totalWins ?: 0L).coerceIn(0L, 999_999L)
                val mergedBlocks = max(localProfile.totalBlocksBroken.toLong(), remote?.totalBlocksBroken ?: 0L).coerceIn(0L, 99_999_999L)
                val mergedGemsCollected = max(localProfile.totalGemsCollected.toLong(), remote?.totalGemsCollected ?: 0L).coerceIn(0L, 99_999_999L)
                val mergedPick = max(localProfile.pickLevel.toLong(), remote?.pickLevel ?: 0L).coerceIn(0L, 5L)
                val mergedLantern = max(localProfile.lanternLevel.toLong(), remote?.lanternLevel ?: 0L).coerceIn(0L, 5L)
                val mergedStamina = max(localProfile.staminaLevel.toLong(), remote?.staminaLevel ?: 0L).coerceIn(0L, 5L)
                val mergedSonar = max(localProfile.sonarLevel.toLong(), remote?.sonarLevel ?: 0L).coerceIn(0L, 5L)
                val mergedHarvester = max(localProfile.harvesterLevel.toLong(), remote?.harvesterLevel ?: 0L).coerceIn(0L, 5L)
                val mergedShield = max(localProfile.shieldLevel.toLong(), remote?.shieldLevel ?: 0L).coerceIn(0L, 3L)
                val mergedFounder = localProfile.founderPackOwned || (remote?.founderPackOwned == true)
                val mergedPvpWins = ((remote?.pvpWins ?: 0L) + pvpWinDelta.toLong()).coerceIn(0L, 999_999L)
                val mergedPvpMatches = ((remote?.pvpMatches ?: 0L) + pvpMatchDelta.toLong()).coerceIn(0L, 999_999L)
                val mergedGauntletCompleted = localProfile.gauntletCompleted || (remote?.gauntletCompleted == true)
                val mergedHighestAbyssal = max(localProfile.highestAbyssalFloor.toLong(), remote?.highestAbyssalFloor ?: 0L).coerceIn(0L, 999L)
                val safeName = sanitizeName(displayName)

                val mutableFields = mapOf<String, Any>(
                    "displayName" to safeName,
                    "gems" to mergedGems,
                    "streak" to mergedStreak,
                    "bestStreak" to mergedBestStreak,
                    "totalWins" to mergedWins,
                    "totalBlocksBroken" to mergedBlocks,
                    "totalGemsCollected" to mergedGemsCollected,
                    "pickLevel" to mergedPick,
                    "lanternLevel" to mergedLantern,
                    "staminaLevel" to mergedStamina,
                    "sonarLevel" to mergedSonar,
                    "harvesterLevel" to mergedHarvester,
                    "shieldLevel" to mergedShield,
                    "founderPackOwned" to mergedFounder,
                    "pvpWins" to mergedPvpWins,
                    "pvpMatches" to mergedPvpMatches,
                    "campaignSummary" to mergedCampaignSummary,
                    "gauntletCompleted" to mergedGauntletCompleted,
                    "highestAbyssalFloor" to mergedHighestAbyssal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (remote == null) {
                    val createPayload = mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(docRef, createPayload)
                } else {
                    tx.update(docRef, mutableFields)
                }

                CloudProfileSave(
                    id = uid,
                    userId = uid,
                    displayName = safeName,
                    gems = mergedGems,
                    streak = mergedStreak,
                    bestStreak = mergedBestStreak,
                    totalWins = mergedWins,
                    totalBlocksBroken = mergedBlocks,
                    totalGemsCollected = mergedGemsCollected,
                    pickLevel = mergedPick,
                    lanternLevel = mergedLantern,
                    staminaLevel = mergedStamina,
                    sonarLevel = mergedSonar,
                    harvesterLevel = mergedHarvester,
                    shieldLevel = mergedShield,
                    founderPackOwned = mergedFounder,
                    pvpWins = mergedPvpWins,
                    pvpMatches = mergedPvpMatches,
                    campaignSummary = mergedCampaignSummary,
                    gauntletCompleted = mergedGauntletCompleted,
                    highestAbyssalFloor = mergedHighestAbyssal,
                    createdAt = remote?.createdAt ?: Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            }.await()

            Result.success(mergedResult)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. LIVE PVP CLOUD MATCHMAKING (/pvp_rooms/{roomId})
    // =========================================================================

    fun observeOpenPvpRooms(): Flow<List<PvpMatchRoom>> = flow {
        val uid = requireUserId()
        val path = "pvp_rooms"
        emitAll(
            db.collection("pvp_rooms")
                .whereEqualTo("status", "WAITING")
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PvpMatchRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePvpRoom(roomId: String): Flow<PvpMatchRoom?> = flow {
        val uid = requireUserId()
        require(roomId.isNotBlank()) { "Room ID must not be blank" }
        val path = "pvp_rooms/$roomId"
        emitAll(
            db.collection("pvp_rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PvpMatchRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPvpRoom(
        displayName: String,
        seed: String,
        biome: Biome,
        boardSize: Int,
        difficulty: Difficulty
    ): Result<PvpMatchRoom> {
        var path = "pvp_rooms"
        return try {
            val uid = requireUserId()
            val safeSeed = seed.trim().ifBlank { "pvp-${System.currentTimeMillis() % 100000}" }.take(64)
            val finalSeed = if (safeSeed.length < 3) "pvp-$safeSeed" else safeSeed
            val safeSize = if (boardSize in listOf(15, 19, 21, 25, 29)) boardSize.toLong() else 21L
            val safeName = sanitizeName(displayName)
            val codeSuffix = ((System.nanoTime() and 0xFFFF) % 9000 + 1000).toInt()
            val roomCode = "MZ-$codeSuffix"
            val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            path = "pvp_rooms/$roomId"

            val payload = mapOf<String, Any>(
                "roomCode" to roomCode,
                "seed" to finalSeed,
                "biome" to biome.name,
                "boardSize" to safeSize,
                "difficulty" to difficulty.name,
                "status" to "WAITING",
                "hostUid" to uid,
                "hostName" to safeName,
                "hostX" to 1L,
                "hostY" to 1L,
                "hostScore" to 0L,
                "hostBeacons" to 0L,
                "hostFinished" to false,
                "guestUid" to "",
                "guestName" to "",
                "guestX" to 1L,
                "guestY" to 1L,
                "guestScore" to 0L,
                "guestBeacons" to 0L,
                "guestFinished" to false,
                "winnerUid" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("pvp_rooms").document(roomId).set(payload).await()
            Result.success(
                PvpMatchRoom(
                    id = roomId,
                    roomCode = roomCode,
                    seed = finalSeed,
                    biome = biome.name,
                    boardSize = safeSize,
                    difficulty = difficulty.name,
                    status = "WAITING",
                    hostUid = uid,
                    hostName = safeName,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinPvpRoom(roomId: String, displayName: String): Result<Unit> {
        val path = "pvp_rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID cannot be blank" }
            val safeName = sanitizeName(displayName)
            db.collection("pvp_rooms").document(roomId).update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to safeName,
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinPvpRoomByCode(rawCode: String, displayName: String): Result<PvpMatchRoom> {
        val code = rawCode.trim().uppercase(Locale.US)
        val path = "pvp_rooms"
        return try {
            val uid = requireUserId()
            require(code.length in 4..12) { "Enter a valid room code (e.g. MZ-4821)." }
            val querySnap = db.collection("pvp_rooms")
                .whereEqualTo("status", "WAITING")
                .whereEqualTo("roomCode", code)
                .limit(1)
                .get()
                .await()
            val room = querySnap.documents.firstOrNull()?.toObject(
                PvpMatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("No open room found for code $code."))

            if (room.hostUid == uid) {
                return Result.failure(IllegalStateException("You are already hosting room $code."))
            }
            joinPvpRoom(room.id, displayName).getOrThrow()
            Result.success(
                room.copy(
                    guestUid = uid,
                    guestName = sanitizeName(displayName),
                    status = "ACTIVE"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun updatePvpTelemetry(
        roomId: String,
        asHost: Boolean,
        x: Int,
        y: Int,
        score: Int,
        beacons: Int
    ): Result<Unit> {
        val path = "pvp_rooms/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID cannot be blank" }
            val safeX = x.coerceIn(0, 30).toLong()
            val safeY = y.coerceIn(0, 30).toLong()
            val safeScore = score.coerceIn(0, 999_999).toLong()
            val safeBeacons = beacons.coerceIn(0, 10).toLong()
            val updateMap = if (asHost) {
                mapOf<String, Any>(
                    "hostX" to safeX,
                    "hostY" to safeY,
                    "hostScore" to safeScore,
                    "hostBeacons" to safeBeacons,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf<String, Any>(
                    "guestX" to safeX,
                    "guestY" to safeY,
                    "guestScore" to safeScore,
                    "guestBeacons" to safeBeacons,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            db.collection("pvp_rooms").document(roomId).update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun finishPvpMatch(
        roomId: String,
        asHost: Boolean,
        x: Int,
        y: Int,
        score: Int,
        beacons: Int
    ): Result<Unit> {
        val path = "pvp_rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID cannot be blank" }
            val safeX = x.coerceIn(0, 30).toLong()
            val safeY = y.coerceIn(0, 30).toLong()
            val safeScore = score.coerceIn(0, 999_999).toLong()
            val safeBeacons = beacons.coerceIn(0, 10).toLong()
            val updateMap = if (asHost) {
                mapOf<String, Any>(
                    "hostX" to safeX,
                    "hostY" to safeY,
                    "hostScore" to safeScore,
                    "hostBeacons" to safeBeacons,
                    "hostFinished" to true,
                    "status" to "FINISHED",
                    "winnerUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf<String, Any>(
                    "guestX" to safeX,
                    "guestY" to safeY,
                    "guestScore" to safeScore,
                    "guestBeacons" to safeBeacons,
                    "guestFinished" to true,
                    "status" to "FINISHED",
                    "winnerUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            val docRef = db.collection("pvp_rooms").document(roomId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists() || snap.getString("status") != "ACTIVE") {
                    return@runTransaction
                }
                tx.update(docRef, updateMap)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelWaitingPvpRoom(roomId: String): Result<Unit> {
        val path = "pvp_rooms/$roomId"
        return try {
            requireUserId()
            require(roomId.isNotBlank()) { "Room ID cannot be blank" }
            db.collection("pvp_rooms").document(roomId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. SHAREABLE COMMUNITY MAZE SEEDS (/community_seeds/{seedId})
    // =========================================================================

    fun observeCommunitySeeds(): Flow<List<CommunityMazeSeed>> = flow {
        val uid = requireUserId()
        val path = "community_seeds"
        emitAll(
            db.collection("community_seeds")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityMazeSeed::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CommunityMazeSeed> { it.upvotes }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun publishCommunitySeed(
        authorName: String,
        title: String,
        seed: String,
        biome: Biome,
        boardSize: Int,
        difficulty: Difficulty,
        strategicMaze: Boolean,
        bestScore: Int
    ): Result<CommunityMazeSeed> {
        var path = "community_seeds"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(60)
            require(cleanTitle.length >= 3) { "Challenge title must be at least 3 characters." }
            val cleanSeed = seed.trim().take(64)
            require(cleanSeed.length >= 3) { "Seed must be at least 3 characters." }
            val safeSize = if (boardSize in listOf(15, 19, 21, 25, 29)) boardSize.toLong() else 21L
            val safeBest = bestScore.coerceIn(0, 999_999).toLong()
            val safeAuthor = sanitizeName(authorName)
            val seedId = "seed_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            path = "community_seeds/$seedId"

            val payload = mapOf<String, Any>(
                "authorUid" to uid,
                "authorName" to safeAuthor,
                "title" to cleanTitle,
                "seed" to cleanSeed,
                "biome" to biome.name,
                "boardSize" to safeSize,
                "difficulty" to difficulty.name,
                "strategicMaze" to strategicMaze,
                "visibility" to "PUBLIC",
                "bestScore" to safeBest,
                "playsCount" to 0L,
                "upvotes" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("community_seeds").document(seedId).set(payload).await()
            Result.success(
                CommunityMazeSeed(
                    id = seedId,
                    authorUid = uid,
                    authorName = safeAuthor,
                    title = cleanTitle,
                    seed = cleanSeed,
                    biome = biome.name,
                    boardSize = safeSize,
                    difficulty = difficulty.name,
                    strategicMaze = strategicMaze,
                    visibility = "PUBLIC",
                    bestScore = safeBest,
                    playsCount = 0L,
                    upvotes = 0L,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCommunitySeedPlay(
        seedId: String,
        currentBest: Long,
        newScore: Long = 0L
    ): Result<Unit> {
        val path = "community_seeds/$seedId"
        return try {
            requireUserId()
            require(seedId.isNotBlank()) { "Seed ID cannot be blank" }
            val best = max(currentBest, newScore).coerceIn(0L, 999_999L)
            db.collection("community_seeds").document(seedId).update(
                mapOf(
                    "playsCount" to FieldValue.increment(1L),
                    "bestScore" to best,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upvoteCommunitySeed(seedId: String): Result<Unit> {
        val path = "community_seeds/$seedId"
        return try {
            requireUserId()
            require(seedId.isNotBlank()) { "Seed ID cannot be blank" }
            db.collection("community_seeds").document(seedId).update(
                mapOf(
                    "upvotes" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunitySeed(seedId: String): Result<Unit> {
        val path = "community_seeds/$seedId"
        return try {
            requireUserId()
            require(seedId.isNotBlank()) { "Seed ID cannot be blank" }
            db.collection("community_seeds").document(seedId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. DAILY RANKED GHOST LEADERBOARD (/daily_ranked/{dateKey}/entries/{uid})
    // =========================================================================

    fun observeDailyRankedGhosts(dateKey: String): Flow<List<DailyRankedGhostEntry>> = flow {
        val uid = requireUserId()
        val safeDate = dateKey.trim().ifBlank { "2026-11-24" }
        val path = "daily_ranked/$safeDate/entries"
        emitAll(
            db.collection("daily_ranked").document(safeDate).collection("entries")
                .whereGreaterThan("score", 0L)
                .limit(25)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyRankedGhostEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<DailyRankedGhostEntry> { it.score }
                            .thenBy { it.clearTimeMs }
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitDailyRankedGhost(
        dateKey: String,
        displayName: String,
        score: Int,
        clearTimeMs: Long,
        sha256Fingerprint: String,
        compressedInputs: String
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Sign in with Google to post Daily Ranked Ghost."))
        }
        val safeDate = dateKey.trim().ifBlank { "2026-11-24" }
        val path = "daily_ranked/$safeDate/entries/$uid"
        return try {
            val safeScore = score.coerceIn(1, 2_500_000).toLong()
            val safeClearMs = clearTimeMs.coerceAtLeast(8_000L)
            val safeInputs = compressedInputs.take(32_768)
            val docRef = db.collection("daily_ranked").document(safeDate).collection("entries").document(uid)
            val posted = db.runTransaction { tx ->
                val existingSnap = tx.get(docRef)
                val existingScore = existingSnap.getLong("score") ?: 0L
                if (existingSnap.exists() && safeScore <= existingScore) {
                    return@runTransaction false
                }
                val payload = mapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to sanitizeName(displayName),
                    "score" to safeScore,
                    "clearTimeMs" to safeClearMs,
                    "sha256Fingerprint" to sha256Fingerprint.lowercase(Locale.US),
                    "compressedInputs" to safeInputs,
                    "submittedAt" to FieldValue.serverTimestamp()
                )
                tx.set(docRef, payload)
                true
            }.await()
            Result.success(posted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. ABYSSAL DEPTH ENDLESS LEADERBOARD (/abyssal_leaderboard/{uid})
    // =========================================================================

    fun observeAbyssalLeaderboard(): Flow<List<AbyssalLeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "abyssal_leaderboard"
        emitAll(
            db.collection("abyssal_leaderboard")
                .whereGreaterThanOrEqualTo("highestFloor", 1L)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AbyssalLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<AbyssalLeaderboardEntry> { it.highestFloor }
                            .thenByDescending { it.totalScore }
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitAbyssalLeaderboard(
        displayName: String,
        highestFloor: Int,
        totalScore: Int,
        runRootSeed: Long,
        runFingerprint: String
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Sign in with Google to post Abyssal Depth rank."))
        }
        val path = "abyssal_leaderboard/$uid"
        return try {
            val safeFloor = highestFloor.coerceIn(1, 999).toLong()
            val safeScore = totalScore.coerceIn(0, 99_999_999).toLong()
            val docRef = db.collection("abyssal_leaderboard").document(uid)
            val posted = db.runTransaction { tx ->
                val existingSnap = tx.get(docRef)
                val prevFloor = existingSnap.getLong("highestFloor") ?: 0L
                val prevScore = existingSnap.getLong("totalScore") ?: 0L
                if (existingSnap.exists() && (safeFloor < prevFloor || (safeFloor == prevFloor && safeScore < prevScore))) {
                    return@runTransaction false
                }
                val payload = mapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to sanitizeName(displayName),
                    "highestFloor" to safeFloor,
                    "totalScore" to safeScore,
                    "runRootSeed" to runRootSeed,
                    "runFingerprint" to runFingerprint.lowercase(Locale.US),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(docRef, payload)
                true
            }.await()
            Result.success(posted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. LIVE-OPS DYNAMIC BALANCE CONFIG (/live_config/balance)
    // =========================================================================

    fun observeLiveBalanceConfig(): Flow<LiveBalanceConfig> = flow {
        requireUserId()
        emitAll(
            db.collection("live_config").document("balance")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(LiveBalanceConfigDto::class.java)?.toConfig() ?: LiveBalanceConfig()
                    } else {
                        LiveBalanceConfig()
                    }
                }
                .catch { error ->
                    val fsError = unwrapFirestoreCause(error)
                    if (fsError == null && error is CancellationException) throw error
                    // Gracefully fall back to built-in balance defaults if live_config/balance isn't provisioned yet
                    emit(LiveBalanceConfig())
                }
        )
    }
}
