package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.goals.PathGoal;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface Navigator {
    /**
     * Find a time-optimal path from start toward the given goal.
     * Edge weights are seconds (from TimeCostModel), not distance.
     *
     * @return ordered waypoints from start (exclusive) to goal (inclusive),
     *         or an empty list if no path exists.
     */
    List<BlockPos> findPath(BlockPos start, PathGoal goal);
}
