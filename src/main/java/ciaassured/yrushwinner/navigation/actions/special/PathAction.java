package ciaassured.yrushwinner.navigation.actions.special;

import ciaassured.yrushwinner.navigation.gamestate.GameState;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PathAction extends Comparable<PathAction> {
    BlockPos getFinalPosition();
    @Nullable PathAction getParent();
    GameState getGameState();
    double getCurrentRealTimeCost();
    double getTotalEstimatedTimeCost();
    void execute();
    List<BlockPos> getCompletePath();

    /** ARGB color used to render this action's segment in the debug overlay. */
    default int renderColor() { return 0xFFFFFFFF; }
}
