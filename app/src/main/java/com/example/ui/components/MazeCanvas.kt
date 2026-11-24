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

    // Tactile mining debris, pickaxe swing & explosion shockwave pulse
    val hitAnim = remember { Animatable(0f) }
    LaunchedEffect(race.hitPulseTick) {
        if (race.hitPulseTick > 0L) {
            hitAnim.snapTo(1f)
            hitAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(if (race.lastHitExploded) 440 else 300, easing = FastOutSlowInEasing)
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

    val biomeVisuals = when (race.biome) {
        Biome.DESERT -> BiomeVisualPalette(
            floorA = Color(0xFF2E2419),
            floorB = Color(0xFF382C1E),
            floorAccent = Color(0xFF5C4529),
            fog = Color(0xFF100C08),
            glow = ForgedGold,
            particleColor = Color(0xFFFDE68A)
        )
        Biome.ICE -> BiomeVisualPalette(
            floorA = Color(0xFF12273A),
            floorB = Color(0xFF18324A),
            floorAccent = Color(0xFF38BDF8),
            fog = Color(0xFF061019),
            glow = TileIce,
            particleColor = Color(0xFFE0F2FE)
        )
        Biome.VOLCANO -> BiomeVisualPalette(
            floorA = Color(0xFF291617),
            floorB = Color(0xFF331B1C),
            floorAccent = Color(0xFF9A3412),
            fog = Color(0xFF110708),
            glow = VolcanicOrange,
            particleColor = Color(0xFFFB923C)
        )
        Biome.CAVERN -> BiomeVisualPalette(
            floorA = Color(0xFF151F34),
            floorB = Color(0xFF1B2842),
            floorAccent = Color(0xFF334155),
            fog = Color(0xFF070C17),
            glow = CrystalCyan,
            particleColor = Color(0xFF67E8F9)
        )
        Biome.RUINS -> BiomeVisualPalette(
            floorA = Color(0xFF1E192E),
            floorB = Color(0xFF26203A),
            floorAccent = Color(0xFF4C1D95),
            fog = Color(0xFF0A0814),
            glow = MysticAmethyst,
            particleColor = Color(0xFFD8B4FE)
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
            .background(biomeVisuals.fog)
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
            val cellSize = boardSide / race.viewSize
            // Tighter gutter padding so enlarged blocks showcase maximum carved detail
            val pad = (cellSize * 0.032f).coerceAtLeast(1.5f)
            val tileCorner = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)

            val shakeStrength = if (race.lastHitExploded) 11f else 5.5f
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

                    val key = "$gx,$gy"
                    val dist = abs(gx - race.playerPos.x) + abs(gy - race.playerPos.y)
                    val inVision = dist <= race.currentVision
                    val explored = key in race.explored

                    if (!explored && !inVision) {
                        drawRoundRect(
                            color = Color(0xFF090F1D),
                            topLeft = Offset(left, top),
                            size = Size(inner, inner),
                            cornerRadius = tileCorner
                        )
                        if ((gx * 7 + gy * 13) % 9 == 0) {
                            drawCircle(
                                color = biomeVisuals.glow.copy(alpha = 0.08f * pulse),
                                radius = inner * 0.06f,
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
                        alpha = alpha
                    )

                    val block = race.blocks[key]
                    if (block != null) {
                        val isDisguisedHidden = block.special == BlockSpecial.HIDDEN && key !in race.hiddenRevealed
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
                                isTarget = race.targetBlock?.x == gx && race.targetBlock.y == gy,
                                isRevealedHidden = block.special == BlockSpecial.HIDDEN
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
                        if (key in hintSet) {
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
                        if (key in race.traps && inVision) {
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

                        // Ghost trail footsteps
                        if (race.ghostTrail.any { it.x == gx && it.y == gy }) {
                            drawCircle(
                                color = TileGhost.copy(alpha = 0.26f * alpha),
                                radius = inner * 0.09f,
                                center = center
                            )
                        }

                        // Multi-Ring Rotating Arcane Finish Portal
                        if (gx == race.finishPos.x && gy == race.finishPos.y) {
                            drawArcaneFinishPortal(
                                topLeft = topLeft,
                                rectSize = rectSize,
                                center = center,
                                corner = tileCorner,
                                pulse = pulse,
                                orbitAngle = orbitAngle,
                                alpha = alpha
                            )
                        }

                        // Faceted Glowing Gem
                        if (key in race.gems) {
                            drawFacetedGem(
                                center = center,
                                radius = inner * 0.33f * (0.92f + 0.10f * pulse),
                                alpha = alpha,
                                pulse = pulse
                            )
                        }

                        // Powerup Orb with bespoke vector icon
                        val power = race.powers[key]
                        if (power != null) {
                            drawPowerupOrb(
                                power = power,
                                center = center,
                                inner = inner,
                                pulse = pulse,
                                alpha = alpha
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

                    // Rival AI Armored Cyber-Driller
                    if (race.rivalPos.x == gx && race.rivalPos.y == gy && (inVision || race.sonarActive)) {
                        drawRivalCyberDriller(
                            center = center,
                            inner = inner,
                            pulse = pulse,
                            orbitAngle = orbitAngle
                        )
                    }
                }
            }

            // Pathfinder BFS Hint glowing trail line
            if (race.hintPath.size >= 2) {
                val hintPathObj = Path()
                race.hintPath.forEachIndexed { i, pos ->
                    val px = originX + (pos.x - camX) * cellSize
                    val py = originY + (pos.y - camY) * cellSize
                    if (i == 0) hintPathObj.moveTo(px, py) else hintPathObj.lineTo(px, py)
                }
                drawPath(
                    path = hintPathObj,
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

                // Explosive blast shockwave ring
                if (race.lastHitExploded) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ForgedGold.copy(alpha = 0.45f * hitAnim.value),
                                VolcanicOrange.copy(alpha = 0.30f * hitAnim.value),
                                Color.Transparent
                            ),
                            center = Offset(hx, hy),
                            radius = cellSize * (0.6f + 1.8f * progress)
                        ),
                        radius = cellSize * (0.6f + 1.8f * progress),
                        center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = VolcanicOrange.copy(alpha = hitAnim.value * 0.9f),
                        radius = cellSize * (0.4f + 1.55f * progress),
                        center = Offset(hx, hy),
                        style = Stroke(width = (cellSize * 0.09f) * hitAnim.value)
                    )
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

            // Directional Headlamp Volumetric Cone
            val coneLen = cellSize * (race.currentVision * 0.78f).coerceAtLeast(1.8f)
            val coneHalfAngle = 0.44f
            val conePath = Path().apply {
                moveTo(playerCenter.x, playerCenter.y)
                lineTo(
                    playerCenter.x + cos(facingAngle - coneHalfAngle) * coneLen,
                    playerCenter.y + sin(facingAngle - coneHalfAngle) * coneLen
                )
                lineTo(
                    playerCenter.x + cos(facingAngle + coneHalfAngle) * coneLen,
                    playerCenter.y + sin(facingAngle + coneHalfAngle) * coneLen
                )
                close()
            }
            drawPath(
                path = conePath,
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

            // Overdrive Fever Golden Flame Ring (Combo >= 4 in Campaign/Practice)
            if (race.overdriveActive) {
                drawCircle(
                    color = VolcanicOrange.copy(alpha = 0.45f * pulse),
                    radius = cellSize * 0.52f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.06f)
                )
            }

            // Directional Finish Compass Chevron
            val fdx = (race.finishPos.x - race.playerPos.x).toFloat()
            val fdy = (race.finishPos.y - race.playerPos.y).toFloat()
            if (abs(fdx) + abs(fdy) > 0f) {
                val angle = atan2(fdy, fdx)
                val tipDist = cellSize * 0.62f
                val baseDist = cellSize * 0.47f
                val spreadAngle = 0.27f
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
                drawPath(arrowPath, color = EmeraldGem.copy(alpha = 0.92f * pulse))
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

            // Shield Aura Ring
            if (race.activePower == PowerupType.SHIELD) {
                drawCircle(
                    color = EmeraldGem.copy(alpha = 0.78f),
                    radius = cellSize * 0.43f,
                    center = playerCenter,
                    style = Stroke(width = cellSize * 0.065f)
                )
            }

            // Articulated Top-Down Miner Explorer Avatar
            drawPlayerMinerAvatar(
                center = playerCenter,
                cellSize = cellSize,
                facingAngle = facingAngle,
                overdrive = race.overdriveActive
            )

            // Continuous Atmospheric Biome Particles (Embers, Snowflakes, Fireflies, Sand Motes)
            drawAmbientBiomeParticles(
                biome = race.biome,
                color = biomeVisuals.particleColor,
                phase = ambientPhase
            )

            // Live Translucent Tactical Mini-Map Radar
            drawTacticalMiniMap(
                race = race,
                pulse = pulse,
                biomeAccent = biomeVisuals.glow
            )
        }

        // Crisp Floating Score/Event Popup Pill
        if (race.popups.isNotEmpty()) {
            val latest = race.popups.last()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 115.dp)
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

private data class BiomeVisualPalette(
    val floorA: Color,
    val floorB: Color,
    val floorAccent: Color,
    val fog: Color,
    val glow: Color,
    val particleColor: Color
)

private fun DrawScope.drawDetailedFloorTile(
    gx: Int,
    gy: Int,
    biome: Biome,
    palette: BiomeVisualPalette,
    topLeft: Offset,
    rectSize: Size,
    corner: CornerRadius,
    alpha: Float
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
        color = Color.Black.copy(alpha = 0.20f * alpha),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner,
        style = Stroke(width = 1.2f)
    )
    // Biome-specific micro surface motif
    val hash = (gx * 31 + gy * 17) and 7
    if (hash == 0) {
        when (biome) {
            Biome.DESERT -> drawLine(
                color = palette.floorAccent.copy(alpha = 0.35f * alpha),
                start = Offset(topLeft.x + rectSize.width * 0.18f, topLeft.y + rectSize.height * 0.65f),
                end = Offset(topLeft.x + rectSize.width * 0.78f, topLeft.y + rectSize.height * 0.55f),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
            Biome.ICE -> drawLine(
                color = palette.floorAccent.copy(alpha = 0.25f * alpha),
                start = Offset(topLeft.x + rectSize.width * 0.22f, topLeft.y + rectSize.height * 0.22f),
                end = Offset(topLeft.x + rectSize.width * 0.68f, topLeft.y + rectSize.height * 0.68f),
                strokeWidth = 1.5f
            )
            Biome.VOLCANO -> drawCircle(
                color = VolcanicOrange.copy(alpha = 0.18f * alpha),
                radius = rectSize.width * 0.08f,
                center = Offset(topLeft.x + rectSize.width * 0.7f, topLeft.y + rectSize.height * 0.3f)
            )
            Biome.CAVERN -> drawCircle(
                color = CrystalCyan.copy(alpha = 0.16f * alpha),
                radius = rectSize.width * 0.06f,
                center = Offset(topLeft.x + rectSize.width * 0.28f, topLeft.y + rectSize.height * 0.72f)
            )
            Biome.RUINS -> drawRect(
                color = palette.floorAccent.copy(alpha = 0.24f * alpha),
                topLeft = Offset(topLeft.x + rectSize.width * 0.22f, topLeft.y + rectSize.height * 0.22f),
                size = Size(rectSize.width * 0.56f, rectSize.height * 0.56f),
                style = Stroke(width = 1.2f)
            )
        }
    }
}

/**
 * High-detail 2.5D extruded block renderer showcasing carved textures on enlarged tiles.
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
    isTarget: Boolean,
    isRevealedHidden: Boolean
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

    // 4. Crisp top-left specular bevel edge
    drawLine(
        color = Color.White.copy(alpha = 0.28f * alpha),
        start = Offset(topLeft.x + w * 0.14f, topLeft.y + 2f),
        end = Offset(topLeft.x + w * 0.86f, topLeft.y + 2f),
        strokeWidth = 2f,
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
            val rivets = arrayOf(
                Offset(topLeft.x + w * 0.20f, topLeft.y + topFaceH * 0.22f),
                Offset(topLeft.x + w * 0.80f, topLeft.y + topFaceH * 0.22f),
                Offset(topLeft.x + w * 0.20f, topLeft.y + topFaceH * 0.74f),
                Offset(topLeft.x + w * 0.80f, topLeft.y + topFaceH * 0.74f)
            )
            for (rv in rivets) {
                drawCircle(color = skirt.copy(alpha = 0.85f * alpha), radius = rivetR + 1f, center = rv)
                drawCircle(color = Color.White.copy(alpha = 0.70f * alpha), radius = rivetR * 0.65f, center = Offset(rv.x - 0.6f, rv.y - 0.6f))
            }
        }

        BlockKind.CRYSTAL -> {
            // Protruding multi-faceted crystalline amethyst/cyan spires + pulsing sparkle
            val spirePath = Path().apply {
                moveTo(center.x, topLeft.y + topFaceH * 0.10f)
                lineTo(center.x + w * 0.22f, topLeft.y + topFaceH * 0.42f)
                lineTo(center.x + w * 0.14f, topLeft.y + topFaceH * 0.78f)
                lineTo(center.x - w * 0.14f, topLeft.y + topFaceH * 0.78f)
                lineTo(center.x - w * 0.22f, topLeft.y + topFaceH * 0.42f)
                close()
            }
            drawPath(
                path = spirePath,
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
                    highlight = Color.White.copy(alpha = 0.90f * alpha)
                )
            }
        }
    }

    // 6. Special Block High-Contrast Vector Emblems
    when (block.special) {
        BlockSpecial.EXPLOSIVE -> {
            // Crimson TNT band + detonator cap + animated spark
            drawRoundRect(
                color = TileExplosive.copy(alpha = 0.92f * alpha),
                topLeft = Offset(topLeft.x + w * 0.08f, center.y - h * 0.13f),
                size = Size(w * 0.84f, h * 0.26f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawCircle(
                color = ObsidianDeep.copy(alpha = alpha),
                radius = w * 0.14f,
                center = center
            )
            drawCircle(
                color = ForgedGold.copy(alpha = pulse * alpha),
                radius = w * 0.09f * pulse,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = pulse * alpha),
                radius = w * 0.045f,
                center = Offset(center.x + w * 0.10f, center.y - h * 0.12f)
            )
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
            // Glowing magma fissures pulsing across basalt
            val magmaPath = Path().apply {
                moveTo(topLeft.x + w * 0.18f, topLeft.y + topFaceH * 0.22f)
                lineTo(center.x - w * 0.04f, center.y)
                lineTo(topLeft.x + w * 0.82f, topLeft.y + topFaceH * 0.30f)
                moveTo(center.x - w * 0.04f, center.y)
                lineTo(topLeft.x + w * 0.56f, topLeft.y + topFaceH * 0.78f)
            }
            drawPath(
                path = magmaPath,
                color = TileLava.copy(alpha = 0.92f * alpha),
                style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = magmaPath,
                color = ForgedGold.copy(alpha = pulse * alpha),
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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

    // 7. Progressive Multi-Stage Fracture Cracks as block takes mining damage
    val damageTaken = block.max - block.hp
    if (damageTaken > 0) {
        val crackPath = Path().apply {
            moveTo(topLeft.x + w * 0.20f, topLeft.y + topFaceH * 0.18f)
            lineTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
            lineTo(topLeft.x + w * 0.34f, topLeft.y + topFaceH * 0.74f)
            if (damageTaken >= 2) {
                moveTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
                lineTo(topLeft.x + w * 0.80f, topLeft.y + topFaceH * 0.34f)
                moveTo(topLeft.x + w * 0.48f, topLeft.y + topFaceH * 0.46f)
                lineTo(topLeft.x + w * 0.72f, topLeft.y + topFaceH * 0.76f)
            }
        }
        drawPath(
            path = crackPath,
            color = ObsidianDeep.copy(alpha = 0.85f * alpha),
            style = Stroke(width = (w * 0.055f).coerceAtLeast(2.2f), cap = StrokeCap.Round)
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

private fun DrawScope.drawArcaneFinishPortal(
    topLeft: Offset,
    rectSize: Size,
    center: Offset,
    corner: CornerRadius,
    pulse: Float,
    orbitAngle: Float,
    alpha: Float
) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(EmeraldGem, TileFinish, ObsidianDeep),
            center = center,
            radius = rectSize.width * 0.68f
        ),
        topLeft = topLeft,
        size = rectSize,
        cornerRadius = corner,
        alpha = alpha
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.90f * pulse * alpha),
        radius = rectSize.width * 0.30f * pulse,
        center = center,
        style = Stroke(width = rectSize.width * 0.075f)
    )
    drawCircle(
        color = ForgedGold.copy(alpha = 0.92f * alpha),
        radius = rectSize.width * 0.14f,
        center = center
    )
    // 3 orbiting emerald-gold portal wisps
    for (i in 0 until 3) {
        val a = orbitAngle + i * (2.0 * PI / 3.0)
        val wx = center.x + (cos(a) * rectSize.width * 0.34f).toFloat()
        val wy = center.y + (sin(a) * rectSize.width * 0.34f).toFloat()
        drawCircle(
            color = ForgedGold.copy(alpha = 0.90f * alpha),
            radius = rectSize.width * 0.06f,
            center = Offset(wx, wy)
        )
    }
}

private fun DrawScope.drawFacetedGem(
    center: Offset,
    radius: Float,
    alpha: Float,
    pulse: Float
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
        highlight = Color.White.copy(alpha = 0.90f * alpha)
    )
    // Inner crown facet line
    val innerFacet = Path().apply {
        moveTo(center.x, center.y - radius * 0.55f)
        lineTo(center.x + radius * 0.45f, center.y)
        lineTo(center.x, center.y + radius * 0.55f)
        lineTo(center.x - radius * 0.45f, center.y)
        close()
    }
    drawPath(
        path = innerFacet,
        color = Color.White.copy(alpha = 0.42f * alpha),
        style = Stroke(width = 1.6f)
    )
}

private fun DrawScope.drawPowerupOrb(
    power: PowerupType,
    center: Offset,
    inner: Float,
    pulse: Float,
    alpha: Float
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
            val bolt = Path().apply {
                moveTo(center.x + r * 0.25f, center.y - r)
                lineTo(center.x - r * 0.55f, center.y + r * 0.1f)
                lineTo(center.x + r * 0.1f, center.y + r * 0.1f)
                lineTo(center.x - r * 0.25f, center.y + r)
            }
            drawPath(bolt, color = Color.White.copy(alpha = alpha), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        }
        PowerupType.CLOCK -> {
            // Clock hands
            drawLine(Color.White.copy(alpha = alpha), center, Offset(center.x, center.y - r * 0.85f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = alpha), center, Offset(center.x + r * 0.65f, center.y + r * 0.2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
        PowerupType.SHIELD -> {
            // Shield crest
            val crest = Path().apply {
                moveTo(center.x, center.y - r * 0.9f)
                lineTo(center.x + r * 0.8f, center.y - r * 0.35f)
                lineTo(center.x, center.y + r * 0.95f)
                lineTo(center.x - r * 0.8f, center.y - r * 0.35f)
                close()
            }
            drawPath(crest, color = Color.White.copy(alpha = alpha))
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

private fun DrawScope.drawAmbientBiomeParticles(
    biome: Biome,
    color: Color,
    phase: Float
) {
    val count = 16
    for (i in 0 until count) {
        val seedX = ((i * 73 + 19) % 100) / 100f
        val seedY = ((i * 41 + 7) % 100) / 100f
        val speed = 0.6f + (i % 3) * 0.35f
        val t = (phase * speed + seedY) % 1f

        val px = when (biome) {
            Biome.DESERT -> ((seedX + phase * 0.45f) % 1f) * size.width
            Biome.ICE -> (seedX + sin((t * 2.0 * PI + i).toDouble()).toFloat() * 0.03f) * size.width
            else -> (seedX + cos((t * 2.0 * PI + i).toDouble()).toFloat() * 0.025f) * size.width
        }
        val py = when (biome) {
            Biome.VOLCANO -> (1f - t) * size.height // rising glowing embers
            Biome.ICE -> t * size.height // falling snow sparkles
            else -> ((seedY + sin((phase * 2.0 * PI + i).toDouble()).toFloat() * 0.04f + 1f) % 1f) * size.height
        }
        val alpha = (sin(t * PI).toFloat().coerceIn(0f, 1f)) * 0.42f
        val radius = (2.2f + (i % 3) * 1.4f)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(px, py)
        )
    }
}

private fun DrawScope.drawTacticalMiniMap(
    race: LiveRaceState,
    pulse: Float,
    biomeAccent: Color
) {
    val mapSize = (min(size.width, size.height) * 0.23f).coerceIn(76f, 116f)
    val marginRight = 14.dp.toPx()
    val marginTop = 154.dp.toPx()
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

    for (k in race.explored) {
        val parts = k.split(",")
        if (parts.size != 2) continue
        val x = parts[0].toIntOrNull() ?: continue
        val y = parts[1].toIntOrNull() ?: continue
        val px = originX + x * cell
        val py = originY + y * cell
        val isWall = race.blocks.containsKey(k)
        drawRect(
            color = if (isWall) Color(0xFF475569).copy(alpha = 0.58f) else Color(0xFF94A3B8).copy(alpha = 0.30f),
            topLeft = Offset(px, py),
            size = Size(cell.coerceAtLeast(1.5f), cell.coerceAtLeast(1.5f))
        )
    }

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

    drawCircle(
        color = EmeraldGem.copy(alpha = 0.92f * pulse),
        radius = (cell * 0.95f).coerceAtLeast(3.2f),
        center = Offset(
            originX + (race.finishPos.x + 0.5f) * cell,
            originY + (race.finishPos.y + 0.5f) * cell
        )
    )

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

    drawCircle(
        color = ForgedGold,
        radius = (cell * 0.95f).coerceAtLeast(3.2f),
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
        radius = radius * 0.28f,
        center = Offset(center.x - radius * 0.16f, center.y - radius * 0.22f)
    )
}
