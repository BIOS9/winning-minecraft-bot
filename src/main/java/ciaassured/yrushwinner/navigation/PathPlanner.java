package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.navigation.actions.PathPlan;
import ciaassured.yrushwinner.navigation.goals.PathGoal;

public interface PathPlanner {
    Iterable<PathPlan> getNeighbours(PathPlan plan, PathGoal goal);
}
