package ciaassured.yrushwinner.navigation.actions;

import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RootPathPlan implements PathPlan {
    private final BlockPos startPos;

    public RootPathPlan(BlockPos startPos) {
        this.startPos = startPos;
    }

    @Override
    public BlockPos getPos() {
        return startPos;
    }

    @Override
    public @Nullable PathPlan getPrevious() {
        return null;
    }

    @Override
    public double getRealTimeCost() {
        return 0;
    }

    @Override
    public double getEstimatedTimeCost() {
        return 0;
    }

    @Override
    public void execute() {
        // Do nothing
    }

    @Override
    public List<BlockPos> getCompletePath() {
        return List.of();
    }

    @Override
    public int compareTo(@NonNull PathPlan o) {
        return 0;
    }
}
