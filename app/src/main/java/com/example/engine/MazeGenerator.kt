package com.example.engine

import kotlin.math.floor
import kotlin.math.sign

/**
 * Canonical deterministic maze generator and BFS pathfinder.
 * Supports 21x21 canonical ranked boards (exact v12 bit-for-bit parity) as well as
 * 15x15 Sprint Shaft and 25x25 Abyssal Megamine campaign boards.
 */
object MazeGenerator {

    private val CARVE_DIRS = arrayOf(
        intArrayOf(2, 0),
        intArrayOf(-2, 0),
        intArrayOf(0, 2),
        intArrayOf(0, -2)
    )

    private val CARDINAL_DIRS = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(-1, 0),
        intArrayOf(0, 1),
        intArrayOf(0, -1)
    )

    private val POWERUP_CYCLE = arrayOf(
        PowerupType.PICK,
        PowerupType.SPEED,
        PowerupType.LANTERN,
        PowerupType.SHIELD,
        PowerupType.SONAR,
        PowerupType.CLOCK
    )

    fun biomeForSeed(seed: String): Biome {
        val h = DeterministicRng.fnv1aHash(seed)
        val biomes = Biome.entries
        return biomes[(h % biomes.size.toUInt()).toInt()]
    }

    fun modifierForSeed(seed: String): Modifier {
        val h = DeterministicRng.fnv1aHash(seed)
        val mods = Modifier.entries
        return mods[(h % mods.size.toUInt()).toInt()]
    }

    fun genMaze(
        seed: String,
        boardSize: Int = MAZE_SIZE,
        randomEndpoint: Boolean = false,
        strategicMaze: Boolean = false
    ): MazeState {
        val size = if (boardSize % 2 == 1 && boardSize >= 11) boardSize else MAZE_SIZE
        var finishX = size - 2
        var finishY = size - 2
        val rng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash(seed))
        val maze = Array(size) { IntArray(size) { 1 } }

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(1 to 1)
        maze[1][1] = 0

        while (stack.isNotEmpty()) {
            val (x, y) = stack.last()
            val opts = ArrayList<IntArray>(4)
            for (d in CARVE_DIRS) {
                val dx = d[0]
                val dy = d[1]
                val nx = x + dx
                val ny = y + dy
                if (nx > 0 && ny > 0 && nx < size - 1 && ny < size - 1 && maze[ny][nx] == 1) {
                    opts.add(intArrayOf(dx, dy, nx, ny))
                }
            }
            if (opts.isEmpty()) {
                stack.removeLast()
                continue
            }
            val chosen = opts[floor(rng() * opts.size).toInt().coerceIn(0, opts.lastIndex)]
            val dx = chosen[0]
            val dy = chosen[1]
            val nx = chosen[2]
            val ny = chosen[3]
            maze[y + dy / 2][x + dx / 2] = 0
            maze[ny][nx] = 0
            stack.addLast(nx to ny)
        }

        // Strategic Maze Braiding (Expedition & Custom Boards):
        // Carve deterministic alternate corridor loops & junctions so players make genuine
        // route choices (long safe corridor vs. breaching a fortified shortcut wall).
        if (strategicMaze) {
            braidStrategicLabyrinth(seed, maze, size)
        }

        // Guarantee open start & endpoint pocket
        maze[1][1] = 0
        if (randomEndpoint) {
            val picked = pickRandomFarEndpoint(
                seed = seed,
                maze = maze,
                size = size,
                minFraction = if (strategicMaze) 0.78 else 0.62
            )
            finishX = picked.x
            finishY = picked.y
            maze[finishY][finishX] = 0
        } else {
            // Canonical Daily Ranked finish pocket (exact v12 bit-for-bit parity)
            maze[finishY][finishX] = 0
            maze[finishY][finishX - 1] = 0
            maze[finishY - 1][finishX] = 0
        }

        val checkpoints = if (strategicMaze) {
            pickStrategicCheckpoints(seed, maze, size, GridPos(finishX, finishY))
        } else {
            emptyList()
        }
        val checkpointKeys = checkpoints.map { it.key }.toSet()

        val blocks = LinkedHashMap<String, BlockState>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (maze[y][x] == 1) {
                    val r = rng()
                    val kind = when {
                        r < 0.35 -> BlockKind.DIRT
                        r < 0.65 -> BlockKind.STONE
                        r < 0.85 -> BlockKind.IRON
                        r < 0.94 -> BlockKind.CRYSTAL
                        else -> BlockKind.GAMBLE
                    }
                    blocks["$x,$y"] = BlockState(
                        kind = kind,
                        hp = kind.maxHp,
                        max = kind.maxHp
                    )
                }
            }
        }

        val innerSpan = (size - 2).toDouble()
        val gemAttempts = when {
            size < 19 -> 10
            size >= 29 -> 30
            size > 21 -> 22
            else -> 14
        }
        val gems = LinkedHashSet<String>()
        for (i in 0 until gemAttempts) {
            val x = 1 + floor(rng() * innerSpan).toInt()
            val y = 1 + floor(rng() * innerSpan).toInt()
            val key = "$x,$y"
            if (maze[y][x] == 0 && !(x == 1 && y == 1) && !(randomEndpoint && x == finishX && y == finishY) && key !in checkpointKeys) {
                gems.add(key)
            }
        }

        val powerAttempts = when {
            size < 19 -> 4
            size >= 29 -> 12
            size > 21 -> 9
            else -> 6
        }
        val powers = LinkedHashMap<String, PowerupType>()
        for (i in 0 until powerAttempts) {
            val x = 1 + floor(rng() * innerSpan).toInt()
            val y = 1 + floor(rng() * innerSpan).toInt()
            val key = "$x,$y"
            if (maze[y][x] == 0 && !(randomEndpoint && x == finishX && y == finishY) && key !in checkpointKeys) {
                powers[key] = POWERUP_CYCLE[i % POWERUP_CYCLE.size]
            }
        }

        var idx = 0
        val keysSnapshot = blocks.keys.toList()
        for (k in keysSnapshot) {
            val special = when {
                idx % 14 == 0 -> BlockSpecial.EXPLOSIVE
                idx % 17 == 0 -> BlockSpecial.ICE
                idx % 19 == 0 -> BlockSpecial.LAVA
                idx % 23 == 0 -> BlockSpecial.LOCKED
                idx % 29 == 0 -> BlockSpecial.HIDDEN
                else -> null
            }
            if (special != null) {
                blocks[k] = blocks.getValue(k).copy(special = special)
            }
            idx++
        }

        // Enrich strategic cross-corridor breach seams with rewarding TNT/Crystal/Iron shortcuts
        if (strategicMaze) {
            enrichStrategicShortcutWalls(seed, maze, size, blocks)
        }

        return MazeState(
            maze = maze.toList(),
            blocks = blocks,
            gems = gems,
            powers = powers,
            boardSize = size,
            finishX = finishX,
            finishY = finishY,
            checkpoints = checkpoints
        )
    }

    /**
     * Converts a pure tree labyrinth into a strategic braided maze by carving ~16% of
     * interior thin wall dividers between corridors (favoring dead-end openings), creating
     * tactical multi-route loops, flanking lanes, and interception intersections.
     */
    private fun braidStrategicLabyrinth(seed: String, maze: Array<IntArray>, size: Int) {
        val braidRng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|braid|$size"))
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                if (maze[y][x] != 1) continue
                val horizBridge = maze[y][x - 1] == 0 && maze[y][x + 1] == 0 && maze[y - 1][x] == 1 && maze[y + 1][x] == 1
                val vertBridge = maze[y - 1][x] == 0 && maze[y + 1][x] == 0 && maze[y][x - 1] == 1 && maze[y][x + 1] == 1
                if (!horizBridge && !vertBridge) continue

                // Boost braid chance near dead ends so corridors flow into strategic loops
                val adjOpenNeighbors = if (horizBridge) {
                    countOpenNeighbors(maze, x - 1, y, size) == 1 || countOpenNeighbors(maze, x + 1, y, size) == 1
                } else {
                    countOpenNeighbors(maze, x, y - 1, size) == 1 || countOpenNeighbors(maze, x, y + 1, size) == 1
                }
                val carveChance = if (adjOpenNeighbors) 0.30 else 0.11
                if (braidRng() < carveChance) {
                    maze[y][x] = 0
                }
            }
        }
    }

    private fun countOpenNeighbors(maze: Array<IntArray>, x: Int, y: Int, size: Int): Int {
        var open = 0
        for (d in CARDINAL_DIRS) {
            val nx = x + d[0]
            val ny = y + d[1]
            if (nx in 1 until size - 1 && ny in 1 until size - 1 && maze[ny][nx] == 0) {
                open++
            }
        }
        return open
    }

    /**
     * Reinforces select single-tile shortcut walls separating two parallel corridors
     * with strategic Iron/Crystal/Explosive breach seams, rewarding tactical mining shortcuts.
     */
    private fun enrichStrategicShortcutWalls(
        seed: String,
        maze: Array<IntArray>,
        size: Int,
        blocks: MutableMap<String, BlockState>
    ) {
        val wallRng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|shortcuts|$size"))
        for (y in 2 until size - 2) {
            for (x in 2 until size - 2) {
                if (maze[y][x] != 1) continue
                val horizCut = maze[y][x - 1] == 0 && maze[y][x + 1] == 0
                val vertCut = maze[y - 1][x] == 0 && maze[y + 1][x] == 0
                if (!horizCut && !vertCut) continue
                val key = "$x,$y"
                val cur = blocks[key] ?: continue
                if (cur.special == BlockSpecial.LOCKED || cur.special == BlockSpecial.HIDDEN) continue
                val roll = wallRng()
                if (roll < 0.08) {
                    blocks[key] = BlockState(BlockKind.STONE, BlockKind.STONE.maxHp, BlockKind.STONE.maxHp, BlockSpecial.EXPLOSIVE)
                } else if (roll < 0.16) {
                    blocks[key] = BlockState(BlockKind.CRYSTAL, BlockKind.CRYSTAL.maxHp, BlockKind.CRYSTAL.maxHp, null)
                }
            }
        }
    }

    /**
     * Selects deterministic multi-quadrant Relay Beacons via greedy farthest-point dispersion
     * across reachable open cells so players must race across the full maze structure.
     */
    private fun pickStrategicCheckpoints(
        seed: String,
        maze: Array<IntArray>,
        size: Int,
        finishPos: GridPos
    ): List<GridPos> {
        val targetCount = when {
            size <= 15 -> 2
            size <= 21 -> 3
            else -> 4
        }
        val distFromStart = computeBfsDistMap(maze, size, GridPos(START_X, START_Y))
        val candidates = ArrayList<GridPos>()
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                if (maze[y][x] == 0 && distFromStart[y][x] >= 5 && !(x == finishPos.x && y == finishPos.y)) {
                    candidates.add(GridPos(x, y))
                }
            }
        }
        if (candidates.isEmpty()) return emptyList()

        val cpRng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|beacons|$size"))
        val anchors = ArrayList<GridPos>()
        anchors.add(GridPos(START_X, START_Y))
        anchors.add(finishPos)

        val chosen = ArrayList<GridPos>(targetCount)
        repeat(targetCount) {
            var bestScore = -1.0
            var bestCandidate: GridPos? = null
            for (c in candidates) {
                if (c in chosen) continue
                // Minimum Manhattan distance to all existing anchors (start, finish, already-picked beacons)
                var minAnchorDist = Int.MAX_VALUE
                for (a in anchors) {
                    val d = kotlin.math.abs(c.x - a.x) + kotlin.math.abs(c.y - a.y)
                    if (d < minAnchorDist) minAnchorDist = d
                }
                // Small deterministic jitter keeps placements fresh across seeds while preserving spread
                val score = minAnchorDist.toDouble() + cpRng() * 1.4
                if (score > bestScore) {
                    bestScore = score
                    bestCandidate = c
                }
            }
            val picked = bestCandidate ?: return@repeat
            chosen.add(picked)
            anchors.add(picked)
        }
        return chosen
    }

    private fun computeBfsDistMap(maze: Array<IntArray>, size: Int, origin: GridPos): Array<IntArray> {
        val dist = Array(size) { IntArray(size) { -1 } }
        val queue = ArrayDeque<GridPos>()
        dist[origin.y][origin.x] = 0
        queue.addLast(origin)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val d = dist[cur.y][cur.x]
            for (dir in CARDINAL_DIRS) {
                val nx = cur.x + dir[0]
                val ny = cur.y + dir[1]
                if (nx in 1 until size - 1 && ny in 1 until size - 1 && maze[ny][nx] == 0 && dist[ny][nx] == -1) {
                    dist[ny][nx] = d + 1
                    queue.addLast(GridPos(nx, ny))
                }
            }
        }
        return dist
    }

    /**
     * Greedy nearest-neighbor remaining circuit distance from [from] through all
     * [uncapturedBeacons] and ending at [finishPos]. Used for accurate multi-leg LEAD 2.0,
     * progress bar scaling, adaptive soundtrack tension, and competitive Rival AI pacing.
     */
    fun computeCircuitDistance(
        from: GridPos,
        uncapturedBeacons: List<GridPos>,
        finishPos: GridPos
    ): Int {
        if (uncapturedBeacons.isEmpty()) {
            return kotlin.math.abs(finishPos.x - from.x) + kotlin.math.abs(finishPos.y - from.y)
        }
        val remaining = uncapturedBeacons.toMutableList()
        var cur = from
        var total = 0
        while (remaining.isNotEmpty()) {
            var bestIdx = 0
            var bestDist = Int.MAX_VALUE
            for (i in remaining.indices) {
                val b = remaining[i]
                val d = kotlin.math.abs(b.x - cur.x) + kotlin.math.abs(b.y - cur.y)
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = i
                }
            }
            total += bestDist
            cur = remaining.removeAt(bestIdx)
        }
        total += kotlin.math.abs(finishPos.x - cur.x) + kotlin.math.abs(finishPos.y - cur.y)
        return total
    }

    /**
     * Picks an unpredictable, deterministically seeded finish portal coordinate among all
     * reachable far-away open maze cells (>= [minFraction] of max BFS path distance from start),
     * making every Expedition and Quick-Play race directionally fresh and surprising.
     */
    private fun pickRandomFarEndpoint(
        seed: String,
        maze: Array<IntArray>,
        size: Int,
        minFraction: Double = 0.62
    ): GridPos {
        val dist = computeBfsDistMap(maze, size, GridPos(START_X, START_Y))
        var maxDist = 0
        val reachable = ArrayList<Pair<GridPos, Int>>()
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val d = dist[y][x]
                if (d > 0) {
                    if (d > maxDist) maxDist = d
                    reachable.add(GridPos(x, y) to d)
                }
            }
        }

        if (reachable.isEmpty()) return GridPos(size - 2, size - 2)
        val minRequiredDist = (maxDist * minFraction).toInt().coerceAtLeast(6)
        val farCandidates = reachable.filter { it.second >= minRequiredDist }.map { it.first }
        val pool = if (farCandidates.isNotEmpty()) farCandidates else reachable.map { it.first }

        val endRng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|endpoint|$size"))
        val pickIdx = floor(endRng() * pool.size).toInt().coerceIn(0, pool.lastIndex)
        return pool[pickIdx]
    }

    fun generateRuinsTraps(seed: String, maze: List<IntArray>, boardSize: Int = maze.size): Set<String> {
        val tr = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|traps"))
        val traps = LinkedHashSet<String>()
        val count = when {
            boardSize >= 29 -> 11
            boardSize > 21 -> 8
            else -> Biome.RUINS.rule.trapCount
        }
        val span = (boardSize - 4).coerceAtLeast(5).toDouble()
        for (i in 0 until count) {
            val x = 2 + floor(tr() * span).toInt()
            val y = 2 + floor(tr() * span).toInt()
            if (y in maze.indices && x in maze[y].indices && maze[y][x] == 0) {
                traps.add("$x,$y")
            }
        }
        return traps
    }

    /**
     * Generates deterministic glowing lava puddles in open Volcano corridors
     * (costs -2s timer when stepped on unless Shield Lv. 2+ or active Shield powerup).
     */
    fun generateVolcanoLavaPuddles(seed: String, maze: List<IntArray>, boardSize: Int = maze.size): Set<String> {
        val rng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|volcano_lava"))
        val puddles = LinkedHashSet<String>()
        val count = when {
            boardSize >= 29 -> 14
            boardSize > 21 -> 10
            else -> 7
        }
        val span = (boardSize - 4).coerceAtLeast(5).toDouble()
        for (i in 0 until count) {
            val x = 2 + floor(rng() * span).toInt()
            val y = 2 + floor(rng() * span).toInt()
            if (y in maze.indices && x in maze[y].indices && maze[y][x] == 0) {
                puddles.add("$x,$y")
            }
        }
        return puddles
    }

    /**
     * Generates deterministic Desert drift-sand corridor obstacles
     * (1 tap or step sweeps the sand drift clear with bonus points).
     */
    fun generateDesertSandDrifts(seed: String, maze: List<IntArray>, boardSize: Int = maze.size): Set<String> {
        val rng = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|desert_sand"))
        val drifts = LinkedHashSet<String>()
        val count = when {
            boardSize >= 29 -> 16
            boardSize > 21 -> 12
            else -> 8
        }
        val span = (boardSize - 4).coerceAtLeast(5).toDouble()
        for (i in 0 until count) {
            val x = 2 + floor(rng() * span).toInt()
            val y = 2 + floor(rng() * span).toInt()
            if (y in maze.indices && x in maze[y].indices && maze[y][x] == 0) {
                drifts.add("$x,$y")
            }
        }
        return drifts
    }

    fun bfsPath(
        blocks: Map<String, BlockState>,
        sx: Int,
        sy: Int,
        tx: Int,
        ty: Int,
        allowMine: Boolean = false,
        boardSize: Int = MAZE_SIZE
    ): List<GridPos>? {
        val queue = ArrayDeque<GridPos>()
        val prev = HashMap<String, GridPos>()
        val seen = HashSet<String>()
        val start = GridPos(sx, sy)
        queue.addLast(start)
        seen.add(start.key)

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur.x == tx && cur.y == ty) break
            for (d in CARDINAL_DIRS) {
                val nx = cur.x + d[0]
                val ny = cur.y + d[1]
                if (nx < 0 || ny < 0 || nx >= boardSize || ny >= boardSize) continue
                val key = "$nx,$ny"
                if (key in seen) continue
                if (!allowMine && blocks.containsKey(key)) continue
                seen.add(key)
                prev[key] = cur
                queue.addLast(GridPos(nx, ny))
            }
        }

        val targetKey = "$tx,$ty"
        if (targetKey !in seen) return null

        val path = ArrayList<GridPos>()
        var cur = GridPos(tx, ty)
        while (!(cur.x == sx && cur.y == sy)) {
            path.add(cur)
            cur = prev[cur.key] ?: break
        }
        path.reverse()
        return path
    }

    fun computeLegalWinningRoute(seed: String, boardSize: Int = MAZE_SIZE): List<ReplayAction> {
        val g = genMaze(seed, boardSize)
        val openPath = bfsPath(g.blocks, START_X, START_Y, g.finishX, g.finishY, allowMine = false, boardSize = g.boardSize)
        val acts = ArrayList<ReplayAction>()
        var px = START_X
        var py = START_Y
        var tick = 1

        if (openPath != null) {
            for (step in openPath) {
                val dx = (step.x - px).sign
                val dy = (step.y - py).sign
                acts.add(ReplayAction.Move(dx = dx, dy = dy, tick = tick))
                px = step.x
                py = step.y
                tick++
            }
            return acts
        }

        val mutableBlocks = g.blocks.toMutableMap()
        while (!(px == g.finishX && py == g.finishY) && acts.size < 900) {
            var dx = (g.finishX - px).sign
            var dy = 0
            if (px == g.finishX) {
                dy = (g.finishY - py).sign
                dx = 0
            }
            val nx = px + dx
            val ny = py + dy
            val key = "$nx,$ny"
            val block = mutableBlocks[key]
            if (block != null) {
                repeat(block.hp) {
                    acts.add(ReplayAction.Mine(x = nx, y = ny, tick = tick++))
                }
                mutableBlocks.remove(key)
            } else {
                acts.add(ReplayAction.Move(dx = dx, dy = dy, tick = tick++))
                px = nx
                py = ny
            }
        }
        return acts
    }
}
