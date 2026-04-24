package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.goals.PathGoal;
import ciaassured.yrushwinner.navigation.plans.PathPlan;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

public interface Navigator {
    /**
     * Find a time-optimal path from start toward the given goal.
     * Edge weights are seconds (from TimeCostModel), not distance.
     *
     * @return ordered waypoints from start (exclusive) to goal (inclusive),
     *         or an empty list if no path exists.
     */
    Optional<PathPlan> findPath(BlockPos start, PathGoal goal, double maxDistance);
}
