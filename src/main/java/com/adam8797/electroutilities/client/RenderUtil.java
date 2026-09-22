package com.adam8797.electroutilities.client;

import com.adam8797.electroutilities.content.utilitypole.CrossarmGeometry;
import com.adam8797.electroutilities.content.utilitypole.PoleRotation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Small helpers for drawing textured cuboids in block-entity renderers. */
public final class RenderUtil {

    private RenderUtil() {}

    public static TextureAtlasSprite blockSprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    /**
     * Draws the crossarm beam across a single block, oriented along the pole {@link PoleRotation}. The
     * beam is authored along the base N/S line and rotated clockwise; diagonal orientations extend it to
     * the block's diagonal (length √2) so it reaches the diagonal neighbour cells.
     */
    public static void crossarmBeam(PoseStack poseStack, VertexConsumer vc, TextureAtlasSprite sprite,
                                    int rotation, int light, int overlay) {
        double y1 = CrossarmGeometry.BEAM_Y1, y2 = CrossarmGeometry.BEAM_Y2;
        double p1 = CrossarmGeometry.PERP1, p2 = CrossarmGeometry.PERP2;
        boolean diagonal = PoleRotation.isDiagonal(rotation);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-PoleRotation.degrees(rotation))); // clockwise from above
        poseStack.translate(-0.5, 0.0, -0.5);
        if (diagonal) {
            // The beam runs the block's diagonal (length √2 > 1); tiled UVs would sample past the sprite
            // tile and tear, so map the full sprite onto each face (slight grain stretch, no gaps).
            double ext = (Math.sqrt(2.0) - 1.0) / 2.0;
            cuboid(poseStack, vc, sprite, p1, y1, -ext, p2, y2, 1.0 + ext, light, overlay);
        } else {
            cuboidTiled(poseStack, vc, sprite, p1, y1, 0.0, p2, y2, 1.0, light, overlay);
        }
        poseStack.popPose();
    }

    /**
     * Draws an axis-aligned textured cuboid (block units) with per-face UVs proportional to each face's
     * size, so the texture keeps a uniform 16px-per-block density and is not stretched on long/thin faces
     * (unlike {@link #cuboid}, which maps the whole sprite onto every face). Suits beams and other
     * elongated shapes. Each face samples from the sprite's origin; extents are assumed within one tile.
     */
    public static void cuboidTiled(PoseStack poseStack, VertexConsumer vc, TextureAtlasSprite sprite,
                                   double x1, double y1, double z1, double x2, double y2, double z2,
                                   int light, int overlay) {
        float su0 = sprite.getU0(), su1 = sprite.getU1(), sv0 = sprite.getV0(), sv1 = sprite.getV1();
        double eX = x2 - x1, eY = y2 - y1, eZ = z2 - z1;
        // Per-face UV extents (as fractions of the tile) match the face's two world-space dimensions:
        //   down/up   -> u along X, v along Z ; north/south -> u along X, v along Y ; west/east -> u along Z, v along Y.
        float uX = lerp(su0, su1, (float) eX), uZ = lerp(su0, su1, (float) eZ);
        float vY = lerp(sv0, sv1, (float) eY), vZ = lerp(sv0, sv1, (float) eZ);
        PoseStack.Pose pose = poseStack.last();
        quad(pose, vc, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, su0, sv0, uX, vZ, light, overlay);
        quad(pose, vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, su0, sv0, uX, vZ, light, overlay);
        quad(pose, vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, su0, sv0, uX, vY, light, overlay);
        quad(pose, vc, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, su0, sv0, uX, vY, light, overlay);
        quad(pose, vc, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, su0, sv0, uZ, vY, light, overlay);
        quad(pose, vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, su0, sv0, uZ, vY, light, overlay);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Draws an axis-aligned textured cuboid (block units) with the full sprite on each face. */
    public static void cuboid(PoseStack poseStack, VertexConsumer vc, TextureAtlasSprite sprite,
                              double x1, double y1, double z1, double x2, double y2, double z2,
                              int light, int overlay) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        PoseStack.Pose pose = poseStack.last();
        quad(pose, vc, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, u0, v0, u1, v1, light, overlay);
    }

    /**
     * Like {@link #cuboid} but maps a sub-rectangle of the sprite (fractions {@code 0..1} within the sprite)
     * onto every face — for sampling one clean region of an atlas-style texture (e.g. EE's concrete pole).
     */
    public static void cuboidRegion(PoseStack poseStack, VertexConsumer vc, TextureAtlasSprite sprite,
                                    double x1, double y1, double z1, double x2, double y2, double z2,
                                    float fu0, float fv0, float fu1, float fv1, int light, int overlay) {
        float u0 = lerp(sprite.getU0(), sprite.getU1(), fu0);
        float u1 = lerp(sprite.getU0(), sprite.getU1(), fu1);
        float v0 = lerp(sprite.getV0(), sprite.getV1(), fv0);
        float v1 = lerp(sprite.getV0(), sprite.getV1(), fv1);
        PoseStack.Pose pose = poseStack.last();
        quad(pose, vc, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, u0, v0, u1, v1, light, overlay);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1,
                             int light, int overlay) {
        vertex(pose, vc, ax, ay, az, nx, ny, nz, u0, v1, light, overlay);
        vertex(pose, vc, bx, by, bz, nx, ny, nz, u0, v0, light, overlay);
        vertex(pose, vc, cx, cy, cz, nx, ny, nz, u1, v0, light, overlay);
        vertex(pose, vc, dx, dy, dz, nx, ny, nz, u1, v1, light, overlay);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z,
                               float nx, float ny, float nz, float u, float v, int light, int overlay) {
        // Bake in vanilla directional face shading so these BER cuboids match adjacent baked block models
        // (which have shade baked into their vertex colours); the solid render type does not shade for us.
        int c = (int) (faceShade(nx, ny, nz) * 255.0f);
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(c, c, c, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** Vanilla overworld face-shade multipliers: up 1.0, down 0.5, N/S 0.8, E/W 0.6. */
    private static float faceShade(float nx, float ny, float nz) {
        if (ny > 0.5f)
            return 1.0f;
        if (ny < -0.5f)
            return 0.5f;
        if (Math.abs(nz) > 0.5f)
            return 0.8f;
        return 0.6f;
    }
}
