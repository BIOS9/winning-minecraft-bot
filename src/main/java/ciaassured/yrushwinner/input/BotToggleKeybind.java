package ciaassured.yrushwinner.input;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.Navigator;
import ciaassured.yrushwinner.navigation.goals.YLevelGoal;
import ciaassured.yrushwinner.navigation.render.PathRenderer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public final class BotToggleKeybind implements ManagedService {

    @InjectLogger private Logger logger;

    private final PathRenderer pathRenderer;
    private final Navigator navigator;
    private KeyBinding binding;

    @Inject
    public BotToggleKeybind(PathRenderer pathRenderer, Navigator navigator) {
        this.pathRenderer = pathRenderer;
        this.navigator = navigator;
    }

    @Override
    public void start() {
        binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yrushwinner.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KeyBinding.Category.create(Identifier.of("yrushwinner", "general"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (binding.wasPressed() && client.player != null) {
                BlockPos origin = client.player.getBlockPos();
                List<BlockPos> path = navigator.findPath(origin, new YLevelGoal(100));
                pathRenderer.setPath(path);
                logger.info("Debug path randomised from {}", origin);
            }
        });
    }

    private static List<BlockPos> randomPath(BlockPos origin) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int count = rng.nextInt(200);
        List<BlockPos> path = new ArrayList<>(count);
        BlockPos cur = origin;
        for (int i = 0; i < count; i++) {
            cur = cur.add(
                rng.nextInt(-6, 7),
                rng.nextInt(-3, 6),
                rng.nextInt(-6, 7)
            );
            path.add(cur);
        }
        return path;
    }
}
