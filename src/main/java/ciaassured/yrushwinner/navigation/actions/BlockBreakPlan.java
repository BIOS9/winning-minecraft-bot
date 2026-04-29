package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.TimeCostModel;
import ciaassured.yrushwinner.util.BlockBreakTimeCalculator;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Mines the block(s) at the destination and moves into it.
 * Handles horizontal movement only (dy=0). Diagonals excluded on first pass.
 */
public class BlockBreakPlan implements PathPlan {

    private final PathPlan previous;
    private final BlockPos pos;
    private final double realTimeCost;
    private final double estimatedTimeCost;

    private BlockBreakPlan(@NonNull PathPlan previous, BlockPos pos, double breakCost, double heuristicTimeCost) {
        this.previous = previous;
        this.pos = pos;
        double moveTime = Math.sqrt(pos.getSquaredDistance(previous.getPos())) / TimeCostModel.SPRINT_SPEED_BPS;
        realTimeCost = previous.getRealTimeCost() + breakCost + moveTime;
        estimatedTimeCost = realTimeCost + heuristicTimeCost;
    }

    public static Optional<BlockBreakPlan> makePlan(@NonNull PathPlan previous, BlockPos dest,
                                                    ClientWorld world, Collection<ItemStack> tools,
                                                    BlockBreakTimeCalculator calc,
                                                    double heuristicTimeCost) {
        BlockPos pos = previous.getPos();
        if (dest.equals(pos)) throw new IllegalArgumentException("next must differ from current");

        int dy = dest.getY() - pos.getY();
        int dx = Math.abs(dest.getX() - pos.getX());
        int dz = Math.abs(dest.getZ() - pos.getZ());

        // Horizontal, non-diagonal only.
        if (dy != 0 || dx + dz != 1) return Optional.empty();

        // Must be standing — underwater penalties and onGround flag derived from this.
        if (!MoveHelpers.isSolid(world, pos.down())) return Optional.empty();

        // dest must be solid: something to mine.
        if (MoveHelpers.isPassable(world, dest)) return Optional.empty();

        // Refuse lava — mining into it would kill the player.
        if (MoveHelpers.isLava(world, dest)) return Optional.empty();
        if (MoveHelpers.isLava(world, dest.down())) return Optional.empty();

        // Water context at the player's current position while mining.
        boolean headUnderwater = MoveHelpers.isWater(world, pos.up()); // eye level = feet + 1
        boolean onGround = true; // guaranteed by isSolid(pos.down()) above

        // Foot block.
        BlockState footState = world.getBlockState(dest);
        double breakCost = calc.fastestBreakTime(footState, world, dest, tools, headUnderwater, onGround);
        if (breakCost == Double.MAX_VALUE) return Optional.empty(); // unbreakable

        // Head block — mine it too if solid.
        BlockPos headPos = dest.up();
        if (!MoveHelpers.isPassable(world, headPos)) {
            if (MoveHelpers.isLava(world, headPos)) return Optional.empty();
            BlockState headState = world.getBlockState(headPos);
            double headCost = calc.fastestBreakTime(headState, world, headPos, tools, headUnderwater, onGround);
            if (headCost == Double.MAX_VALUE) return Optional.empty();
            breakCost += headCost;
        }

        return Optional.of(new BlockBreakPlan(previous, dest, breakCost, heuristicTimeCost));
    }

    @Override public BlockPos getPos() { return pos; }
    @Override public @Nullable PathPlan getPrevious() { return previous; }
    @Override public double getRealTimeCost() { return realTimeCost; }
    @Override public double getEstimatedTimeCost() { return estimatedTimeCost; }
    @Override public void execute() {}

    @Override
    public List<BlockPos> getCompletePath() {
        ArrayList<BlockPos> path = new ArrayList<>();
        PathPlan current = this;
        while (current != null) {
            path.add(current.getPos());
            current = current.getPrevious();
        }
        return path.reversed();
    }

    @Override
    public int compareTo(@NonNull PathPlan other) {
        return Double.compare(getEstimatedTimeCost(), other.getEstimatedTimeCost());
    }
}
