package ciaassured.yrushwinner.navigation.actions;

import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PathPlan extends Comparable<PathPlan> {
    BlockPos getPos();
    @Nullable PathPlan getPrevious();
    double getRealTimeCost();
    double getEstimatedTimeCost();
    void execute();
    List<BlockPos> getCompletePath();
}
