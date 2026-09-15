package com.adam8797.electroutilities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
