package ciaassured.yrushwinner.navigation.gamestate;

public class BaseGameState implements GameState {
    private final float playerHealth;
    private final int playerAir;

    public BaseGameState(float playerHealth, int playerAir) {
        this.playerHealth = playerHealth;
        this.playerAir = playerAir;
    }

    @Override
    public float getPlayerHealth() {
        return playerHealth;
    }

    @Override
    public int getPlayerAir() {
        return playerAir;
    }
}
// Currently swimming
// Feet touching ground
// Head under-water
// Mined/placed blocks in the world
// Health
// Air
// Inventory
// Pick usage