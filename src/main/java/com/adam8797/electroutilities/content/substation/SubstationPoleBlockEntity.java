package com.adam8797.electroutilities.content.substation;

import com.adam8797.electroutilities.content.label.LabelableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds a substation pole's optional label (text + face). Synced to clients for rendering.
 */
public class SubstationPoleBlockEntity extends BlockEntity implements LabelableBlockEntity {

    // The substation post is only 4x4, so its plate fits far less text than the utility pole's.
    public static final int MAX_LABEL_LENGTH = 2;

    private String labelText = "";
    private Direction labelFace = Direction.NORTH;

    public SubstationPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getLabelText() {
        return labelText;
    }

    public Direction getLabelFace() {
        return labelFace;
    }

    public boolean hasLabel() {
        return !labelText.isEmpty();
    }

    @Override
    public int maxLabelLength() {
        return MAX_LABEL_LENGTH;
    }

    public void setLabel(String text, Direction face) {
        this.labelText = truncateLabel(text);
        this.labelFace = face;
        sync();
    }

    public void clearLabel() {
        this.labelText = "";
        sync();
    }

    public static String truncateLabel(String s) {
        if (s == null)
            return "";
        s = s.strip();
        return s.length() > MAX_LABEL_LENGTH ? s.substring(0, MAX_LABEL_LENGTH) : s;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Label", labelText);
        tag.putByte("LabelFace", (byte) labelFace.get3DDataValue());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        labelText = truncateLabel(tag.getString("Label"));
        labelFace = Direction.from3DDataValue(tag.getByte("LabelFace"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
