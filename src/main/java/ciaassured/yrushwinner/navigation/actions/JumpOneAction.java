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

public class JumpOneAction extends BasePathAction {

    private JumpOneAction(@NonNull PathAction parent, BlockPos finalPosition,
                          double currentRealTimeCost, double totalEstimatedTimeCost,
                          GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    public static Optional<JumpOneAction> makePlan(PathAction parent, BlockPos destination,
                                                   ClientWorld world, double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();

        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaX    = destination.getX() - currentPos.getX();
        int deltaY    = destination.getY() - currentPos.getY();
        int deltaZ    = destination.getZ() - currentPos.getZ();
        int absDeltaX = Math.abs(deltaX);
        int absDeltaZ = Math.abs(deltaZ);

        if (absDeltaX > 1 || deltaY != 1 || absDeltaZ > 1) return Optional.empty();

        // No jump-in-place: deltaY=1 with no horizontal delta returns player to same block.
        if (absDeltaX == 0 && absDeltaZ == 0) return Optional.empty();

        // Gravity: if no solid block below, player is in freefall — only straight down valid.
        if (MoveHelpers.isPassable(world, currentPos.down())) return Optional.empty();

        // Feet and head at destination must be clear.
        if (!MoveHelpers.isPassable(world, destination) || !MoveHelpers.isPassable(world, destination.up())) return Optional.empty();

        // Head +1 at current block must be clear.
        if (!MoveHelpers.isPassable(world, currentPos.up(2))) return Optional.empty();

        // Never jump to a position directly above lava — player would fall in on landing.
        if (MoveHelpers.isLava(world, destination.down())) return Optional.empty();

        // Diagonal: player cannot clip through the two elbow blocks.
        if (absDeltaX == 1 && absDeltaZ == 1) {
            BlockPos elbowX = new BlockPos(currentPos.getX() + deltaX, currentPos.getY(), currentPos.getZ());
            BlockPos elbowZ = new BlockPos(currentPos.getX(),           currentPos.getY(), currentPos.getZ() + deltaZ);
            if (!MoveHelpers.isPassable(world, elbowX.up())  || !MoveHelpers.isPassable(world, elbowX.up(2)) ||
                !MoveHelpers.isPassable(world, elbowZ.up())  || !MoveHelpers.isPassable(world, elbowZ.up(2))) {
                return Optional.empty();
            }
        }

        double distance = Math.sqrt(destination.getSquaredDistance(currentPos));
        double currentRealTimeCost = parent.getCurrentRealTimeCost()
                + distance / TimeCostModel.WALK_SPEED_BPS
                + TimeCostModel.JUMP_PENALTY_S;
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new JumpOneAction(parent, destination, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
    }

    @Override
    public void execute() { }
}
