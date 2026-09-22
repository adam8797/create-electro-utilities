package com.adam8797.electroutilities.content.utilitypole;

import com.adam8797.electroutilities.client.EUClient;
import com.adam8797.electroutilities.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;

/**
 * Renders a crossarm arm block: a wooden beam segment (matching the pole's interior wood) across the
 * block along the arm axis, plus one EE connector on top. Node/beam heights come from
 * {@link CrossarmGeometry}.
 */
public class CrossarmArmRenderer implements BlockEntityRenderer<CrossarmArmBlockEntity> {

    public CrossarmArmRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CrossarmArmBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pole = be.getPolePos();
        BlockPos pos = be.getBlockPos();
        // The line direction from the pole to this arm gives the rotation to draw the beam along.
        int rot = PoleRotation.fromDelta(pos.getX() - pole.getX(), pos.getZ() - pole.getZ());
        if (rot < 0)
            rot = 2; // not yet configured: fall back to E/W

        // Beam (solid) — render fully before switching buffers.
        TextureAtlasSprite sprite = RenderUtil.blockSprite(be.getWood().interiorSide());
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        RenderUtil.crossarmBeam(poseStack, solid, sprite, rot, packedLight, packedOverlay);

        // Connector on top (cutout).
        BakedModel connector = mc.getModelManager().getModel(EUClient.CONNECTOR_MODEL);
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
        poseStack.pushPose();
        poseStack.translate(0.0, CrossarmGeometry.CONNECTOR_BASE_Y, 0.0);
        renderer.renderModel(poseStack.last(), cutout, null, connector, 1, 1, 1, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
