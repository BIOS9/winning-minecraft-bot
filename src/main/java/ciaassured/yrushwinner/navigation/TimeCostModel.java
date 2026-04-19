package ciaassured.yrushwinner.navigation;

public final class TimeCostModel {

    // Movement speeds (blocks per second)
    // Source: https://minecraft.wiki/w/Sprinting
    public static final double SPRINT_SPEED_BPS = 5.612;
    public static final double WALK_SPEED_BPS   = 4.317;
    public static final double SWIM_SPEED_BPS   = 2.2;    // horizontal underwater
    public static final double SINK_SPEED_BPS   = 1.5;    // descending in water
    public static final double JUMP_PENALTY_S   = 0.5;    // extra time per jump in navigation

    // Block placement (tower-jump rhythm)
    // Source: empirical — validated by calibration suite
    public static final double PLACE_BLOCK_S = 0.25;

    // Mining times (seconds per block) — barehanded
    // Source: https://minecraft.wiki/w/Breaking
    public static final double MINE_DIRT_BARE_S   = 0.75;
    public static final double MINE_GRAVEL_BARE_S = 0.9;
    public static final double MINE_SAND_BARE_S   = 0.75;
    public static final double MINE_LEAVES_BARE_S = 0.3;
    public static final double MINE_WOOD_BARE_S   = 3.0;

    // Mining times — wooden pickaxe
    public static final double MINE_STONE_WOOD_PICK_S = 1.15;
    public static final double MINE_DIRT_WOOD_PICK_S  = 0.6;
    // Ores are impractically slow with wooden pickaxe — treat as impassable
    public static final double MINE_ORE_WOOD_PICK_S   = 15.0;

    // Crafting times (seconds per step, including placing crafting table)
    public static final double CRAFT_PLANKS_S         = 0.5;
    public static final double PLACE_CRAFTING_TABLE_S = 0.5;
    public static final double CRAFT_STICKS_S         = 0.5;
    public static final double CRAFT_WOOD_PICKAXE_S   = 0.5;
    // Total time to craft a wooden pickaxe from scratch (assumes 1 log already in hand)
    public static final double TOTAL_CRAFT_PICKAXE_FROM_LOG_S =
            CRAFT_PLANKS_S + PLACE_CRAFTING_TABLE_S + CRAFT_STICKS_S + CRAFT_WOOD_PICKAXE_S;

    // Breathing
    // Source: https://minecraft.wiki/w/Oxygen
    public static final double MAX_AIR_TICKS    = 300.0; // 15 seconds at 20 tps
    public static final double AIR_DANGER_TICKS = 60.0;  // AirManager triggers below this

    private TimeCostModel() {}
}
