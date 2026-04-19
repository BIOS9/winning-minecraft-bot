package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.goals.PathGoal;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Singleton
public class AStarNavigator implements Navigator {
    // IDEAS
    // Replace StepChecker with PathPlanner
    // Path planner checks if the next block is able to be moved to
    // It returns an array of ways to get to the next block, usually there will be only one
    // Each way to get to the next block is a MovementPlan or something, it could perform multiple actions including walking, mining, towering, climbing ladder, swimming, falling, etc.
    // Each MovementPlan has a time cost, which will include all the actions it needs to perform.
    // It's possible each plan will need to know about all the previous plans in the path so it can take into account broken blocks, placed blocks, etc.
    // Just keep it simple for now, basically check all movement plans, if movement plan exists, then movement is possible. Movement plan will be able to be executed in future, this couples the possibility of the action with the execution of the action which is what we want.


    private static final int MAX_NODES = 100_000;

    private final StepChecker stepChecker;

    @Inject
    public AStarNavigator(StepChecker stepChecker) {
        this.stepChecker = stepChecker;
    }

    @Override
    public List<BlockPos> findPath(BlockPos start, PathGoal goal) {
        PriorityQueue<NavNode> open = new PriorityQueue<>();
        Map<BlockPos, Double> gScores = new HashMap<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();

        double startH = goal.heuristic(start);
        open.add(new NavNode(start, 0.0, startH, null));
        gScores.put(start, 0.0);

        int visited = 0;
        while (!open.isEmpty() && visited < MAX_NODES) {
            NavNode current = open.poll();

            if (goal.isGoal(current.pos)) {
                return reconstructPath(parents, current.pos, start);
            }

            // Skip stale entries (open set may hold outdated nodes for same pos).
            if (current.gCost > gScores.getOrDefault(current.pos, Double.MAX_VALUE)) {
                continue;
            }
            visited++;

            for (BlockPos neighbour : neighbours(current.pos)) {
                if (!stepChecker.canMove(current.pos, neighbour)) {
                    continue;
                }

                double tentativeG = current.gCost + edgeCost(current.pos, neighbour);
                if (tentativeG < gScores.getOrDefault(neighbour, Double.MAX_VALUE)) {
                    gScores.put(neighbour, tentativeG);
                    parents.put(neighbour, current.pos);
                    open.add(new NavNode(neighbour, tentativeG, goal.heuristic(neighbour), current));
                }
            }
        }

        return Collections.emptyList();
    }

    // All positions reachable in a single step (≤1 block in each axis, excluding self).
    private static Iterable<BlockPos> neighbours(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    result.add(pos.add(dx, dy, dz));
                }
            }
        }
        return result;
    }

    /**
     * Edge weight in seconds.
     *   same Y  → horizontal distance / sprint speed
     *   up 1    → above + JUMP_PENALTY
     *   down 1  → horizontal distance / sprint speed (fall is free)
     */
    private static double edgeCost(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        double horizDist = Math.sqrt((double) dx * dx + (double) dz * dz);
        double moveCost = horizDist / TimeCostModel.SPRINT_SPEED_BPS;
        if (dy == 1) {
            moveCost += TimeCostModel.JUMP_PENALTY_S;
        }
        return moveCost;
    }

    private static List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> parents,
                                                   BlockPos goal, BlockPos start) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos cur = goal;
        while (!cur.equals(start)) {
            path.add(cur);
            cur = parents.get(cur);
        }
        Collections.reverse(path);
        return path;
    }
}
