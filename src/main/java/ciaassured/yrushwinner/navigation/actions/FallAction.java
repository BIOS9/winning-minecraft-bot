package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.MoveHelpers;
import ciaassured.yrushwinner.navigation.actions.special.BasePathAction;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.gamestate.GameState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Falling straight down from a position with no ground below.
 *
 * Triggered when the block directly beneath the player is passable (the player
 * is standing at the edge of a cliff). Scans downward until landing, spanning
 * multiple blocks in one node.
 *
 * Time cost = Minecraft gravity simulation (tick-by-tick, terminal velocity).
 */
public class FallAction extends BasePathAction {

    private static final int MAX_FALL_BLOCKS = 100;

    private FallAction(@NonNull PathAction parent, BlockPos finalPosition,
                       double currentRealTimeCost, double totalEstimatedTimeCost,
                       GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    /**
     * Valid when:
     *   - destination is directly below parent (deltaX=0, deltaZ=0, deltaY=-1)
     *   - destination is passable (no solid ground — player will fall)
     *   - a solid, non-lava landing block exists within MAX_FALL_BLOCKS
     */
    public static Optional<FallAction> makePlan(PathAction parent, BlockPos destination,
                                                ClientWorld world, double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();

        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaX = destination.getX() - currentPos.getX();
        int deltaY = destination.getY() - currentPos.getY();
        int deltaZ = destination.getZ() - currentPos.getZ();

        // Straight down only.
        if (deltaX != 0 || deltaY != -1 || deltaZ != 0) return Optional.empty();

        // destination (= currentPos.down()) must be passable — player is not on solid ground.
        if (!MoveHelpers.isPassable(world, destination)) return Optional.empty();

        // Scan downward from destination until landing.
        BlockPos landingPos = destination;
        int fallBlocks = 0;
        while (fallBlocks < MAX_FALL_BLOCKS) {
            BlockPos blockBelow = landingPos.down();
            if (!MoveHelpers.isPassable(world, blockBelow)) {
                if (MoveHelpers.isLava(world, blockBelow)) return Optional.empty(); // never land above lava
                break; // solid ground found
            }
            landingPos = blockBelow;
            fallBlocks++;
        }

        if (fallBlocks >= MAX_FALL_BLOCKS) return Optional.empty(); // no ground found

        // totalFall: 1 block from currentPos to destination, plus however many more we scanned.
        int totalFall = 1 + fallBlocks;
        double currentRealTimeCost = parent.getCurrentRealTimeCost() + fallTimeSeconds(totalFall);
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new FallAction(parent, landingPos, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
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
    public void execute() { }

    @Override
    public int renderColor() { return 0xFFFF6600; } // orange
}
