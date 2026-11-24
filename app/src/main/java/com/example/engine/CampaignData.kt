package com.example.engine

enum class BoardArchetype(val size: Int, val label: String) {
    SPRINT_15(15, "15×15 Sprint Shaft"),
    CLASSIC_21(21, "21×21 Classic Labyrinth"),
    ABYSSAL_25(25, "25×25 Abyssal Megamine");

    val finishX: Int get() = size - 2
    val finishY: Int get() = size - 2
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
    val firstClearGems: Int
)

object CampaignCatalog {

    val levels: List<CampaignLevel> = listOf(
        // Act I — Sprint Shafts (15×15)
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
            subtitle = "Learn Ice slides & stamina regen",
            seed = "campaign-02-frostline",
            archetype = BoardArchetype.SPRINT_15,
            biome = Biome.ICE,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.EASY,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 430,
            threeStarScore = 510,
            firstClearGems = 35
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

        // Act II — Classic Labyrinths (21×21 Canonical)
        CampaignLevel(
            id = 5,
            title = "Cinder Drift",
            subtitle = "Classic 21×21 volcanic heat race",
            seed = "campaign-05-cinder",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.VOLCANO,
            modifier = Modifier.NORMAL,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 420,
            threeStarScore = 520,
            firstClearGems = 50
        ),
        CampaignLevel(
            id = 6,
            title = "Dune Mirage",
            subtitle = "Low vision + desert stamina drain",
            seed = "campaign-06-mirage",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.DESERT,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 440,
            threeStarScore = 540,
            firstClearGems = 55
        ),
        CampaignLevel(
            id = 7,
            title = "Glacier Vault",
            subtitle = "Slippery ice corridors vs Vex",
            seed = "campaign-07-glacier",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.ICE,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.VEX,
            twoStarScore = 470,
            threeStarScore = 580,
            firstClearGems = 60
        ),
        CampaignLevel(
            id = 8,
            title = "Crystal Sanctum",
            subtitle = "Rich gem cavern without Sonar",
            seed = "campaign-08-sanctum",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.CAVERN,
            modifier = Modifier.NO_SONAR,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.GOLDIE,
            twoStarScore = 490,
            threeStarScore = 610,
            firstClearGems = 65
        ),
        CampaignLevel(
            id = 9,
            title = "Pharaoh's Catacomb",
            subtitle = "Hidden traps & Darkness modifier",
            seed = "campaign-09-catacomb",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.RUINS,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 460,
            threeStarScore = 570,
            firstClearGems = 70
        ),
        CampaignLevel(
            id = 10,
            title = "Magma Core",
            subtitle = "Volcano Heat Wave boss sprint",
            seed = "campaign-10-magmacore",
            archetype = BoardArchetype.CLASSIC_21,
            biome = Biome.VOLCANO,
            modifier = Modifier.HEAT_WAVE,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 480,
            threeStarScore = 590,
            firstClearGems = 80
        ),

        // Act III — Abyssal Megamines (25×25)
        CampaignLevel(
            id = 11,
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
            firstClearGems = 90
        ),
        CampaignLevel(
            id = 12,
            title = "Permafrost Spire",
            subtitle = "25×25 high-speed ice labyrinth",
            seed = "campaign-12-permafrost",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.ICE,
            modifier = Modifier.MINING_X2,
            difficulty = Difficulty.NORMAL,
            rival = RivalPersonality.RUBBLE,
            twoStarScore = 500,
            threeStarScore = 640,
            firstClearGems = 95
        ),
        CampaignLevel(
            id = 13,
            title = "Scorpion Abyss",
            subtitle = "Endurance desert marathon in Darkness",
            seed = "campaign-13-scorpion",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.DESERT,
            modifier = Modifier.DARKNESS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 520,
            threeStarScore = 660,
            firstClearGems = 100
        ),
        CampaignLevel(
            id = 14,
            title = "Relic Labyrinth",
            subtitle = "Trap-laden ancient 25×25 ruins",
            seed = "campaign-14-relic",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.RUINS,
            modifier = Modifier.NO_SONAR,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 540,
            threeStarScore = 680,
            firstClearGems = 110
        ),
        CampaignLevel(
            id = 15,
            title = "Crown of the Deep",
            subtitle = "Final 25×25 Volcanic Master Gauntlet",
            seed = "campaign-15-crown",
            archetype = BoardArchetype.ABYSSAL_25,
            biome = Biome.VOLCANO,
            modifier = Modifier.DOUBLE_GEMS,
            difficulty = Difficulty.HARD,
            rival = RivalPersonality.VEX,
            twoStarScore = 580,
            threeStarScore = 740,
            firstClearGems = 150
        )
    )

    fun byId(id: Int): CampaignLevel? = levels.find { it.id == id }
}
