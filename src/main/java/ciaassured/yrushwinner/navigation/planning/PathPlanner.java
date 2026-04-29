package ciaassured.yrushwinner.navigation.planning;

import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.goals.PathGoal;

public interface PathPlanner {
    Iterable<PathAction> getNeighbours(PathAction plan, PathGoal goal);
}
