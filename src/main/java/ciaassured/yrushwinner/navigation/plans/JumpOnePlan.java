package ciaassured.yrushwinner.navigation.plans;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.TimeCostModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JumpOnePlan implements PathPlan {
    private final PathPlan previous;
    private final BlockPos pos;
    private final double realTimeCost;
    private final double estimatedTimeCost;

    private JumpOnePlan(@NonNull PathPlan previous, BlockPos pos, double heuristicTimeCost) {
        this.previous = previous;
        this.pos = pos;

        // Calculate cost
        double distance = Math.sqrt(pos.getSquaredDistance(previous.getPos()));
        realTimeCost = (distance / TimeCostModel.WALK_SPEED_BPS) + TimeCostModel.JUMP_PENALTY_S + previous.getRealTimeCost();

        this.estimatedTimeCost = realTimeCost + heuristicTimeCost;
    }

    public static Optional<JumpOnePlan> makePlan(PathPlan previous, BlockPos dest, ClientWorld world, double heuristicTimeCost) {
        BlockPos pos = previous.getPos();

        if (dest.equals(pos)) {
            throw new IllegalArgumentException("next must differ from current");
        }

        int sdx = dest.getX() - pos.getX();
        int dy = dest.getY() - pos.getY();
        int sdz = dest.getZ() - pos.getZ();
        int dx = Math.abs(sdx);
        int dz = Math.abs(sdz);

        if (dx > 1 || dy != 1 || dz > 1) {
            return Optional.empty();
        }

        // No jump-in-place: dy=1 with no horizontal delta returns player to same block.
        if (dy == 1 && dx == 0 && dz == 0) {
            return Optional.empty();
        }

        // Gravity: if no solid block below, player is in freefall — only straight down valid.
        if (MoveHelpers.isPassable(world, pos.down())) {
            return Optional.empty();
        }

        // Feet and head at destination must be clear.
        if (!MoveHelpers.isPassable(world, dest) || !MoveHelpers.isPassable(world, dest.up())) {
            return Optional.empty();
        }

        // Never jump to a position directly above lava — player would fall in on landing.
        if (MoveHelpers.isLava(world, dest.down())) {
            return Optional.empty();
        }

        // Diagonal: player cannot clip through the two elbow blocks.
        if (dx == 1 && dz == 1) {
            BlockPos elbowX = new BlockPos(pos.getX() + sdx, pos.getY(), pos.getZ());
            BlockPos elbowZ = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + sdz);
            if (!MoveHelpers.isPassable(world, elbowX) || !MoveHelpers.isPassable(world, elbowX.up())
                    || !MoveHelpers.isPassable(world, elbowZ) || !MoveHelpers.isPassable(world, elbowZ.up())) {
                return Optional.empty();
            }
        }

        return Optional.of(new JumpOnePlan(previous, dest, heuristicTimeCost));
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public @Nullable PathPlan getPrevious() {
        return previous;
    }

    @Override
    public double getRealTimeCost() {
        return realTimeCost;
    }

    @Override
    public double getEstimatedTimeCost() {
        return estimatedTimeCost;
    }

    @Override
    public void execute() {
    }

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
