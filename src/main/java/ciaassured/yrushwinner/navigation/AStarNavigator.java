package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.goals.PathGoal;
import ciaassured.yrushwinner.navigation.actions.PathPlan;
import ciaassured.yrushwinner.navigation.actions.RootPathPlan;
import jakarta.inject.Inject;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class AStarNavigator implements Navigator {
    // IDEAS
    // Replace StepChecker with PathPlanner
    // Path planner checks if the next block is able to be moved to
    // It returns an array of ways to get to the next block, usually there will be only one
    // Each way to get to the next block is a MovementPlan or something, it could perform multiple actions including walking, mining, towering, climbing ladder, swimming, falling, etc.
    // Each MovementPlan has a time cost, which will include all the actions it needs to perform.
    // It's possible each plan will need to know about all the previous plans in the path so it can take into account broken blocks, placed blocks, etc.
    // Just keep it simple for now, basically check all movement plans, if movement plan exists, then movement is possible. Movement plan will be able to be executed in future, this couples the possibility of the action with the execution of the action which is what we want.


    private static final int MAX_NODES = 10_000_000;

    private final PathPlanner pathPlanner;

    @Inject
    public AStarNavigator(PathPlanner pathPlanner) {
        this.pathPlanner = pathPlanner;
    }

    @Override
    public Optional<PathPlan> findPath(BlockPos start, PathGoal goal, double maxDistance) {
        PriorityQueue<PathPlan> open = new PriorityQueue<>();
        Map<BlockPos, Double> lowestTimeCosts = new HashMap<>();

        open.add(new RootPathPlan(start));
        lowestTimeCosts.put(start, 0.0);

        int visited = 0;
        while (!open.isEmpty() && visited < MAX_NODES) {
            PathPlan current = open.poll();

            if (goal.isGoal(current.getPos())) {
                return Optional.of(current);
            }

            // Skip stale entries (open set may hold outdated nodes for same pos).
            if (current.getRealTimeCost() > lowestTimeCosts.getOrDefault(current.getPos(), Double.MAX_VALUE)) {
                continue;
            }
            visited++;

            for (PathPlan neighbour : pathPlanner.getNeighbours(current, goal)) {
                if (neighbour.getPos().getSquaredDistance(start) > maxDistance * maxDistance) {
                    continue;
                }
                if (neighbour.getRealTimeCost() < lowestTimeCosts.getOrDefault(neighbour.getPos(), Double.MAX_VALUE)) {
                    lowestTimeCosts.put(neighbour.getPos(), neighbour.getRealTimeCost());
                    open.add(neighbour);
                }
            }
        }

        return Optional.empty();
    }
}
