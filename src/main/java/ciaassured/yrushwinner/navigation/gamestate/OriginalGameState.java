package ciaassured.yrushwinner.navigation.gamestate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public class OriginalGameState extends BaseGameState {
    private final MinecraftClient client;

    public OriginalGameState(MinecraftClient client) {
        super(client.player.getHealth(), client.player.getAir());

        this.client = client;
    }
}

// Probably implement a base class again that stores all the values at init time.
// Pass in a parent
// One child class will do a state change and the output will be used to init the base class.
// That means the action must store the game state with it, there's no parent link to the game state at run time, that should hopefully reduce some compute usage since it's effectively cached.