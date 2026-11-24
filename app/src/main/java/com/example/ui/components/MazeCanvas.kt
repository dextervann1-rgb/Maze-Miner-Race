package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.engine.PowerupType
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
 * Full-bleed immersive Canvas maze renderer with:
 * - Dynamic multi-board support (15x15, 21x21, 25x25) filling 100% of screen aspect ratio
 * - Biome-tinted floor palettes & ambient depth lighting
 * - Spring camera interpolation + mining impact micro-shake & radial spark bursts
 * - Intelligent BFS Pathfinder hint trail shimmer on Sonar
 * - Directional Finish Compass chevron & Rival proximity warning beacon
 * - Live translucent Mini-Map Radar in the upper-right viewport
 * - Founder's Golden Aura halo & Shield/Sonar visual feedback
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

    // Tactile mining spark & micro-shake pulse
    val hitAnim = remember { Animatable(0f) }
    LaunchedEffect(race.hitPulseTick) {
        if (race.hitPulseTick > 0L) {
            hitAnim.snapTo(1f)
            hitAnim.animateTo(0f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        }
    }

    val inf = rememberInfiniteTransition(label = "board_fx")
    val pulse by inf.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tile_pulse"
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

    val (floorA, floorB, fogColor, biomeGlow) = when (race.biome) {
        Biome.DESERT -> QuadColors(
            Color(0xFF2B2218), Color(0xFF33281C), Color(0xFF100C08), ForgedGold
        )
        Biome.ICE -> QuadColors(
            Color(0xFF142738), Color(0xFF193044), Color(0xFF07111A), TileIce
        )
        Biome.VOLCANO -> QuadColors(
            Color(0xFF281617), Color(0xFF311A1B), Color(0xFF120809), VolcanicOrange
        )
        Biome.CAVERN -> QuadColors(
            Color(0xFF161F33), Color(0xFF1B2740), Color(0xFF080D18), CrystalCyan
        )
        Biome.RUINS -> QuadColors(
            Color(0xFF1E1A2D), Color(0xFF252038), Color(0xFF0B0914), MysticAmethyst
        )
    }

    val ghostPos = race.ghostTrail.getOrNull(
        race.ghostIndex.coerceAtMost((race.ghostTrail.size - 1).coerceAtLeast(0))
    )
    val hintSet = remember(race.hintPath) { race.hintPath.map { it.key }.toSet() }
    val rivalManhattan = abs(race.rivalPos.x - race.playerPos.x) + abs(race.rivalPos.y - race.playerPos.y)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fogColor)
            .semantics { contentDescription = "Immersive maze board" }
            .testTag("maze_board_canvas")
    ) {
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
                            val threshold = 44f
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
            val cellSize = boardSide / race.viewSize
            val pad = (cellSize * 0.055f).coerceAtLeast(1.5f)
            val tileCorner = CornerRadius(cellSize * 0.19f, cellSize * 0.19f)

            val shakePx = hitAnim.value * 5.5f * sin(hitAnim.value * 28f)
            val originX = size.width / 2f + shakePx
            val originY = size.height / 2f + (shakePx * 0.5f)

            val halfCols = ceil((size.width / 2f) / cellSize).toInt() + 1
            val halfRows = ceil((size.height / 2f) / cellSize).toInt() + 1
            val minGx = race.playerPos.x - halfCols
            val maxGx = race.playerPos.x + halfCols
            val minGy = race.playerPos.y - halfRows
            val maxGy = race.playerPos.y + halfRows

            for (gy in minGy..maxGy) {
                for (gx in minGx..maxGx) {
                    val cx = originX + (gx - camX) * cellSize
                    val cy = originY + (gy - camY) * cellSize
                    val left = cx - cellSize / 2f + pad
                    val top = cy - cellSize / 2f + pad
                    val inner = cellSize - pad * 2f

                    if (gx !in 0 until race.boardSize || gy !in 0 until race.boardSize) {
                        // Subtle deep-bedrock outer border tiles
                        drawRoundRect(
                            color = fogColor,
                            topLeft = Offset(left, top),
                            size = Size(inner, inner),
                            cornerRadius = tileCorner
                        )
                        continue
                    }

                    val key = "$gx,$gy"
                    val dist = abs(gx - race.playerPos.x) + abs(gy - race.playerPos.y)
                    val inVision = dist <= race.currentVision
                    val explored = key in race.explored

                    if (!explored && !inVision) {
                        drawRoundRect(
                            color = Color(0xFF0A101E),
                            topLeft = Offset(left, top),
                            size = Size(inner, inner),
                            cornerRadius = tileCorner
                        )
                        // Faint mystical fog speck for depth
                        if ((gx * 7 + gy * 13) % 11 == 0) {
                            drawCircle(
                                color = biomeGlow.copy(alpha = 0.07f),
                                radius = inner * 0.08f,
                                center = Offset(cx, cy)
                            )
                        }
                        continue
                    }

                    val alpha = if (inVision) 1f else 0.40f
                    val topLeft = Offset(left, top)
                    val rectSize = Size(inner, inner)
                    val center = Offset(cx, cy)

                    // Biome-tinted floor base
                    val floorColor = if ((gx + gy) % 2 == 0) floorA else floorB
                    drawRoundRect(
                        color = floorColor.copy(alpha = alpha),
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = tileCorner
                    )

                    val block = race.blocks[key]
                    if (block != null) {
                        val isDisguisedHidden = block.special == BlockSpecial.HIDDEN && key !in race.hiddenRevealed
                        if (!isDisguisedHidden) {
                            drawBlockTile(
                                block = block,
                                topLeft = topLeft,
                                rectSize = rectSize,
                                corner = tileCorner,
                                alpha = alpha,
                                isTarget = race.targetBlock?.x == gx && race.targetBlock.y == gy,
                                isRevealedHidden = block.special == BlockSpecial.HIDDEN
                            )
                        } else if (inVision && dist == 1) {
                            // Subtle shimmer clue on adjacent hidden block for observant players!
                            drawRoundRect(
                                color = CrystalCyan.copy(alpha = 0.14f * pulse),
                                topLeft = topLeft,
                                size = rectSize,
                                cornerRadius = tileCorner,
                                style = Stroke(width = 1.5f)
                            )
                        }
                    } else {
                        // Pathfinder BFS Hint breadcrumb shimmer
                        if (key in hintSet) {
                            drawCircle(
                                color = CrystalCyan.copy(alpha = 0.28f * pulse),
                                radius = inner * 0.30f,
                                center = center
                            )
                            drawCircle(
                                color = CrystalCyan.copy(alpha = 0.85f * pulse),
                                radius = inner * 0.12f,
                                center = center
                            )
                        }

                        // Ruins trap floor glyph when visible
                        if (key in race.traps && inVision) {
                            drawCircle(
                                color = MysticAmethyst.copy(alpha = 0.34f * pulse),
                                radius = inner * 0.22f,
                                center = center,
                                style = Stroke(width = inner * 0.06f)
                            )
                            drawCircle(
                                color = RivalCrimson.copy(alpha = 0.45f),
                                radius = inner * 0.08f,
                                center = center
                            )
                        }

                        // Ghost trail breadcrumbs
                        if (race.ghostTrail.any { it.x == gx && it.y == gy }) {
                            drawCircle(
                                color = TileGhost.copy(alpha = 0.24f * alpha),
                                radius = inner * 0.10f,
                                center = center
                            )
                        }

                        // Finish Portal (dynamic board finish coordinate)
                        if (gx == race.finishPos.x && gy == race.finishPos.y) {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(EmeraldGem, TileFinish),
                                    center = center,
                                    radius = inner * 0.72f
                                ),
                                topLeft = topLeft,
                                size = rectSize,
                                cornerRadius = tileCorner,
                                alpha = alpha
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.88f * pulse),
                                radius = inner * 0.27f * pulse,
                                center = center,
                                style = Stroke(width = inner * 0.08f)
                            )
                            drawCircle(
                                color = ForgedGold.copy(alpha = 0.90f),
                                radius = inner * 0.11f,
                                center = center
                            )
                        }

                        // Collectible Gem
                        if (key in race.gems) {
                            drawDiamond(
                                center = center,
                                radius = inner * 0.31f * (0.9f + 0.12f * pulse),
                                fill = CrystalCyan.copy(alpha = alpha),
                                highlight = Color.White.copy(alpha = 0.80f * alpha)
                            )
                        }

                        // Powerup Orb
                        val power = race.powers[key]
                        if (power != null) {
                            val pColor = when (power) {
                                PowerupType.PICK -> ForgedGold
                                PowerupType.SPEED -> CrystalCyan
                                PowerupType.LANTERN -> Color(0xFFFDE047)
                                PowerupType.SHIELD -> EmeraldGem
                                PowerupType.SONAR -> MysticAmethyst
                                PowerupType.CLOCK -> VolcanicOrange
                            }
                            drawCircle(
                                color = pColor.copy(alpha = 0.28f * alpha),
                                radius = inner * 0.42f * pulse,
                                center = center
                            )
                            drawCircle(
                                color = pColor.copy(alpha = alpha),
                                radius = inner * 0.24f,
                                center = center
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f * alpha),
                                radius = inner * 0.08f,
                                center = Offset(center.x - inner * 0.06f, center.y - inner * 0.06f)
                            )
                        }
                    }

                    // Ghost Racer ring
                    if (ghostPos != null && ghostPos.x == gx && ghostPos.y == gy && inVision) {
                        drawCircle(
                            color = TileGhost.copy(alpha = 0.58f),
                            radius = inner * 0.30f,
                            center = center,
                            style = Stroke(width = inner * 0.09f)
                        )
                    }

                    // Rival AI Racer
                    if (race.rivalPos.x == gx && race.rivalPos.y == gy && (inVision || race.sonarActive)) {
                        drawCircle(
                            color = RivalCrimson.copy(alpha = 0.32f),
                            radius = inner * 0.46f * pulse,
                            center = center
                        )
                        drawCircle(
                            color = RivalCrimson,
                            radius = inner * 0.30f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = inner * 0.06f,
                            center = Offset(center.x - inner * 0.10f, center.y - inner * 0.04f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = inner * 0.06f,
                            center = Offset(center.x + inner * 0.10f, center.y - inner * 0.04f)
                        )
                    }
                }
            }

            // Pathfinder BFS Hint connecting glowing line
            if (race.hintPath.size >= 2) {
                val hintPathObj = Path()
                race.hintPath.forEachIndexed { i, pos ->
                    val px = originX + (pos.x - camX) * cellSize
                    val py = originY + (pos.y - camY) * cellSize
                    if (i == 0) hintPathObj.moveTo(px, py) else hintPathObj.lineTo(px, py)
                }
                drawPath(
                    path = hintPathObj,
                    color = CrystalCyan.copy(alpha = 0.65f * pulse),
                    style = Stroke(width = cellSize * 0.08f, cap = StrokeCap.Round)
                )
            }

            // Radial mining impact spark burst particles
            val lastHit = race.lastHitPos
            if (lastHit != null && hitAnim.value > 0.02f) {
                val hx = originX + (lastHit.x - camX) * cellSize
                val hy = originY + (lastHit.y - camY) * cellSize
                val progress = 1f - hitAnim.value
                val spread = cellSize * (0.20f + 0.55f * progress)
                for (i in 0 until 8) {
                    val angle = (i * (2.0 * PI / 8.0)) + 0.35
                    val sx = hx + (cos(angle) * spread).toFloat()
                    val sy = hy + (sin(angle) * spread).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) ForgedGold.copy(alpha = hitAnim.value)
                        else CrystalCyan.copy(alpha = hitAnim.value),
                        radius = (cellSize * 0.08f) * hitAnim.value,
                        center = Offset(sx, sy)
                    )
                }
            }

            // Player Miner centered on viewport with warm lantern halo
            val playerCenter = Offset(originX, originY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        biomeGlow.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = playerCenter,
                    radius = cellSize * (race.currentVision * 0.85f)
                ),
                radius = cellSize * (race.currentVision * 0.85f),
                center = playerCenter
            )

            // Active Sonar expanding radar ring
            if (race.sonarActive) {
                drawCircle(
                    color = CrystalCyan.copy(alpha = (1f - sonarRing) * 0.78f),
                    radius = cellSize * 4.8f * sonarRing,
                    center = playerCenter,
                    style = Stroke(width = 4.5f)
                )
            }

            // Directional Finish Compass Chevron around the miner
            val fdx = (race.finishPos.x - race.playerPos.x).toFloat()
            val fdy = (race.finishPos.y - race.playerPos.y).toFloat()
            if (abs(fdx) + abs(fdy) > 0f) {
                val angle = atan2(fdy, fdx)
                val tipDist = cellSize * 0.64f
                val baseDist = cellSize * 0.49f
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
                val arrowPath = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(leftWing.x, leftWing.y)
                    lineTo(rightWing.x, rightWing.y)
                    close()
                }
                drawPath(arrowPath, color = EmeraldGem.copy(alpha = 0.88f * pulse))
            }

            // Rival Proximity Danger Pulse when Rival is within 5 tiles
            if (rivalManhattan in 1..5) {
                val rAngle = atan2(
                    (race.rivalPos.y - race.playerPos.y).toFloat(),
                    (race.rivalPos.x - race.playerPos.x).toFloat()
                )
                val beaconPos = Offset(
                    playerCenter.x + cos(rAngle) * cellSize * 0.54f,
                    playerCenter.y + sin(rAngle) * cellSize * 0.54f
                )
                drawCircle(
                    color = RivalCrimson.copy(alpha = 0.85f * pulse),
                    radius = cellSize * 0.10f,
                    center = beaconPos
                )
            }

            // Founder's Golden Aura Ring
            if (founderAura) {
                drawCircle(
                    color = ForgedGold.copy(alpha = 0.55f * pulse),
                    radius = cellSize * 0.46f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.055f)
                )
            }

            // Shield Aura Ring
            if (race.activePower == PowerupType.SHIELD) {
                drawCircle(
                    color = EmeraldGem.copy(alpha = 0.72f),
                    radius = cellSize * 0.44f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.07f)
                )
            }

            // Player Miner Avatar
            drawCircle(
                color = ForgedGold,
                radius = cellSize * 0.33f,
                center = playerCenter
            )
            drawCircle(
                color = ObsidianDeep,
                radius = cellSize * 0.16f,
                center = Offset(playerCenter.x, playerCenter.y - cellSize * 0.04f)
            )
            drawCircle(
                color = CrystalCyan,
                radius = cellSize * 0.09f,
                center = Offset(playerCenter.x, playerCenter.y - cellSize * 0.06f)
            )

            // Live Translucent Tactical Mini-Map Radar (Bottom-Right above controls / Top-Right below HUD)
            drawTacticalMiniMap(
                race = race,
                pulse = pulse,
                biomeAccent = biomeGlow
            )
        }

        // Crisp floating popups overlay
        if (race.popups.isNotEmpty()) {
            val latest = race.popups.last()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 110.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ObsidianDeep.copy(alpha = 0.88f))
                    .border(
                        1.dp,
                        if (latest.isPositive) ForgedGold else RivalCrimson,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = latest.text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (latest.isPositive) ForgedGold else RivalCrimson
                )
            }
        }
    }
}

private data class QuadColors(
    val floorA: Color,
    val floorB: Color,
    val fog: Color,
    val accent: Color
)

private fun DrawScope.drawTacticalMiniMap(
    race: LiveRaceState,
    pulse: Float,
    biomeAccent: Color
) {
    val mapSize = (min(size.width, size.height) * 0.24f).coerceIn(76f, 118f)
    val marginRight = 14.dp.toPx()
    val marginTop = 152.dp.toPx()
    if (size.height < marginTop + mapSize + 100f) return

    val originX = size.width - mapSize - marginRight
    val originY = marginTop
    val cell = mapSize / race.boardSize.toFloat()

    // Radar frame
    drawRoundRect(
        color = ObsidianDeep.copy(alpha = 0.76f),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = biomeAccent.copy(alpha = 0.45f),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(mapSize + 8f, mapSize + 8f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 1.5f)
    )

    // Draw explored tunnels & walls
    for (k in race.explored) {
        val parts = k.split(",")
        if (parts.size != 2) continue
        val x = parts[0].toIntOrNull() ?: continue
        val y = parts[1].toIntOrNull() ?: continue
        val px = originX + x * cell
        val py = originY + y * cell
        val isWall = race.blocks.containsKey(k)
        drawRect(
            color = if (isWall) Color(0xFF475569).copy(alpha = 0.55f) else Color(0xFF94A3B8).copy(alpha = 0.30f),
            topLeft = Offset(px, py),
            size = Size(cell.coerceAtLeast(1.5f), cell.coerceAtLeast(1.5f))
        )
    }

    // Explored Gems on radar
    for (gKey in race.gems) {
        if (gKey in race.explored || race.sonarActive) {
            val parts = gKey.split(",")
            val gx = parts[0].toIntOrNull() ?: continue
            val gy = parts[1].toIntOrNull() ?: continue
            drawCircle(
                color = CrystalCyan,
                radius = (cell * 0.55f).coerceAtLeast(2f),
                center = Offset(originX + (gx + 0.5f) * cell, originY + (gy + 0.5f) * cell)
            )
        }
    }

    // Finish Portal Beacon on radar (always visible as objective target!)
    drawCircle(
        color = EmeraldGem.copy(alpha = 0.9f * pulse),
        radius = (cell * 0.95f).coerceAtLeast(3.2f),
        center = Offset(
            originX + (race.finishPos.x + 0.5f) * cell,
            originY + (race.finishPos.y + 0.5f) * cell
        )
    )

    // Rival AI dot on radar (visible during Sonar or within 7 tiles)
    val rivalDist = abs(race.rivalPos.x - race.playerPos.x) + abs(race.rivalPos.y - race.playerPos.y)
    if (race.sonarActive || rivalDist <= 7) {
        drawCircle(
            color = RivalCrimson,
            radius = (cell * 0.85f).coerceAtLeast(2.8f),
            center = Offset(
                originX + (race.rivalPos.x + 0.5f) * cell,
                originY + (race.rivalPos.y + 0.5f) * cell
            )
        )
    }

    // Player dot on radar
    drawCircle(
        color = ForgedGold,
        radius = (cell * 0.95f).coerceAtLeast(3.2f),
        center = Offset(
            originX + (race.playerPos.x + 0.5f) * cell,
            originY + (race.playerPos.y + 0.5f) * cell
        )
    )
}

private fun DrawScope.drawBlockTile(
    block: BlockState,
    topLeft: Offset,
    rectSize: Size,
    corner: CornerRadius,
    alpha: Float,
    isTarget: Boolean,
    isRevealedHidden: Boolean
) {
    val (base, highlight) = when (block.kind) {
        BlockKind.DIRT -> TileDirt to TileDirtLight
        BlockKind.STONE -> TileStone to TileStoneLight
        BlockKind.IRON -> TileIron to TileIronLight
        BlockKind.CRYSTAL -> TileCrystal to TileCrystalLight
        BlockKind.GAMBLE -> TileGamble to TileGambleLight
    }

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(highlight.copy(alpha = alpha), base.copy(alpha = alpha)),
            start = topLeft,
            end = Offset(topLeft.x + rectSize.width, topLeft.y + rectSize.height)
        ),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner
    )

    // Crack fracture lines when damaged
    if (block.hp < block.max) {
        val crackPath = Path().apply {
            moveTo(topLeft.x + rectSize.width * 0.25f, topLeft.y + rectSize.height * 0.22f)
            lineTo(topLeft.x + rectSize.width * 0.52f, topLeft.y + rectSize.height * 0.48f)
            lineTo(topLeft.x + rectSize.width * 0.38f, topLeft.y + rectSize.height * 0.68f)
        }
        drawPath(
            path = crackPath,
            color = ObsidianDeep.copy(alpha = 0.72f * alpha),
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )
    }

    // Special block accent badge
    val center = Offset(topLeft.x + rectSize.width / 2f, topLeft.y + rectSize.height / 2f)
    when (block.special) {
        BlockSpecial.EXPLOSIVE -> {
            drawCircle(
                color = TileExplosive.copy(alpha = alpha),
                radius = rectSize.width * 0.22f,
                center = center
            )
            drawCircle(
                color = ForgedGold.copy(alpha = alpha),
                radius = rectSize.width * 0.10f,
                center = center
            )
        }
        BlockSpecial.ICE -> {
            drawCircle(
                color = TileIce.copy(alpha = alpha),
                radius = rectSize.width * 0.22f,
                center = center,
                style = Stroke(width = rectSize.width * 0.08f)
            )
        }
        BlockSpecial.LAVA -> {
            drawCircle(
                color = TileLava.copy(alpha = alpha),
                radius = rectSize.width * 0.22f,
                center = center
            )
        }
        BlockSpecial.LOCKED -> {
            drawRoundRect(
                color = TileLocked.copy(alpha = alpha),
                topLeft = Offset(center.x - rectSize.width * 0.15f, center.y - rectSize.height * 0.12f),
                size = Size(rectSize.width * 0.30f, rectSize.height * 0.26f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        else -> Unit
    }

    if (block.kind == BlockKind.GAMBLE && block.special == null) {
        drawDiamond(
            center = center,
            radius = rectSize.width * 0.22f,
            fill = ForgedGold.copy(alpha = alpha),
            highlight = Color.White.copy(alpha = 0.7f * alpha)
        )
    }

    // HP pips at bottom of tile
    val pipCount = block.max
    val pipWidth = (rectSize.width * 0.70f) / pipCount
    val startX = topLeft.x + rectSize.width * 0.15f
    val pipY = topLeft.y + rectSize.height * 0.78f
    val pipHeight = (rectSize.height * 0.12f).coerceAtLeast(2.5f)
    for (i in 0 until pipCount) {
        val filled = i < block.hp
        drawRoundRect(
            color = if (filled) Color.White.copy(alpha = 0.90f * alpha) else ObsidianDeep.copy(alpha = 0.6f * alpha),
            topLeft = Offset(startX + i * pipWidth + 1f, pipY),
            size = Size((pipWidth - 2f).coerceAtLeast(2f), pipHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    if (isTarget || isRevealedHidden) {
        drawRoundRect(
            color = if (isTarget) ForgedGold else CrystalCyan,
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = corner,
            style = Stroke(width = if (isTarget) 3.5f else 2.2f)
        )
    }
}

private fun DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    fill: Color,
    highlight: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.85f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.85f, center.y)
        close()
    }
    drawPath(path = path, color = fill)
    drawCircle(
        color = highlight,
        radius = radius * 0.26f,
        center = Offset(center.x - radius * 0.15f, center.y - radius * 0.22f)
    )
}
