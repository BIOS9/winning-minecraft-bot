package ciaassured.yrushwinner.navigation.plans;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Falling straight down from a position with no ground below.
 *
 * Triggered when the block directly beneath the player is passable (the player
 * is standing at the edge of a cliff — typically after a WalkPlan step placed
 * them there). Scans downward until landing, spanning multiple blocks in one node.
 *
 * Time cost = Minecraft gravity simulation (tick-by-tick, terminal velocity).
 */
public class FallPlan implements PathPlan {

    private static final int MAX_FALL_BLOCKS = 100;

    private final PathPlan previous;
    private final BlockPos pos;   // landing position (feet)
    private final double realTimeCost;
    private final double estimatedTimeCost;

    private FallPlan(@NonNull PathPlan previous, BlockPos landingPos, int totalFallBlocks, double heuristicTimeCost) {
        this.previous = previous;
        this.pos = landingPos;

        realTimeCost = previous.getRealTimeCost() + fallTimeSeconds(totalFallBlocks);
        estimatedTimeCost = realTimeCost + heuristicTimeCost;
    }

    /**
     * Attempt to build a FallPlan.
     *
     * Valid when:
     *   - dest is directly below previous (dx=0, dz=0, dy=-1)
     *   - dest is passable (no solid ground — player will fall)
     *   - a solid, non-lava landing block exists within MAX_FALL_BLOCKS
     */
    public static Optional<FallPlan> makePlan(PathPlan previous, BlockPos dest, ClientWorld world, double heuristicTimeCost) {
        BlockPos pos = previous.getPos();

        if (dest.equals(pos)) {
            throw new IllegalArgumentException("next must differ from current");
        }

        int dx = dest.getX() - pos.getX();
        int dy = dest.getY() - pos.getY();
        int dz = dest.getZ() - pos.getZ();

        // Straight down only.
        if (dx != 0 || dy != -1 || dz != 0) {
            return Optional.empty();
        }

        // dest (= pos.down()) must be passable — player is not on solid ground.
        if (!MoveHelpers.isPassable(world, dest)) {
            return Optional.empty();
        }

        // Scan downward from dest until landing.
        // scan is always passable (player's feet fit). Head clearance is pos, then each
        // previous scan position, which were confirmed passable in prior iterations.
        BlockPos scan = dest;
        int fallBlocks = 0;
        while (fallBlocks < MAX_FALL_BLOCKS) {
            BlockPos below = scan.down();
            if (!MoveHelpers.isPassable(world, below)) {
                if (MoveHelpers.isLava(world, below)) {
                    return Optional.empty(); // never land above lava
                }
                break; // solid ground found
            }
            scan = below;
            fallBlocks++;
        }

        if (fallBlocks >= MAX_FALL_BLOCKS) {
            return Optional.empty(); // no ground found
        }

        // totalFall: 1 block from pos to dest, plus however many more we scanned.
        int totalFall = 1 + fallBlocks;
        return Optional.of(new FallPlan(previous, scan, totalFall, heuristicTimeCost));
    }

    /**
     * Simulates Minecraft's falling physics tick by tick to compute fall duration.
     *
     * Each tick: vy = (vy - 0.08) * 0.98
     * Terminal velocity ≈ −3.92 blocks/tick (≈ 78.4 blocks/second).
     * Source: https://minecraft.wiki/w/Transportation#Falling
     */
    private static double fallTimeSeconds(int blocks) {
        double vy = 0;
        double fallen = 0;
        int ticks = 0;
        while (fallen < blocks) {
            vy = (vy - 0.08) * 0.98;
            fallen -= vy;
            ticks++;
            if (ticks > 2000) break;
        }
        return ticks / 20.0;
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
