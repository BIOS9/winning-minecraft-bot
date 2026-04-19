package ciaassured.yrushwinner.navigation;

import ciaassured.yrushwinner.infrastructure.InjectLogger;
import jakarta.inject.Singleton;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;

@Singleton
public class DefaultStepChecker implements StepChecker {

    private static final long CHUNK_WAIT_TIMEOUT_MS = 10_000;
    private static final long CHUNK_POLL_MS         = 50;

    @InjectLogger private Logger logger;

    @Override
    public boolean canMove(BlockPos current, BlockPos next) {
        if (next.equals(current)) {
            throw new IllegalArgumentException("next must differ from current");
        }

        int sdx = next.getX() - current.getX();
        int dy  = next.getY() - current.getY();
        int sdz = next.getZ() - current.getZ();
        int dx  = Math.abs(sdx);
        int dz  = Math.abs(sdz);

        if (dx > 1 || Math.abs(dy) > 1 || dz > 1) {
            return false;
        }

        // No jump-in-place: dy=1 with no horizontal delta returns player to same block.
        if (dy == 1 && dx == 0 && dz == 0) {
            return false;
        }

        ClientWorld world = MinecraftClient.getInstance().world; // why every single block AI
        if (world == null) return false;

        // Gravity: if no solid block below, player is in freefall — only straight down valid.
        boolean grounded = !isPassable(world, current.down());
        if (!grounded) {
            return dy == -1 && dx == 0 && dz == 0;
        }

        // Feet and head at destination must be clear.
        if (!isPassable(world, next) || !isPassable(world, next.up())) {
            return false;
        }

        // Diagonal: player cannot clip through the two elbow blocks.
        if (dx == 1 && dz == 1) {
            BlockPos elbowX = new BlockPos(current.getX() + sdx, current.getY(), current.getZ());
            BlockPos elbowZ = new BlockPos(current.getX(),       current.getY(), current.getZ() + sdz);
            if (!isPassable(world, elbowX) || !isPassable(world, elbowX.up())
             || !isPassable(world, elbowZ) || !isPassable(world, elbowZ.up())) {
                return false;
            }
        }

        if (dy == -1) {
            return isPassable(world, next.up(2));
        }

        if (dy == 1) {
            return isPassable(world, current.up(2));
        }

        return true;
    }

    /** True if a player can occupy this block. Blocks until the chunk loads or timeout. */
    private boolean isPassable(ClientWorld world, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!hasChunkData(world, cx, cz)) {
            logger.debug("Waiting for chunk ({}, {})", cx, cz);
            if (!awaitChunk(world, cx, cz)) return false;
        }
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * Returns true only if the chunk at (cx, cz) has real block data from the server.
     * ClientChunkManager.getChunk() returns a shared emptyChunk sentinel when data has
     * not arrived yet — that sentinel's ChunkPos won't match the requested coords.
     */
    private boolean hasChunkData(ClientWorld world, int cx, int cz) {
        ClientChunkManager mgr = world.getChunkManager();
        WorldChunk chunk = mgr.getChunk(cx, cz, ChunkStatus.FULL, false);
        return chunk != null && chunk.getPos().x == cx && chunk.getPos().z == cz;
    }

    private boolean awaitChunk(ClientWorld world, int cx, int cz) {
        long deadline = System.currentTimeMillis() + CHUNK_WAIT_TIMEOUT_MS;
        while (!hasChunkData(world, cx, cz)) {
            if (System.currentTimeMillis() > deadline) {
                logger.warn("Chunk ({}, {}) did not load within timeout — treating as impassable", cx, cz);
                return false;
            }
            try {
                Thread.sleep(CHUNK_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        logger.debug("Chunk ({}, {}) loaded", cx, cz);
        return true;
    }
}
