package ciaassured.yrushwinner.input;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.gamestate.GameState;
import ciaassured.yrushwinner.navigation.gamestate.OriginalGameState;
import ciaassured.yrushwinner.navigation.goals.YLevelGoal;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import ciaassured.yrushwinner.navigation.validators.AbsoluteTimeLimitValidator;
import ciaassured.yrushwinner.navigation.validators.PathValidator;
import ciaassured.yrushwinner.navigation.validators.RadiusValidator;
import ciaassured.yrushwinner.network.GameChatListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public final class BotToggleKeybind implements ManagedService {

    @InjectLogger private Logger logger;

    private final PathRenderer pathRenderer;
    private final Navigator navigator;
    private final GameChatListener chatListener;
    private final AtomicReference<Thread> calculationThread = new AtomicReference<>(null);
    private KeyBinding binding;

    @Inject
    public BotToggleKeybind(PathRenderer pathRenderer, Navigator navigator, GameChatListener chatListener) {
        this.pathRenderer = pathRenderer;
        this.navigator = navigator;
        this.chatListener = chatListener;
    }

    @Override
    public void start() {
        binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yrushwinner.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KeyBinding.Category.create(Identifier.of("yrushwinner", "general"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!binding.wasPressed() || client.player == null) return;

            Optional<Integer> targetY = chatListener.getTargetY();
            Optional<Instant> gameStartTime = chatListener.getGameStartTime();
            if (targetY.isEmpty() || gameStartTime.isEmpty()) {
                client.player.sendMessage(Text.literal("[YRush] No target Y — wait for round start"), false);
                return;
            }

            int y = targetY.get();
            Instant startTime = gameStartTime.get();
            BlockPos start = client.player.getBlockPos();

            Thread existing = calculationThread.get();
            if (existing != null) {
                existing.interrupt();
                client.player.sendMessage(Text.literal(String.format("[YRush] Cancelled, recalculating to Y=%d...", y)), false);
            } else {
                client.player.sendMessage(Text.literal(String.format("[YRush] Calculating path to Y=%d...", y)), false);
            }
            logger.info("Path calculation triggered: start={} targetY={}", start, y);

            Thread thread = Thread.ofVirtual().name("yrushwinner-pathfind").unstarted(() -> {
                try {
                    List<PathValidator> validators = new ArrayList<>();
                    validators.add(new RadiusValidator(500, start)); // Only paths up to 300 blocks around
                    validators.add(new AbsoluteTimeLimitValidator(startTime.plusSeconds(60 * 4).plusSeconds(2))); // Only paths up to 4 minutes

                    GameState gameState = new OriginalGameState(MinecraftClient.getInstance());

                    Optional<PathAction> path = navigator.findPath(start, new YLevelGoal(y), gameState, validators);
                    if (Thread.currentThread().isInterrupted()) { return; }
                    client.execute(() -> {
                        if (path.isEmpty()) {
                            client.player.sendMessage(Text.literal(String.format("[YRush] No path found to Y=%d", y)), false);
                            logger.info("goto Y={} from {} — no path found", y, start);
                        } else {
                            List<BlockPos> completePath = path.get().getCompletePath();
                            pathRenderer.setPath(completePath);
                            client.player.sendMessage(Text.literal(String.format("[YRush] Path ready: %d steps to Y=%d", completePath.size(), y)), false);
                            logger.info("goto Y={} from {} → {} steps", y, start, completePath.size());
                        }
                    });
                }
                catch (InterruptedException e) {
                    logger.warn("Interrupted");
                }
                finally {
                    calculationThread.compareAndSet(Thread.currentThread(), null);
                }
            });
            calculationThread.set(thread);
            thread.start();
        });
    }
}
