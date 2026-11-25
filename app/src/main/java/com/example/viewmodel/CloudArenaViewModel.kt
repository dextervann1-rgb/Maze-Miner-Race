package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloud.AbyssalLeaderboardEntry
import com.example.cloud.CloudProfileSave
import com.example.cloud.CloudRepository
import com.example.cloud.CloudUiState
import com.example.cloud.CommunityMazeSeed
import com.example.cloud.DailyRankedGhostEntry
import com.example.cloud.PvpMatchRoom
import com.example.cloud.unwrapFirestoreCause
import com.example.data.CampaignProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.engine.Biome
import com.example.engine.Difficulty
import com.example.engine.LiveBalanceConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Auth-scoped ViewModel managing Path 9.8+ Cloud Services:
 * - Cross-Device Cloud Profile Saves (/users/{uid})
 * - Live PvP Cloud Matchmaking & Real-Time Telemetry (/pvp_rooms/{roomId})
 * - Shareable Community Maze Seeds (/community_seeds/{seedId})
 *
 * Instantiated exclusively when signed in via viewModel(key = currentUserId, factory = ...).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloudArenaViewModel(
    private val repository: CloudRepository,
    val currentUserId: String,
    val displayName: String
) : ViewModel() {

    val cloudProfileState: StateFlow<CloudUiState<CloudProfileSave?>> =
        repository.observeCloudProfile(currentUserId)
            .map<CloudProfileSave?, CloudUiState<CloudProfileSave?>> { CloudUiState.Success(it) }
            .catch { error ->
                val fsError = unwrapFirestoreCause(error)
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing cloud profile", root)
                emit(CloudUiState.Error(root.message ?: "Failed to load cloud profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val openPvpRoomsState: StateFlow<CloudUiState<List<PvpMatchRoom>>> =
        repository.observeOpenPvpRooms()
            .map<List<PvpMatchRoom>, CloudUiState<List<PvpMatchRoom>>> { CloudUiState.Success(it) }
            .catch { error ->
                val fsError = unwrapFirestoreCause(error)
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing open PvP rooms", root)
                emit(CloudUiState.Error(root.message ?: "Failed to load PvP lobby"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val communitySeedsState: StateFlow<CloudUiState<List<CommunityMazeSeed>>> =
        repository.observeCommunitySeeds()
            .map<List<CommunityMazeSeed>, CloudUiState<List<CommunityMazeSeed>>> { CloudUiState.Success(it) }
            .catch { error ->
                val fsError = unwrapFirestoreCause(error)
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing community seeds", root)
                emit(CloudUiState.Error(root.message ?: "Failed to load community seeds"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val _activePvpRoomId = MutableStateFlow<String?>(null)
    val activePvpRoomId: StateFlow<String?> = _activePvpRoomId.asStateFlow()

    val activePvpRoom: StateFlow<PvpMatchRoom?> =
        _activePvpRoomId
            .flatMapLatest { roomId ->
                if (roomId.isNullOrBlank()) {
                    flowOf(null)
                } else {
                    repository.observePvpRoom(roomId)
                        .catch { error ->
                            val fsError = unwrapFirestoreCause(error)
                            if (fsError == null && error is CancellationException) throw error
                            Log.w(TAG, "Error observing active PvP room $roomId", fsError ?: error)
                            emit(null)
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    private val _dailyGhostDateKey = MutableStateFlow(todayUtcSeed())

    val dailyGhostsState: StateFlow<CloudUiState<List<DailyRankedGhostEntry>>> =
        _dailyGhostDateKey
            .flatMapLatest { dateKey ->
                repository.observeDailyRankedGhosts(dateKey)
                    .map<List<DailyRankedGhostEntry>, CloudUiState<List<DailyRankedGhostEntry>>> { CloudUiState.Success(it) }
                    .catch { error ->
                        val fsError = unwrapFirestoreCause(error)
                        if (fsError == null && error is CancellationException) throw error
                        val root = fsError ?: error
                        Log.w(TAG, "Error observing daily ranked ghosts", root)
                        emit(CloudUiState.Error(root.message ?: "Failed to load daily ghost racers"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val abyssalLeaderboardState: StateFlow<CloudUiState<List<AbyssalLeaderboardEntry>>> =
        repository.observeAbyssalLeaderboard()
            .map<List<AbyssalLeaderboardEntry>, CloudUiState<List<AbyssalLeaderboardEntry>>> { CloudUiState.Success(it) }
            .catch { error ->
                val fsError = unwrapFirestoreCause(error)
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing Abyssal leaderboard", root)
                emit(CloudUiState.Error(root.message ?: "Failed to load Abyssal Depth leaderboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val liveBalanceConfig: StateFlow<LiveBalanceConfig> =
        repository.observeLiveBalanceConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = LiveBalanceConfig()
            )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var lastTelemetryPushMs: Long = 0L

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun syncCloudProfile(
        localProfile: PlayerProfileEntity,
        localCampaign: List<CampaignProgressEntity>,
        pvpWinDelta: Int = 0,
        pvpMatchDelta: Int = 0,
        silent: Boolean = false,
        onMerged: (CloudProfileSave) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncAndMergeCloudProfile(
                localProfile = localProfile,
                localCampaign = localCampaign,
                displayName = displayName,
                pvpWinDelta = pvpWinDelta,
                pvpMatchDelta = pvpMatchDelta
            )
            _isSyncing.value = false
            res.onSuccess { merged ->
                onMerged(merged)
                if (!silent) {
                    _statusMessage.value = "☁️ Cloud Save synced (${merged.gems} 💎 • ${merged.totalWins} Wins)"
                }
            }.onFailure { err ->
                if (!silent) {
                    _statusMessage.value = err.localizedMessage ?: "Cloud sync failed"
                }
            }
        }
    }

    fun hostPvpRoom(
        seed: String,
        biome: Biome,
        boardSize: Int,
        difficulty: Difficulty
    ) {
        viewModelScope.launch {
            val res = repository.createPvpRoom(
                displayName = displayName,
                seed = seed,
                biome = biome,
                boardSize = boardSize,
                difficulty = difficulty
            )
            res.onSuccess { room ->
                _activePvpRoomId.value = room.id
                _statusMessage.value = "📡 PvP Room ${room.roomCode} opened! Share code or wait for a challenger."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not create PvP room"
            }
        }
    }

    fun joinPvpRoom(room: PvpMatchRoom) {
        if (room.hostUid == currentUserId) {
            _activePvpRoomId.value = room.id
            return
        }
        viewModelScope.launch {
            val res = repository.joinPvpRoom(room.id, displayName)
            res.onSuccess {
                _activePvpRoomId.value = room.id
                _statusMessage.value = "⚡ Joined ${room.hostName}'s room (${room.roomCode})!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to join PvP room"
            }
        }
    }

    fun joinPvpRoomByCode(code: String) {
        viewModelScope.launch {
            val res = repository.joinPvpRoomByCode(code, displayName)
            res.onSuccess { room ->
                _activePvpRoomId.value = room.id
                _statusMessage.value = "⚡ Joined room ${room.roomCode} vs ${room.hostName}!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Room code not found"
            }
        }
    }

    fun cancelActiveWaitingRoom() {
        val roomId = _activePvpRoomId.value ?: return
        viewModelScope.launch {
            repository.cancelWaitingPvpRoom(roomId)
            _activePvpRoomId.value = null
            _statusMessage.value = "PvP room closed."
        }
    }

    fun leaveActivePvpRoom() {
        _activePvpRoomId.value = null
    }

    fun pushLivePvpTelemetry(
        roomId: String,
        asHost: Boolean,
        x: Int,
        y: Int,
        score: Int,
        beacons: Int
    ) {
        val now = System.currentTimeMillis()
        if (now - lastTelemetryPushMs < 320L) return
        lastTelemetryPushMs = now
        viewModelScope.launch {
            repository.updatePvpTelemetry(
                roomId = roomId,
                asHost = asHost,
                x = x,
                y = y,
                score = score,
                beacons = beacons
            )
        }
    }

    fun finishLivePvpMatch(
        roomId: String,
        asHost: Boolean,
        x: Int,
        y: Int,
        score: Int,
        beacons: Int
    ) {
        viewModelScope.launch {
            repository.finishPvpMatch(
                roomId = roomId,
                asHost = asHost,
                x = x,
                y = y,
                score = score,
                beacons = beacons
            )
        }
    }

    fun publishCommunitySeed(
        title: String,
        seed: String,
        biome: Biome,
        boardSize: Int,
        difficulty: Difficulty,
        strategicMaze: Boolean,
        bestScore: Int,
        onPublished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.publishCommunitySeed(
                authorName = displayName,
                title = title,
                seed = seed,
                biome = biome,
                boardSize = boardSize,
                difficulty = difficulty,
                strategicMaze = strategicMaze,
                bestScore = bestScore
            )
            res.onSuccess { published ->
                _statusMessage.value = "🌍 Published '${published.title}' to Community Seeds!"
                onPublished()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not publish seed"
            }
        }
    }

    fun onPlayCommunitySeed(seed: CommunityMazeSeed) {
        viewModelScope.launch {
            repository.recordCommunitySeedPlay(
                seedId = seed.id,
                currentBest = seed.bestScore
            )
        }
    }

    fun upvoteCommunitySeed(seed: CommunityMazeSeed) {
        viewModelScope.launch {
            val res = repository.upvoteCommunitySeed(seed.id)
            res.onSuccess {
                _statusMessage.value = "👍 Upvoted '${seed.title}'"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not upvote"
            }
        }
    }

    fun deleteCommunitySeed(seed: CommunityMazeSeed) {
        viewModelScope.launch {
            val res = repository.deleteCommunitySeed(seed.id)
            res.onSuccess {
                _statusMessage.value = "Removed '${seed.title}'"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not delete seed"
            }
        }
    }

    fun submitDailyRankedGhost(
        dateKey: String,
        score: Int,
        clearTimeMs: Long,
        sha256Fingerprint: String,
        compressedInputs: String
    ) {
        _dailyGhostDateKey.value = dateKey
        viewModelScope.launch {
            val res = repository.submitDailyRankedGhost(
                dateKey = dateKey,
                displayName = displayName,
                score = score,
                clearTimeMs = clearTimeMs,
                sha256Fingerprint = sha256Fingerprint,
                compressedInputs = compressedInputs
            )
            res.onSuccess { posted ->
                if (posted) {
                    _statusMessage.value = "👻 New Daily Ranked PB Ghost uploaded ($score pts)!"
                }
            }
        }
    }

    fun submitAbyssalRun(
        highestFloor: Int,
        totalScore: Int,
        runRootSeed: Long,
        runFingerprint: String
    ) {
        viewModelScope.launch {
            val res = repository.submitAbyssalLeaderboard(
                displayName = displayName,
                highestFloor = highestFloor,
                totalScore = totalScore,
                runRootSeed = runRootSeed,
                runFingerprint = runFingerprint
            )
            res.onSuccess { posted ->
                if (posted) {
                    _statusMessage.value = "🕳️ Abyssal Depth Floor $highestFloor posted to Cloud Leaderboard!"
                }
            }
        }
    }

    private companion object {
        const val TAG = "CloudArenaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
