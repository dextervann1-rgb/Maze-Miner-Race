package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.Biome
import com.example.engine.BlockKind
import com.example.engine.BlockSpecial
import com.example.engine.BlockState
import com.example.engine.GauntletStep
import com.example.engine.GhostFrame
import com.example.engine.GhostReplayCodec
import com.example.engine.PowerupType
import com.example.engine.TacticalJuiceController
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.EmeraldGem
import com.example.ui.theme.ForgedGold
import com.example.ui.theme.MysticAmethyst
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RivalCrimson
import com.example.ui.theme.TileCrystal
import com.example.ui.theme.TileCrystalLight
import com.example.ui.theme.TileDirt
import com.example.ui.theme.TileDirtLight
import com.example.ui.theme.TileExplosive
import com.example.ui.theme.TileFinish
import com.example.ui.theme.TileGamble
import com.example.ui.theme.TileGambleLight
import com.example.ui.theme.TileGhost
import com.example.ui.theme.TileIce
import com.example.ui.theme.TileIron
import com.example.ui.theme.TileIronLight
import com.example.ui.theme.TileLava
import com.example.ui.theme.TileLocked
import com.example.ui.theme.TileStone
import com.example.ui.theme.TileStoneLight
import com.example.ui.theme.VolcanicOrange
import com.example.viewmodel.LiveRaceState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-bleed high-craft 2.5D immersive Canvas game engine surface:
 * - Enlarged 2.5D extruded blocks (default 7x7 viewport, macro 5x5 zoom) with rich procedural material textures
 * - Multi-stage fracture cracks, glowing crystal spires, forged iron rivets, ashlar masonry, gilded gamble filigree
 * - Custom vector overlays for Explosive TNT + fuse spark, Ice snowflakes, pulsing Lava magma veins, and Locked padlocks
 * - Directional headlamp light cone, torch flicker, and continuous drifting ambient biome particles
 * - Material-colored rock shatter debris, explosive shockwave rings, and pickaxe strike flash arc
 * - Tactical Mini-Map Radar, Directional Finish Compass, Rival Cyber-Driller AI, and BFS Pathfinder shimmer
 */
@Composable
fun MazeCanvas(
    race: LiveRaceState,
    founderAura: Boolean = false,
    onCellTapped: (Int, Int) -> Unit,
    onSwipeDirection: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val camX by animateFloatAsState(
        targetValue = race.playerPos.x.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cam_x"
    )
    val camY by animateFloatAsState(
        targetValue = race.playerPos.y.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cam_y"
    )
    // Smooth spring-interpolated Rival Cyber-Driller motion (no jarring tile snaps)
    val rivalAnimX by animateFloatAsState(
        targetValue = race.rivalPos.x.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rival_x"
    )
    val rivalAnimY by animateFloatAsState(
        targetValue = race.rivalPos.y.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rival_y"
    )

    // Tactical Impact Juice: Hitstop (<=45ms) + Dynamic Camera Zoom Impulse (1.03..1.08x blast / 0.92x Surge)
    val juice = remember { TacticalJuiceController() }
    var isHitstopFrozen by remember { mutableStateOf(false) }
    var frozenRivalX by remember { mutableFloatStateOf(race.rivalPos.x.toFloat()) }
    var frozenRivalY by remember { mutableFloatStateOf(race.rivalPos.y.toFloat()) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val deltaMs = ((nowNanos - lastNanos) / 1_000_000f).coerceIn(0f, 50f)
                    isHitstopFrozen = juice.update(deltaMs)
                }
                lastNanos = nowNanos
            }
        }
    }

    val gauntletFocusZoom by animateFloatAsState(
        targetValue = if (race.isGauntlet) race.gauntletStep.cameraFocusZoom else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "gauntlet_zoom"
    )

    // Tactile mining debris, pickaxe swing & explosion shockwave pulse
    val hitAnim = remember { Animatable(0f) }
    LaunchedEffect(race.hitPulseTick) {
        if (race.hitPulseTick > 0L) {
            if (race.lastHitExploded) {
                frozenRivalX = rivalAnimX
                frozenRivalY = rivalAnimY
                juice.triggerChainDetonation(race.lastChainDepth.coerceAtLeast(1))
            }
            hitAnim.snapTo(1f)
            hitAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(if (race.lastHitExploded) 440 else 300, easing = FastOutSlowInEasing)
            )
        }
    }

    // Kinetic SURGE dash / power-strike expanding shockwave ring + wide-view camera pull-out
    val surgeAnim = remember { Animatable(0f) }
    LaunchedEffect(race.surgePulseTick) {
        if (race.surgePulseTick > 0L) {
            juice.triggerKineticSurge()
            surgeAnim.snapTo(1f)
            surgeAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing)
            )
        }
    }

    val inf = rememberInfiniteTransition(label = "board_fx")
    val pulse by inf.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(920, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tile_pulse"
    )
    val orbitAngle by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val ambientPhase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )
    val sonarRing by inf.animateFloat(
        initialValue = 0.08f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_ring"
    )

    val scratchPath = remember { Path() }
    val biomeVisuals = remember(race.biome) {
        when (race.biome) {
            Biome.DESERT -> BiomeVisualPalette(
                floorA = Color(0xFF362616),
                floorB = Color(0xFF43301B),
                floorAccent = Color(0xFFD97706),
                fog = Color(0xFF140D06),
                fogTile = Color(0xFF1A1109),
                glow = ForgedGold,
                particleColor = Color(0xFFFDE68A),
                particleSecondary = Color(0xFFF59E0B),
                vignetteTint = Color(0xFFB45309),
                blockRimTint = Color(0xFFFCD34D)
            )
            Biome.ICE -> BiomeVisualPalette(
                floorA = Color(0xFF0E263D),
                floorB = Color(0xFF153654),
                floorAccent = Color(0xFF38BDF8),
                fog = Color(0xFF050E18),
                fogTile = Color(0xFF081624),
                glow = TileIce,
                particleColor = Color(0xFFE0F2FE),
                particleSecondary = Color(0xFF38BDF8),
                vignetteTint = Color(0xFF0284C7),
                blockRimTint = Color(0xFFBAE6FD)
            )
            Biome.VOLCANO -> BiomeVisualPalette(
                floorA = Color(0xFF281213),
                floorB = Color(0xFF351719),
                floorAccent = Color(0xFFEA580C),
                fog = Color(0xFF120607),
                fogTile = Color(0xFF1A090B),
                glow = VolcanicOrange,
                particleColor = Color(0xFFFEF08A),
                particleSecondary = Color(0xFFFF5722),
                vignetteTint = Color(0xFFDC2626),
                blockRimTint = Color(0xFFFB923C)
            )
            Biome.CAVERN -> BiomeVisualPalette(
                floorA = Color(0xFF101D33),
                floorB = Color(0xFF162845),
                floorAccent = Color(0xFF22D3EE),
                fog = Color(0xFF060B16),
                fogTile = Color(0xFF091224),
                glow = CrystalCyan,
                particleColor = Color(0xFF67E8F9),
                particleSecondary = Color(0xFF34D399),
                vignetteTint = Color(0xFF0E7490),
                blockRimTint = Color(0xFF67E8F9)
            )
            Biome.RUINS -> BiomeVisualPalette(
                floorA = Color(0xFF1D1630),
                floorB = Color(0xFF271E40),
                floorAccent = Color(0xFFA855F7),
                fog = Color(0xFF0A0714),
                fogTile = Color(0xFF120D22),
                glow = MysticAmethyst,
                particleColor = Color(0xFFFDE047),
                particleSecondary = Color(0xFF34D399),
                vignetteTint = Color(0xFF7E22CE),
                blockRimTint = Color(0xFFD8B4FE)
            )
        }
    }

    val ghostPos = race.ghostTrail.getOrNull(
        race.ghostIndex.coerceAtMost((race.ghostTrail.size - 1).coerceAtLeast(0))
    )
    val activeGhostFrame: GhostFrame? = remember(race.ghostFrames, race.ghostIndex, race.tickCount) {
        if (race.ghostFrames.isEmpty()) null
        else GhostReplayCodec.frameAtTick(race.ghostFrames, maxOf(race.ghostIndex, race.tickCount))
    }
    // Pre-packed Int coordinate maps/sets eliminate all "$gx,$gy" String allocations inside 60fps Canvas loop
    val blocksByCoord = remember(race.blocks) {
        HashMap<Int, BlockState>(race.blocks.size * 2).apply {
            race.blocks.forEach { (k, v) -> parsePackedCoord(k)?.let { put(it, v) } }
        }
    }
    val exploredPacked = remember(race.explored) {
        HashSet<Int>(race.explored.size * 2).apply {
            race.explored.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val hiddenRevealedPacked = remember(race.hiddenRevealed) {
        HashSet<Int>(race.hiddenRevealed.size * 2).apply {
            race.hiddenRevealed.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val gemsPacked = remember(race.gems) {
        HashSet<Int>(race.gems.size * 2).apply {
            race.gems.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val powersByCoord = remember(race.powers) {
        HashMap<Int, PowerupType>(race.powers.size * 2).apply {
            race.powers.forEach { (k, v) -> parsePackedCoord(k)?.let { put(it, v) } }
        }
    }
    val trapsPacked = remember(race.traps) {
        HashSet<Int>(race.traps.size * 2).apply {
            race.traps.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val lavaPacked = remember(race.lavaPuddles) {
        HashSet<Int>(race.lavaPuddles.size * 2).apply {
            race.lavaPuddles.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val sandPacked = remember(race.sandDrifts) {
        HashSet<Int>(race.sandDrifts.size * 2).apply {
            race.sandDrifts.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val checkpointsPacked = remember(race.checkpoints) {
        HashSet<Int>(race.checkpoints.size * 2).apply {
            race.checkpoints.forEach { cp -> add(packGridCoord(cp.x, cp.y)) }
        }
    }
    val playerCheckpointsPacked = remember(race.playerCheckpointsCleared) {
        HashSet<Int>(race.playerCheckpointsCleared.size * 2).apply {
            race.playerCheckpointsCleared.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val rivalCheckpointsPacked = remember(race.rivalCheckpointsCleared) {
        HashSet<Int>(race.rivalCheckpointsCleared.size * 2).apply {
            race.rivalCheckpointsCleared.forEach { k -> parsePackedCoord(k)?.let { add(it) } }
        }
    }
    val ghostTrailPacked = remember(race.ghostTrail) {
        HashSet<Int>(race.ghostTrail.size * 2).apply {
            race.ghostTrail.forEach { add(packGridCoord(it.x, it.y)) }
        }
    }
    val hintPacked = remember(race.hintPath) {
        HashSet<Int>(race.hintPath.size * 2).apply {
            race.hintPath.forEach { add(packGridCoord(it.x, it.y)) }
        }
    }
    val recentTrail = remember(race.myTrail) { race.myTrail.takeLast(10) }
    val exploredCoords = remember(race.explored) {
        race.explored.mapNotNull { k ->
            val comma = k.indexOf(',')
            if (comma <= 0) null
            else {
                val x = k.substring(0, comma).toIntOrNull()
                val y = k.substring(comma + 1).toIntOrNull()
                if (x != null && y != null) Triple(k, x, y) else null
            }
        }
    }
    val gemCoords = remember(race.gems) {
        race.gems.mapNotNull { k ->
            val comma = k.indexOf(',')
            if (comma <= 0) null
            else {
                val x = k.substring(0, comma).toIntOrNull()
                val y = k.substring(comma + 1).toIntOrNull()
                if (x != null && y != null) Triple(k, x, y) else null
            }
        }
    }
    val rivalManhattan = abs(race.rivalPos.x - race.playerPos.x) + abs(race.rivalPos.y - race.playerPos.y)
    val rivalDistToFinish = abs(race.finishPos.x - race.rivalPos.x) + abs(race.finishPos.y - race.rivalPos.y)
    val isHighTension = race.timer <= 20 || rivalDistToFinish <= 3

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(biomeVisuals.fog)
            .semantics { contentDescription = "Immersive maze board" }
            .testTag("maze_board_canvas")
    ) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(race.viewSize, race.playerPos, race.boardSize) {
                    detectTapGestures { offset ->
                        val cellPx = min(size.width, size.height).toFloat() / race.viewSize
                        if (cellPx <= 0f) return@detectTapGestures
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val gx = (race.playerPos.x + (offset.x - centerX) / cellPx).roundToInt()
                        val gy = (race.playerPos.y + (offset.y - centerY) / cellPx).roundToInt()
                        onCellTapped(gx, gy)
                    }
                }
                .pointerInput(race.seed, race.status) {
                    var totalDx = 0f
                    var totalDy = 0f
                    var fired = false
                    detectDragGestures(
                        onDragStart = {
                            totalDx = 0f
                            totalDy = 0f
                            fired = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (fired) return@detectDragGestures
                            totalDx += dragAmount.x
                            totalDy += dragAmount.y
                            val threshold = 42f
                            if (abs(totalDx) >= threshold || abs(totalDy) >= threshold) {
                                fired = true
                                if (abs(totalDx) > abs(totalDy)) {
                                    onSwipeDirection(if (totalDx > 0) 1 else -1, 0)
                                } else {
                                    onSwipeDirection(0, if (totalDy > 0) 1 else -1)
                                }
                            }
                        }
                    )
                }
        ) {
            val boardSide = min(size.width, size.height)
            val dynamicZoom = (juice.cameraZoomImpulse * gauntletFocusZoom).coerceIn(0.84f, 1.24f)
            val cellSize = (boardSide / race.viewSize) * dynamicZoom
            // Tighter gutter padding so enlarged blocks showcase maximum carved detail
            val pad = (cellSize * 0.032f).coerceAtLeast(1.5f)
            val tileCorner = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)

            val shakeStrength = if (race.lastHitExploded) {
                11f + (race.lastChainDepth.coerceAtMost(3) * 3.8f)
            } else {
                5.5f
            }
            val shakePx = hitAnim.value * shakeStrength * sin(hitAnim.value * 30f)
            val originX = size.width / 2f + shakePx
            val originY = size.height / 2f + (shakePx * 0.55f)

            val halfCols = ceil((size.width / 2f) / cellSize).toInt() + 1
            val halfRows = ceil((size.height / 2f) / cellSize).toInt() + 1
            val minGx = race.playerPos.x - halfCols
            val maxGx = race.playerPos.x + halfCols
            val minGy = race.playerPos.y - halfRows
            val maxGy = race.playerPos.y + halfRows

            // Pass 1: Recessed Floors, Walkable Corridor Details, Pickups & Entities
            for (gy in minGy..maxGy) {
                for (gx in minGx..maxGx) {
                    val cx = originX + (gx - camX) * cellSize
                    val cy = originY + (gy - camY) * cellSize
                    val left = cx - cellSize / 2f + pad
                    val top = cy - cellSize / 2f + pad
                    val inner = cellSize - pad * 2f

                    if (gx !in 0 until race.boardSize || gy !in 0 until race.boardSize) {
                        drawRoundRect(
                            color = biomeVisuals.fog,
                            topLeft = Offset(left, top),
                            size = Size(inner, inner),
                            cornerRadius = tileCorner
                        )
                        continue
                    }

                    val coordKey = packGridCoord(gx, gy)
                    val dist = abs(gx - race.playerPos.x) + abs(gy - race.playerPos.y)
                    val inVision = dist <= race.currentVision
                    val explored = coordKey in exploredPacked

                    if (!explored && !inVision) {
                        drawRoundRect(
                            color = biomeVisuals.fogTile,
                            topLeft = Offset(left, top),
                            size = Size(inner, inner),
                            cornerRadius = tileCorner
                        )
                        if ((gx * 7 + gy * 13) % 7 == 0) {
                            drawCircle(
                                color = biomeVisuals.glow.copy(alpha = 0.12f * pulse),
                                radius = inner * 0.065f,
                                center = Offset(cx, cy)
                            )
                        }
                        continue
                    }

                    val alpha = if (inVision) 1f else 0.38f
                    val topLeft = Offset(left, top)
                    val rectSize = Size(inner, inner)
                    val center = Offset(cx, cy)

                    // Rich biome-textured recessed stone floor
                    drawDetailedFloorTile(
                        gx = gx,
                        gy = gy,
                        biome = race.biome,
                        palette = biomeVisuals,
                        topLeft = topLeft,
                        rectSize = rectSize,
                        corner = tileCorner,
                        alpha = alpha,
                        pulse = pulse,
                        scratchPath = scratchPath
                    )

                    val block = blocksByCoord[coordKey]
                    if (block != null) {
                        val isDisguisedHidden = block.special == BlockSpecial.HIDDEN && coordKey !in hiddenRevealedPacked
                        if (!isDisguisedHidden) {
                            drawEnlargedDetailedBlock(
                                block = block,
                                gx = gx,
                                gy = gy,
                                topLeft = topLeft,
                                rectSize = rectSize,
                                corner = tileCorner,
                                alpha = alpha,
                                pulse = pulse,
                                biomeRimTint = biomeVisuals.blockRimTint,
                                isTarget = race.targetBlock?.x == gx && race.targetBlock.y == gy,
                                isRevealedHidden = block.special == BlockSpecial.HIDDEN,
                                scratchPath = scratchPath
                            )
                        } else if (inVision && dist == 1) {
                            // Subtle crystalline seam shimmer clue on adjacent hidden blocks
                            drawRoundRect(
                                color = CrystalCyan.copy(alpha = 0.18f * pulse),
                                topLeft = topLeft,
                                size = rectSize,
                                cornerRadius = tileCorner,
                                style = Stroke(width = 2f)
                            )
                        }
                    } else {
                        // Pathfinder BFS Hint breadcrumb shimmer
                        if (coordKey in hintPacked) {
                            drawCircle(
                                color = CrystalCyan.copy(alpha = 0.28f * pulse),
                                radius = inner * 0.28f,
                                center = center
                            )
                            drawCircle(
                                color = CrystalCyan.copy(alpha = 0.90f * pulse),
                                radius = inner * 0.11f,
                                center = center
                            )
                        }

                        // Ruins trap etched pressure glyph when visible
                        if (coordKey in trapsPacked && inVision) {
                            drawCircle(
                                color = MysticAmethyst.copy(alpha = 0.38f * pulse),
                                radius = inner * 0.24f,
                                center = center,
                                style = Stroke(width = inner * 0.055f)
                            )
                            drawCircle(
                                color = RivalCrimson.copy(alpha = 0.52f),
                                radius = inner * 0.09f,
                                center = center
                            )
                        }

                        // Volcano molten floor lava puddle (-2s hazard unless Shield active/Lv2+)
                        if (coordKey in lavaPacked) {
                            val puddleW = inner * 0.78f
                            val puddleH = inner * 0.62f
                            val puddleTopLeft = Offset(
                                center.x - puddleW / 2f,
                                center.y - puddleH / 2f
                            )
                            // Scorched basalt rim
                            drawOval(
                                color = Color(0xFF2B120C).copy(alpha = 0.88f * alpha),
                                topLeft = Offset(puddleTopLeft.x - 2f, puddleTopLeft.y - 2f),
                                size = Size(puddleW + 4f, puddleH + 4f)
                            )
                            // Molten core radial glow
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFEA00).copy(alpha = (0.85f + 0.12f * pulse) * alpha),
                                        Color(0xFFFF6D00).copy(alpha = 0.92f * alpha),
                                        Color(0xFFDC2626).copy(alpha = 0.82f * alpha)
                                    ),
                                    center = center,
                                    radius = puddleW * 0.56f
                                ),
                                topLeft = puddleTopLeft,
                                size = Size(puddleW, puddleH)
                            )
                            // Animated bubbling heat ripple
                            drawCircle(
                                color = Color(0xFFFFF59D).copy(alpha = (0.35f + 0.45f * pulse) * alpha),
                                radius = inner * (0.12f + 0.09f * pulse),
                                center = Offset(center.x - inner * 0.08f, center.y - inner * 0.04f),
                                style = Stroke(width = (inner * 0.035f).coerceAtLeast(1.4f))
                            )
                            drawCircle(
                                color = Color(0xFFFFCC00).copy(alpha = (0.45f + 0.40f * (1f - pulse)) * alpha),
                                radius = inner * 0.07f,
                                center = Offset(center.x + inner * 0.14f, center.y + inner * 0.06f)
                            )
                        }

                        // Desert wind-rippled sand drift (tap/step to sweep clear for bonus points)
                        if (coordKey in sandPacked) {
                            val driftPad = inner * 0.10f
                            val driftSize = Size(inner - driftPad * 2f, inner - driftPad * 2f)
                            val driftTopLeft = Offset(topLeft.x + driftPad, topLeft.y + driftPad)
                            // Soft ground drop shadow
                            drawOval(
                                color = Color(0xFF3E2723).copy(alpha = 0.45f * alpha),
                                topLeft = Offset(driftTopLeft.x + 1.5f, driftTopLeft.y + inner * 0.22f),
                                size = Size(driftSize.width, driftSize.height * 0.68f)
                            )
                            // Sculpted golden-ochre dune mound
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF6D38B).copy(alpha = 0.95f * alpha),
                                        Color(0xFFD99B4E).copy(alpha = 0.94f * alpha),
                                        Color(0xFFA86A28).copy(alpha = 0.92f * alpha)
                                    ),
                                    start = driftTopLeft,
                                    end = Offset(driftTopLeft.x + driftSize.width, driftTopLeft.y + driftSize.height)
                                ),
                                topLeft = Offset(driftTopLeft.x, driftTopLeft.y + inner * 0.10f),
                                size = Size(driftSize.width, driftSize.height * 0.78f),
                                cornerRadius = CornerRadius(inner * 0.34f, inner * 0.34f)
                            )
                            // Wind-swept specular sand ridges
                            val ridgeStroke = (inner * 0.045f).coerceAtLeast(1.5f)
                            drawLine(
                                color = Color(0xFFFFF3C4).copy(alpha = (0.58f + 0.20f * pulse) * alpha),
                                start = Offset(driftTopLeft.x + driftSize.width * 0.18f, center.y - inner * 0.06f),
                                end = Offset(driftTopLeft.x + driftSize.width * 0.76f, center.y - inner * 0.14f),
                                strokeWidth = ridgeStroke,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFFFE099).copy(alpha = 0.50f * alpha),
                                start = Offset(driftTopLeft.x + driftSize.width * 0.26f, center.y + inner * 0.12f),
                                end = Offset(driftTopLeft.x + driftSize.width * 0.82f, center.y + inner * 0.04f),
                                strokeWidth = ridgeStroke * 0.85f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Ghost trail footsteps (O(1) packed Int lookup)
                        if (coordKey in ghostTrailPacked) {
                            drawCircle(
                                color = TileGhost.copy(alpha = 0.26f * alpha),
                                radius = inner * 0.09f,
                                center = center
                            )
                        }

                        // Strategic Relay Beacon Pylon (O(1) packed Int lookup)
                        if (coordKey in checkpointsPacked) {
                            val playerCleared = coordKey in playerCheckpointsPacked
                            val rivalCleared = coordKey in rivalCheckpointsPacked
                            drawRelayBeaconPylon(
                                center = center,
                                inner = inner,
                                pulse = pulse,
                                orbitAngle = orbitAngle,
                                alpha = if (inVision) 1f else 0.72f,
                                playerCleared = playerCleared,
                                rivalCleared = rivalCleared,
                                scratchPath = scratchPath
                            )
                        }

                        // Multi-Ring Rotating Arcane Finish Portal (Sealed Amber-Iron until all Beacons synced)
                        if (gx == race.finishPos.x && gy == race.finishPos.y) {
                            drawArcaneFinishPortal(
                                topLeft = topLeft,
                                rectSize = rectSize,
                                center = center,
                                corner = tileCorner,
                                pulse = pulse,
                                orbitAngle = orbitAngle,
                                alpha = alpha,
                                isSealed = !race.portalUnlocked
                            )
                        }

                        // Faceted Glowing Gem
                        if (coordKey in gemsPacked) {
                            drawFacetedGem(
                                center = center,
                                radius = inner * 0.33f * (0.92f + 0.10f * pulse),
                                alpha = alpha,
                                pulse = pulse,
                                scratchPath = scratchPath
                            )
                        }

                        // Powerup Orb with bespoke vector icon
                        val power = powersByCoord[coordKey]
                        if (power != null) {
                            drawPowerupOrb(
                                power = power,
                                center = center,
                                inner = inner,
                                pulse = pulse,
                                alpha = alpha,
                                scratchPath = scratchPath
                            )
                        }
                    }

                    // Ghost Racer Ring
                    if (ghostPos != null && ghostPos.x == gx && ghostPos.y == gy && inVision) {
                        drawCircle(
                            color = TileGhost.copy(alpha = 0.62f),
                            radius = inner * 0.31f,
                            center = center,
                            style = Stroke(width = inner * 0.08f)
                        )
                    }
                }
            }

            // Luminous Fading Player Breadcrumb Trail Ribbon
            if (recentTrail.size >= 2) {
                for (i in 1 until recentTrail.size) {
                    val p0 = recentTrail[i - 1]
                    val p1 = recentTrail[i]
                    val segAlpha = (i.toFloat() / recentTrail.size.toFloat()) * 0.38f
                    val trailCol = if (race.overdriveActive) VolcanicOrange else ForgedGold
                    drawLine(
                        color = trailCol.copy(alpha = segAlpha),
                        start = Offset(originX + (p0.x - camX) * cellSize, originY + (p0.y - camY) * cellSize),
                        end = Offset(originX + (p1.x - camX) * cellSize, originY + (p1.y - camY) * cellSize),
                        strokeWidth = (cellSize * 0.085f) * (0.4f + 0.6f * (i.toFloat() / recentTrail.size)),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Deterministic Holographic Ghost Racer (Daily Ranked Cloud PB / Local Best Ghost)
            if (activeGhostFrame != null) {
                val ghostPulseAlpha = 0.45f + (0.10f * sin(orbitAngle * 2f)) // 0.35f..0.55f sine wave
                val screenGhostFrame = activeGhostFrame.copy(
                    gridX = (originX / cellSize) + (activeGhostFrame.gridX - camX) - 0.5f,
                    gridY = (originY / cellSize) + (activeGhostFrame.gridY - camY) - 0.5f
                )
                drawGhostMiner(
                    ghostFrame = screenGhostFrame,
                    tileSizePx = cellSize,
                    pulseAlpha = ghostPulseAlpha.coerceIn(0.35f, 0.55f)
                )
            }

            // Diegetic First-Run Playable Gauntlet pulsing target reticle
            if (race.isGauntlet && race.gauntletStep != GauntletStep.COMPLETED) {
                val gTarget = race.gauntletStep.targetPosOrNull()
                if (gTarget != null) {
                    val gcx = originX + (gTarget.x - camX) * cellSize
                    val gcy = originY + (gTarget.y - camY) * cellSize
                    drawCircle(
                        color = ForgedGold.copy(alpha = 0.24f * pulse),
                        radius = cellSize * (0.48f + 0.14f * pulse),
                        center = Offset(gcx, gcy)
                    )
                    drawCircle(
                        color = CrystalCyan.copy(alpha = 0.92f * pulse),
                        radius = cellSize * 0.44f,
                        center = Offset(gcx, gcy),
                        style = Stroke(width = (cellSize * 0.065f).coerceAtLeast(2.5f))
                    )
                }
            }

            // Smooth Spring-Interpolated Rival AI Armored Cyber-Driller (pauses cleanly during <=45ms Hitstop)
            val rivalVisible = !race.isGauntlet && ((rivalManhattan <= race.currentVision) || race.sonarActive)
            if (rivalVisible) {
                val drawRX = if (isHitstopFrozen) frozenRivalX else rivalAnimX
                val drawRY = if (isHitstopFrozen) frozenRivalY else rivalAnimY
                val rcx = originX + (drawRX - camX) * cellSize
                val rcy = originY + (drawRY - camY) * cellSize
                drawRivalCyberDriller(
                    center = Offset(rcx, rcy),
                    inner = cellSize - pad * 2f,
                    pulse = pulse,
                    orbitAngle = orbitAngle
                )
            }

            // Pathfinder BFS Hint glowing trail line (reusing scratchPath — zero GC churn)
            if (race.hintPath.size >= 2) {
                scratchPath.rewind()
                race.hintPath.forEachIndexed { i, pos ->
                    val px = originX + (pos.x - camX) * cellSize
                    val py = originY + (pos.y - camY) * cellSize
                    if (i == 0) scratchPath.moveTo(px, py) else scratchPath.lineTo(px, py)
                }
                drawPath(
                    path = scratchPath,
                    color = CrystalCyan.copy(alpha = 0.72f * pulse),
                    style = Stroke(width = cellSize * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Material-Colored Shatter Debris, Pickaxe Strike Arc & Explosive Blast Shockwave
            val lastHit = race.lastHitPos
            if (lastHit != null && hitAnim.value > 0.02f) {
                val hx = originX + (lastHit.x - camX) * cellSize
                val hy = originY + (lastHit.y - camY) * cellSize
                val progress = 1f - hitAnim.value
                val debrisColor = when (race.lastHitKind) {
                    BlockKind.DIRT -> TileDirtLight
                    BlockKind.STONE -> TileStoneLight
                    BlockKind.IRON -> TileIronLight
                    BlockKind.CRYSTAL -> CrystalCyan
                    BlockKind.GAMBLE -> ForgedGold
                    null -> ForgedGold
                }

                // Explosive & Recursive Chain Blast multi-origin shockwave rings
                if (race.lastHitExploded) {
                    val origins = if (race.lastBlastOrigins.isNotEmpty()) race.lastBlastOrigins else listOf(lastHit)
                    origins.forEachIndexed { idx, blastPos ->
                        val bx = originX + (blastPos.x - camX) * cellSize
                        val by = originY + (blastPos.y - camY) * cellSize
                        val ringScale = 1f + (idx.coerceAtMost(3) * 0.16f)
                        val rFill = cellSize * (0.6f + 1.85f * progress) * ringScale
                        val rStroke = cellSize * (0.4f + 1.55f * progress) * ringScale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ForgedGold.copy(alpha = 0.48f * hitAnim.value),
                                    VolcanicOrange.copy(alpha = 0.32f * hitAnim.value),
                                    Color.Transparent
                                ),
                                center = Offset(bx, by),
                                radius = rFill
                            ),
                            radius = rFill,
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = (if (idx == 0) VolcanicOrange else ForgedGold).copy(alpha = hitAnim.value * 0.92f),
                            radius = rStroke,
                            center = Offset(bx, by),
                            style = Stroke(width = (cellSize * 0.095f) * hitAnim.value)
                        )
                    }
                }

                // Pickaxe impact flash slash across hit tile
                drawLine(
                    color = Color.White.copy(alpha = hitAnim.value * 0.92f),
                    start = Offset(hx - cellSize * 0.28f, hy - cellSize * 0.24f),
                    end = Offset(hx + cellSize * 0.28f, hy + cellSize * 0.24f),
                    strokeWidth = (cellSize * 0.07f) * hitAnim.value,
                    cap = StrokeCap.Round
                )

                // Flying rock/crystal debris chunks
                val particleCount = if (race.lastHitBroke) 12 else 7
                val spread = cellSize * (0.18f + (if (race.lastHitBroke) 0.72f else 0.45f) * progress)
                for (i in 0 until particleCount) {
                    val angle = (i * (2.0 * PI / particleCount)) + 0.27
                    val sx = hx + (cos(angle) * spread).toFloat()
                    val sy = hy + (sin(angle) * spread).toFloat()
                    val chunkSize = (cellSize * (if (i % 2 == 0) 0.11f else 0.075f)) * hitAnim.value
                    drawRoundRect(
                        color = if (i % 3 == 0) ForgedGold.copy(alpha = hitAnim.value)
                        else debrisColor.copy(alpha = hitAnim.value),
                        topLeft = Offset(sx - chunkSize / 2f, sy - chunkSize / 2f),
                        size = Size(chunkSize, chunkSize),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // Player Miner + Directional Headlamp Beam + Compass + Overdrive Fever Aura
            val playerCenter = Offset(originX, originY)
            val facingAngle = atan2(race.facingDir.y.toFloat(), race.facingDir.x.toFloat())

            // Directional Headlamp Volumetric Cone (reusing scratchPath)
            val coneLen = cellSize * (race.currentVision * 0.78f).coerceAtLeast(1.8f)
            val coneHalfAngle = 0.44f
            scratchPath.rewind()
            scratchPath.moveTo(playerCenter.x, playerCenter.y)
            scratchPath.lineTo(
                playerCenter.x + cos(facingAngle - coneHalfAngle) * coneLen,
                playerCenter.y + sin(facingAngle - coneHalfAngle) * coneLen
            )
            scratchPath.lineTo(
                playerCenter.x + cos(facingAngle + coneHalfAngle) * coneLen,
                playerCenter.y + sin(facingAngle + coneHalfAngle) * coneLen
            )
            scratchPath.close()
            drawPath(
                path = scratchPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        ForgedGold.copy(alpha = 0.22f * pulse),
                        CrystalCyan.copy(alpha = 0.08f * pulse),
                        Color.Transparent
                    ),
                    center = playerCenter,
                    radius = coneLen
                )
            )

            // Warm ambient lantern halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        biomeVisuals.glow.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = playerCenter,
                    radius = cellSize * (race.currentVision * 0.75f)
                ),
                radius = cellSize * (race.currentVision * 0.75f),
                center = playerCenter
            )

            // Active Sonar expanding radar wave
            if (race.sonarActive) {
                drawCircle(
                    color = CrystalCyan.copy(alpha = (1f - sonarRing) * 0.80f),
                    radius = cellSize * 4.5f * sonarRing,
                    center = playerCenter,
                    style = Stroke(width = 4.5f)
                )
            }

            // Kinetic SURGE expanding Cyan-Gold shockwave rings
            if (surgeAnim.value > 0.02f) {
                val surgeProgress = 1f - surgeAnim.value
                val outerRadius = cellSize * (0.45f + 1.55f * surgeProgress)
                val innerRadius = cellSize * (0.28f + 1.05f * surgeProgress)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CrystalCyan.copy(alpha = 0.42f * surgeAnim.value),
                            ForgedGold.copy(alpha = 0.24f * surgeAnim.value),
                            Color.Transparent
                        ),
                        center = playerCenter,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = playerCenter
                )
                drawCircle(
                    color = CrystalCyan.copy(alpha = 0.92f * surgeAnim.value),
                    radius = outerRadius,
                    center = playerCenter,
                    style = Stroke(width = (cellSize * 0.09f) * surgeAnim.value)
                )
                drawCircle(
                    color = ForgedGold.copy(alpha = 0.85f * surgeAnim.value),
                    radius = innerRadius,
                    center = playerCenter,
                    style = Stroke(width = (cellSize * 0.055f) * surgeAnim.value)
                )
            }

            // Overdrive Fever Golden Flame Ring (Combo >= 4 in Campaign/Practice)
            if (race.overdriveActive) {
                drawCircle(
                    color = VolcanicOrange.copy(alpha = 0.45f * pulse),
                    radius = cellSize * 0.52f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.06f)
                )
            }

            // Smart Strategic Objective Compass Chevron:
            // Guides toward nearest uncaptured Relay Beacon (ForgedGold/Cyan), then shifts to Emerald for Unlocked Portal!
            val objective = race.nextPlayerObjective
            val fdx = (objective.x - race.playerPos.x).toFloat()
            val fdy = (objective.y - race.playerPos.y).toFloat()
            if (abs(fdx) + abs(fdy) > 0f) {
                val angle = atan2(fdy, fdx)
                val tipDist = cellSize * 0.63f
                val baseDist = cellSize * 0.47f
                val spreadAngle = 0.28f
                val tip = Offset(
                    playerCenter.x + cos(angle) * tipDist,
                    playerCenter.y + sin(angle) * tipDist
                )
                val leftWing = Offset(
                    playerCenter.x + cos(angle - spreadAngle) * baseDist,
                    playerCenter.y + sin(angle - spreadAngle) * baseDist
                )
                val rightWing = Offset(
                    playerCenter.x + cos(angle + spreadAngle) * baseDist,
                    playerCenter.y + sin(angle + spreadAngle) * baseDist
                )
                scratchPath.rewind()
                scratchPath.moveTo(tip.x, tip.y)
                scratchPath.lineTo(leftWing.x, leftWing.y)
                scratchPath.lineTo(rightWing.x, rightWing.y)
                scratchPath.close()
                val compassColor = if (race.portalUnlocked) EmeraldGem else ForgedGold
                drawPath(scratchPath, color = compassColor.copy(alpha = 0.94f * pulse))
            }

            // Rival Proximity Danger Beacon
            if (rivalManhattan in 1..5) {
                val rAngle = atan2(
                    (race.rivalPos.y - race.playerPos.y).toFloat(),
                    (race.rivalPos.x - race.playerPos.x).toFloat()
                )
                val beaconPos = Offset(
                    playerCenter.x + cos(rAngle) * cellSize * 0.53f,
                    playerCenter.y + sin(rAngle) * cellSize * 0.53f
                )
                drawCircle(
                    color = RivalCrimson.copy(alpha = 0.90f * pulse),
                    radius = cellSize * 0.10f,
                    center = beaconPos
                )
            }

            // Founder's Golden Aura Ring
            if (founderAura) {
                drawCircle(
                    color = ForgedGold.copy(alpha = 0.62f * pulse),
                    radius = cellSize * 0.45f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.055f)
                )
            }

            // Translucent Cyan Energy Shield Bubble & Kinetic Speed Trails
            if (race.activePower == PowerupType.SHIELD || race.activePower == PowerupType.SPEED) {
                val auraCol = if (race.activePower == PowerupType.SHIELD) CrystalCyan else EmeraldGem
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            auraCol.copy(alpha = 0.14f),
                            auraCol.copy(alpha = 0.38f * pulse)
                        ),
                        center = playerCenter,
                        radius = cellSize * 0.48f
                    ),
                    radius = cellSize * 0.48f,
                    center = playerCenter
                )
                drawCircle(
                    color = auraCol.copy(alpha = 0.82f),
                    radius = cellSize * 0.45f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.06f)
                )
                // Kinetic energy speed streaks trailing opposite facing direction
                val backX = -cos(facingAngle)
                val backY = -sin(facingAngle)
                val perpX = -sin(facingAngle)
                val perpY = cos(facingAngle)
                for (s in -1..1) {
                    val startPt = Offset(
                        playerCenter.x + backX * cellSize * 0.38f + perpX * s * cellSize * 0.16f,
                        playerCenter.y + backY * cellSize * 0.38f + perpY * s * cellSize * 0.16f
                    )
                    val endPt = Offset(
                        startPt.x + backX * cellSize * (0.28f + 0.10f * pulse),
                        startPt.y + backY * cellSize * (0.28f + 0.10f * pulse)
                    )
                    drawLine(
                        color = auraCol.copy(alpha = 0.72f * pulse),
                        start = startPt,
                        end = endPt,
                        strokeWidth = cellSize * 0.045f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Forged Golden Pickaxe Strike Swing Arc during active mining hit
            if (lastHit != null && hitAnim.value > 0.05f) {
                val strikeAngle = atan2(
                    (lastHit.y - race.playerPos.y).toFloat(),
                    (lastHit.x - race.playerPos.x).toFloat()
                )
                val swingOffset = (hitAnim.value - 0.5f) * 1.15f
                val handleTip = Offset(
                    playerCenter.x + cos(strikeAngle + swingOffset) * cellSize * 0.62f,
                    playerCenter.y + sin(strikeAngle + swingOffset) * cellSize * 0.62f
                )
                drawLine(
                    color = ForgedGold.copy(alpha = hitAnim.value * 0.92f),
                    start = playerCenter,
                    end = handleTip,
                    strokeWidth = cellSize * 0.07f,
                    cap = StrokeCap.Round
                )
                val perpX = -sin(strikeAngle + swingOffset) * cellSize * 0.22f
                val perpY = cos(strikeAngle + swingOffset) * cellSize * 0.22f
                drawLine(
                    color = Color.White.copy(alpha = hitAnim.value),
                    start = Offset(handleTip.x - perpX, handleTip.y - perpY),
                    end = Offset(handleTip.x + perpX, handleTip.y + perpY),
                    strokeWidth = cellSize * 0.065f,
                    cap = StrokeCap.Round
                )
            }

            // Articulated Top-Down Miner Explorer Avatar
            drawPlayerMinerAvatar(
                center = playerCenter,
                cellSize = cellSize,
                facingAngle = facingAngle,
                overdrive = race.overdriveActive
            )

            // Atmospheric Biome Edge Vignette (volcanic heat haze, arctic aurora sheen, desert mist, cavern depth, arcane glow)
            drawBiomeAtmosphereVignette(
                biome = race.biome,
                palette = biomeVisuals,
                pulse = pulse
            )

            // Pulsing Golden Fever 5x Full-Screen Vignette & Radiant Edge Frame
            if (race.feverRemainingSec > 0 && race.status == com.example.viewmodel.RaceStatus.PLAYING) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.58f to Color.Transparent,
                            0.88f to ForgedGold.copy(alpha = 0.18f * pulse),
                            1.0f to VolcanicOrange.copy(alpha = 0.34f * pulse)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = kotlin.math.max(size.width, size.height) * 0.70f
                    ),
                    size = size
                )
                drawRect(
                    color = ForgedGold.copy(alpha = 0.58f * pulse),
                    topLeft = Offset(3f, 3f),
                    size = Size(size.width - 6f, size.height - 6f),
                    style = Stroke(width = 6f)
                )
            }

            // High-Stakes Urgency Crimson Heartbeat Perimeter Pulse (low timer or Rival at Finish)
            if (isHighTension && race.status == com.example.viewmodel.RaceStatus.PLAYING) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.64f to Color.Transparent,
                            1.0f to RivalCrimson.copy(alpha = 0.28f * pulse)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = kotlin.math.max(size.width, size.height) * 0.72f
                    ),
                    size = size
                )
            }

            // Radiant Golden-Emerald Victory Burst Rays on Win
            if (race.status == com.example.viewmodel.RaceStatus.WON) {
                val rayCount = 12
                val rayLen = kotlin.math.max(size.width, size.height) * 0.55f
                for (i in 0 until rayCount) {
                    val a = orbitAngle * 0.5 + i * (2.0 * PI / rayCount)
                    drawLine(
                        color = (if (i % 2 == 0) ForgedGold else EmeraldGem).copy(alpha = 0.22f * pulse),
                        start = playerCenter,
                        end = Offset(
                            playerCenter.x + (cos(a) * rayLen).toFloat(),
                            playerCenter.y + (sin(a) * rayLen).toFloat()
                        ),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Bespoke 5-Biome Animated Particle Effects (zero Path allocations)
            drawAmbientBiomeParticles(
                biome = race.biome,
                primary = biomeVisuals.particleColor,
                secondary = biomeVisuals.particleSecondary,
                phase = ambientPhase,
                pulse = pulse,
                scratchPath = scratchPath
            )

            // Live Translucent Tactical Mini-Map Radar (zero-allocation pre-parsed coordinates)
            drawTacticalMiniMap(
                race = race,
                exploredCoords = exploredCoords,
                gemCoords = gemCoords,
                pulse = pulse,
                biomeAccent = biomeVisuals.glow,
                scratchPath = scratchPath
            )
        }

        // World-Space Animated Floating Score, Chain Blast & Fever Popups
        val cellPxUi = (min(viewportWidthPx, viewportHeightPx) / race.viewSize.toFloat()).coerceAtLeast(1f)
        race.popups.forEachIndexed { idx, pop ->
            key(pop.id) {
                val screenX = (viewportWidthPx / 2f) + (pop.gridX - camX) * cellPxUi
                val screenY = (viewportHeightPx / 2f) + (pop.gridY - camY) * cellPxUi - (cellPxUi * 0.48f) - (idx % 3) * 14f
                FloatingWorldPopup(
                    text = pop.text,
                    isPositive = pop.isPositive,
                    anchorX = screenX.coerceIn(68f, (viewportWidthPx - 68f).coerceAtLeast(68f)),
                    anchorY = screenY.coerceIn(190f, (viewportHeightPx - 140f).coerceAtLeast(190f))
                )
            }
        }
    }
}

@Composable
private fun FloatingWorldPopup(
    text: String,
    isPositive: Boolean,
    anchorX: Float,
    anchorY: Float
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing))
    }
    val floatOffsetPx = -(progress.value * 64f)
    val fadeAlpha = (1f - ((progress.value - 0.55f).coerceAtLeast(0f) / 0.45f)).coerceIn(0f, 1f)
    val isFeverOrChain = text.contains("FEVER") || text.contains("CHAIN") || text.contains("💥")
    val accent = when {
        !isPositive -> RivalCrimson
        isFeverOrChain -> Color(0xFFFFEA00)
        else -> ForgedGold
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((anchorX - 48f).roundToInt(), (anchorY + floatOffsetPx).roundToInt()) }
            .alpha(fadeAlpha)
            .clip(RoundedCornerShape(999.dp))
            .background(ObsidianDeep.copy(alpha = 0.88f))
            .border(if (isFeverOrChain) 1.5.dp else 1.dp, accent, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = accent
        )
    }
}

private data class BiomeVisualPalette(
    val floorA: Color,
    val floorB: Color,
    val floorAccent: Color,
    val fog: Color,
    val fogTile: Color,
    val glow: Color,
    val particleColor: Color,
    val particleSecondary: Color,
    val vignetteTint: Color,
    val blockRimTint: Color
)

private fun packGridCoord(x: Int, y: Int): Int = (x shl 16) or (y and 0xFFFF)

private fun parsePackedCoord(key: String): Int? {
    val comma = key.indexOf(',')
    if (comma <= 0) return null
    val x = key.substring(0, comma).toIntOrNull() ?: return null
    val y = key.substring(comma + 1).toIntOrNull() ?: return null
    return packGridCoord(x, y)
}

private fun DrawScope.drawDetailedFloorTile(
    gx: Int,
    gy: Int,
    biome: Biome,
    palette: BiomeVisualPalette,
    topLeft: Offset,
    rectSize: Size,
    corner: CornerRadius,
    alpha: Float,
    pulse: Float,
    scratchPath: Path
) {
    val baseColor = if ((gx + gy) % 2 == 0) palette.floorA else palette.floorB
    drawRoundRect(
        color = baseColor.copy(alpha = alpha),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner
    )
    // Subtle recessed bevel border on corridor stone slabs
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.24f * alpha),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner,
        style = Stroke(width = 1.2f)
    )

    val w = rectSize.width
    val h = rectSize.height
    val hash = (gx * 31 + gy * 17) and 7

    // Distinct per-biome environmental corridor art
    when (biome) {
        Biome.VOLCANO -> {
            // Glowing magma floor cracks & ember pockets across basalt corridors
            if (hash == 0 || hash == 3) {
                scratchPath.rewind()
                scratchPath.moveTo(topLeft.x + w * 0.16f, topLeft.y + h * 0.72f)
                scratchPath.lineTo(topLeft.x + w * 0.48f, topLeft.y + h * 0.46f)
                scratchPath.lineTo(topLeft.x + w * 0.82f, topLeft.y + h * 0.28f)
                if (hash == 0) {
                    scratchPath.moveTo(topLeft.x + w * 0.48f, topLeft.y + h * 0.46f)
                    scratchPath.lineTo(topLeft.x + w * 0.72f, topLeft.y + h * 0.78f)
                }
                drawPath(
                    path = scratchPath,
                    color = palette.floorAccent.copy(alpha = 0.34f * pulse * alpha),
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = scratchPath,
                    color = palette.particleColor.copy(alpha = 0.24f * pulse * alpha),
                    style = Stroke(width = 1.0f, cap = StrokeCap.Round)
                )
            } else if (hash == 5) {
                drawCircle(
                    color = VolcanicOrange.copy(alpha = 0.22f * pulse * alpha),
                    radius = w * 0.10f,
                    center = Offset(topLeft.x + w * 0.68f, topLeft.y + h * 0.34f)
                )
            }
        }

        Biome.ICE -> {
            // Crystalline arctic frost specular sheen & glacial fracture glints
            drawLine(
                color = palette.floorAccent.copy(alpha = 0.16f * alpha),
                start = Offset(topLeft.x + w * 0.14f, topLeft.y + h * 0.14f),
                end = Offset(topLeft.x + w * 0.86f, topLeft.y + h * 0.14f),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round
            )
            if (hash == 0 || hash == 2 || hash == 5) {
                drawLine(
                    color = Color.White.copy(alpha = 0.20f * alpha),
                    start = Offset(topLeft.x + w * 0.20f, topLeft.y + h * 0.24f),
                    end = Offset(topLeft.x + w * 0.72f, topLeft.y + h * 0.76f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = palette.floorAccent.copy(alpha = 0.28f * alpha),
                    start = Offset(topLeft.x + w * 0.56f, topLeft.y + h * 0.22f),
                    end = Offset(topLeft.x + w * 0.80f, topLeft.y + h * 0.46f),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        Biome.DESERT -> {
            // Wind-carved sandstone dune ripples & sunlit ochre grain
            if (hash == 0 || hash == 2 || hash == 4) {
                drawLine(
                    color = palette.floorAccent.copy(alpha = 0.34f * alpha),
                    start = Offset(topLeft.x + w * 0.14f, topLeft.y + h * 0.38f),
                    end = Offset(topLeft.x + w * 0.82f, topLeft.y + h * 0.28f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = palette.particleColor.copy(alpha = 0.22f * alpha),
                    start = Offset(topLeft.x + w * 0.20f, topLeft.y + h * 0.70f),
                    end = Offset(topLeft.x + w * 0.86f, topLeft.y + h * 0.60f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        Biome.CAVERN -> {
            // Bioluminescent mineral clusters & glowing aqua cave flecks
            if (hash == 0 || hash == 3 || hash == 6) {
                val glowCol = if (hash == 3) palette.particleSecondary else palette.floorAccent
                drawCircle(
                    color = glowCol.copy(alpha = 0.16f * pulse * alpha),
                    radius = w * 0.13f,
                    center = Offset(topLeft.x + w * 0.30f, topLeft.y + h * 0.70f)
                )
                drawCircle(
                    color = glowCol.copy(alpha = 0.42f * pulse * alpha),
                    radius = w * 0.045f,
                    center = Offset(topLeft.x + w * 0.30f, topLeft.y + h * 0.70f)
                )
                drawCircle(
                    color = palette.particleColor.copy(alpha = 0.32f * pulse * alpha),
                    radius = w * 0.032f,
                    center = Offset(topLeft.x + w * 0.42f, topLeft.y + h * 0.62f)
                )
            }
        }

        Biome.RUINS -> {
            // Carved ancient temple flagstone inlay borders & golden corner glyphs
            if (hash == 0 || hash == 2 || hash == 4) {
                drawRect(
                    color = palette.floorAccent.copy(alpha = 0.26f * alpha),
                    topLeft = Offset(topLeft.x + w * 0.18f, topLeft.y + h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = Stroke(width = 1.3f)
                )
                drawCircle(
                    color = palette.particleColor.copy(alpha = 0.30f * pulse * alpha),
                    radius = w * 0.04f,
                    center = Offset(topLeft.x + w * 0.50f, topLeft.y + h * 0.50f)
                )
            }
        }
    }
}

/**
 * High-detail 2.5D extruded block renderer showcasing carved textures & biome environmental lighting.
 */
private fun DrawScope.drawEnlargedDetailedBlock(
    block: BlockState,
    gx: Int,
    gy: Int,
    topLeft: Offset,
    rectSize: Size,
    corner: CornerRadius,
    alpha: Float,
    pulse: Float,
    biomeRimTint: Color,
    isTarget: Boolean,
    isRevealedHidden: Boolean,
    scratchPath: Path
) {
    val (base, highlight, skirt) = when (block.kind) {
        BlockKind.DIRT -> Triple(TileDirt, TileDirtLight, Color(0xFF4A2B11))
        BlockKind.STONE -> Triple(TileStone, TileStoneLight, Color(0xFF283344))
        BlockKind.IRON -> Triple(TileIron, TileIronLight, Color(0xFF475569))
        BlockKind.CRYSTAL -> Triple(TileCrystal, TileCrystalLight, Color(0xFF4C1D95))
        BlockKind.GAMBLE -> Triple(TileGamble, TileGambleLight, Color(0xFF92400E))
    }

    val w = rectSize.width
    val h = rectSize.height
    val skirtH = h * 0.17f
    val topFaceH = h - skirtH * 0.72f

    // 1. Soft ambient drop shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f * alpha),
        topLeft = Offset(topLeft.x + 2f, topLeft.y + 3.5f),
        size = rectSize,
        cornerRadius = corner
    )

    // 2. Dark 2.5D extruded lower skirt wall
    drawRoundRect(
        color = skirt.copy(alpha = alpha),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner
    )

    // 3. Elevated sculpted top face with rich diagonal lighting gradient
    val topFaceSize = Size(w, topFaceH)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(highlight.copy(alpha = alpha), base.copy(alpha = alpha)),
            start = topLeft,
            end = Offset(topLeft.x + w, topLeft.y + topFaceH)
        ),
        topLeft = topLeft,
        size = topFaceSize,
        cornerRadius = corner
    )

    // Subtle biome ambient color wash so blocks harmonize with Volcanic/Arctic/Desert/Cavern/Ruins lighting
    drawRoundRect(
        color = biomeRimTint.copy(alpha = 0.08f * alpha),
        topLeft = topLeft,
        size = topFaceSize,
        cornerRadius = corner
    )

    // 4. Crisp top specular bevel edge tinted by biome atmosphere
    drawLine(
        color = biomeRimTint.copy(alpha = 0.40f * alpha),
        start = Offset(topLeft.x + w * 0.14f, topLeft.y + 2f),
        end = Offset(topLeft.x + w * 0.86f, topLeft.y + 2f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )

    val center = Offset(topLeft.x + w / 2f, topLeft.y + topFaceH * 0.50f)

    // 5. Procedural material-specific texture details
    when (block.kind) {
        BlockKind.DIRT -> {
            // Layered earth strata & clay pebbles
            drawLine(
                color = skirt.copy(alpha = 0.35f * alpha),
                start = Offset(topLeft.x + w * 0.10f, topLeft.y + topFaceH * 0.38f),
                end = Offset(topLeft.x + w * 0.90f, topLeft.y + topFaceH * 0.44f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = skirt.copy(alpha = 0.28f * alpha),
                start = Offset(topLeft.x + w * 0.14f, topLeft.y + topFaceH * 0.68f),
                end = Offset(topLeft.x + w * 0.86f, topLeft.y + topFaceH * 0.63f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )
            val pebbleOff = if ((gx + gy) % 2 == 0) 0.26f else 0.72f
            drawCircle(
                color = Color(0xFFD97706).copy(alpha = 0.45f * alpha),
                radius = w * 0.055f,
                center = Offset(topLeft.x + w * pebbleOff, topLeft.y + topFaceH * 0.26f)
            )
            drawCircle(
                color = skirt.copy(alpha = 0.55f * alpha),
                radius = w * 0.045f,
                center = Offset(topLeft.x + w * (1f - pebbleOff), topLeft.y + topFaceH * 0.54f)
            )
        }

        BlockKind.STONE -> {
            // Carved ashlar stone brick mortar lines
            val midY = topLeft.y + topFaceH * 0.50f
            drawLine(
                color = skirt.copy(alpha = 0.55f * alpha),
                start = Offset(topLeft.x + w * 0.06f, midY),
                end = Offset(topLeft.x + w * 0.94f, midY),
                strokeWidth = 2f
            )
            drawLine(
                color = skirt.copy(alpha = 0.55f * alpha),
                start = Offset(topLeft.x + w * 0.52f, topLeft.y + 3f),
                end = Offset(topLeft.x + w * 0.52f, midY),
                strokeWidth = 2f
            )
            drawLine(
                color = skirt.copy(alpha = 0.55f * alpha),
                start = Offset(topLeft.x + w * 0.28f, midY),
                end = Offset(topLeft.x + w * 0.28f, topLeft.y + topFaceH - 2f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.16f * alpha),
                start = Offset(topLeft.x + w * 0.10f, midY + 2f),
                end = Offset(topLeft.x + w * 0.88f, midY + 2f),
                strokeWidth = 1.2f
            )
        }

        BlockKind.IRON -> {
            // Forged metallic plate with brushed diagonal sheen + 4 bolted corner rivets
            drawLine(
                color = Color.White.copy(alpha = 0.24f * alpha),
                start = Offset(topLeft.x + w * 0.16f, topLeft.y + topFaceH * 0.76f),
                end = Offset(topLeft.x + w * 0.76f, topLeft.y + topFaceH * 0.16f),
                strokeWidth = w * 0.11f,
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = skirt.copy(alpha = 0.55f * alpha),
                topLeft = Offset(topLeft.x + w * 0.10f, topLeft.y + topFaceH * 0.11f),
                size = Size(w * 0.80f, topFaceH * 0.76f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = 1.8f)
            )
            val rivetR = (w * 0.045f).coerceAtLeast(2f)
            val rx0 = topLeft.x + w * 0.20f
            val rx1 = topLeft.x + w * 0.80f
            val ry0 = topLeft.y + topFaceH * 0.22f
            val ry1 = topLeft.y + topFaceH * 0.74f
            val skirtCol = skirt.copy(alpha = 0.85f * alpha)
            val specCol = Color.White.copy(alpha = 0.70f * alpha)
            drawCircle(color = skirtCol, radius = rivetR + 1f, center = Offset(rx0, ry0))
            drawCircle(color = specCol, radius = rivetR * 0.65f, center = Offset(rx0 - 0.6f, ry0 - 0.6f))
            drawCircle(color = skirtCol, radius = rivetR + 1f, center = Offset(rx1, ry0))
            drawCircle(color = specCol, radius = rivetR * 0.65f, center = Offset(rx1 - 0.6f, ry0 - 0.6f))
            drawCircle(color = skirtCol, radius = rivetR + 1f, center = Offset(rx0, ry1))
            drawCircle(color = specCol, radius = rivetR * 0.65f, center = Offset(rx0 - 0.6f, ry1 - 0.6f))
            drawCircle(color = skirtCol, radius = rivetR + 1f, center = Offset(rx1, ry1))
            drawCircle(color = specCol, radius = rivetR * 0.65f, center = Offset(rx1 - 0.6f, ry1 - 0.6f))
        }

        BlockKind.CRYSTAL -> {
            // Protruding multi-faceted crystalline amethyst/cyan spires + pulsing sparkle
            scratchPath.rewind()
            scratchPath.moveTo(center.x, topLeft.y + topFaceH * 0.10f)
            scratchPath.lineTo(center.x + w * 0.22f, topLeft.y + topFaceH * 0.42f)
            scratchPath.lineTo(center.x + w * 0.14f, topLeft.y + topFaceH * 0.78f)
            scratchPath.lineTo(center.x - w * 0.14f, topLeft.y + topFaceH * 0.78f)
            scratchPath.lineTo(center.x - w * 0.22f, topLeft.y + topFaceH * 0.42f)
            scratchPath.close()
            drawPath(
                path = scratchPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f * alpha), CrystalCyan.copy(alpha = 0.75f * alpha)),
                    startY = topLeft.y + topFaceH * 0.10f,
                    endY = topLeft.y + topFaceH * 0.78f
                )
            )
            drawLine(
                color = Color.White.copy(alpha = 0.88f * pulse * alpha),
                start = Offset(center.x, topLeft.y + topFaceH * 0.14f),
                end = Offset(center.x, topLeft.y + topFaceH * 0.70f),
                strokeWidth = 2f
            )
        }

        BlockKind.GAMBLE -> {
            // Ornate gilded vault border + central shimmering treasure diamond
            drawRoundRect(
                color = Color(0xFFFEF08A).copy(alpha = 0.72f * alpha),
                topLeft = Offset(topLeft.x + w * 0.10f, topLeft.y + topFaceH * 0.10f),
                size = Size(w * 0.80f, topFaceH * 0.78f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                style = Stroke(width = 2.2f)
            )
            if (block.special == null) {
                drawDiamond(
                    center = center,
                    radius = w * 0.24f * (0.94f + 0.08f * pulse),
                    fill = Color(0xFFFEF08A).copy(alpha = alpha),
                    highlight = Color.White.copy(alpha = 0.90f * alpha),
                    scratchPath = scratchPath
                )
            }
        }
    }

    // 6. Special Block High-Contrast Vector Emblems
    when (block.special) {
        BlockSpecial.EXPLOSIVE -> {
            // 3D Crimson TNT Stick Bundle + Hazard Collar + Curved Fuse & Crackling 4-Point Spark
            val tntLeft = topLeft.x + w * 0.22f
            val tntTop = topLeft.y + topFaceH * 0.26f
            val tntW = w * 0.56f
            val tntH = topFaceH * 0.54f
            drawRoundRect(
                color = Color(0xFF991B1B).copy(alpha = alpha),
                topLeft = Offset(tntLeft + 2f, tntTop + 3f),
                size = Size(tntW, tntH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = TileExplosive.copy(alpha = 0.96f * alpha),
                topLeft = Offset(tntLeft, tntTop),
                size = Size(tntW, tntH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Vertical TNT stick ribs
            for (rIdx in 1..2) {
                val rx = tntLeft + (tntW / 3f) * rIdx
                drawLine(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.8f * alpha),
                    start = Offset(rx, tntTop + 2f),
                    end = Offset(rx, tntTop + tntH - 2f),
                    strokeWidth = 1.8f
                )
            }
            // Bold dark-gold center collar band
            drawRoundRect(
                color = ObsidianDeep.copy(alpha = 0.90f * alpha),
                topLeft = Offset(tntLeft - 1f, tntTop + tntH * 0.34f),
                size = Size(tntW + 2f, tntH * 0.30f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawLine(
                color = ForgedGold.copy(alpha = 0.95f * alpha),
                start = Offset(tntLeft + tntW * 0.16f, tntTop + tntH * 0.49f),
                end = Offset(tntLeft + tntW * 0.84f, tntTop + tntH * 0.49f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            // Curved fuse wick + flickering 4-point star spark
            val fuseTip = Offset(center.x + w * 0.14f, topLeft.y + topFaceH * 0.12f)
            drawLine(
                color = Color(0xFFE2E8F0).copy(alpha = alpha),
                start = Offset(center.x, tntTop),
                end = fuseTip,
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            val sparkR = w * 0.12f * (0.75f + 0.35f * pulse)
            drawCircle(color = VolcanicOrange.copy(alpha = 0.65f * pulse * alpha), radius = sparkR * 1.35f, center = fuseTip)
            drawLine(ForgedGold.copy(alpha = alpha), Offset(fuseTip.x - sparkR, fuseTip.y), Offset(fuseTip.x + sparkR, fuseTip.y), strokeWidth = 2f)
            drawLine(ForgedGold.copy(alpha = alpha), Offset(fuseTip.x, fuseTip.y - sparkR), Offset(fuseTip.x, fuseTip.y + sparkR), strokeWidth = 2f)
            drawCircle(color = Color.White.copy(alpha = alpha), radius = w * 0.045f, center = fuseTip)
        }

        BlockSpecial.ICE -> {
            // Frosted rim + 6-point crystalline snowflake star
            drawRoundRect(
                color = TileIce.copy(alpha = 0.65f * alpha),
                topLeft = topLeft,
                size = topFaceSize,
                cornerRadius = corner,
                style = Stroke(width = 2.4f)
            )
            val r = w * 0.24f
            for (k in 0 until 3) {
                val ang = k * (PI / 3.0)
                val dx = (cos(ang) * r).toFloat()
                val dy = (sin(ang) * r).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.90f * alpha),
                    start = Offset(center.x - dx, center.y - dy),
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = TileIce.copy(alpha = alpha), radius = w * 0.07f, center = center)
        }

        BlockSpecial.LAVA -> {
            // Radiant magma lattice glowing between volcanic basalt plates
            drawRoundRect(
                color = VolcanicOrange.copy(alpha = 0.28f * pulse * alpha),
                topLeft = topLeft,
                size = topFaceSize,
                cornerRadius = corner
            )
            drawRoundRect(
                color = TileLava.copy(alpha = 0.75f * pulse * alpha),
                topLeft = topLeft,
                size = topFaceSize,
                cornerRadius = corner,
                style = Stroke(width = 2.4f)
            )
            scratchPath.rewind()
            scratchPath.moveTo(topLeft.x + w * 0.14f, topLeft.y + topFaceH * 0.22f)
            scratchPath.lineTo(center.x - w * 0.06f, center.y)
            scratchPath.lineTo(topLeft.x + w * 0.86f, topLeft.y + topFaceH * 0.28f)
            scratchPath.moveTo(center.x - w * 0.06f, center.y)
            scratchPath.lineTo(topLeft.x + w * 0.58f, topLeft.y + topFaceH * 0.82f)
            scratchPath.moveTo(center.x - w * 0.06f, center.y)
            scratchPath.lineTo(topLeft.x + w * 0.18f, topLeft.y + topFaceH * 0.74f)
            drawPath(
                path = scratchPath,
                color = TileLava.copy(alpha = 0.96f * alpha),
                style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = scratchPath,
                color = Color(0xFFFEF08A).copy(alpha = pulse * alpha),
                style = Stroke(width = w * 0.048f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        BlockSpecial.LOCKED -> {
            // Heavy golden padlock body + metallic U-shackle + keyhole
            val shackleR = w * 0.13f
            drawCircle(
                color = Color(0xFFE2E8F0).copy(alpha = alpha),
                radius = shackleR,
                center = Offset(center.x, center.y - h * 0.10f),
                style = Stroke(width = w * 0.055f)
            )
            drawRoundRect(
                color = TileLocked.copy(alpha = alpha),
                topLeft = Offset(center.x - w * 0.18f, center.y - h * 0.06f),
                size = Size(w * 0.36f, h * 0.28f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawCircle(
                color = ObsidianDeep.copy(alpha = alpha),
                radius = w * 0.05f,
                center = Offset(center.x, center.y + h * 0.05f)
            )
        }

        else -> Unit
    }

    // 7. Progressive Multi-Stage Fracture Cracks + High-Contrast Emissive Stress Sheen
    val damageTaken = block.max - block.hp
    if (damageTaken > 0) {
        // Subtle luminous lift on damaged blocks so cracked targets pop instantly at speed
        drawRoundRect(
            color = Color.White.copy(alpha = (0.11f + 0.05f * damageTaken).coerceAtMost(0.24f) * alpha),
            topLeft = topLeft,
            size = topFaceSize,
            cornerRadius = corner
        )
        scratchPath.rewind()
        scratchPath.moveTo(topLeft.x + w * 0.20f, topLeft.y + topFaceH * 0.18f)
        scratchPath.lineTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
        scratchPath.lineTo(topLeft.x + w * 0.34f, topLeft.y + topFaceH * 0.74f)
        if (damageTaken >= 2) {
            scratchPath.moveTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
            scratchPath.lineTo(topLeft.x + w * 0.80f, topLeft.y + topFaceH * 0.34f)
            scratchPath.moveTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
            scratchPath.lineTo(topLeft.x + w * 0.72f, topLeft.y + topFaceH * 0.76f)
        }
        drawPath(
            path = scratchPath,
            color = ForgedGold.copy(alpha = 0.42f * alpha),
            style = Stroke(width = (w * 0.085f).coerceAtLeast(3.4f), cap = StrokeCap.Round)
        )
        drawPath(
            path = scratchPath,
            color = ObsidianDeep.copy(alpha = 0.90f * alpha),
            style = Stroke(width = (w * 0.052f).coerceAtLeast(2.2f), cap = StrokeCap.Round)
        )
    }

    // 8. Sculpted HP Bar Pips embedded cleanly along lower extruded skirt
    val pipCount = block.max
    val pipWidth = (w * 0.74f) / pipCount
    val startX = topLeft.x + w * 0.13f
    val pipY = topLeft.y + h * 0.83f
    val pipHeight = (h * 0.105f).coerceAtLeast(3f)
    for (i in 0 until pipCount) {
        val filled = i < block.hp
        drawRoundRect(
            color = if (filled) Color.White.copy(alpha = 0.94f * alpha) else ObsidianDeep.copy(alpha = 0.72f * alpha),
            topLeft = Offset(startX + i * pipWidth + 1.5f, pipY),
            size = Size((pipWidth - 3f).coerceAtLeast(2.5f), pipHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }

    // 9. Target Lock / Revealed Hidden Glow Outline
    if (isTarget || isRevealedHidden) {
        drawRoundRect(
            color = if (isTarget) ForgedGold else CrystalCyan,
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = corner,
            style = Stroke(width = if (isTarget) 4f else 2.6f)
        )
    }
}

private fun DrawScope.drawRelayBeaconPylon(
    center: Offset,
    inner: Float,
    pulse: Float,
    orbitAngle: Float,
    alpha: Float,
    playerCleared: Boolean,
    rivalCleared: Boolean,
    scratchPath: Path
) {
    if (playerCleared) {
        // Energized captured beacon pad (Emerald)
        drawCircle(
            color = EmeraldGem.copy(alpha = 0.22f * alpha),
            radius = inner * 0.36f,
            center = center
        )
        drawCircle(
            color = EmeraldGem.copy(alpha = 0.80f * alpha),
            radius = inner * 0.24f,
            center = center,
            style = Stroke(width = inner * 0.055f)
        )
        drawDiamond(
            center = center,
            radius = inner * 0.14f,
            fill = EmeraldGem.copy(alpha = 0.92f * alpha),
            highlight = Color.White.copy(alpha = 0.85f * alpha),
            scratchPath = scratchPath
        )
        return
    }

    // Uncaptured Strategic Relay Beacon Pylon: pulsing radiant halo + rotating Gold/Cyan energy rings
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ForgedGold.copy(alpha = (0.48f + 0.18f * pulse) * alpha),
                CrystalCyan.copy(alpha = 0.22f * pulse * alpha),
                Color.Transparent
            ),
            center = center,
            radius = inner * 0.48f
        ),
        radius = inner * 0.48f,
        center = center
    )
    // Outer ring turns crimson-amber if Rival already captured this beacon (competitive alert!)
    drawCircle(
        color = (if (rivalCleared) RivalCrimson else ForgedGold).copy(alpha = 0.88f * pulse * alpha),
        radius = inner * 0.36f,
        center = center,
        style = Stroke(width = inner * 0.065f)
    )
    // Inner spinning cyan energy ring
    drawCircle(
        color = CrystalCyan.copy(alpha = 0.86f * alpha),
        radius = inner * 0.24f * (0.92f + 0.10f * pulse),
        center = center,
        style = Stroke(width = inner * 0.045f)
    )
    // Rotating energy nodes
    for (i in 0 until 3) {
        val a = -orbitAngle * 1.15 + i * (2.0 * PI / 3.0)
        val nx = center.x + (cos(a) * inner * 0.34f).toFloat()
        val ny = center.y + (sin(a) * inner * 0.34f).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.92f * alpha),
            radius = inner * 0.05f,
            center = Offset(nx, ny)
        )
    }
    // Radiant golden core crystal
    drawDiamond(
        center = center,
        radius = inner * 0.21f * (0.94f + 0.08f * pulse),
        fill = ForgedGold.copy(alpha = 0.96f * alpha),
        highlight = Color.White.copy(alpha = 0.95f * alpha),
        scratchPath = scratchPath
    )
}

private fun DrawScope.drawArcaneFinishPortal(
    topLeft: Offset,
    rectSize: Size,
    center: Offset,
    corner: CornerRadius,
    pulse: Float,
    orbitAngle: Float,
    alpha: Float,
    isSealed: Boolean = false
) {
    if (isSealed) {
        // Sealed Portal State: Obsidian-Amber containment field with iron-gold barrier rings
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF78350F), Color(0xFF3B1D0A), ObsidianDeep),
                center = center,
                radius = rectSize.width * 0.68f
            ),
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = corner,
            alpha = alpha
        )
        drawCircle(
            color = ForgedGold.copy(alpha = 0.58f * pulse * alpha),
            radius = rectSize.width * 0.37f,
            center = center,
            style = Stroke(width = rectSize.width * 0.065f)
        )
        // Crossed barrier bars indicating locked portal
        val r = rectSize.width * 0.24f
        drawLine(
            color = VolcanicOrange.copy(alpha = 0.85f * alpha),
            start = Offset(center.x - r, center.y - r),
            end = Offset(center.x + r, center.y + r),
            strokeWidth = rectSize.width * 0.065f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = VolcanicOrange.copy(alpha = 0.85f * alpha),
            start = Offset(center.x + r, center.y - r),
            end = Offset(center.x - r, center.y + r),
            strokeWidth = rectSize.width * 0.065f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = ForgedGold.copy(alpha = 0.90f * alpha),
            radius = rectSize.width * 0.10f,
            center = center
        )
        return
    }

    // Deep radiant cyan-emerald vortex well
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, CrystalCyan, EmeraldGem, ObsidianDeep),
            center = center,
            radius = rectSize.width * 0.72f
        ),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner,
        alpha = alpha
    )
    // Outer swirling cyan-emerald vortex ring
    drawCircle(
        color = CrystalCyan.copy(alpha = 0.88f * pulse * alpha),
        radius = rectSize.width * 0.39f,
        center = center,
        style = Stroke(width = rectSize.width * 0.075f)
    )
    // Inner glowing white-emerald core ring
    drawCircle(
        color = Color.White.copy(alpha = 0.92f * pulse * alpha),
        radius = rectSize.width * 0.24f * (0.90f + 0.12f * pulse),
        center = center,
        style = Stroke(width = rectSize.width * 0.06f)
    )
    drawCircle(
        color = EmeraldGem.copy(alpha = 0.95f * alpha),
        radius = rectSize.width * 0.13f,
        center = center
    )
    // 4 counter-rotating cyan & gold spiral portal wisps
    for (i in 0 until 4) {
        val a = orbitAngle + i * (2.0 * PI / 4.0)
        val wx = center.x + (cos(a) * rectSize.width * 0.34f).toFloat()
        val wy = center.y + (sin(a) * rectSize.width * 0.34f).toFloat()
        drawCircle(
            color = (if (i % 2 == 0) Color.White else ForgedGold).copy(alpha = 0.92f * alpha),
            radius = rectSize.width * 0.055f,
            center = Offset(wx, wy)
        )
    }
}

private fun DrawScope.drawFacetedGem(
    center: Offset,
    radius: Float,
    alpha: Float,
    pulse: Float,
    scratchPath: Path
) {
    drawCircle(
        color = CrystalCyan.copy(alpha = 0.24f * pulse * alpha),
        radius = radius * 1.35f,
        center = center
    )
    drawDiamond(
        center = center,
        radius = radius,
        fill = CrystalCyan.copy(alpha = alpha),
        highlight = Color.White.copy(alpha = 0.90f * alpha),
        scratchPath = scratchPath
    )
    // Inner crown facet line
    scratchPath.rewind()
    scratchPath.moveTo(center.x, center.y - radius * 0.55f)
    scratchPath.lineTo(center.x + radius * 0.45f, center.y)
    scratchPath.lineTo(center.x, center.y + radius * 0.55f)
    scratchPath.lineTo(center.x - radius * 0.45f, center.y)
    scratchPath.close()
    drawPath(
        path = scratchPath,
        color = Color.White.copy(alpha = 0.42f * alpha),
        style = Stroke(width = 1.6f)
    )
}

private fun DrawScope.drawPowerupOrb(
    power: PowerupType,
    center: Offset,
    inner: Float,
    pulse: Float,
    alpha: Float,
    scratchPath: Path
) {
    val pColor = when (power) {
        PowerupType.PICK -> ForgedGold
        PowerupType.SPEED -> CrystalCyan
        PowerupType.LANTERN -> Color(0xFFFDE047)
        PowerupType.SHIELD -> EmeraldGem
        PowerupType.SONAR -> MysticAmethyst
        PowerupType.CLOCK -> VolcanicOrange
    }
    drawCircle(
        color = pColor.copy(alpha = 0.28f * pulse * alpha),
        radius = inner * 0.42f,
        center = center
    )
    drawCircle(
        color = ObsidianDeep.copy(alpha = 0.85f * alpha),
        radius = inner * 0.28f,
        center = center
    )
    drawCircle(
        color = pColor.copy(alpha = alpha),
        radius = inner * 0.28f,
        center = center,
        style = Stroke(width = inner * 0.065f)
    )

    val r = inner * 0.14f
    when (power) {
        PowerupType.SPEED -> {
            // Lightning bolt
            scratchPath.rewind()
            scratchPath.moveTo(center.x + r * 0.25f, center.y - r)
            scratchPath.lineTo(center.x - r * 0.55f, center.y + r * 0.1f)
            scratchPath.lineTo(center.x + r * 0.1f, center.y + r * 0.1f)
            scratchPath.lineTo(center.x - r * 0.25f, center.y + r)
            drawPath(scratchPath, color = Color.White.copy(alpha = alpha), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        }
        PowerupType.CLOCK -> {
            // Clock hands
            drawLine(Color.White.copy(alpha = alpha), center, Offset(center.x, center.y - r * 0.85f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = alpha), center, Offset(center.x + r * 0.65f, center.y + r * 0.2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
        PowerupType.SHIELD -> {
            // Shield crest
            scratchPath.rewind()
            scratchPath.moveTo(center.x, center.y - r * 0.9f)
            scratchPath.lineTo(center.x + r * 0.8f, center.y - r * 0.35f)
            scratchPath.lineTo(center.x, center.y + r * 0.95f)
            scratchPath.lineTo(center.x - r * 0.8f, center.y - r * 0.35f)
            scratchPath.close()
            drawPath(scratchPath, color = Color.White.copy(alpha = alpha))
        }
        PowerupType.SONAR -> {
            drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.35f, center = center)
            drawCircle(Color.White.copy(alpha = 0.75f * alpha), radius = r * 0.85f, center = center, style = Stroke(width = 1.8f))
        }
        PowerupType.LANTERN -> {
            drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.65f, center = center)
        }
        PowerupType.PICK -> {
            // Crossed pickaxe head
            drawLine(Color.White.copy(alpha = alpha), Offset(center.x - r * 0.7f, center.y + r * 0.7f), Offset(center.x + r * 0.65f, center.y - r * 0.65f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = alpha), Offset(center.x - r * 0.1f, center.y - r * 0.85f), Offset(center.x + r * 0.85f, center.y + r * 0.1f), strokeWidth = 2.6f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawPlayerMinerAvatar(
    center: Offset,
    cellSize: Float,
    facingAngle: Float,
    overdrive: Boolean
) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = cellSize * 0.36f,
        center = Offset(center.x + 2f, center.y + 3f)
    )
    // Explorer backpack / shoulder armor
    drawCircle(
        color = if (overdrive) VolcanicOrange else Color(0xFF334155),
        radius = cellSize * 0.35f,
        center = center
    )
    // Forged Golden Hard Hat
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF08A), ForgedGold, Color(0xFFB45309)),
            center = Offset(center.x - cellSize * 0.08f, center.y - cellSize * 0.08f),
            radius = cellSize * 0.34f
        ),
        radius = cellSize * 0.29f,
        center = center
    )
    // Helmet Ridge Crest
    drawLine(
        color = Color(0xFFB45309),
        start = Offset(center.x - cos(facingAngle) * cellSize * 0.22f, center.y - sin(facingAngle) * cellSize * 0.22f),
        end = Offset(center.x + cos(facingAngle) * cellSize * 0.18f, center.y + sin(facingAngle) * cellSize * 0.18f),
        strokeWidth = cellSize * 0.06f,
        cap = StrokeCap.Round
    )
    // Forward-Facing Glowing Cyan Headlamp Lens
    val lampPos = Offset(
        center.x + cos(facingAngle) * cellSize * 0.21f,
        center.y + sin(facingAngle) * cellSize * 0.21f
    )
    drawCircle(
        color = ObsidianDeep,
        radius = cellSize * 0.11f,
        center = lampPos
    )
    drawCircle(
        color = CrystalCyan,
        radius = cellSize * 0.075f,
        center = lampPos
    )
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.032f,
        center = lampPos
    )
}

private fun DrawScope.drawRivalCyberDriller(
    center: Offset,
    inner: Float,
    pulse: Float,
    orbitAngle: Float
) {
    drawCircle(
        color = RivalCrimson.copy(alpha = 0.30f * pulse),
        radius = inner * 0.46f,
        center = center
    )
    // Armored treads
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(center.x - inner * 0.34f, center.y - inner * 0.28f),
        size = Size(inner * 0.68f, inner * 0.56f),
        cornerRadius = CornerRadius(inner * 0.16f, inner * 0.16f)
    )
    // Crimson cyber hull
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFB7185), RivalCrimson, Color(0xFF881337)),
            center = Offset(center.x - inner * 0.06f, center.y - inner * 0.06f),
            radius = inner * 0.32f
        ),
        radius = inner * 0.28f,
        center = center
    )
    // Glowing scanner visor
    drawRoundRect(
        color = ObsidianDeep,
        topLeft = Offset(center.x - inner * 0.17f, center.y - inner * 0.08f),
        size = Size(inner * 0.34f, inner * 0.13f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val scanX = center.x + (sin(orbitAngle * 2f) * inner * 0.09f)
    drawCircle(
        color = Color(0xFFFEF08A),
        radius = inner * 0.05f,
        center = Offset(scanX, center.y - inner * 0.015f)
    )
}

/**
 * Subtle full-bleed atmospheric perimeter vignette tailored to each biome so every environment
 * immediately feels distinct without obscuring center gameplay clarity.
 */
private fun DrawScope.drawBiomeAtmosphereVignette(
    biome: Biome,
    palette: BiomeVisualPalette,
    pulse: Float
) {
    val maxR = kotlin.math.max(size.width, size.height) * 0.78f
    val edgeAlpha = when (biome) {
        Biome.VOLCANO -> 0.24f * (0.85f + 0.15f * pulse)
        Biome.ICE -> 0.20f
        Biome.DESERT -> 0.22f
        Biome.CAVERN -> 0.24f
        Biome.RUINS -> 0.22f * (0.88f + 0.12f * pulse)
    }
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.56f to Color.Transparent,
                1.0f to palette.vignetteTint.copy(alpha = edgeAlpha)
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxR
        ),
        size = size
    )
    // Extra thermal under-glow in Volcano or aurora top sheen in Arctic Ice
    if (biome == Biome.VOLCANO) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    VolcanicOrange.copy(alpha = 0.16f * pulse)
                )
            ),
            size = size
        )
    } else if (biome == Biome.ICE) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TileIce.copy(alpha = 0.14f * pulse),
                    Color.Transparent,
                    Color.Transparent
                )
            ),
            size = size
        )
    }
}

/**
 * Bespoke animated particle effects for all 5 biomes:
 * - VOLCANO: Rising flickering lava sparkles, glowing embers & upward thermal trails
 * - ICE: Spinning razor-sharp crystalline ice shards & 6-point arctic snow stars
 * - DESERT: Sweeping diagonal sandstorm streaks & shimmering sunlit gold mica motes
 * - CAVERN: Pulsing bioluminescent cyan/emerald fireflies & rising crystal spores
 * - RUINS: Floating golden arcane rune diamonds & glowing emerald/violet spirit wisps
 */
private fun DrawScope.drawAmbientBiomeParticles(
    biome: Biome,
    primary: Color,
    secondary: Color,
    phase: Float,
    pulse: Float,
    scratchPath: Path
) {
    val count = 28
    for (i in 0 until count) {
        val seedX = ((i * 73 + 19) % 100) / 100f
        val seedY = ((i * 41 + 7) % 100) / 100f
        val speed = 0.55f + (i % 4) * 0.28f
        val t = (phase * speed + seedY) % 1f
        val fade = sin(t * PI).toFloat().coerceIn(0f, 1f)

        when (biome) {
            Biome.VOLCANO -> {
                // Rising glowing lava sparkles & trailing embers
                val px = (seedX + sin((t * 4.0 * PI + i * 1.3).toDouble()).toFloat() * 0.045f) * size.width
                val py = (1f - t) * size.height
                val flicker = 0.55f + 0.45f * sin((phase * 12.0 * PI + i * 2.1).toDouble()).toFloat()
                val a = (fade * flicker * 0.82f).coerceIn(0f, 0.9f)
                val r = 2.2f + (i % 3) * 1.4f

                // Ember heat trail
                drawLine(
                    color = secondary.copy(alpha = a * 0.52f),
                    start = Offset(px, py),
                    end = Offset(px - sin((t * 4.0 * PI + i).toDouble()).toFloat() * 6f, py + r * 3.8f),
                    strokeWidth = r * 0.85f,
                    cap = StrokeCap.Round
                )
                // Fiery outer halo
                drawCircle(
                    color = secondary.copy(alpha = a * 0.42f),
                    radius = r * 2.1f,
                    center = Offset(px, py)
                )
                // White-hot lava spark core
                drawCircle(
                    color = primary.copy(alpha = a),
                    radius = r * 0.85f,
                    center = Offset(px, py)
                )
                // Occasional 4-point volcanic sparkle flash
                if (i % 4 == 0) {
                    val sparkLen = r * 2.4f * flicker
                    drawLine(primary.copy(alpha = a * 0.9f), Offset(px - sparkLen, py), Offset(px + sparkLen, py), strokeWidth = 1.4f)
                    drawLine(primary.copy(alpha = a * 0.9f), Offset(px, py - sparkLen), Offset(px, py + sparkLen), strokeWidth = 1.4f)
                }
            }

            Biome.ICE -> {
                // Spinning crystalline ice shards & 6-point arctic frost crystals
                val px = ((seedX + sin((t * 2.5 * PI + i).toDouble()).toFloat() * 0.04f + phase * 0.08f) % 1f) * size.width
                val py = t * size.height
                val a = (fade * 0.78f).coerceIn(0f, 0.85f)
                val rot = (phase * 2.0 * PI * (if (i % 2 == 0) 1.0 else -1.0) + i * 0.9)
                val r = 3.8f + (i % 4) * 1.6f

                if (i % 3 != 0) {
                    // Sharp spinning diamond ice shard (reusing scratchPath)
                    val cosR = cos(rot).toFloat()
                    val sinR = sin(rot).toFloat()
                    scratchPath.rewind()
                    scratchPath.moveTo(px + cosR * r * 1.45f, py + sinR * r * 1.45f)
                    scratchPath.lineTo(px - sinR * r * 0.55f, py + cosR * r * 0.55f)
                    scratchPath.lineTo(px - cosR * r * 1.45f, py - sinR * r * 1.45f)
                    scratchPath.lineTo(px + sinR * r * 0.55f, py - cosR * r * 0.55f)
                    scratchPath.close()
                    drawPath(scratchPath, color = secondary.copy(alpha = a * 0.65f))
                    drawPath(scratchPath, color = primary.copy(alpha = a * 0.95f), style = Stroke(width = 1.2f))
                } else {
                    // 6-point crystalline frost star
                    for (k in 0 until 3) {
                        val ang = rot + k * (PI / 3.0)
                        val dx = (cos(ang) * r * 1.1f).toFloat()
                        val dy = (sin(ang) * r * 1.1f).toFloat()
                        drawLine(
                            color = primary.copy(alpha = a * 0.88f),
                            start = Offset(px - dx, py - dy),
                            end = Offset(px + dx, py + dy),
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(color = Color.White.copy(alpha = a), radius = 1.6f, center = Offset(px, py))
                }
            }

            Biome.DESERT -> {
                // Sweeping diagonal sandstorm dust streaks & sunlit golden motes
                val px = ((seedX + phase * (0.75f + (i % 3) * 0.25f)) % 1f) * size.width
                val py = ((seedY + phase * 0.22f + sin((t * 2.0 * PI + i).toDouble()).toFloat() * 0.025f) % 1f) * size.height
                val a = (fade * 0.68f).coerceIn(0f, 0.78f)
                if (i % 3 != 0) {
                    val streakLen = 14f + (i % 4) * 7f
                    drawLine(
                        color = secondary.copy(alpha = a * 0.55f),
                        start = Offset(px - streakLen, py - streakLen * 0.28f),
                        end = Offset(px, py),
                        strokeWidth = 1.8f + (i % 2) * 0.9f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = primary.copy(alpha = a * 0.85f),
                        radius = 1.8f,
                        center = Offset(px, py)
                    )
                } else {
                    // Sunlit desert mica sparkle
                    val r = 2.4f + (i % 2) * 1.2f
                    drawCircle(color = primary.copy(alpha = a * 0.45f), radius = r * 2.0f, center = Offset(px, py))
                    drawCircle(color = Color.White.copy(alpha = a * 0.90f), radius = r * 0.7f, center = Offset(px, py))
                }
            }

            Biome.CAVERN -> {
                // Pulsing bioluminescent fireflies & rising subterranean crystal spores
                val px = (seedX + cos((phase * 2.0 * PI * (0.7 + (i % 3) * 0.3) + i * 1.4).toDouble()).toFloat() * 0.06f) * size.width
                val py = ((seedY - phase * 0.16f + sin((phase * 2.0 * PI + i).toDouble()).toFloat() * 0.045f + 2f) % 1f) * size.height
                val bioPulse = 0.40f + 0.60f * sin((phase * 6.0 * PI + i * 1.7).toDouble()).toFloat().coerceAtLeast(0f)
                val col = if (i % 3 == 0) secondary else primary
                val a = (fade * bioPulse * 0.85f).coerceIn(0f, 0.90f)
                val r = 2.4f + (i % 3) * 1.3f

                drawCircle(
                    color = col.copy(alpha = a * 0.30f),
                    radius = r * 3.1f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = col.copy(alpha = a * 0.85f),
                    radius = r * 1.15f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = r * 0.45f,
                    center = Offset(px, py)
                )
            }

            Biome.RUINS -> {
                // Floating golden arcane rune diamonds & emerald spirit wisps
                val px = (seedX + sin((t * 2.0 * PI + i * 1.1).toDouble()).toFloat() * 0.035f) * size.width
                val py = (1f - t) * size.height
                val a = (fade * 0.78f * (0.75f + 0.25f * pulse)).coerceIn(0f, 0.85f)
                val r = 3.4f + (i % 3) * 1.5f

                if (i % 2 == 0) {
                    // Floating golden arcane diamond rune (reusing scratchPath)
                    scratchPath.rewind()
                    scratchPath.moveTo(px, py - r * 1.3f)
                    scratchPath.lineTo(px + r * 0.95f, py)
                    scratchPath.lineTo(px, py + r * 1.3f)
                    scratchPath.lineTo(px - r * 0.95f, py)
                    scratchPath.close()
                    drawPath(scratchPath, color = primary.copy(alpha = a * 0.28f))
                    drawPath(scratchPath, color = primary.copy(alpha = a * 0.92f), style = Stroke(width = 1.4f))
                    drawCircle(color = Color.White.copy(alpha = a * 0.85f), radius = 1.4f, center = Offset(px, py))
                } else {
                    // Rising emerald-amethyst spirit wisp
                    val wispCol = if (i % 4 == 1) secondary else MysticAmethyst
                    drawCircle(color = wispCol.copy(alpha = a * 0.34f), radius = r * 2.4f, center = Offset(px, py))
                    drawCircle(color = wispCol.copy(alpha = a * 0.88f), radius = r * 0.90f, center = Offset(px, py))
                    drawLine(
                        color = wispCol.copy(alpha = a * 0.45f),
                        start = Offset(px, py),
                        end = Offset(px, py + r * 3.4f),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTacticalMiniMap(
    race: LiveRaceState,
    exploredCoords: List<Triple<String, Int, Int>>,
    gemCoords: List<Triple<String, Int, Int>>,
    pulse: Float,
    biomeAccent: Color,
    scratchPath: Path
) {
    val mapSize = (min(size.width, size.height) * 0.22f).coerceIn(72f, 112f)
    val marginRight = 14.dp.toPx()
    val marginTop = 204.dp.toPx()
    if (size.height < marginTop + mapSize + 110f) return

    val originX = size.width - mapSize - marginRight
    val originY = marginTop
    val cell = mapSize / race.boardSize.toFloat()

    drawRoundRect(
        color = ObsidianDeep.copy(alpha = 0.78f),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = biomeAccent.copy(alpha = 0.48f),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 1.6f)
    )

    for ((k, x, y) in exploredCoords) {
        val px = originX + x * cell
        val py = originY + y * cell
        val isWall = race.blocks.containsKey(k)
        drawRect(
            color = if (isWall) Color(0xFF475569).copy(alpha = 0.58f) else Color(0xFF94A3B8).copy(alpha = 0.30f),
            topLeft = Offset(px, py),
            size = Size(cell.coerceAtLeast(1.5f), cell.coerceAtLeast(1.5f))
        )
    }

    for ((gKey, gx, gy) in gemCoords) {
        if (gKey in race.explored || race.sonarActive) {
            drawCircle(
                color = CrystalCyan,
                radius = (cell * 0.55f).coerceAtLeast(2f),
                center = Offset(originX + (gx + 0.5f) * cell, originY + (gy + 0.5f) * cell)
            )
        }
    }

    // Plot Strategic Relay Beacons on Tactical Mini-Map so players can plan multi-sector routes
    for (cp in race.checkpoints) {
        val cx = originX + (cp.x + 0.5f) * cell
        val cy = originY + (cp.y + 0.5f) * cell
        val cleared = cp.key in race.playerCheckpointsCleared
        if (cleared) {
            drawCircle(
                color = EmeraldGem.copy(alpha = 0.78f),
                radius = (cell * 0.65f).coerceAtLeast(2.2f),
                center = Offset(cx, cy)
            )
        } else {
            drawDiamond(
                center = Offset(cx, cy),
                radius = (cell * 1.15f).coerceAtLeast(3.8f) * (0.90f + 0.15f * pulse),
                fill = ForgedGold,
                highlight = Color.White,
                scratchPath = scratchPath
            )
        }
    }

    // Finish Portal on Mini-Map (Amber when sealed, pulsing Emerald when unlocked!)
    drawCircle(
        color = (if (race.portalUnlocked) EmeraldGem else VolcanicOrange).copy(alpha = 0.94f * pulse),
        radius = (cell * 1.05f).coerceAtLeast(3.4f),
        center = Offset(
            originX + (race.finishPos.x + 0.5f) * cell,
            originY + (race.finishPos.y + 0.5f) * cell
        )
    )

    // Rival Cyber-Driller Radar Blip (always tracked on strategic multi-beacon circuits + nearby/Sonar)
    val rivalDist = abs(race.rivalPos.x - race.playerPos.x) + abs(race.rivalPos.y - race.playerPos.y)
    if (race.isPractice || race.sonarActive || rivalDist <= 7) {
        drawCircle(
            color = RivalCrimson.copy(alpha = 0.92f),
            radius = (cell * 0.90f).coerceAtLeast(3.0f),
            center = Offset(
                originX + (race.rivalPos.x + 0.5f) * cell,
                originY + (race.rivalPos.y + 0.5f) * cell
            )
        )
    }

    drawCircle(
        color = Color.White,
        radius = (cell * 1.15f).coerceAtLeast(3.8f),
        center = Offset(
            originX + (race.playerPos.x + 0.5f) * cell,
            originY + (race.playerPos.y + 0.5f) * cell
        )
    )
    drawCircle(
        color = ForgedGold,
        radius = (cell * 0.85f).coerceAtLeast(2.8f),
        center = Offset(
            originX + (race.playerPos.x + 0.5f) * cell,
            originY + (race.playerPos.y + 0.5f) * cell
        )
    )
}

private fun DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    fill: Color,
    highlight: Color,
    scratchPath: Path
) {
    scratchPath.rewind()
    scratchPath.moveTo(center.x, center.y - radius)
    scratchPath.lineTo(center.x + radius * 0.85f, center.y)
    scratchPath.lineTo(center.x, center.y + radius)
    scratchPath.lineTo(center.x - radius * 0.85f, center.y)
    scratchPath.close()
    drawPath(path = scratchPath, color = fill)
    drawCircle(
        color = highlight,
        radius = radius * 0.28f,
        center = Offset(center.x - radius * 0.16f, center.y - radius * 0.22f)
    )
}

/**
 * Renders a deterministic holographic Ghost Racer silhouette & pulsing aura ring.
 */
fun DrawScope.drawGhostMiner(
    ghostFrame: GhostFrame,
    tileSizePx: Float,
    pulseAlpha: Float // 0.35f to 0.55f sine wave
) {
    val center = Offset(
        x = (ghostFrame.gridX + 0.5f) * tileSizePx,
        y = (ghostFrame.gridY + 0.5f) * tileSizePx
    )
    // Holographic trail ring
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.4f),
        radius = tileSizePx * 0.48f,
        center = center
    )
    // Surge burst outer halo if ghost triggered Kinetic Surge on this tick
    if (ghostFrame.isSurgeActive) {
        drawCircle(
            color = ForgedGold.copy(alpha = (pulseAlpha * 1.15f).coerceAtMost(0.75f)),
            radius = tileSizePx * 0.60f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    // Core silhouette
    drawCircle(
        color = Color(0xFF84FFFF).copy(alpha = pulseAlpha),
        radius = tileSizePx * 0.32f,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    // Inner holographic beacon dot
    drawCircle(
        color = Color(0xFFE0F7FA).copy(alpha = (pulseAlpha + 0.20f).coerceAtMost(0.85f)),
        radius = tileSizePx * 0.10f,
        center = center
    )
}
