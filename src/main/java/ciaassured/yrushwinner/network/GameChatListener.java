package ciaassured.yrushwinner.network;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.goals.YLevelGoal;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public final class GameChatListener implements ManagedService {

    // Matches: "CLIMB 17 BLOCKS" (up) or "DIG DOWN 25 BLOCKS" (down).
    // Group 1 = verb phrase, group 2 = block count.
    private static final Pattern ROUND_PATTERN = Pattern.compile("^(CLIMB|DIG DOWN) (\\d+) BLOCKS$");

    @InjectLogger private Logger logger;

    private final Navigator navigator;
    private final PathRenderer pathRenderer;

    @Inject
    public GameChatListener(Navigator navigator, PathRenderer pathRenderer) {
        this.navigator = navigator;
        this.pathRenderer = pathRenderer;
    }

    @Override
    public void start() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // action bar, not a round announcement

            String text = message.getString().trim();
            Matcher m = ROUND_PATTERN.matcher(text);
            if (!m.matches()) return;

            String verb = m.group(1);
            int delta = Integer.parseInt(m.group(2));
            int yDelta = verb.equals("CLIMB") ? delta : -delta;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            int targetY = client.player.getBlockPos().getY() + yDelta;
            logger.info("Round start: {} {} BLOCKS → target Y={}", verb, delta, targetY);
            planPathAsync(targetY, client);
        });
    }

    private void planPathAsync(int targetY, MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        BlockPos start = client.player.getBlockPos();
        ClientWorld world = client.world;

        Thread.ofVirtual().name("yrushwinner-pathfind").start(() -> {
            List<BlockPos> path = navigator.findPath(start, new YLevelGoal(targetY));
            client.execute(() -> {
                if (path.isEmpty()) {
                    logger.warn("No path found to Y={} from {}", targetY, start);
                } else {
                    pathRenderer.setPath(path);
                    logger.info("Path planned to Y={}: {} steps", targetY, path.size());
                }
            });
        });
    }
}
