package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.TimeCostModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Movement while swimming. Applies when the player's current position is in water.
 *
 * Differences from land movement:
 *   - Any direction allowed (including straight up/down).
 *   - Player is 1 block tall — no head clearance check at destination.
 *   - No gravity constraint — water provides buoyancy.
 *   - No diagonal elbow check — movement is fluid in water.
 *   - Destination must be water or open air (exiting to surface).
 *   - Cost uses SWIM_SPEED_BPS horizontally/upward, SINK_SPEED_BPS downward.
 */
public class SwimPlan implements PathPlan {

    private final PathPlan previous;
    private final BlockPos pos;
    private final double realTimeCost;
    private final double estimatedTimeCost;

    private SwimPlan(@NonNull PathPlan previous, BlockPos pos, double heuristicTimeCost) {
        this.previous = previous;
        this.pos = pos;

        int dx = pos.getX() - previous.getPos().getX();
        int dy = pos.getY() - previous.getPos().getY();
        int dz = pos.getZ() - previous.getPos().getZ();

        double dist = Math.sqrt((double) (dx * dx + dy * dy + dz * dz));
        double speed = dy < 0 ? TimeCostModel.SINK_SPEED_BPS : TimeCostModel.SWIM_SPEED_BPS;

        realTimeCost = previous.getRealTimeCost() + dist / speed;
        estimatedTimeCost = realTimeCost + heuristicTimeCost;
    }

    public static Optional<SwimPlan> makePlan(PathPlan previous, BlockPos dest, ClientWorld world, double heuristicTimeCost) {
        BlockPos pos = previous.getPos();

        if (dest.equals(pos)) {
            throw new IllegalArgumentException("next must differ from current");
        }

        int sdx = dest.getX() - pos.getX();
        int sdy = dest.getY() - pos.getY();
        int sdz = dest.getZ() - pos.getZ();
        int dx = Math.abs(sdx);
        int dy = Math.abs(sdy);
        int dz = Math.abs(sdz);

        if (dx > 1 || dy > 1 || dz > 1) {
            return Optional.empty();
        }

        // Only applies when fully submerged — head block (pos.up()) must also be water.
        // 1-block-deep water (head above surface) is handled by WadeWaterPlan instead.
        if (!MoveHelpers.isWater(world, pos) || !MoveHelpers.isWater(world, pos.up())) {
            return Optional.empty();
        }

        // Destination must be water (continuing to swim) or open air (surfacing).
        // 1 block tall: no head clearance check.
        if (!isSwimmable(world, dest)) {
            return Optional.empty();
        }

        // Diagonal: cannot clip through elbow blocks in any plane.
        // For each pair of non-zero movement axes, check the two single-axis elbows.
        if (dx == 1 && dz == 1) {
            if (!isSwimmable(world, pos.add(sdx, 0,   0  ))
             || !isSwimmable(world, pos.add(0,   0,   sdz))) return Optional.empty();
        }
        if (dx == 1 && dy == 1) {
            if (!isSwimmable(world, pos.add(sdx, 0,   0  ))
             || !isSwimmable(world, pos.add(0,   sdy, 0  ))) return Optional.empty();
        }
        if (dy == 1 && dz == 1) {
            if (!isSwimmable(world, pos.add(0,   sdy, 0  ))
             || !isSwimmable(world, pos.add(0,   0,   sdz))) return Optional.empty();
        }

        return Optional.of(new SwimPlan(previous, dest, heuristicTimeCost));
    }

    /** Block is enterable while swimming: water or open air. */
    private static boolean isSwimmable(ClientWorld world, BlockPos pos) {
        return MoveHelpers.isWater(world, pos) || MoveHelpers.isPassable(world, pos);
    }

    @Override
    public BlockPos getPos() { return pos; }

    @Override
    public @Nullable PathPlan getPrevious() { return previous; }

    @Override
    public double getRealTimeCost() { return realTimeCost; }

    @Override
    public double getEstimatedTimeCost() { return estimatedTimeCost; }

    @Override
    public void execute() { }

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
