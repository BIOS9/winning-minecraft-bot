package ciaassured.yrushwinner.util;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;

/**
 * Calculates block break time in seconds using the Minecraft breaking formula.
 * Source: https://minecraft.wiki/w/Breaking#Speed
 *
 * Does not account for situational modifiers (water, not-on-ground, enchantments).
 * Those multiply ticks by 5 each and should be applied by the caller if needed.
 */
@Singleton
public final class BlockBreakTimeCalculator {

    // Ticks divisor: correct-tool harvest → /30, wrong tool → /100
    // Source: https://minecraft.wiki/w/Breaking#Speed
    private static final double DIVISOR_CORRECT_TOOL = 30.0;
    private static final double DIVISOR_WRONG_TOOL   = 100.0;
    private static final double TICKS_PER_SECOND     = 20.0;

    @Inject
    public BlockBreakTimeCalculator() {}

    /**
     * Fastest break time in seconds using best tool from given set.
     * Bare hand always considered; pass empty collection for bare-hand only.
     */
    public double fastestBreakTime(BlockState state, ClientWorld world, BlockPos pos,
                                   Collection<ItemStack> tools) {
        return fastestBreakTime(state, world, pos, tools, false, true);
    }

    /**
     * Fastest break time in seconds using best tool from given set.
     *
     * @param headUnderwater true if player's eye position is inside a water block (×5 penalty)
     * @param onGround       true if player is standing on a solid block (false = ×5 penalty)
     */
    public double fastestBreakTime(BlockState state, ClientWorld world, BlockPos pos,
                                   Collection<ItemStack> tools,
                                   boolean headUnderwater, boolean onGround) {
        double best = breakTime(state, world, pos, ItemStack.EMPTY, headUnderwater, onGround);
        for (ItemStack tool : tools) {
            best = Math.min(best, breakTime(state, world, pos, tool, headUnderwater, onGround));
        }
        return best;
    }

    /**
     * Break time in seconds for a specific tool (or bare hand via {@link ItemStack#EMPTY}).
     * Returns {@link Double#MAX_VALUE} for unbreakable blocks (hardness &lt; 0, e.g. bedrock).
     * Assumes no mining penalties (on ground, head above water).
     */
    public double breakTime(BlockState state, ClientWorld world, BlockPos pos, ItemStack tool) {
        return breakTime(state, world, pos, tool, false, true);
    }

    /**
     * Break time in seconds for a specific tool (or bare hand via {@link ItemStack#EMPTY}).
     * Returns {@link Double#MAX_VALUE} for unbreakable blocks (hardness &lt; 0, e.g. bedrock).
     *
     * @param headUnderwater true if player's eye position is inside a water block (×5 penalty)
     * @param onGround       true if player is standing on a solid block (false = ×5 penalty)
     */
    public double breakTime(BlockState state, ClientWorld world, BlockPos pos, ItemStack tool,
                            boolean headUnderwater, boolean onGround) {
        float hardness = state.getHardness(world, pos);
        if (hardness < 0) return Double.MAX_VALUE;
        if (hardness == 0) return 1.0 / TICKS_PER_SECOND; // instant-break = 1 tick

        float speed = tool.getMiningSpeedMultiplier(state);
        // Penalties divide effective speed before ceil — order matters for rounding accuracy.
        // Source: https://minecraft.wiki/w/Breaking#Speed
        double divisor = tool.isSuitableFor(state) ? DIVISOR_CORRECT_TOOL : DIVISOR_WRONG_TOOL;
        if (headUnderwater) divisor *= 5;
        if (!onGround)      divisor *= 5;

        int ticks = (int) Math.ceil(hardness * divisor / speed);
        return ticks / TICKS_PER_SECOND;
    }
}
