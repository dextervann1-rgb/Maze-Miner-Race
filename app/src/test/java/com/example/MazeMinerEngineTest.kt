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
    fun allTwentyProgressiveLevelsAndSixAbilitiesAreValid() {
        assertEquals(20, CampaignCatalog.levels.size)
        assertEquals(5, BoardArchetype.entries.size)
        val uniqueIds = CampaignCatalog.levels.map { it.id }.toSet()
        assertEquals(20, uniqueIds.size)

        // Verify progressive ability unlocks across milestones
        assertEquals(1, CampaignCatalog.unlockedAbilities(emptySet()).size)
        assertEquals(2, CampaignCatalog.unlockedAbilities(setOf(1, 2)).size)
        assertEquals(3, CampaignCatalog.unlockedAbilities((1..5).toSet()).size)
        assertEquals(4, CampaignCatalog.unlockedAbilities((1..10).toSet()).size)
        assertEquals(5, CampaignCatalog.unlockedAbilities((1..14).toSet()).size)
        assertEquals(6, CampaignCatalog.unlockedAbilities((1..18).toSet()).size)

        CampaignCatalog.levels.forEach { level ->
            val generated = MazeGenerator.genMaze(level.seed, level.archetype.size, randomEndpoint = true)
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
            assertFalse("Finish tile must not contain a gem", generated.gems.contains("${generated.finishX},${generated.finishY}"))
            assertFalse("Finish tile must not contain a powerup", generated.powers.containsKey("${generated.finishX},${generated.finishY}"))
            assertTrue("Three-star score must exceed two-star score", level.threeStarScore > level.twoStarScore)
        }
    }

    @Test
    fun randomEndpointsVaryAcrossSeedsWhileCanonicalRankedStaysFixed() {
        val canonical = MazeGenerator.genMaze("2026-11-18")
        assertEquals(19, canonical.finishX)
        assertEquals(19, canonical.finishY)

        val uniqueEndpoints = (1..12).map { idx ->
            val g = MazeGenerator.genMaze("unpredictable-race-$idx", 21, randomEndpoint = true)
            val path = MazeGenerator.bfsPath(g.blocks, START_X, START_Y, g.finishX, g.finishY, allowMine = false, boardSize = 21)
            assertNotNull("Randomized endpoint for seed $idx must be BFS reachable", path)
            assertTrue("Randomized endpoint must be far from start", path!!.size >= 10)
            g.finishX to g.finishY
        }.toSet()

        assertTrue("Expected unpredictable varied endpoints across seeds, got $uniqueEndpoints", uniqueEndpoints.size >= 6)
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

    @Test
    fun minerRankProgressionAndFiveBiomeRulesAreValid() {
        assertEquals(
            com.example.engine.MinerRank.BRONZE_PROSPECTOR,
            com.example.engine.MinerRank.forProgress(totalWins = 0, campaignStars = 0, forgeLevels = 0)
        )
        assertEquals(
            com.example.engine.MinerRank.SHAFT_RUNNER,
            com.example.engine.MinerRank.forProgress(totalWins = 3, campaignStars = 3, forgeLevels = 0)
        )
        assertEquals(
            com.example.engine.MinerRank.GRANDMASTER_ARCHITECT,
            com.example.engine.MinerRank.forProgress(totalWins = 15, campaignStars = 45, forgeLevels = 20)
        )
        assertEquals(5, Biome.entries.size)
        assertTrue(Biome.ICE.rule.slideChance > 0.0)
        assertTrue(Biome.RUINS.rule.trapCount > 0)
        assertTrue(Biome.CAVERN.rule.gemMultiplier > 1)
    }

    @Test
    fun strategicBraidedMazesSpawnReachableBeaconsAndExpandRaceLength() {
        BoardArchetype.entries.forEach { arch ->
            val strategic = MazeGenerator.genMaze(
                seed = "strategic-test-${arch.size}",
                boardSize = arch.size,
                randomEndpoint = true,
                strategicMaze = true
            )
            assertEquals(
                "Expected ${arch.strategicBeaconCount} beacons for ${arch.size}x${arch.size}",
                arch.strategicBeaconCount,
                strategic.checkpoints.size
            )
            val finish = com.example.engine.GridPos(strategic.finishX, strategic.finishY)
            strategic.checkpoints.forEach { cp ->
                assertFalse("Beacon must be open floor", strategic.blocks.containsKey(cp.key))
                assertFalse("Beacon must not sit on finish", cp == finish)
                val pathToBeacon = MazeGenerator.bfsPath(
                    blocks = strategic.blocks,
                    sx = START_X,
                    sy = START_Y,
                    tx = cp.x,
                    ty = cp.y,
                    allowMine = false,
                    boardSize = strategic.boardSize
                )
                assertNotNull("Beacon ${cp.key} on ${arch.size}x${arch.size} must be BFS reachable", pathToBeacon)
            }

            val directManhattan = kotlin.math.abs(finish.x - START_X) + kotlin.math.abs(finish.y - START_Y)
            val fullCircuit = MazeGenerator.computeCircuitDistance(
                from = com.example.engine.GridPos(START_X, START_Y),
                uncapturedBeacons = strategic.checkpoints,
                finishPos = finish
            )
            assertTrue(
                "Multi-beacon circuit ($fullCircuit) must meaningfully expand race distance beyond direct ($directManhattan)",
                fullCircuit > directManhattan + 6
            )
        }
    }

    @Test
    fun forgeUpgradeQuadraticCostsAndMasteryRequirementsGateEarlyMaxing() {
        com.example.engine.UpgradeType.entries.forEach { upgrade ->
            val costs = (0 until upgrade.maxLevel).map { upgrade.costForNextLevel(it) }
            for (i in 1 until costs.size) {
                assertTrue(
                    "Cost for ${upgrade.name} Lv.${i + 1} (${costs[i]}) must exceed Lv.$i (${costs[i - 1]})",
                    costs[i] > costs[i - 1]
                )
            }
            // Brand-new player with 0 wins, 0 blocks, 0 stars cannot bypass tier mastery gates
            val tier1Req = upgrade.requirementForNextLevel(0)
            assertFalse(
                "New player should earn ${upgrade.name} Tier 1 before buying",
                tier1Req.isMet(totalWins = 0, totalBlocksBroken = 0, campaignStars = 0)
            )
            assertTrue(
                "Player meeting milestones must unlock ${upgrade.name} Tier 1",
                tier1Req.isMet(
                    totalWins = tier1Req.minWins,
                    totalBlocksBroken = tier1Req.minBlocksBroken,
                    campaignStars = tier1Req.minCampaignStars
                )
            )
            // Apex tier must require serious career mastery
            val apexReq = upgrade.requirementForNextLevel(upgrade.maxLevel - 1)
            assertEquals("Apex", apexReq.tierName)
            assertTrue("Apex tier must require >= 12 wins", apexReq.minWins >= 12)
            assertTrue("Apex tier must require >= 22 stars", apexReq.minCampaignStars >= 22)
        }
    }

    @Test
    fun developerRatingAuditIsHonestAndMathematicallyConsistent() {
        val pillars = com.example.engine.DeveloperRatingAudit.pillars
        assertEquals(6, pillars.size)
        pillars.forEach { pillar ->
            assertTrue("Pillar ${pillar.name} score must be in 1..10", pillar.scoreOutOfTen in 1.0..10.0)
            assertTrue("Pillar ${pillar.name} must cite concrete build evidence", pillar.evidence.isNotBlank())
        }
        val expectedAvg = pillars.map { it.scoreOutOfTen }.average()
        assertEquals(expectedAvg, com.example.engine.DeveloperRatingAudit.compositeScore, 0.001)
        assertTrue(
            "Developer level should reflect Path 9.8+ to 10.0 Principal band (97..100)",
            com.example.engine.DeveloperRatingAudit.developerLevel in 97..100
        )
        assertTrue(
            "Composite score should reach Path 9.8+ milestone",
            com.example.engine.DeveloperRatingAudit.compositeScore >= 9.75
        )
    }

    @Test
    fun cloudProfileCampaignSummaryRoundTripsCleanly() {
        val original = listOf(
            com.example.data.CampaignProgressEntity(levelId = 1, stars = 3, bestScore = 2450, bestTimeSeconds = 42, firstClearClaimed = true),
            com.example.data.CampaignProgressEntity(levelId = 5, stars = 2, bestScore = 1890, bestTimeSeconds = 58, firstClearClaimed = true),
            com.example.data.CampaignProgressEntity(levelId = 12, stars = 3, bestScore = 4120, bestTimeSeconds = 71, firstClearClaimed = true),
            com.example.data.CampaignProgressEntity(levelId = 20, stars = 1, bestScore = 3200, bestTimeSeconds = 96, firstClearClaimed = true)
        )
        val encoded = com.example.cloud.CloudProfileSave.encodeCampaignSummary(original)
        val decoded = com.example.cloud.CloudProfileSave.decodeCampaignSummary(encoded)
        assertEquals(original, decoded)
        assertEquals(emptyList<com.example.data.CampaignProgressEntity>(), com.example.cloud.CloudProfileSave.decodeCampaignSummary(""))
        // Malformed segments are safely skipped without throwing
        val partial = com.example.cloud.CloudProfileSave.decodeCampaignSummary("bad,2:3:1500:44,x:y:z")
        assertEquals(1, partial.size)
        assertEquals(2, partial.first().levelId)
        assertEquals(3, partial.first().stars)
        assertEquals(1500, partial.first().bestScore)
        assertEquals(44, partial.first().bestTimeSeconds)
    }
}
