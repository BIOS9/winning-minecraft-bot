package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.TimeCostModel;
import ciaassured.yrushwinner.navigation.actions.special.BasePathAction;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.gamestate.GameState;
import ciaassured.yrushwinner.util.BlockBreakTimeCalculator;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Mines the block(s) at the destination and moves into it.
 * Handles horizontal movement only (deltaY=0). Diagonals excluded on first pass.
 */
public class BlockBreakAction extends BasePathAction {

    private BlockBreakAction(@NonNull PathAction parent, BlockPos finalPosition,
                             double currentRealTimeCost, double totalEstimatedTimeCost,
                             GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    public static Optional<BlockBreakAction> makePlan(@NonNull PathAction parent, BlockPos destination,
                                                      ClientWorld world, Collection<ItemStack> tools,
                                                      BlockBreakTimeCalculator calc,
                                                      double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();
        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaY    = destination.getY() - currentPos.getY();
        int absDeltaX = Math.abs(destination.getX() - currentPos.getX());
        int absDeltaZ = Math.abs(destination.getZ() - currentPos.getZ());

        // Horizontal, non-diagonal only.
        if (deltaY != 0 || absDeltaX + absDeltaZ != 1) return Optional.empty();

        // Must be standing — underwater penalties and onGround flag derived from this.
        if (!MoveHelpers.isSolid(world, currentPos.down())) return Optional.empty();

        // destination must be solid: something to mine.
        if (MoveHelpers.isPassable(world, destination)) return Optional.empty();

        // Refuse lava — mining into it would kill the player.
        if (MoveHelpers.isLava(world, destination)) return Optional.empty();
        if (MoveHelpers.isLava(world, destination.down())) return Optional.empty();

        // Water context at the player's current position while mining.
        boolean headUnderwater = MoveHelpers.isWater(world, currentPos.up()); // eye level = feet + 1
        boolean onGround = true; // guaranteed by isSolid(currentPos.down()) above

        // Foot block.
        BlockState footState = world.getBlockState(destination);
        double breakCost = calc.fastestBreakTime(footState, world, destination, tools, headUnderwater, onGround);
        if (breakCost == Double.MAX_VALUE) return Optional.empty(); // unbreakable

        // Head block — mine it too if solid.
        BlockPos headPos = destination.up();
        if (!MoveHelpers.isPassable(world, headPos)) {
            if (MoveHelpers.isLava(world, headPos)) return Optional.empty();
            BlockState headState = world.getBlockState(headPos);
            double headCost = calc.fastestBreakTime(headState, world, headPos, tools, headUnderwater, onGround);
            if (headCost == Double.MAX_VALUE) return Optional.empty();
            breakCost += headCost;
        }

        double distance = Math.sqrt(destination.getSquaredDistance(currentPos));
        double moveTime = distance / TimeCostModel.SPRINT_SPEED_BPS;
        double currentRealTimeCost = parent.getCurrentRealTimeCost() + breakCost + moveTime;
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new BlockBreakAction(parent, destination, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
    }

    @Override
    public void execute() { }

    @Override
    public int renderColor() { return 0xFFFF2222; } // red
}
