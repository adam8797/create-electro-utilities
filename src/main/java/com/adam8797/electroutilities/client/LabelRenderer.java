package com.adam8797.electroutilities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared in-world rendering for a pole label: a brass plate (Create's brass_block texture) with the text
 * on top, placed on the wooden post surface rather than floating on the full-block face.
 */
public final class LabelRenderer {

    /** Create's brass block texture — a beveled brass tile that reads as a small plate at label size. */
    private static final ResourceLocation BRASS_TEXTURE = ResourceLocation.fromNamespaceAndPath("create", "block/brass_block");
    private static final int TEXT_COLOR = 0xFF2A1A0A; // dark, for contrast on brass
    private static final float SCALE = 1.0f / 64.0f;

    private LabelRenderer() {}

    /**
     * @param postFaceFromCenter the distance from the block centre to the post's face, in blocks — i.e.
     *                           half the block minus half the post width (utility 4/16, substation 2/16).
     *                           A small extra offset keeps the plate just proud of the wood.
     */
    public static void render(Font font, PoseStack poseStack, MultiBufferSource buffer, int light,
                              String text, Direction facing, float postFaceFromCenter) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -postFaceFromCenter - 0.01);

        // Brass plate: a thin slab sized to the text (+ a little padding), sitting just behind the text
        // (toward the pole) so the text reads on top. Full-sprite mapping shows the tile's bevel as a border.
        float halfW = (font.width(text) * SCALE) / 2.0f + 2.0f * SCALE;
        float halfH = (font.lineHeight * SCALE) / 2.0f + 1.0f * SCALE;
        TextureAtlasSprite brass = RenderUtil.blockSprite(BRASS_TEXTURE);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        RenderUtil.cuboid(poseStack, solid, brass, -halfW, -halfH, 0.002, halfW, halfH, 0.006,
                light, OverlayTexture.NO_OVERLAY);

        // Text on top (no background — the plate is the background).
        poseStack.scale(-SCALE, -SCALE, SCALE);
        int width = font.width(text);
        font.drawInBatch(text, -width / 2.0f, -font.lineHeight / 2.0f + 1.0f, TEXT_COLOR, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
    }
}
