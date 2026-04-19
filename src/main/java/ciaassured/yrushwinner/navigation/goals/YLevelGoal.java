package ciaassured.yrushwinner.navigation.goals;

import net.minecraft.util.math.BlockPos;

public class YLevelGoal implements PathGoal {
    int yLevel;

    public YLevelGoal(int yLevel) {
        this.yLevel = yLevel;
    }

    @Override
    public boolean isGoal(BlockPos pos) {
        return pos.getY() == yLevel;
    }

    @Override
    public double heuristic(BlockPos pos) {
        return Math.abs(pos.getY() - yLevel);
    }
}
