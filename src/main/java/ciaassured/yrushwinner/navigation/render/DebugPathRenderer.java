package ciaassured.yrushwinner.navigation.render;

import ciaassured.yrushwinner.infrastructure.ManagedService;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@Singleton
public final class DebugPathRenderer implements PathRenderer, ManagedService {

    private static final float LINE_HALF_WIDTH = 0.05f; // world-space half-width in blocks

    // Static: GPU pipeline descriptor is a module-level constant, registered once at class load.
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("yrushwinner", "pipeline/debug_path_through_walls"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    private final RenderLayer layer;
    // Volatile: written from bot thread, read from render thread.
    // Replaced atomically with a fully-built immutable list — no partial reads possible.
    private volatile List<Vec3d> activePath = null;

    @Inject
    public DebugPathRenderer() {
        // ~20 bytes/vertex × 4 vertices/segment × up to ~200 segments ≈ 16 KB
        RenderSetup setup = RenderSetup.builder(PIPELINE)
                .expectedBufferSize(16384)
                .build();
        layer = RenderLayer.of("yrushwinner:debug_path_through_walls", setup);
    }

    @Override
    public void start() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            List<Vec3d> path = activePath;
            if (path == null || path.size() < 2) return;

            Vec3d cam = context.worldState().cameraRenderState.pos;
            int segments = path.size() - 1;

            context.commandQueue().submitCustom(
                    context.matrices(),
                    layer,
                    (entry, consumer) -> {
                        for (int i = 0; i < segments; i++) {
                            Vec3d a = path.get(i);
                            Vec3d b = path.get(i + 1);
                            // Gradient: green at path start → red at path end.
                            float t = (float) i / segments;
                            int r = (int) (255 * t);
                            int g = (int) (255 * (1f - t));

                            // Billboard quad: expand segment perpendicular to both
                            // segment direction and camera view direction so the quad
                            // always faces the camera regardless of viewing angle.
                            Vec3d dir = b.subtract(a).normalize();
                            Vec3d eye = a.add(b).multiply(0.5).subtract(cam).normalize();
                            Vec3d right = dir.crossProduct(eye).normalize().multiply(LINE_HALF_WIDTH);

                            float ax = (float)(a.x - cam.x), ay = (float)(a.y - cam.y), az = (float)(a.z - cam.z);
                            float bx = (float)(b.x - cam.x), by = (float)(b.y - cam.y), bz = (float)(b.z - cam.z);
                            float rx = (float) right.x,      ry = (float) right.y,      rz = (float) right.z;

                            // Quad corners (CCW when facing camera):
                            consumer.vertex(entry, ax - rx, ay - ry, az - rz).color(r, g, 0, 255);
                            consumer.vertex(entry, bx - rx, by - ry, bz - rz).color(r, g, 0, 255);
                            consumer.vertex(entry, bx + rx, by + ry, bz + rz).color(r, g, 0, 255);
                            consumer.vertex(entry, ax + rx, ay + ry, az + rz).color(r, g, 0, 255);
                        }
                    }
            );
        });
    }

    @Override
    public void stop() {
        clearPath();
    }

    @Override
    public void setPath(List<BlockPos> waypoints) {
        activePath = (waypoints == null || waypoints.isEmpty()) ? null
                : waypoints.stream().map(Vec3d::ofCenter).toList();
    }

    @Override
    public void clearPath() {
        activePath = null;
    }
}
