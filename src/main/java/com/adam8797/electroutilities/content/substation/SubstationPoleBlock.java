package com.adam8797.electroutilities.content.substation;

import javax.annotation.Nullable;

import com.adam8797.electroutilities.EUBlockEntityTypes;
import com.adam8797.electroutilities.EUItems;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin 4x4 substation pole. Placed shaft-style along an axis, it forms a column (max
 * {@link #MAX_HEIGHT}) that relays an analog redstone signal from the input end to the output end
 * instantly: the input end reads redstone from any adjacent neighbour, and the output end strongly
 * powers the block just past it (so an item placed on top of a vertical pole is powered).
 *
 * <p>Also supports the utility pole label. Comes in ten woods and concrete (see {@link SubstationMaterial}).
 */
public class SubstationPoleBlock extends RotatedPillarBlock
        implements SimpleWaterloggedBlock, EntityBlock {

    public static final int MAX_HEIGHT = 16;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Catnip placement-helper id, set once in {@link com.adam8797.electroutilities.CreateElectroUtilities}. */
    public static int placementHelperId;

    private static final VoxelShape SHAPE_Y = box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_X = box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_Z = box(6, 6, 0, 10, 10, 16);

    private final SubstationMaterial material;

    public SubstationPoleBlock(Properties properties, SubstationMaterial material) {
        super(properties);
        this.material = material;
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(WATERLOGGED, false));
    }

    public SubstationMaterial getMaterial() {
        return material;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    // ---- shape / placement / waterlog ----

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return state.setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ---- block entity ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SubstationPoleBlockEntity(EUBlockEntityTypes.SUBSTATION_POLE.get(), pos, state);
    }

    // ---- label ----

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        // Shaft-style placement: using a substation item extends the column along its axis.
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SubstationPoleBlock) {
            IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
            if (helper.matchesItem(stack)) {
                PlacementOffset offset = helper.getOffset(player, level, state, pos, hit);
                if (offset.isSuccessful()) {
                    if (!level.isClientSide)
                        offset.placeInWorld(level, blockItem, player, hand, hit);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() != EUItems.UTILITY_POLE_LABEL.get())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Direction face = hit.getDirection();
        if (face.getAxis().isVertical() || !(level.getBlockEntity(pos) instanceof SubstationPoleBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            String text = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
            be.setLabel(text, face);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof SubstationPoleBlockEntity be))
            return InteractionResult.PASS;
        if (be.hasLabel() && be.getLabelFace() == hit.getDirection()) {
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                be.clearLabel();
                popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && !player.isCreative()
                && level.getBlockEntity(pos) instanceof SubstationPoleBlockEntity be && be.hasLabel())
            popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- redstone relay ----

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return relayedSignal(state, level, pos, direction);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return relayedSignal(state, level, pos, direction);
    }

    /** The output end emits the input-end signal toward the block just past it (+axis). */
    private int relayedSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction out = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        // A consumer sitting at the +axis side reads us with direction == out.getOpposite().
        if (direction != out.getOpposite())
            return 0;
        BlockPos outputEnd = walkEnd(level, pos, axis, true);
        if (!pos.equals(outputEnd))
            return 0;
        BlockPos inputEnd = walkEnd(level, pos, axis, false);
        if (level instanceof Level lvl)
            return Math.min(15, lvl.getBestNeighborSignal(inputEnd));
        return 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // Ignore updates that come from within our own column to avoid update loops.
        if (level.getBlockState(neighborPos).getBlock() instanceof SubstationPoleBlock)
            return;
        // An external redstone change: refresh whatever sits past the output end (instant relay).
        BlockPos outputEnd = walkEnd(level, pos, state.getValue(AXIS), true);
        level.updateNeighborsAt(outputEnd, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide)
            level.updateNeighborsAt(walkEnd(level, pos, state.getValue(AXIS), true), this);
    }

    /** Walks along the column axis to the far block. forward=true walks +axis (output), false walks -axis (input). */
    private static BlockPos walkEnd(BlockGetter level, BlockPos pos, Direction.Axis axis, boolean forward) {
        Direction step = Direction.fromAxisAndDirection(axis,
                forward ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        BlockPos current = pos;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos next = current.relative(step);
            BlockState nextState = level.getBlockState(next);
            if (nextState.getBlock() instanceof SubstationPoleBlock && nextState.getValue(AXIS) == axis)
                current = next;
            else
                break;
        }
        return current;
    }
}
