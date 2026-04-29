package ciaassured.yrushwinner.navigation.validators;

import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import net.minecraft.util.math.BlockPos;

import java.time.Instant;

public class RadiusValidator implements PathValidator {
    private final int maxRadius;
    private final BlockPos start;

    public RadiusValidator(int maxRadius, BlockPos start) {
        this.maxRadius = maxRadius * maxRadius; // Must square to use squaredDistance method.
        this.start = start;
    }

    @Override
    public boolean isValid(PathAction action) {
        // If the path is not expected to reach the goal by the deadline, it's not valid.
        return action.getFinalPosition().getSquaredDistance(start) <= maxRadius;
    }
}
