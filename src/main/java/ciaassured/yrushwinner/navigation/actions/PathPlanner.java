package ciaassured.yrushwinner.navigation.actions;

import ciaassured.yrushwinner.navigation.goals.PathGoal;

public interface PathPlanner {
    Iterable<PathPlan> getNeighbours(PathPlan plan, PathGoal goal);
}
