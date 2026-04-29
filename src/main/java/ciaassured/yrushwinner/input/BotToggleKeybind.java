package ciaassured.yrushwinner.input;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.goals.YLevelGoal;
import ciaassured.yrushwinner.navigation.actions.PathPlan;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import ciaassured.yrushwinner.network.GameChatListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

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
            if (targetY.isEmpty()) {
                client.player.sendMessage(Text.literal("[YRush] No target Y — wait for round start"), false);
                return;
            }

            int y = targetY.get();
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
                    Optional<PathPlan> path = navigator.findPath(start, new YLevelGoal(y), 500);
                    if (Thread.currentThread().isInterrupted()) return;
                    client.execute(() -> {
                        if (!calculationThread.compareAndSet(Thread.currentThread(), null)) return;
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
                } finally {
                    calculationThread.compareAndSet(Thread.currentThread(), null);
                }
            });
            calculationThread.set(thread);
            thread.start();
        });
    }
}
