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
        boolean axisX = pole.getX() != pos.getX() || pole.getZ() == pos.getZ();

        // Beam (solid) — render fully before switching buffers.
        TextureAtlasSprite sprite = RenderUtil.blockSprite(be.getWood().interiorSide());
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        double y1 = CrossarmGeometry.BEAM_Y1, y2 = CrossarmGeometry.BEAM_Y2;
        double p1 = CrossarmGeometry.PERP1, p2 = CrossarmGeometry.PERP2;
        if (axisX)
            RenderUtil.cuboid(poseStack, solid, sprite, 0.0, y1, p1, 1.0, y2, p2, packedLight, packedOverlay);
        else
            RenderUtil.cuboid(poseStack, solid, sprite, p1, y1, 0.0, p2, y2, 1.0, packedLight, packedOverlay);

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
