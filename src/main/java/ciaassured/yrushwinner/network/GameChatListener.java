package ciaassured.yrushwinner.network;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import ciaassured.yrushwinner.infrastructure.ManagedService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public final class GameChatListener implements ManagedService {

    // Matches: "CLIMB 17 BLOCKS" (up) or "DIG DOWN 25 BLOCKS" (down).
    // Group 1 = verb phrase, group 2 = block count.
    private static final Pattern ROUND_PATTERN = Pattern.compile("^(CLIMB|DIG DOWN) (\\d+) BLOCKS$");

    @InjectLogger private Logger logger;

    private volatile @Nullable Integer targetY;

    public Optional<Integer> getTargetY() {
        return Optional.ofNullable(targetY);
    }

    @Inject
    public GameChatListener() {}

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

            int currentY = client.player.getBlockPos().getY();
            int newTargetY = currentY + yDelta;
            this.targetY = newTargetY;

            String direction = yDelta > 0 ? "UP" : "DOWN";
            logger.info("Round start: {} {} BLOCKS → target Y={}", verb, delta, newTargetY);

            client.player.sendMessage(
                Text.literal(String.format("[YRush] Target Y=%d (%s %d blocks) — press C to calculate path",
                    newTargetY, direction, Math.abs(yDelta))),
                false
            );
        });
    }
}
