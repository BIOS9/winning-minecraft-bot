package ciaassured.yrushwinner.navigation.actions.special;

import ciaassured.yrushwinner.navigation.gamestate.GameState;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BasePathAction implements PathAction {
    private final PathAction parent;
    private final BlockPos finalPosition;
    private final double currentRealTimeCost;
    private final double totalEstimatedTimeCost;
    private final GameState gameState;

    protected BasePathAction(@NonNull PathAction parent, @NonNull BlockPos finalPosition,
                             double currentRealTimeCost, double totalEstimatedTimeCost,
                             GameState gameState) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.finalPosition = Objects.requireNonNull(finalPosition, "finalPosition");
        if (currentRealTimeCost < 0) throw new IllegalArgumentException("currentRealTimeCost < 0");
        if (totalEstimatedTimeCost < 0) throw new IllegalArgumentException("totalEstimatedTimeCost < 0");
        this.currentRealTimeCost = currentRealTimeCost;
        this.totalEstimatedTimeCost = totalEstimatedTimeCost;
        this.gameState = Objects.requireNonNull(gameState, "gameState");
    }

    @Override
    public BlockPos getFinalPosition() {
        return finalPosition;
    }

    @Override
    public @Nullable PathAction getParent() {
        return parent;
    }

    @Override
    public double getCurrentRealTimeCost() {
        return currentRealTimeCost;
    }

    @Override
    public double getTotalEstimatedTimeCost() {
        return totalEstimatedTimeCost;
    }

    @Override
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public List<BlockPos> getCompletePath() {
        ArrayList<BlockPos> path = new ArrayList<>();
        PathAction current = this;

        while (current != null) {
            path.add(current.getFinalPosition());
            current = current.getParent();
        }

        return path.reversed();
    }

    @Override
    public int compareTo(@NonNull PathAction other) {
        return Double.compare(getTotalEstimatedTimeCost(), other.getTotalEstimatedTimeCost());
    }
}
