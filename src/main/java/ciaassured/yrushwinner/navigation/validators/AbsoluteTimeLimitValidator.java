package ciaassured.yrushwinner.navigation.validators;

import ciaassured.yrushwinner.navigation.actions.special.PathAction;

import java.time.Instant;

public class AbsoluteTimeLimitValidator implements PathValidator {
    private final Instant endTime;

    public AbsoluteTimeLimitValidator(Instant endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean isValid(PathAction action) {
        // If the path is not expected to reach the goal by the deadline, it's not valid.
        return !Instant.now().plusSeconds((long)action.getTotalEstimatedTimeCost()).isAfter(endTime);
    }
}
