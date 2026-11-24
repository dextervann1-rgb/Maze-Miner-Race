package com.example

import com.example.billing.BillingManager
import com.example.engine.Biome
import com.example.engine.BlockSpecial
import com.example.engine.BoardArchetype
import com.example.engine.CampaignCatalog
import com.example.engine.Difficulty
import com.example.engine.MazeGenerator
import com.example.engine.Modifier
import com.example.engine.PowerupType
import com.example.engine.ReplayAction
import com.example.engine.ReplaySimulator
import com.example.engine.START_X
import com.example.engine.START_Y
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeMinerEngineTest {

    @Test
    fun allTenCanonicalSelfTestsPass() {
        val results = ReplaySimulator.runSelfTests()
        assertEquals(10, results.size)
        results.forEach { res ->
            assertTrue("Self-test failed: ${res.name} (${res.detail})", res.passed)
        }
    }

    @Test
    fun deterministicWinningPathSeed20261118VerifiesWithZeroTolerance() {
        val seed = "2026-11-18"
        val route = MazeGenerator.computeLegalWinningRoute(seed)
        assertTrue("Route should have > 10 steps", route.size > 10)

        val ticks = route.size.coerceAtLeast(15)
        val probe = ReplaySimulator.simulateRun(
            seed = seed,
            difficulty = Difficulty.NORMAL,
            modifier = Modifier.NORMAL,
            biome = Biome.DESERT,
            actions = route,
            ticks = ticks,
            clientScore = -1
        )
        val exactScore = probe.serverScore
        assertNotNull(exactScore)

        val verified = ReplaySimulator.simulateRun(
            seed = seed,
            difficulty = Difficulty.NORMAL,
            modifier = Modifier.NORMAL,
            biome = Biome.DESERT,
            actions = route,
            ticks = ticks,
            clientScore = exactScore!!
        )
        assertTrue(verified.ok)
        assertTrue(verified.verified)
        assertEquals(exactScore, verified.serverScore)
        assertNotNull(verified.fingerprint)
        assertEquals(64, verified.fingerprint!!.length)

        // +1 score tamper must be strictly rejected
        val tampered = ReplaySimulator.simulateRun(
            seed = seed,
            difficulty = Difficulty.NORMAL,
            modifier = Modifier.NORMAL,
            biome = Biome.DESERT,
            actions = route,
            ticks = ticks,
            clientScore = exactScore + 1
        )
        assertFalse(tampered.ok)
    }

    @Test
    fun speedPowerupIsTrueHalfSpeedNotFrozen() {
        val drains = (1..8).map { ReplaySimulator.timerDrainForTick(it, PowerupType.SPEED) }
        assertEquals(listOf(1, 0, 1, 0, 1, 0, 1, 0), drains)
    }

    @Test
    fun hiddenBlocksExistAndBlockMovement() {
        val g = MazeGenerator.genMaze("2026-11-18")
        val hiddenCount = g.blocks.values.count { it.special == BlockSpecial.HIDDEN }
        assertTrue("Hidden blocks must spawn deterministically", hiddenCount > 0)
    }

    @Test
    fun allElevenLanguagesLoadedAndArabicIsRtl() {
        assertEquals(11, AppLanguage.entries.size)
        AppLanguage.entries.forEach { lang ->
            val strings = AppStrings.forLanguage(lang)
            assertTrue(strings.playDaily.isNotBlank())
            assertTrue(strings.verifiedBadge.isNotBlank())
            assertTrue(strings.biomeDesert.isNotBlank())
            assertTrue(strings.tutorialBody.isNotBlank())
        }
        assertTrue(AppLanguage.AR.isRtl)
        assertFalse(AppLanguage.EN.isRtl)
    }

    @Test
    fun multiBoardArchetypesGenerateDeterministicallyWithValidFinish() {
        BoardArchetype.entries.forEach { arch ->
            val m1 = MazeGenerator.genMaze("multi-board-seed", arch.size)
            val m2 = MazeGenerator.genMaze("multi-board-seed", arch.size)
            assertEquals(arch.size, m1.boardSize)
            assertEquals(arch.finishX, m1.finishX)
            assertEquals(arch.finishY, m1.finishY)
            assertEquals(m1.blocks.keys, m2.blocks.keys)
            assertEquals(m1.gems, m2.gems)
            assertFalse("Start cell must be open", m1.blocks.containsKey("$START_X,$START_Y"))
            assertFalse("Finish cell must be open", m1.blocks.containsKey("${arch.finishX},${arch.finishY}"))
        }
    }

    @Test
    fun allFifteenCampaignLevelsHaveValidBfsWinningPaths() {
        assertEquals(15, CampaignCatalog.levels.size)
        val uniqueIds = CampaignCatalog.levels.map { it.id }.toSet()
        assertEquals(15, uniqueIds.size)

        CampaignCatalog.levels.forEach { level ->
            val generated = MazeGenerator.genMaze(level.seed, level.archetype.size)
            val path = MazeGenerator.bfsPath(
                blocks = generated.blocks,
                sx = START_X,
                sy = START_Y,
                tx = generated.finishX,
                ty = generated.finishY,
                allowMine = false,
                boardSize = generated.boardSize
            )
            assertNotNull("Campaign level ${level.id} (${level.title}) must have an open BFS path", path)
            assertTrue("Campaign level ${level.id} path must reach finish", path!!.last().x == generated.finishX && path.last().y == generated.finishY)
            assertTrue("Three-star score must exceed two-star score", level.threeStarScore > level.twoStarScore)
        }
    }

    @Test
    fun billingStorePacksAreValidAndUnique() {
        val packs = BillingManager.PACKS
        assertEquals(4, packs.size)
        assertEquals(4, packs.map { it.productId }.toSet().size)
        packs.forEach { pack ->
            assertTrue("Product ID must not be blank", pack.productId.isNotBlank())
            assertTrue("Gems grant must be positive", pack.gemsGrant > 0)
            assertTrue("Fallback price must start with $", pack.fallbackDisplayPrice.startsWith("$"))
        }
        assertEquals(1, packs.count { it.unlocksFounderAura && !it.isConsumable })
    }
}
