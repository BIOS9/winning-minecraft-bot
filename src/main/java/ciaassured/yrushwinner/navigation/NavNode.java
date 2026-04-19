package ciaassured.yrushwinner.navigation;

import net.minecraft.util.math.BlockPos;

final class NavNode implements Comparable<NavNode> {
    final BlockPos pos;
    final double gCost;
    final double fCost;
    final NavNode parent;

    NavNode(BlockPos pos, double gCost, double hCost, NavNode parent) {
        this.pos   = pos;
        this.gCost = gCost;
        this.fCost = gCost + hCost;
        this.parent = parent;
    }

    @Override
    public int compareTo(NavNode other) {
        return Double.compare(this.fCost, other.fCost);
    }
}
