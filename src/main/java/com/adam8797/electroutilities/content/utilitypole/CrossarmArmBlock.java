package com.adam8797.electroutilities.content.utilitypole;

import java.util.Map;

import javax.annotation.Nullable;

import com.adam8797.electroutilities.EUBlockEntityTypes;
import com.adam8797.electroutilities.EUSimulatedDevices;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.base.SimpleElectricalDeviceBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One arm segment of a crossarm multiblock. Placed automatically (never a normal block item) to the
 * side of a utility pole when a crossarm is applied. Hosts a single EE connector node at its centre,
 * so it is independently wireable, and is rendered (beam + connector) by {@link CrossarmArmRenderer}
 * using its {@link CrossarmArmBlockEntity}. Breaking it, or its pole, tears down the whole crossarm.
 */
public class CrossarmArmBlock extends SimpleElectricalDeviceBlock<UtilityPoleDevice> implements EntityBlock, IWrenchable {

    public CrossarmArmBlock(Properties properties) {
        super(properties);
    }

    // ---- wrench: an arm is the crossarm's "extent" — wrenching it slides the crossarm along its line ----

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof CrossarmArmBlockEntity be) {
            if (!level.isClientSide)
                UtilityPoleBlock.slideCrossarm(level, be.getPolePos());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof CrossarmArmBlockEntity be
                && level.getBlockState(be.getPolePos()).getBlock() instanceof UtilityPoleBlock) {
            if (!level.isClientSide) {
                Player player = context.getPlayer();
                removeWiresByPlayer(player, level, be.getPolePos());
                UtilityPoleBlock.removeCrossarm(level, be.getPolePos(), player == null || !player.isCreative());
                level.playSound(null, context.getClickedPos(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.onSneakWrenched(state, context);
    }

    // ---- EE device + node ----

    @Override
    public SimulatedDeviceType<UtilityPoleDevice> getDevice() {
        return EUSimulatedDevices.UTILITY_POLE.get();
    }

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return Map.of(0, CrossarmGeometry.nodeLocal());
    }

    @Nullable
    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int id) {
        return id == 0 ? CrossarmGeometry.nodeLocal() : null;
    }

    // ---- block entity ----

    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossarmArmBlockEntity(EUBlockEntityTypes.CROSSARM_ARM.get(), pos, state);
    }

    // ---- shape ----

    /** The rotation (line direction) from the owning pole to this arm, or -1 if not yet configured. */
    private int rotationAt(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrossarmArmBlockEntity be) {
            BlockPos pole = be.getPolePos();
            return PoleRotation.fromDelta(pos.getX() - pole.getX(), pos.getZ() - pole.getZ());
        }
        return -1;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rot = rotationAt(level, pos);
        VoxelShape beam = CrossarmGeometry.beamOutline(rot < 0 ? 2 : rot); // rot 2 = E/W fallback
        // beam + connector: the box tracks the rendered connector (x/z 5-11, y 15-25), poking above
        // the block so the insulator itself is clickable, not just the wood.
        return Shapes.or(beam, Block.box(5, 15, 5, 11, 25, 11));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // ---- multiblock lifecycle ----

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CrossarmArmBlockEntity be))
            return true; // not configured yet
        return level.getBlockState(be.getPolePos()).getBlock() instanceof UtilityPoleBlock;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel && !player.isCreative())
            removeWiresByPlayer(player, level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CrossarmArmBlockEntity be) {
            // If the pole still thinks it has this crossarm, this arm was broken directly: tear it down.
            UtilityPoleBlock.removeCrossarm(level, be.getPolePos(), true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
