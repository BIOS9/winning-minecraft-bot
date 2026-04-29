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
 * Walking into or through shallow water (1 block deep — head above surface).
 *
 * Bridges WalkAction (land) and SwimAction (fully submerged):
 *   - Player must be grounded on solid ground (not floating).
 *   - Destination is water but not covered — head stays above surface.
 *   - Normal 2-block height and diagonal elbow checks apply.
 *   - Speed: WADE_SPEED_BPS (water resistance).
 *
 * Exiting shallow water back to land is handled by WalkAction since WalkAction
 * only requires the source to be grounded — which wading still satisfies.
 */
public class WadeWaterAction extends BasePathAction {

    private WadeWaterAction(@NonNull PathAction parent, BlockPos finalPosition,
                            double currentRealTimeCost, double totalEstimatedTimeCost,
                            GameState gameState) {
        super(parent, finalPosition, currentRealTimeCost, totalEstimatedTimeCost, gameState);
    }

    public static Optional<WadeWaterAction> makePlan(PathAction parent, BlockPos destination,
                                                     ClientWorld world, double heuristicTimeCost) {
        BlockPos currentPos = parent.getFinalPosition();

        if (destination.equals(currentPos)) throw new IllegalArgumentException("destination must differ from current");

        int deltaX    = destination.getX() - currentPos.getX();
        int deltaY    = destination.getY() - currentPos.getY();
        int deltaZ    = destination.getZ() - currentPos.getZ();
        int absDeltaX = Math.abs(deltaX);
        int absDeltaZ = Math.abs(deltaZ);

        // Horizontal or dropping down one block into water; never jumping up.
        if (absDeltaX > 1 || deltaY > 0 || absDeltaZ > 1) return Optional.empty();

        // Destination must be shallow water: feet in water, head above.
        if (!MoveHelpers.isWater(world, destination)) return Optional.empty();

        if (MoveHelpers.isWater(world, currentPos) && MoveHelpers.isWater(world, currentPos.up())) return Optional.empty(); // fully submerged — SwimAction handles this

        if (!MoveHelpers.isEnterable(world, destination.up())) return Optional.empty(); // head blocked by something solid

        // Diagonal: elbow blocks must be enterable — cannot clip through walls.
        if (absDeltaX == 1 && absDeltaZ == 1) {
            BlockPos elbowX = new BlockPos(currentPos.getX() + deltaX, currentPos.getY(), currentPos.getZ());
            BlockPos elbowZ = new BlockPos(currentPos.getX(),           currentPos.getY(), currentPos.getZ() + deltaZ);
            if (!MoveHelpers.isEnterable(world, elbowX) || !MoveHelpers.isEnterable(world, elbowX.up())
             || !MoveHelpers.isEnterable(world, elbowZ) || !MoveHelpers.isEnterable(world, elbowZ.up())) {
                return Optional.empty();
            }
        }

        double distance = Math.sqrt((double) (deltaX * deltaX + deltaZ * deltaZ));
        double currentRealTimeCost = parent.getCurrentRealTimeCost() + distance / TimeCostModel.WADE_SPEED_BPS;
        double totalEstimatedTimeCost = currentRealTimeCost + heuristicTimeCost;

        return Optional.of(new WadeWaterAction(parent, destination, currentRealTimeCost, totalEstimatedTimeCost, parent.getGameState()));
    }

    @Override
    public void execute() { }

    @Override
    public int renderColor() { return 0xFF66AAFF; } // light blue
}
