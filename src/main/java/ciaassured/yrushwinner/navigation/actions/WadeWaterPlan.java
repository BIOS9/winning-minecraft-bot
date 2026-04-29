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
 * Walking into or through shallow water (1 block deep — head above surface).
 *
 * Bridges WalkPlan (land) and SwimPlan (fully submerged):
 *   - Player must be grounded on solid ground (not floating).
 *   - Destination is water but not covered — head stays above surface.
 *   - Normal 2-block height and diagonal elbow checks apply.
 *   - Speed: SWIM_SPEED_BPS (water resistance).
 *
 * Exiting shallow water back to land is handled by WalkPlan since WalkPlan
 * only requires the source to be grounded — which wading still satisfies.
 */
public class WadeWaterPlan implements PathPlan {

    private final PathPlan previous;
    private final BlockPos pos;
    private final double realTimeCost;
    private final double estimatedTimeCost;

    private WadeWaterPlan(@NonNull PathPlan previous, BlockPos pos, double heuristicTimeCost) {
        this.previous = previous;
        this.pos = pos;

        double dx = pos.getX() - previous.getPos().getX();
        double dz = pos.getZ() - previous.getPos().getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        realTimeCost = previous.getRealTimeCost() + dist / TimeCostModel.WADE_SPEED_BPS;
        estimatedTimeCost = realTimeCost + heuristicTimeCost;
    }

    public static Optional<WadeWaterPlan> makePlan(PathPlan previous, BlockPos dest, ClientWorld world, double heuristicTimeCost) {
        BlockPos pos = previous.getPos();

        if (dest.equals(pos)) {
            throw new IllegalArgumentException("next must differ from current");
        }

        int sdx = dest.getX() - pos.getX();
        int dy  = dest.getY() - pos.getY();
        int sdz = dest.getZ() - pos.getZ();
        int dx  = Math.abs(sdx);
        int dz  = Math.abs(sdz);

        // Horizontal or dropping down one block into water; never jumping up.
        if (dx > 1 || dy > 0 || dz > 1) {
            return Optional.empty();
        }

        // Destination must be shallow water: feet in water, head above.
        if (!MoveHelpers.isWater(world, dest)) {
            return Optional.empty();
        }
        if (MoveHelpers.isWater(world, pos) && MoveHelpers.isWater(world, pos.up())) {
            return Optional.empty(); // fully submerged — SwimPlan handles this
        }
        if (!MoveHelpers.isEnterable(world, dest.up())) {
            return Optional.empty(); // head blocked by something solid
        }

        // Diagonal: elbow blocks must be enterable — cannot clip through walls.
        if (dx == 1 && dz == 1) {
            BlockPos elbowX = new BlockPos(pos.getX() + sdx, pos.getY(), pos.getZ());
            BlockPos elbowZ = new BlockPos(pos.getX(),       pos.getY(), pos.getZ() + sdz);
            if (!MoveHelpers.isEnterable(world, elbowX) || !MoveHelpers.isEnterable(world, elbowX.up())
             || !MoveHelpers.isEnterable(world, elbowZ) || !MoveHelpers.isEnterable(world, elbowZ.up())) {
                return Optional.empty();
            }
        }

        return Optional.of(new WadeWaterPlan(previous, dest, heuristicTimeCost));
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
