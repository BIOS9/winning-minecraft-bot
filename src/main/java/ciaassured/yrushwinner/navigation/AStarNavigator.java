package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.navigation.gamestate.GameState;
import ciaassured.yrushwinner.navigation.goals.PathGoal;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.actions.special.RootPathPlan;
import ciaassured.yrushwinner.navigation.planning.PathPlanner;
import ciaassured.yrushwinner.navigation.validators.PathValidator;
import jakarta.inject.Inject;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import javax.xml.validation.Validator;
import java.util.*;

public class AStarNavigator implements Navigator {
    // IDEAS
    // Replace StepChecker with PathPlanner
    // Path planner checks if the next block is able to be moved to
    // It returns an array of ways to get to the next block, usually there will be only one
    // Each way to get to the next block is a MovementPlan or something, it could perform multiple actions including walking, mining, towering, climbing ladder, swimming, falling, etc.
    // Each MovementPlan has a time cost, which will include all the actions it needs to perform.
    // It's possible each plan will need to know about all the previous plans in the path so it can take into account broken blocks, placed blocks, etc.
    // Just keep it simple for now, basically check all movement plans, if movement plan exists, then movement is possible. Movement plan will be able to be executed in future, this couples the possibility of the action with the execution of the action which is what we want.


    private static final int MAX_NODES = 10_000_000;

    private final PathPlanner pathPlanner;

    @InjectLogger private Logger logger;

    @Inject
    public AStarNavigator(PathPlanner pathPlanner) {
        this.pathPlanner = pathPlanner;
    }

    @Override
    public Optional<PathAction> findPath(BlockPos start, PathGoal goal, GameState gameState, Collection<PathValidator> validators) throws InterruptedException {
        PriorityQueue<PathAction> open = new PriorityQueue<>();
        Map<BlockPos, Double> lowestTimeCosts = new HashMap<>();

        open.add(new RootPathPlan(start, gameState));
        lowestTimeCosts.put(start, 0.0);

        int visited = 0;
        while (!open.isEmpty() && visited < MAX_NODES) {
            if (Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }

            PathAction current = open.poll();

            // Throw away any paths that do not pass all validation rules
            if (validators.stream().anyMatch(x -> !x.isValid(current))) {
                logger.info("Invalid path thrown out with estimated cost of {}s", current.getTotalEstimatedTimeCost());
                continue;
            }

            if (visited % 10000 == 0)
                logger.info("Current path: {}", current.getTotalEstimatedTimeCost());

            if (goal.isGoal(current.getFinalPosition())) {
                logger.info("Path found");
                return Optional.of(current);
            }

            // Skip stale entries (open set may hold outdated nodes for same pos).
            if (current.getCurrentRealTimeCost() > lowestTimeCosts.getOrDefault(current.getFinalPosition(), Double.MAX_VALUE)) {
                continue;
            }
            visited++;

            for (PathAction neighbour : pathPlanner.getNeighbours(current, goal)) {
                if (neighbour.getCurrentRealTimeCost() < lowestTimeCosts.getOrDefault(neighbour.getFinalPosition(), Double.MAX_VALUE)) {
                    lowestTimeCosts.put(neighbour.getFinalPosition(), neighbour.getCurrentRealTimeCost());
                    open.add(neighbour);
                }
            }
        }

        logger.info("No path found");
        return Optional.empty();
    }
}
