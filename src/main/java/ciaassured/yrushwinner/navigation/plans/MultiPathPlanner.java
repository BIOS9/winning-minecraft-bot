package ciaassured.yrushwinner.navigation.plans;

import ciaassured.yrushwinner.navigation.goals.PathGoal;
import jakarta.inject.Inject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MultiPathPlanner implements PathPlanner {

    @Inject
    public MultiPathPlanner() { }

    // All positions reachable in a single step (≤1 block in each axis, excluding self).
    public Iterable<PathPlan> getNeighbours(PathPlan plan, PathGoal goal) {
        List<PathPlan> result = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos nextBlock = plan.getPos().add(dx, dy, dz);
                    result.addAll(findNextPlans(plan, nextBlock, goal.heuristic(nextBlock)));
                }
            }
        }
        return result;
    }

    public Collection<PathPlan> findNextPlans(PathPlan plan, BlockPos dest, double heuristicTimeCost) {
        HashMap<BlockPos, PathPlan> result = new HashMap<>();

        ClientWorld world = MinecraftClient.getInstance().world;

        WalkPlan.makePlan(plan, dest, world, heuristicTimeCost).ifPresent(p -> addIfCheaper(result, p));
        JumpOnePlan.makePlan(plan, dest, world, heuristicTimeCost).ifPresent(p -> addIfCheaper(result, p));
        FallPlan.makePlan(plan, dest, world, heuristicTimeCost).ifPresent(p -> addIfCheaper(result, p));
        SwimPlan.makePlan(plan, dest, world, heuristicTimeCost).ifPresent(p -> addIfCheaper(result, p));
        WadeWaterPlan.makePlan(plan, dest, world, heuristicTimeCost).ifPresent(p -> addIfCheaper(result, p));

        return result.values();
    }

    // Key by plan.getPos() (the actual landing position) so FallPlan landing positions
    // are deduplicated correctly even though they differ from the dest passed in.
    private void addIfCheaper(HashMap<BlockPos, PathPlan> map, PathPlan plan) {
        BlockPos pos = plan.getPos();
        PathPlan existing = map.get(pos);
        if (existing == null || plan.getRealTimeCost() < existing.getRealTimeCost()) {
            map.put(pos, plan);
        }
    }
}
