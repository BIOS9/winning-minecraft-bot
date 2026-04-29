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

public class WalkAction extends BasePathAction {

    private WalkAction(@NonNull PathAction parent, BlockPos finalPosition,
                       double currentRealTimeCost, double totalEstimatedTimeCost,
                       GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    public static Optional<WalkAction> makePlan(PathAction parent, BlockPos destination,
                                                ClientWorld world, double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();

        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaX    = destination.getX() - currentPos.getX();
        int deltaY    = destination.getY() - currentPos.getY();
        int deltaZ    = destination.getZ() - currentPos.getZ();
        int absDeltaX = Math.abs(deltaX);
        int absDeltaZ = Math.abs(deltaZ);

        if (absDeltaX > 1 || deltaY != 0 || absDeltaZ > 1) return Optional.empty();

        if (!MoveHelpers.isSolid(world, currentPos.down())) return Optional.empty();

        if (!MoveHelpers.isPassable(world, destination) || !MoveHelpers.isPassable(world, destination.up())) return Optional.empty();

        if (MoveHelpers.isLava(world, destination.down())) return Optional.empty();

        if (absDeltaX == 1 && absDeltaZ == 1) {
            BlockPos elbowX = new BlockPos(currentPos.getX() + deltaX, currentPos.getY(), currentPos.getZ());
            BlockPos elbowZ = new BlockPos(currentPos.getX(),           currentPos.getY(), currentPos.getZ() + deltaZ);
            if (!MoveHelpers.isPassable(world, elbowX) || !MoveHelpers.isPassable(world, elbowX.up())
                    || !MoveHelpers.isPassable(world, elbowZ) || !MoveHelpers.isPassable(world, elbowZ.up())) {
                return Optional.empty();
            }
        }

        double distance = Math.sqrt(destination.getSquaredDistance(currentPos));
        double currentRealTimeCost = parent.getCurrentRealTimeCost() + distance / TimeCostModel.SPRINT_SPEED_BPS;
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new WalkAction(parent, destination, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
    }

    @Override
    public void execute() { }
}
