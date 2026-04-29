package ciaassured.yrushwinner.navigation.planning;

import ciaassured.yrushwinner.navigation.actions.*;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.goals.PathGoal;
import ciaassured.yrushwinner.util.BlockBreakTimeCalculator;
import jakarta.inject.Inject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MultiPathPlanner implements PathPlanner {

    private final BlockBreakTimeCalculator breakCalc;

    @Inject
    public MultiPathPlanner(BlockBreakTimeCalculator breakCalc) {
        this.breakCalc = breakCalc;
    }

    // All positions reachable in a single step (≤1 block in each axis, excluding self).
    public Iterable<PathAction> getNeighbours(PathAction action, PathGoal goal) {
        List<PathAction> result = new ArrayList<>(26);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;

                    BlockPos candidatePos = action.getFinalPosition().add(offsetX, offsetY, offsetZ);
                    result.addAll(findNextActions(action, candidatePos, goal.heuristic(candidatePos)));
                }
            }
        }
        return result;
    }

    public Collection<PathAction> findNextActions(PathAction action, BlockPos destination, double heuristicTimeCost) {
        HashMap<BlockPos, PathAction> result = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        List<ItemStack> tools = client.player != null
                ? client.player.getInventory().getMainStacks()
                : Collections.emptyList();

        WalkAction.makePlan(action, destination, world, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));
        JumpOneAction.makePlan(action, destination, world, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));
        FallAction.makePlan(action, destination, world, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));
        SwimAction.makePlan(action, destination, world, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));
        WadeWaterAction.makePlan(action, destination, world, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));
        BlockBreakAction.makePlan(action, destination, world, tools, breakCalc, heuristicTimeCost).ifPresent(a -> addIfCheaper(result, a));

        return result.values();
    }

    // Key by getFinalPosition() so FallAction landing positions are deduplicated correctly
    // even though they may differ from the destination passed in.
    private void addIfCheaper(HashMap<BlockPos, PathAction> map, PathAction action) {
        BlockPos finalPosition = action.getFinalPosition();
        PathAction existing = map.get(finalPosition);
        if (existing == null || action.getCurrentRealTimeCost() < existing.getCurrentRealTimeCost()) {
            map.put(finalPosition, action);
        }
    }
}
