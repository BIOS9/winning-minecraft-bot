package ciaassured.yrushwinner.navigation.render;

import ciaassured.yrushwinner.navigation.actions.special.PathAction;

public interface PathRenderer {
    /** Replace the rendered path from the terminal PathAction node. Null clears it. Safe to call from any thread. */
    void setPath(PathAction terminal);

    /** Clear the rendered path. */
    void clearPath();
}
