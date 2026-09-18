package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * State for one arm block of a crossarm multiblock: the wood it should render as, and the position of
 * the utility pole it belongs to (used for rendering direction and for tearing the crossarm down).
 * Synced to clients for rendering.
 */
public class CrossarmArmBlockEntity extends BlockEntity {

    private WoodSet wood = WoodSet.OAK;
    private BlockPos polePos = BlockPos.ZERO;

    public CrossarmArmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public WoodSet getWood() {
        return wood;
    }

    public BlockPos getPolePos() {
        return polePos;
    }

    public void configure(WoodSet wood, BlockPos polePos) {
        this.wood = wood;
        this.polePos = polePos;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Wood", wood.id());
        tag.putLong("Pole", polePos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wood = WoodSet.byId(tag.getString("Wood"));
        polePos = BlockPos.of(tag.getLong("Pole"));
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
