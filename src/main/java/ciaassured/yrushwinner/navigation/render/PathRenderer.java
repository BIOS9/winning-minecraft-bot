package ciaassured.yrushwinner.navigation.render;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface PathRenderer {
    /** Replace the rendered path. Null or empty clears it. Safe to call from any thread. */
    void setPath(List<BlockPos> waypoints);

    /** Clear the rendered path. */
    void clearPath();
}
