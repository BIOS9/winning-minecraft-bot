package ciaassured.yrushwinner.input;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.goals.YLevelGoal;
import ciaassured.yrushwinner.navigation.actions.PathPlan;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@Singleton
public final class GotoCommand implements ManagedService {

    @InjectLogger private Logger logger;

    private final Navigator navigator;
    private final PathRenderer pathRenderer;

    @Inject
    public GotoCommand(Navigator navigator, PathRenderer pathRenderer) {
        this.navigator = navigator;
        this.pathRenderer = pathRenderer;
    }

    @Override
    public void start() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("clearpath")
                .executes(ctx -> {
                    pathRenderer.clearPath();
                    ctx.getSource().sendFeedback(Text.literal("Path cleared."));
                    return Command.SINGLE_SUCCESS;
                }));

            dispatcher.register(ClientCommandManager.literal("goto")
                .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int targetY = IntegerArgumentType.getInteger(ctx, "y");
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player == null || client.world == null) {
                            return Command.SINGLE_SUCCESS;
                        }

                        BlockPos start = client.player.getBlockPos();
                        Optional<PathPlan> path = navigator.findPath(start, new YLevelGoal(targetY), 500);

                        if (path.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No path found to Y=" + targetY));
                            logger.info("goto Y={} from {} — no path found", targetY, start);
                        } else {
                            List<BlockPos> completePath = path.get().getCompletePath();
                            pathRenderer.setPath(completePath);
                            ctx.getSource().sendFeedback(Text.literal("Path to Y=" + targetY + " (" + completePath.size() + " steps)"));
                            logger.info("goto Y={} from {} → {} steps", targetY, start, completePath.size());
                        }

                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }
}
