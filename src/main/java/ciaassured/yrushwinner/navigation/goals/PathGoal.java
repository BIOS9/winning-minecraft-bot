package ciaassured.yrushwinner.navigation.goals;

import net.minecraft.util.math.BlockPos;

public interface PathGoal {
    /** Returns true when the player has reached the goal. */
    boolean isGoal(BlockPos pos);

    /**
     * Admissible heuristic — estimated seconds remaining from {@code pos} to the goal.
     * Must never overestimate (consistent with TimeCostModel edge weights).
     */
    double heuristic(BlockPos pos);
}
