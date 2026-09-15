package com.adam8797.electroutilities.content.utilitypole;

import com.adam8797.electroutilities.client.EUClient;
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
            case CROSSARM -> renderCrossarm(be, connectorModel, renderer, poseStack, buffer, cutout, packedLight, packedOverlay);
            default -> {}
        }

        if (be.hasLabel())
            renderLabel(be, poseStack, buffer, packedLight);
    }

    // ---- face connectors ----

    private void renderFaceConnectors(UtilityPoleBlockEntity be, BakedModel model, ModelBlockRenderer renderer,
                                      PoseStack poseStack, VertexConsumer vc, int light, int overlay) {
        for (Direction face : PoleConnectorGeometry.FACES) {
            PoleConnector connector = be.getConnector(face);
            if (connector == PoleConnector.NONE)
                continue;
            for (double h : PoleConnectorGeometry.pinHeights(connector.nodeCount()))
                renderFaceConnector(model, renderer, poseStack, vc, light, overlay, face, h);
        }
    }

    private void renderFaceConnector(BakedModel model, ModelBlockRenderer renderer, PoseStack poseStack,
                                     VertexConsumer vc, int light, int overlay, Direction face, double height) {
        double mountX = 0.5, mountZ = 0.5;
        Axis rotAxis;
        float angle;
        switch (face) {
            case NORTH -> { mountZ = 0.5 - PoleConnectorGeometry.SURFACE; rotAxis = Axis.XP; angle = -90f; }
            case SOUTH -> { mountZ = 0.5 + PoleConnectorGeometry.SURFACE; rotAxis = Axis.XP; angle = 90f; }
            case EAST -> { mountX = 0.5 + PoleConnectorGeometry.SURFACE; rotAxis = Axis.ZP; angle = -90f; }
            case WEST -> { mountX = 0.5 - PoleConnectorGeometry.SURFACE; rotAxis = Axis.ZP; angle = 90f; }
            default -> { return; }
        }
        poseStack.pushPose();
        poseStack.translate(mountX, height, mountZ);
        poseStack.mulPose(rotAxis.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderer.renderModel(poseStack.last(), vc, null, model, 1, 1, 1, light, overlay);
        poseStack.popPose();
    }

    // ---- crossarm ----

    private void renderCrossarm(UtilityPoleBlockEntity be, BakedModel connectorModel, ModelBlockRenderer renderer,
                                PoseStack poseStack, MultiBufferSource buffer, VertexConsumer cutout, int light, int overlay) {
        Direction.Axis axis = be.getCrossarmAxis();
        int offset = be.getCrossarmOffset();
        double shift = CrossarmGeometry.shift(offset);

        // Beam, textured with the pole's interior (stripped) wood texture.
        TextureAtlasSprite sprite = interiorSprite(be);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        double lo = -0.5 + shift, hi = 1.5 + shift;
        double y1 = 11.0 / 16.0, y2 = 15.0 / 16.0, p1 = 6.0 / 16.0, p2 = 10.0 / 16.0;
        if (axis == Direction.Axis.X)
            renderCuboid(poseStack, solid, sprite, lo, y1, p1, hi, y2, p2, light, overlay);
        else
            renderCuboid(poseStack, solid, sprite, p1, y1, lo, p2, y2, hi, light, overlay);

        // Three connectors pointing up, sitting on top of the beam.
        for (int i = 0; i < 3; i++) {
            double a = CrossarmGeometry.along(i, offset);
            double x = axis == Direction.Axis.X ? a : 0.5;
            double z = axis == Direction.Axis.X ? 0.5 : a;
            poseStack.pushPose();
            poseStack.translate(x - 0.5, y2, z - 0.5);
            renderer.renderModel(poseStack.last(), cutout, null, connectorModel, 1, 1, 1, light, overlay);
            poseStack.popPose();
        }
    }

    private TextureAtlasSprite interiorSprite(UtilityPoleBlockEntity be) {
        Block block = be.getBlockState().getBlock();
        WoodSet wood = block instanceof UtilityPoleBlock pole ? pole.getWood() : WoodSet.OAK;
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(wood.interiorSide());
    }

    /** Renders an axis-aligned textured cuboid (block units) with the full sprite on each face. */
    private void renderCuboid(PoseStack poseStack, VertexConsumer vc, TextureAtlasSprite sprite,
                              double x1, double y1, double z1, double x2, double y2, double z2,
                              int light, int overlay) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        PoseStack.Pose pose = poseStack.last();
        // down / up
        quad(pose, vc, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, u0, v0, u1, v1, light, overlay);
        // north / south
        quad(pose, vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, u0, v0, u1, v1, light, overlay);
        // west / east
        quad(pose, vc, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, u0, v0, u1, v1, light, overlay);
        quad(pose, vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, u0, v0, u1, v1, light, overlay);
    }

    private void quad(PoseStack.Pose pose, VertexConsumer vc,
                      double ax, double ay, double az, double bx, double by, double bz,
                      double cx, double cy, double cz, double dx, double dy, double dz,
                      float nx, float ny, float nz, float u0, float v0, float u1, float v1,
                      int light, int overlay) {
        vertex(pose, vc, ax, ay, az, nx, ny, nz, u0, v1, light, overlay);
        vertex(pose, vc, bx, by, bz, nx, ny, nz, u0, v0, light, overlay);
        vertex(pose, vc, cx, cy, cz, nx, ny, nz, u1, v0, light, overlay);
        vertex(pose, vc, dx, dy, dz, nx, ny, nz, u1, v1, light, overlay);
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z,
                        float nx, float ny, float nz, float u, float v, int light, int overlay) {
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
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
