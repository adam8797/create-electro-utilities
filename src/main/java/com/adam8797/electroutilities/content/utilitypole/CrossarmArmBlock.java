package com.adam8797.electroutilities.content.utilitypole;

import java.util.Map;

import javax.annotation.Nullable;

import com.adam8797.electroutilities.EUBlockEntityTypes;
import com.adam8797.electroutilities.EUSimulatedDevices;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.base.SimpleElectricalDeviceBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
public class CrossarmArmBlock extends SimpleElectricalDeviceBlock<UtilityPoleDevice> implements EntityBlock {

    public CrossarmArmBlock(Properties properties) {
        super(properties);
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

    private Direction.Axis axisAt(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrossarmArmBlockEntity be) {
            BlockPos pole = be.getPolePos();
            if (pole.getX() != pos.getX())
                return Direction.Axis.X;
            if (pole.getZ() != pos.getZ())
                return Direction.Axis.Z;
        }
        return Direction.Axis.X;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape beam = axisAt(level, pos) == Direction.Axis.X
                ? Block.box(0, 11, 6, 16, 15, 10)
                : Block.box(6, 11, 0, 10, 15, 16);
        return Shapes.or(beam, Block.box(6, 11, 6, 10, 16, 10)); // beam + connector stalk for clicking
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
