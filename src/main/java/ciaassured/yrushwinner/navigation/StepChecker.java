package ciaassured.yrushwinner.navigation;

import net.minecraft.util.math.BlockPos;

public interface StepChecker {
    /**
     * Returns true if a player can move one step from {@code from} to {@code to}.
     * Positions must be adjacent (≤1 block in each axis). Throws if identical.
     */
    boolean canMove(BlockPos from, BlockPos to);
}
