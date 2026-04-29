package ciaassured.yrushwinner.navigation;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class MoveHelpers {

    /**
     * True if a non-swimming player can occupy this block (air, flowers, etc.).
     * Water is explicitly excluded — it is handled by SwimAction via isWater().
     */
    public static boolean isPassable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getFluidState().isIn(FluidTags.WATER)) return false;
        if (state.getFluidState().isIn(FluidTags.LAVA)) return false;
        return state.getCollisionShape(world, pos).isEmpty();
    }

    /** Solid non-water block — suitable ground to stand on. */
    public static boolean isSolid(ClientWorld world, BlockPos pos) {
        return !isPassable(world, pos) && !MoveHelpers.isWater(world, pos);
    }

    /** Block can be entered while wading: open air/plants or water. */
    public static boolean isEnterable(ClientWorld world, BlockPos pos) {
        return isPassable(world, pos) || MoveHelpers.isWater(world, pos);
    }

    /**
     * True if this block contains water (full water block or waterlogged block).
     */
    public static boolean isWater(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }

    /**
     * True if this block contains lava.
     */
    public static boolean isLava(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos).getFluidState().isIn(FluidTags.LAVA);
    }
}
