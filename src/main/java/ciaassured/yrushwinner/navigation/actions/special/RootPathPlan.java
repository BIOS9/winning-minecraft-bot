package ciaassured.yrushwinner.navigation.actions.special;

import ciaassured.yrushwinner.navigation.gamestate.GameState;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RootPathPlan implements PathAction {
    private final BlockPos startPos;
    private final GameState gameState;

    public RootPathPlan(BlockPos startPos, GameState gameState) {
        this.startPos = startPos;
        this.gameState = gameState;
    }

    @Override public BlockPos getFinalPosition() { return startPos; }
    @Override public @Nullable PathAction getParent() { return null; }

    @Override public GameState getGameState() { return gameState; }
    @Override public double getCurrentRealTimeCost() { return 0; }
    @Override public double getTotalEstimatedTimeCost() { return 0; }
    @Override public void execute() { }
    @Override public List<BlockPos> getCompletePath() { return List.of(startPos); }
    @Override public int compareTo(@NonNull PathAction o) { return Double.compare(0, o.getTotalEstimatedTimeCost()); }
}
