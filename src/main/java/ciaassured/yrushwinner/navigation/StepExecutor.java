package ciaassured.yrushwinner.navigation;

import net.minecraft.util.math.BlockPos;

public interface StepExecutor {
    /**
     * Execute a single movement step from {@code from} to {@code to}.
     * Called on the bot thread. Blocks until the step completes or fails.
     *
     * @return true if the step succeeded; false if the player could not reach {@code to}
     */
    boolean executeStep(BlockPos from, BlockPos to);
}
