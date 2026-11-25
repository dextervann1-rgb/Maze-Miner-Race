package com.example.engine

enum class BoardArchetype(
    val size: Int,
    val label: String,
    val actLabel: String
) {
    SPRINT_15(15, "15×15 Sprint Shaft", "Act I • 15×15"),
    CAVERN_19(19, "19×19 Crystal Cavern", "Act II • 19×19"),
    CLASSIC_21(21, "21×21 Classic Labyrinth", "Act III • 21×21"),
    ABYSSAL_25(25, "25×25 Abyssal Megamine", "Act IV • 25×25"),
    GRAND_29(29, "29×29 Grand Labyrinth", "Apex • 29×29");

    val finishX: Int get() = size - 2
    val finishY: Int get() = size - 2

    val strategicBeaconCount: Int
        get() = when {
            size <= 15 -> 2
            size <= 21 -> 3
            else -> 4
        }
}

/**
 * Progressive Miner Abilities unlocked by clearing Expedition Campaign milestones.
 * Active across all Expedition Campaign & Custom Board runs (Daily Ranked 21x21 remains canonical).
 */
enum class MinerAbility(
    val displayName: String,
    val shortEffect: String,
    val unlockAfterLevel: Int
) {
    QUICK_STEP(
        displayName = "Quick-Step Pathing",
        shortEffect = "Tap up to 5 tiles ahead for smooth corridor auto-stepping",
        unlockAfterLevel = 0
    ),
    FEVER_OVERDRIVE(
        displayName = "Fever Overdrive",
        shortEffect = "3×+ mining combo ignites Fever (+1 pickaxe power & molten aura)",
        unlockAfterLevel = 2
    ),
    SEISMIC_BLAST(
        displayName = "Seismic Shockwave",
        shortEffect = "Every 5th broken block blasts all 4 adjacent rock walls",
        unlockAfterLevel = 5
    ),
    AEGIS_START(
        displayName = "Aegis Auto-Shield",
        shortEffect = "Start every Expedition & Board run with 15s Hazard Shield",
        unlockAfterLevel = 10
    ),
    CRYSTAL_MAGNET(
        displayName = "Crystal Magnet & Sonar+",
        shortEffect = "+1 Sonar charge & auto-pulls gems from adjacent corridors",
        unlockAfterLevel = 14
    ),
    MASTER_DRILL(
        displayName = "Master Drill Surge",
        shortEffect = "+1 permanent Pickaxe damage & +15s starting clock on all boards",
        unlockAfterLevel = 18
    )
}

data class CampaignLevel(
    val id: Int,
    val title: String,
    val subtitle: String,
    val seed: String,
    val archetype: BoardArchetype,
    val biome: Biome,
    val modifier: Modifier,
    val difficulty: Difficulty,
    val rival: RivalPersonality,
    val twoStarScore: Int,
    val threeStarScore: Int,
    val firstClearGems: Int,
    val unlocksAbility: MinerAbility? = null
)

object CampaignCatalog {

    const val TOTAL_LEVELS: Int = 20
    const val MAX_STARS: Int = TOTAL_LEVELS * 3

    val levels: List<CampaignLevel> = listOf(
        // Act I — Sprint Shafts (15×15) — Levels 1–5
        CampaignLevel(
            id = 1,
            title = "Sunlit Quarry",
            subtitle = "Fast 15×15 warm-up shaft",
            seed = "campaign-01-sunlit",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.DESERT,
            modifier = Modifier.NORMAL,
            difficulty = Difficulty.EASY,
            rival = RivalPersonality.MOSS,
            twoStarScore = 400,
            threeStarScore = 470,
            firstClearGems = 30
        ),
        CampaignLevel(
            id = 2,
            title = "Frostline Tunnel",
            subtitle = "Ice slides & unlocks Fever Overdrive",
            seed = "campaign-02-frostline",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.ICE,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.EASY,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 430,
            threeStarScore = 510,
            firstClearGems = 35,
            unlocksAbility = MinerAbility.FEVER_OVERDRIVE
        ),
        CampaignLevel(
            id = 3,
            title = "Glowing Grotto",
            subtitle = "Double-value Cavern gems",
            seed = "campaign-03-grotto",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.CAVERN,
            modifier = Modifier.NORMAL,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 460,
            threeStarScore = 550,
            firstClearGems = 40
        ),
        CampaignLevel(
            id = 4,
            title = "Sunken Pillars",
            subtitle = "Navigate Ruins traps at sprint speed",
            seed = "campaign-04-pillars",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.RUINS,
            modifier = Modifier.MINING_X2,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 450,
            threeStarScore = 540,
            firstClearGems = 45
        ),
        CampaignLevel(
            id = 5,
            title = "Ember Chute",
            subtitle = "Act I Finale — unlocks Seismic Shockwave",
            seed = "campaign-05-emberchute",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.VOLCANO,
            modifier = Modifier.HEAT_WAVE,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.VEX,
            twoStarScore = 470,
            threeStarScore = 560,
            firstClearGems = 55,
            unlocksAbility = MinerAbility.SEISMIC_BLAST
        ),

        // Act II — Crystal Caverns (19×19) — Levels 6–10
        CampaignLevel(
            id = 6,
            title = "Sapphire Seam",
            subtitle = "19×19 crystal corridors & double gems",
            seed = "campaign-06-sapphire",
            archetype = BoardArchetype.CAVERN_19,
            biome = Biome.CAVERN,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 450,
            threeStarScore = 550,
            firstClearGems = 55
        ),
        CampaignLevel(
            id = 7,
            title = "Sandstorm Cut",
            subtitle = "Mid-scale desert endurance race",
            seed = "campaign-07-sandstorm",
            archetype = BoardArchetype.CAVERN_19,
            biome = Biome.DESERT,
            modifier = Modifier.NORMAL,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 460,
            threeStarScore = 560,
            firstClearGems = 60
        ),
        CampaignLevel(
            id = 8,
            title = "Cryo Fissure",
            subtitle = "19×19 frozen slides & rapid mining",
            seed = "campaign-08-cryo",
            archetype = BoardArchetype.CAVERN_19,
            biome = Biome.ICE,
            modifier = Modifier.MINING_X2,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.VEX,
            twoStarScore = 470,
            threeStarScore = 575,
            firstClearGems = 65
        ),
        CampaignLevel(
            id = 9,
            title = "Obelisk Vault",
            subtitle = "Low-vision ancient ruin labyrinth",
            seed = "campaign-09-obelisk",
            archetype = BoardArchetype.CAVERN_19,
            biome = Biome.RUINS,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 480,
            threeStarScore = 590,
            firstClearGems = 70
        ),
        CampaignLevel(
            id = 10,
            title = "Caldera Forge",
            subtitle = "Act II Finale — unlocks Aegis Auto-Shield",
            seed = "campaign-10-caldera",
            archetype = BoardArchetype.CAVERN_19,
            biome = Biome.VOLCANO,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 490,
            threeStarScore = 605,
            firstClearGems = 80,
            unlocksAbility = MinerAbility.AEGIS_START
        ),

        // Act III — Classic Labyrinths (21×21 Canonical) — Levels 11–15
        CampaignLevel(
            id = 11,
            title = "Cinder Drift",
            subtitle = "Classic 21×21 volcanic heat race",
            seed = "campaign-05-cinder",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.VOLCANO,
            modifier = Modifier.NORMAL,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 440,
            threeStarScore = 540,
            firstClearGems = 80
        ),
        CampaignLevel(
            id = 12,
            title = "Dune Mirage",
            subtitle = "Low vision + desert stamina drain",
            seed = "campaign-06-mirage",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.DESERT,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 460,
            threeStarScore = 560,
            firstClearGems = 85
        ),
        CampaignLevel(
            id = 13,
            title = "Glacier Vault",
            subtitle = "Slippery ice corridors vs Vex",
            seed = "campaign-07-glacier",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.ICE,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.VEX,
            twoStarScore = 480,
            threeStarScore = 590,
            firstClearGems = 90
        ),
        CampaignLevel(
            id = 14,
            title = "Crystal Sanctum",
            subtitle = "No-Sonar cavern — unlocks Crystal Magnet",
            seed = "campaign-08-sanctum",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.CAVERN,
            modifier = Modifier.NO_SONAR,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 500,
            threeStarScore = 620,
            firstClearGems = 95,
            unlocksAbility = MinerAbility.CRYSTAL_MAGNET
        ),
        CampaignLevel(
            id = 15,
            title = "Magma Core",
            subtitle = "Act III Boss — Heat Wave sprint",
            seed = "campaign-10-magmacore",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.VOLCANO,
            modifier = Modifier.HEAT_WAVE,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 510,
            threeStarScore = 630,
            firstClearGems = 105
        ),

        // Act IV — Abyssal Megamines (25×25) — Levels 16–20
        CampaignLevel(
            id = 16,
            title = "Obsidian Trench",
            subtitle = "Massive 25×25 deep cavern expedition",
            seed = "campaign-11-trench",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.CAVERN,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 520,
            threeStarScore = 660,
            firstClearGems = 110
        ),
        CampaignLevel(
            id = 17,
            title = "Permafrost Spire",
            subtitle = "25×25 high-speed ice labyrinth",
            seed = "campaign-12-permafrost",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.ICE,
            modifier = Modifier.MINING_X2,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 530,
            threeStarScore = 670,
            firstClearGems = 115
        ),
        CampaignLevel(
            id = 18,
            title = "Scorpion Abyss",
            subtitle = "Darkness marathon — unlocks Master Drill",
            seed = "campaign-13-scorpion",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.DESERT,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 540,
            threeStarScore = 680,
            firstClearGems = 125,
            unlocksAbility = MinerAbility.MASTER_DRILL
        ),
        CampaignLevel(
            id = 19,
            title = "Relic Labyrinth",
            subtitle = "Trap-laden ancient 25×25 ruins",
            seed = "campaign-14-relic",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.RUINS,
            modifier = Modifier.NO_SONAR,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 560,
            threeStarScore = 700,
            firstClearGems = 135
        ),
        CampaignLevel(
            id = 20,
            title = "Crown of the Deep",
            subtitle = "Final 25×25 Volcanic Master Gauntlet",
            seed = "campaign-15-crown",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.VOLCANO,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 600,
            threeStarScore = 760,
            firstClearGems = 200
        )
    )

    fun byId(id: Int): CampaignLevel? = levels.find { it.id == id }

    fun unlockedAbilities(clearedLevelIds: Set<Int>): Set<MinerAbility> {
        val clearedCount = clearedLevelIds.size
        return MinerAbility.entries.filter { ability ->
            ability.unlockAfterLevel == 0 ||
                ability.unlockAfterLevel in clearedLevelIds ||
                clearedCount >= ability.unlockAfterLevel
        }.toSet()
    }

    fun nextRecommendedLevel(clearedLevelIds: Set<Int>): CampaignLevel {
        return levels.firstOrNull { it.id !in clearedLevelIds } ?: levels.last()
    }
}

/**
 * Career Mastery Rank tiers reflecting overall progression across wins, campaign stars, and Forge gear.
 */
enum class MinerRank(
    val title: String,
    val badge: String,
    val minPowerPoints: Int
) {
    BRONZE_PROSPECTOR("Bronze Prospector", "TIER I", 0),
    SHAFT_RUNNER("Shaft Runner", "TIER II", 12),
    CRYSTAL_HUNTER("Crystal Hunter", "TIER III", 30),
    ABYSSAL_VETERAN("Abyssal Veteran", "TIER IV", 55),
    OBSIDIAN_LEGEND("Obsidian Legend", "TIER V", 85),
    GRANDMASTER_ARCHITECT("Grandmaster Architect", "APEX TIER", 120);

    companion object {
        fun forProgress(totalWins: Int, campaignStars: Int, forgeLevels: Int): MinerRank {
            val power = (totalWins * 4) + (campaignStars * 2) + (forgeLevels * 2)
            return entries.lastOrNull { power >= it.minPowerPoints } ?: BRONZE_PROSPECTOR
        }
    }
}
