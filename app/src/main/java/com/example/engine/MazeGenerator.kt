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

    fun genMaze(seed: String, boardSize: Int = MAZE_SIZE): MazeState {
        val size = if (boardSize % 2 == 1 && boardSize >= 11) boardSize else MAZE_SIZE
        val finishX = size - 2
        val finishY = size - 2
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

        // Guarantee open start & finish pocket
        maze[1][1] = 0
        maze[finishY][finishX] = 0
        maze[finishY][finishX - 1] = 0
        maze[finishY - 1][finishX] = 0

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
            size > 21 -> 22
            else -> 14
        }
        val gems = LinkedHashSet<String>()
        for (i in 0 until gemAttempts) {
            val x = 1 + floor(rng() * innerSpan).toInt()
            val y = 1 + floor(rng() * innerSpan).toInt()
            if (maze[y][x] == 0 && !(x == 1 && y == 1)) {
                gems.add("$x,$y")
            }
        }

        val powerAttempts = when {
            size < 19 -> 4
            size > 21 -> 9
            else -> 6
        }
        val powers = LinkedHashMap<String, PowerupType>()
        for (i in 0 until powerAttempts) {
            val x = 1 + floor(rng() * innerSpan).toInt()
            val y = 1 + floor(rng() * innerSpan).toInt()
            if (maze[y][x] == 0) {
                powers["$x,$y"] = POWERUP_CYCLE[i % POWERUP_CYCLE.size]
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

        return MazeState(
            maze = maze.toList(),
            blocks = blocks,
            gems = gems,
            powers = powers,
            boardSize = size,
            finishX = finishX,
            finishY = finishY
        )
    }

    fun generateRuinsTraps(seed: String, maze: List<IntArray>, boardSize: Int = maze.size): Set<String> {
        val tr = DeterministicRng.mulberry(DeterministicRng.fnv1aHash("$seed|traps"))
        val traps = LinkedHashSet<String>()
        val count = if (boardSize > 21) 8 else Biome.RUINS.rule.trapCount
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
