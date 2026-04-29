package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.gamestate.GameState;
import ciaassured.yrushwinner.navigation.goals.PathGoal;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.validators.PathValidator;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Optional;

public interface Navigator {
    /**
     * Find a time-optimal path from start toward the given goal.
     * Edge weights are seconds (from TimeCostModel), not distance.
     *
     * @return ordered waypoints from start (exclusive) to goal (inclusive),
     *         or an empty list if no path exists.
     */
    Optional<PathAction> findPath(BlockPos start, PathGoal goal, GameState gameState, Collection<PathValidator> validators) throws InterruptedException;
}
