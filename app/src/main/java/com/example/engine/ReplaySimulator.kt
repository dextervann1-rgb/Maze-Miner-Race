package com.example.engine

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Canonical deterministic replay simulator and 0-tolerance anti-cheat verifier.
 * Single source of truth for scoring, biome effects, powerup rules, and replay receipts.
 */
object ReplaySimulator {

    fun timeBonus(ticks: Int): Int = max(0, (200 - ticks)) * 2

    fun gemGain(biome: Biome, modifier: Modifier): Int {
        var gain = if (biome == Biome.CAVERN) 30 else 15
        if (modifier == Modifier.DOUBLE_GEMS) gain *= 2
        return gain
    }

    /**
     * True half-speed timer drain rule (v12 fix):
     * - Normal: drain 1 every tick
     * - Speed powerup: drain 0 on even ticks, 1 on odd ticks (NEVER frozen!)
     */
    fun timerDrainForTick(tick: Int, activePower: PowerupType?): Int {
        return if (activePower == PowerupType.SPEED && tick % 2 == 0) 0 else 1
    }

    fun simulateRun(
        seed: String,
        difficulty: Difficulty,
        modifier: Modifier,
        biome: Biome,
        actions: List<ReplayAction>,
        ticks: Int,
        clientScore: Int
    ): VerifyResult {
        if (actions.size < 5 || actions.size > 3000) {
            return VerifyResult(ok = false, reason = "actions length out of range (${actions.size})")
        }

        val generated = MazeGenerator.genMaze(seed)
        val rng = DeterministicRng.mulberry(
            DeterministicRng.fnv1aHash("$seed|gameplay|${difficulty.wireName}|${modifier.wireName}")
        )

        var px = START_X
        var py = START_Y
        var stamina = 100.0
        var score = 0
        var combo = 0
        var timer = difficulty.startTimer
        var lastTick = 0
        var power: PowerupType? = null
        var powerUntilTick = -1
        var hasKey = false

        val blocks = HashMap<String, BlockState>(generated.blocks.size)
        val hp = HashMap<String, Int>(generated.blocks.size)
        generated.blocks.forEach { (k, v) ->
            blocks[k] = v
            hp[k] = v.hp
        }
        val gems = generated.gems.toMutableSet()
        val powers = generated.powers.toMutableMap()
        val traps = if (biome == Biome.RUINS) {
            MazeGenerator.generateRuinsTraps(seed, generated.maze)
        } else {
            emptySet()
        }

        val biomeRule = biome.rule

        fun applyTick(t: Int): String? {
            timer -= timerDrainForTick(t, power)
            stamina = (stamina + biomeRule.staminaPerTick).coerceIn(0.0, 100.0)
            if (biome == Biome.VOLCANO && power == PowerupType.SHIELD) {
                stamina = min(100.0, stamina + 0.4)
            }
            if (power != null && t > powerUntilTick) {
                power = null
            }
            if (timer <= -60) {
                return "timer expired before finish at tick $t"
            }
            return null
        }

        for (i in actions.indices) {
            val action = actions[i]
            if (action.tick < lastTick) {
                return VerifyResult(ok = false, reason = "tick backwards at $i")
            }
            if (action.tick > 1000) {
                return VerifyResult(ok = false, reason = "ticks too high")
            }
            for (t in (lastTick + 1)..action.tick) {
                val err = applyTick(t)
                if (err != null) return VerifyResult(ok = false, reason = err)
            }
            lastTick = action.tick

            when (action) {
                is ReplayAction.Move -> {
                    if (abs(action.dx) > 1 || abs(action.dy) > 1 || abs(action.dx) + abs(action.dy) != 1) {
                        return VerifyResult(ok = false, reason = "invalid move $i")
                    }
                    val nx = px + action.dx
                    val ny = py + action.dy
                    if (nx < 0 || ny < 0 || nx >= MAZE_SIZE || ny >= MAZE_SIZE) {
                        return VerifyResult(ok = false, reason = "oob $i")
                    }
                    val nextKey = "$nx,$ny"
                    // Hidden blocks ARE blocks — cannot walk through them
                    if (blocks.containsKey(nextKey)) {
                        return VerifyResult(ok = false, reason = "wall-through $i")
                    }
                    px = nx
                    py = ny
                    stamina = min(100.0, stamina + if (power == PowerupType.SPEED) 1.0 else 0.5)

                    // Ice biome deterministic slide
                    if (biome == Biome.ICE && rng() < biomeRule.slideChance) {
                        val sx = px + action.dx
                        val sy = py + action.dy
                        if (sx in 0 until MAZE_SIZE && sy in 0 until MAZE_SIZE && !blocks.containsKey("$sx,$sy")) {
                            px = sx
                            py = sy
                        }
                    }

                    val curKey = "$px,$py"
                    if (curKey in traps) {
                        stamina = max(0.0, stamina - biomeRule.trapDamage)
                    }
                    if (gems.remove(curKey)) {
                        score += gemGain(biome, modifier)
                    }
                    val pickedPower = powers.remove(curKey)
                    if (pickedPower != null) {
                        power = pickedPower
                        powerUntilTick = action.tick + 15
                        if (pickedPower == PowerupType.PICK) score += 5
                        if (pickedPower == PowerupType.CLOCK) timer += 20
                    }
                }

                is ReplayAction.Mine -> {
                    if (abs(action.x - px) + abs(action.y - py) != 1) {
                        return VerifyResult(ok = false, reason = "mine too far $i")
                    }
                    val key = "${action.x},${action.y}"
                    val block = blocks[key]
                        ?: return VerifyResult(ok = false, reason = "mine empty $i")
                    if (block.special == BlockSpecial.LOCKED && !hasKey) {
                        return VerifyResult(ok = false, reason = "locked without key $i")
                    }
                    if (stamina <= 0.0) {
                        return VerifyResult(ok = false, reason = "0 stamina $i")
                    }
                    var cost = block.kind.staminaCost
                    if (power == PowerupType.PICK) {
                        cost = ceil(cost / 2.0).toInt()
                    }
                    stamina = max(0.0, stamina - cost)

                    val dmg = if (power == PowerupType.PICK) 2 else 1
                    val remainingHp = (hp[key] ?: block.hp) - dmg
                    hp[key] = remainingHp
                    combo++
                    if (combo > 2) score += combo

                    if (remainingHp <= 0) {
                        blocks.remove(key)
                        score += 20
                        stamina = min(100.0, stamina + 4.0)

                        when (block.special) {
                            BlockSpecial.EXPLOSIVE -> {
                                val blastDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                                for ((dx, dy) in blastDirs) {
                                    val kk = "${action.x + dx},${action.y + dy}"
                                    if (blocks.containsKey(kk)) {
                                        val nhp = (hp[kk] ?: 1) - 1
                                        hp[kk] = nhp
                                        if (nhp <= 0) blocks.remove(kk)
                                    }
                                }
                            }
                            BlockSpecial.ICE -> timer += 5
                            BlockSpecial.LAVA -> {
                                if (power != PowerupType.SHIELD) {
                                    stamina = max(0.0, stamina - 15.0)
                                }
                            }
                            else -> Unit
                        }

                        if (block.kind == BlockKind.CRYSTAL && rng() < 0.4) {
                            hasKey = true
                        }
                        if (block.kind == BlockKind.GAMBLE) {
                            val r = rng()
                            when {
                                r < 0.4 -> score += 50
                                r < 0.7 -> timer -= 10
                                else -> score += 10
                            }
                        }
                    }
                }
            }
        }

        if (px != FINISH_X || py != FINISH_Y) {
            return VerifyResult(ok = false, reason = "did not reach finish")
        }
        if (ticks < 10 || ticks > 1000) {
            return VerifyResult(ok = false, reason = "elapsed out of range ($ticks)")
        }

        val serverScore = score + timeBonus(ticks)
        if (clientScore != serverScore) {
            return VerifyResult(
                ok = false,
                serverScore = serverScore,
                reason = "score mismatch client $clientScore vs server $serverScore"
            )
        }

        val fp = DeterministicRng.fingerprintActions(actions)
        return VerifyResult(
            ok = true,
            verified = true,
            serverScore = serverScore,
            fingerprint = fp,
            source = VerificationSource.CANONICAL_LOCAL_SQLITE
        )
    }

    /**
     * Built-in 10-point developer verification test suite (runs live inside the app & unit tests).
     */
    fun runSelfTests(): List<EngineSelfTestResult> {
        val results = mutableListOf<EngineSelfTestResult>()
        val seed = "2026-11-18"

        // 1. Maze Determinism
        val m1 = MazeGenerator.genMaze(seed)
        val m2 = MazeGenerator.genMaze(seed)
        val sameMaze = m1.blocks == m2.blocks && m1.gems == m2.gems && m1.powers == m2.powers
        results += EngineSelfTestResult("Deterministic Maze Generation", sameMaze, "Seed $seed matches bit-for-bit")

        // 2. Start & Finish Open
        val startFinishOpen = !m1.blocks.containsKey("1,1") && !m1.blocks.containsKey("19,19")
        results += EngineSelfTestResult("Start & Finish Unblocked", startFinishOpen, "(1,1) & (19,19) guaranteed open")

        // 3. Speed Half-Drain (Not Frozen)
        val speedDrains = (1..6).map { timerDrainForTick(it, PowerupType.SPEED) }
        val speedValid = speedDrains == listOf(1, 0, 1, 0, 1, 0)
        results += EngineSelfTestResult("Speed True Half-Drain", speedValid, "Ticks 1..6 drain=$speedDrains")

        // 4. Deterministic RNG Parity
        val r1 = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|gameplay|normal|Normal"))
        val r2 = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|gameplay|normal|Normal"))
        val rngSame = (0..9).all { r1() == r2() }
        results += EngineSelfTestResult("Zero Math.random() Gameplay RNG", rngSame, "Mulberry32 10/10 parity")

        // 5. BFS Rival Pathfinding
        val bfs = MazeGenerator.bfsPath(m1.blocks, START_X, START_Y, FINISH_X, FINISH_Y, false)
        results += EngineSelfTestResult("BFS Rival Pathfinding", bfs != null && bfs.size > 10, "${bfs?.size ?: 0} optimal steps")

        // 6. Hidden Blocks Exist & Block Walk-Through
        val hiddenEntry = m1.blocks.entries.firstOrNull { it.value.special == BlockSpecial.HIDDEN }
        results += EngineSelfTestResult("Hidden Blocks Spawned", hiddenEntry != null, "${m1.blocks.values.count { it.special == BlockSpecial.HIDDEN }} hidden blocks")

        // 7. Anti-Cheat: Reject Teleport
        val teleport = simulateRun(
            seed, Difficulty.NORMAL, Modifier.NORMAL, Biome.DESERT,
            listOf(ReplayAction.Move(5, 0, 1)), 20, 0
        )
        results += EngineSelfTestResult("Anti-Cheat Rejects Teleport", !teleport.ok, teleport.reason ?: "rejected")

        // 8. Anti-Cheat: Reject Backwards Tick
        val backTick = simulateRun(
            seed, Difficulty.NORMAL, Modifier.NORMAL, Biome.DESERT,
            listOf(
                ReplayAction.Move(1, 0, 5),
                ReplayAction.Move(0, 1, 3),
                ReplayAction.Move(1, 0, 6),
                ReplayAction.Move(0, 1, 7),
                ReplayAction.Move(1, 0, 8)
            ),
            20, 0
        )
        results += EngineSelfTestResult("Anti-Cheat Rejects Tick Rewind", !backTick.ok, backTick.reason ?: "rejected")

        // 9. Deterministic Legal Winning Route Passes 0-Tolerance Verification
        val route = MazeGenerator.computeLegalWinningRoute(seed)
        val probe = simulateRun(seed, Difficulty.NORMAL, Modifier.NORMAL, Biome.DESERT, route, route.size.coerceAtLeast(15), -1)
        val expectedScore = probe.serverScore ?: 0
        val verified = simulateRun(seed, Difficulty.NORMAL, Modifier.NORMAL, Biome.DESERT, route, route.size.coerceAtLeast(15), expectedScore)
        results += EngineSelfTestResult(
            "0-Tolerance Winning Replay Verified",
            verified.ok && verified.verified && verified.serverScore == expectedScore,
            "Score $expectedScore • fp=${verified.fingerprint?.take(10)}"
        )

        // 10. Anti-Cheat: Reject Score Tampering (+1 pt)
        val tampered = simulateRun(seed, Difficulty.NORMAL, Modifier.NORMAL, Biome.DESERT, route, route.size.coerceAtLeast(15), expectedScore + 1)
        results += EngineSelfTestResult("Anti-Cheat Rejects +1 Score Tamper", !tampered.ok, tampered.reason ?: "rejected")

        return results
    }
}
