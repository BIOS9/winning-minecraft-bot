package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.TimeCostModel;
import ciaassured.yrushwinner.navigation.actions.special.BasePathAction;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.gamestate.GameState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Movement while swimming. Applies when the player's current position is in water.
 *
 * Differences from land movement:
 *   - Any direction allowed (including straight up/down).
 *   - Player is 1 block tall — no head clearance check at destination.
 *   - No gravity constraint — water provides buoyancy.
 *   - Destination must be water or open air (exiting to surface).
 *   - Cost uses SWIM_SPEED_BPS horizontally/upward, SINK_SPEED_BPS downward.
 */
public class SwimAction extends BasePathAction {

    private SwimAction(@NonNull PathAction parent, BlockPos finalPosition,
                       double currentRealTimeCost, double totalEstimatedTimeCost, GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    public static Optional<SwimAction> makePlan(PathAction parent, BlockPos destination,
                                                ClientWorld world, double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();

        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaX    = destination.getX() - currentPos.getX();
        int deltaY    = destination.getY() - currentPos.getY();
        int deltaZ    = destination.getZ() - currentPos.getZ();
        int absDeltaX = Math.abs(deltaX);
        int absDeltaY = Math.abs(deltaY);
        int absDeltaZ = Math.abs(deltaZ);

        if (absDeltaX > 1 || absDeltaY > 1 || absDeltaZ > 1) return Optional.empty();

        // Only applies when fully submerged — head block (currentPos.up()) must also be water.
        // 1-block-deep water (head above surface) is handled by WadeWaterAction instead.
        if (!MoveHelpers.isWater(world, currentPos) || !MoveHelpers.isWater(world, currentPos.up())) return Optional.empty();

        // Destination must be water (continuing to swim) or open air (surfacing).
        // 1 block tall: no head clearance check.
        if (!isSwimmable(world, destination)) return Optional.empty();

        // Diagonal: cannot clip through elbow blocks in any plane.
        if (absDeltaX == 1 && absDeltaZ == 1) {
            if (!isSwimmable(world, currentPos.add(deltaX, 0,      0     ))
             || !isSwimmable(world, currentPos.add(0,      0,      deltaZ))) return Optional.empty();
        }
        if (absDeltaX == 1 && absDeltaY == 1) {
            if (!isSwimmable(world, currentPos.add(deltaX, 0,      0     ))
             || !isSwimmable(world, currentPos.add(0,      deltaY, 0     ))) return Optional.empty();
        }
        if (absDeltaY == 1 && absDeltaZ == 1) {
            if (!isSwimmable(world, currentPos.add(0,      deltaY, 0     ))
             || !isSwimmable(world, currentPos.add(0,      0,      deltaZ))) return Optional.empty();
        }

        double distance = Math.sqrt((double) (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
        double speed = deltaY < 0 ? TimeCostModel.SINK_SPEED_BPS : TimeCostModel.SWIM_SPEED_BPS;
        double currentRealTimeCost = parent.getCurrentRealTimeCost() + distance / speed;
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new SwimAction(parent, destination, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
    }

    /** Block is enterable while swimming: water or open air. */
    private static boolean isSwimmable(ClientWorld world, BlockPos pos) {
        return MoveHelpers.isWater(world, pos) || MoveHelpers.isPassable(world, pos);
    }

    @Override
    public void execute() { }

    @Override
    public int renderColor() { return 0xFF00CCFF; } // cyan
}
