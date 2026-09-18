package com.adam8797.electroutilities.content.utilitypole;

import com.adam8797.electroutilities.client.EUClient;
import com.adam8797.electroutilities.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a utility pole's active feature(s): EE connector models (for face-connectors or the three
 * crossarm connectors), a simple wooden crossarm beam, and the pole label text.
 *
 * <p>NOTE: this is functional-but-rough placeholder rendering — orientation/fit are expected to be
 * tuned. The electrical node positions are authoritative (see {@link PoleConnectorGeometry} /
 * {@link CrossarmGeometry}); the visuals are cosmetic.
 */
public class UtilityPoleRenderer implements BlockEntityRenderer<UtilityPoleBlockEntity> {

    private final Font font;

    public UtilityPoleRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(UtilityPoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel connectorModel = mc.getModelManager().getModel(EUClient.CONNECTOR_MODEL);
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());

        switch (be.getMount()) {
            case CONNECTORS -> renderFaceConnectors(be, connectorModel, renderer, poseStack, cutout, packedLight, packedOverlay);
            case CROSSARM -> renderCrossarm(be, connectorModel, renderer, poseStack, buffer, packedLight, packedOverlay);
            default -> {}
        }

        if (be.hasLabel())
            renderLabel(be, poseStack, buffer, packedLight);
    }

    // ---- face connectors ----

    private void renderFaceConnectors(UtilityPoleBlockEntity be, BakedModel model, ModelBlockRenderer renderer,
                                      PoseStack poseStack, VertexConsumer vc, int light, int overlay) {
        Direction.Axis poleAxis = be.getBlockState().getValue(RotatedPillarBlock.AXIS);
        for (Direction face : PoleConnectorGeometry.FACES) {
            PoleConnector connector = be.getConnector(face);
            if (connector == PoleConnector.NONE)
                continue;
            for (double spread : PoleConnectorGeometry.pinSpreads(connector.nodeCount()))
                renderFaceConnector(model, renderer, poseStack, vc, light, overlay, face, poleAxis, spread);
        }
    }

    private void renderFaceConnector(BakedModel model, ModelBlockRenderer renderer, PoseStack poseStack,
                                     VertexConsumer vc, int light, int overlay, Direction face,
                                     Direction.Axis poleAxis, double spread) {
        // Base at the block centre (offset along the pole axis by the pin spread), pointing outward:
        // the model's insulator ball lands at the block edge = the node (NODE_OUT = 0.5).
        Axis rotAxis;
        float angle;
        switch (face) {
            case UP -> { rotAxis = Axis.YP; angle = 0f; }
            case DOWN -> { rotAxis = Axis.XP; angle = 180f; }
            case NORTH -> { rotAxis = Axis.XP; angle = -90f; }
            case SOUTH -> { rotAxis = Axis.XP; angle = 90f; }
            case EAST -> { rotAxis = Axis.ZP; angle = -90f; }
            case WEST -> { rotAxis = Axis.ZP; angle = 90f; }
            default -> { return; }
        }
        Vec3 axisUnit = PoleConnectorGeometry.axisVec(poleAxis);
        poseStack.pushPose();
        poseStack.translate(0.5 + axisUnit.x * spread, 0.5 + axisUnit.y * spread, 0.5 + axisUnit.z * spread);
        poseStack.mulPose(rotAxis.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderer.renderModel(poseStack.last(), vc, null, model, 1, 1, 1, light, overlay);
        poseStack.popPose();
    }

    // ---- crossarm ----

    private void renderCrossarm(UtilityPoleBlockEntity be, BakedModel connectorModel, ModelBlockRenderer renderer,
                                PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        // The pole renders only its own slot: the beam segment across this block, and the centre
        // connector when it is the topmost block. The two side connectors are their own arm blocks.
        Direction.Axis axis = be.getCrossarmAxis();
        double y1 = CrossarmGeometry.BEAM_Y1, y2 = CrossarmGeometry.BEAM_Y2;
        double p1 = CrossarmGeometry.PERP1, p2 = CrossarmGeometry.PERP2;

        TextureAtlasSprite sprite = interiorSprite(be);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        if (axis == Direction.Axis.X)
            RenderUtil.cuboidTiled(poseStack, solid, sprite, 0.0, y1, p1, 1.0, y2, p2, light, overlay);
        else
            RenderUtil.cuboidTiled(poseStack, solid, sprite, p1, y1, 0.0, p2, y2, 1.0, light, overlay);

        boolean top = be.getLevel() != null && UtilityPoleBlock.isCrossarmTop(be.getLevel(), be.getBlockPos());
        if (top) {
            VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
            poseStack.pushPose();
            poseStack.translate(0.0, CrossarmGeometry.CONNECTOR_BASE_Y, 0.0);
            renderer.renderModel(poseStack.last(), cutout, null, connectorModel, 1, 1, 1, light, overlay);
            poseStack.popPose();
        }
    }

    private TextureAtlasSprite interiorSprite(UtilityPoleBlockEntity be) {
        Block block = be.getBlockState().getBlock();
        WoodSet wood = block instanceof UtilityPoleBlock pole ? pole.getWood() : WoodSet.OAK;
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(wood.interiorSide());
    }

    // ---- label ----

    private void renderLabel(UtilityPoleBlockEntity be, PoseStack poseStack, MultiBufferSource buffer, int light) {
        Direction facing = be.getLabelFace();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 + 1.0 / 16.0);
        float scale = 1.0f / 64.0f;
        poseStack.scale(-scale, -scale, scale);
        int width = font.width(be.getLabelText());
        font.drawInBatch(be.getLabelText(), -width / 2.0f, -4.0f, 0xFF202020, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();
    }
}
