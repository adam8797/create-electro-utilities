package com.adam8797.electroutilities.content.substation;

import com.adam8797.electroutilities.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Renders a substation pole's label, and extends the model toward any adjacent Create Redstone Link
 * so the link doesn't appear to float next to the thin 4x4 pole.
 */
public class SubstationPoleRenderer implements BlockEntityRenderer<SubstationPoleBlockEntity> {

    private static final ResourceLocation REDSTONE_LINK = ResourceLocation.fromNamespaceAndPath("create", "redstone_link");
    private static final double S0 = 6.0 / 16.0, S1 = 10.0 / 16.0;

    private final Font font;

    public SubstationPoleRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(SubstationPoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderLinkExtensions(be, poseStack, buffer, packedLight, packedOverlay);
        if (be.hasLabel())
            renderLabel(be, poseStack, buffer, packedLight);
    }

    private void renderLinkExtensions(SubstationPoleBlockEntity be, PoseStack poseStack,
                                      MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || !(be.getBlockState().getBlock() instanceof SubstationPoleBlock pole))
            return;
        BlockPos pos = be.getBlockPos();
        TextureAtlasSprite sprite = RenderUtil.blockSprite(pole.getMaterial().side());
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        for (Direction dir : Direction.values()) {
            Block neighbor = level.getBlockState(pos.relative(dir)).getBlock();
            if (!REDSTONE_LINK.equals(BuiltInRegistries.BLOCK.getKey(neighbor)))
                continue;
            switch (dir) {
                case EAST -> RenderUtil.cuboid(poseStack, solid, sprite, S1, S0, S0, 1.0, S1, S1, light, overlay);
                case WEST -> RenderUtil.cuboid(poseStack, solid, sprite, 0.0, S0, S0, S0, S1, S1, light, overlay);
                case SOUTH -> RenderUtil.cuboid(poseStack, solid, sprite, S0, S0, S1, S1, S1, 1.0, light, overlay);
                case NORTH -> RenderUtil.cuboid(poseStack, solid, sprite, S0, S0, 0.0, S1, S1, S0, light, overlay);
                case UP -> RenderUtil.cuboid(poseStack, solid, sprite, S0, S1, S0, S1, 1.0, S1, light, overlay);
                case DOWN -> RenderUtil.cuboid(poseStack, solid, sprite, S0, 0.0, S0, S1, S0, S1, light, overlay);
            }
        }
    }

    private void renderLabel(SubstationPoleBlockEntity be, PoseStack poseStack, MultiBufferSource buffer, int light) {
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
