package ciaassured.yrushwinner.navigation.render;

import ciaassured.yrushwinner.infrastructure.ManagedService;
import ciaassured.yrushwinner.navigation.actions.special.PathAction;
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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
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
    private record Seg(Vec3d a, Vec3d b, int argb) {}

    // Volatile: written from bot thread, read from render thread.
    // Replaced atomically with a fully-built immutable list — no partial reads possible.
    private volatile List<Seg> activePath = null;

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
            List<Seg> path = activePath;
            if (path == null || path.isEmpty()) return;

            Vec3d cam = context.worldState().cameraRenderState.pos;

            context.commandQueue().submitCustom(
                    context.matrices(),
                    layer,
                    (entry, consumer) -> {
                        for (Seg seg : path) {
                            Vec3d a = seg.a();
                            Vec3d b = seg.b();
                            int argb = seg.argb();
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >>  8) & 0xFF;
                            int bl = argb & 0xFF;

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
                            consumer.vertex(entry, ax - rx, ay - ry, az - rz).color(r, g, bl, 255);
                            consumer.vertex(entry, bx - rx, by - ry, bz - rz).color(r, g, bl, 255);
                            consumer.vertex(entry, bx + rx, by + ry, bz + rz).color(r, g, bl, 255);
                            consumer.vertex(entry, ax + rx, ay + ry, az + rz).color(r, g, bl, 255);
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
    public void setPath(PathAction terminal) {
        if (terminal == null || terminal.getParent() == null) { activePath = null; return; }

        ArrayList<Seg> segs = new ArrayList<>();
        PathAction curr = terminal;
        PathAction prev = terminal.getParent();
        while (prev != null) {
            segs.add(new Seg(Vec3d.ofCenter(prev.getFinalPosition()),
                             Vec3d.ofCenter(curr.getFinalPosition()),
                             curr.renderColor()));
            curr = prev;
            prev = prev.getParent();
        }
        Collections.reverse(segs);
        activePath = segs.isEmpty() ? null : Collections.unmodifiableList(segs);
    }

    @Override
    public void clearPath() {
        activePath = null;
    }
}
