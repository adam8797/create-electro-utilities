package com.adam8797.electroutilities.content.substation;

import com.adam8797.electroutilities.client.LabelRenderer;
import com.adam8797.electroutilities.client.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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

    /** EE's concrete pole texture; the nub samples a square chunk of the same side strip the post uses. */
    private static final ResourceLocation CONCRETE_TEX = ResourceLocation.fromNamespaceAndPath("electroenergetics", "block/concrete_pole");
    // px 18-22 x 16-20 of 32: a 4x4 corner of the post's side strip (uv 9,8 area) — same shade, square so it
    // maps onto the nub's faces without the stretch a tall strip caused.
    private static final float C_U0 = 18f / 32f, C_V0 = 16f / 32f, C_U1 = 22f / 32f, C_V1 = 20f / 32f;

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
        boolean concrete = pole.getMaterial().isConcrete();
        // Match the post: concrete extends from EE's concrete texture (clean strip), wood from its log.
        TextureAtlasSprite sprite = RenderUtil.blockSprite(concrete ? CONCRETE_TEX : pole.getMaterial().side());
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        for (Direction dir : Direction.values()) {
            Block neighbor = level.getBlockState(pos.relative(dir)).getBlock();
            if (!REDSTONE_LINK.equals(BuiltInRegistries.BLOCK.getKey(neighbor)))
                continue;
            double x1, y1, z1, x2, y2, z2;
            switch (dir) {
                case EAST -> { x1 = S1; y1 = S0; z1 = S0; x2 = 1.0; y2 = S1; z2 = S1; }
                case WEST -> { x1 = 0.0; y1 = S0; z1 = S0; x2 = S0; y2 = S1; z2 = S1; }
                case SOUTH -> { x1 = S0; y1 = S0; z1 = S1; x2 = S1; y2 = S1; z2 = 1.0; }
                case NORTH -> { x1 = S0; y1 = S0; z1 = 0.0; x2 = S1; y2 = S1; z2 = S0; }
                case UP -> { x1 = S0; y1 = S1; z1 = S0; x2 = S1; y2 = 1.0; z2 = S1; }
                case DOWN -> { x1 = S0; y1 = 0.0; z1 = S0; x2 = S1; y2 = S0; z2 = S1; }
                default -> { continue; }
            }
            if (concrete)
                RenderUtil.cuboidRegion(poseStack, solid, sprite, x1, y1, z1, x2, y2, z2, C_U0, C_V0, C_U1, C_V1, light, overlay);
            else
                RenderUtil.cuboid(poseStack, solid, sprite, x1, y1, z1, x2, y2, z2, light, overlay);
        }
    }

    private void renderLabel(SubstationPoleBlockEntity be, PoseStack poseStack, MultiBufferSource buffer, int light) {
        // 4x4 post: its face is 2/16 from the block centre, so the plate sits on the wood.
        LabelRenderer.render(font, poseStack, buffer, light, be.getLabelText(), be.getLabelFace(), 2.0f / 16.0f);
    }
}
