package com.adam8797.electroutilities.content.utilitypole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.adam8797.electroutilities.EUBlockEntityTypes;
import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.EUSimulatedDevices;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.device.ElectricalDeviceBlock;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An 8x8x16 wooden utility pole — placed like a log, mined with an axe. It is the single block for all
 * of this mod's pole features: its {@link UtilityPoleBlockEntity} carries a mutually-exclusive mount
 * (none / crossarm / face-connectors) plus an optional label, all applied by using the relevant item
 * on the pole and rendered by {@code UtilityPoleRenderer}. It participates in EE's electrical network,
 * exposing the crossarm's 3 nodes or the face connectors' nodes as appropriate.
 */
public class UtilityPoleBlock extends RotatedPillarBlock
        implements SimpleWaterloggedBlock, IWrenchable, EntityBlock, ElectricalDeviceBlock<UtilityPoleDevice> {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Catnip placement-helper id, set once in {@link com.adam8797.electroutilities.CreateElectroUtilities}. */
    public static int placementHelperId;

    private static final VoxelShape SHAPE_Y = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_X = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape SHAPE_Z = box(4, 4, 0, 12, 12, 16);

    private final WoodSet wood;

    public UtilityPoleBlock(Properties properties, WoodSet wood) {
        super(properties);
        this.wood = wood;
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(WATERLOGGED, false));
    }

    public WoodSet getWood() {
        return wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    // ---- shape ----

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    // ---- placement / waterlog ----

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
        return new UtilityPoleBlockEntity(EUBlockEntityTypes.UTILITY_POLE.get(), pos, state);
    }

    // ---- applying features (item use) ----

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Shaft-style placement: using a utility pole item extends the column along its existing axis.
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof UtilityPoleBlock) {
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

        if (!(level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Item item = stack.getItem();
        Direction face = hit.getDirection();

        if (item == EUItems.CROSSARM.get())
            return applyCrossarm(be, level, pos, player, stack);

        PoleConnector connector = PoleConnector.fromItem(item);
        if (connector != null)
            return applyConnector(be, level, pos, player, stack, face, connector);

        if (item == EUItems.UTILITY_POLE_LABEL.get())
            return applyLabel(be, level, pos, player, stack, face);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemInteractionResult applyCrossarm(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                                Player player, ItemStack stack) {
        if (be.getMount() != PoleMount.NONE)
            return ItemInteractionResult.FAIL; // exclusive: clear the current mount first
        if (!level.isClientSide) {
            Direction.Axis axis = player.getDirection().getClockWise().getAxis();
            be.setCrossarm(axis, 0);
            level.scheduleTick(pos, this, 1);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            consume(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemInteractionResult applyConnector(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                                 Player player, ItemStack stack, Direction face, PoleConnector connector) {
        if (be.getMount() == PoleMount.CROSSARM || face.getAxis().isVertical())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (be.getConnector(face) != PoleConnector.NONE)
            return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            be.setConnector(face, connector);
            level.scheduleTick(pos, this, 1);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            consume(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemInteractionResult applyLabel(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                             Player player, ItemStack stack, Direction face) {
        if (face.getAxis().isVertical())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            String text = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
            be.setLabel(text, face);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            consume(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild)
            stack.shrink(1);
    }

    // ---- wrench: cycle crossarm offset, or remove a connector ----

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be) {
            if (be.getMount() == PoleMount.CROSSARM) {
                if (!level.isClientSide) {
                    int next = be.getCrossarmOffset() >= 1 ? -1 : be.getCrossarmOffset() + 1;
                    be.setCrossarmOffset(next);
                    level.scheduleTick(pos, this, 1);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (be.getMount() == PoleMount.CONNECTORS && !face.getAxis().isVertical()
                    && be.getConnector(face) != PoleConnector.NONE) {
                if (!level.isClientSide && level instanceof ServerLevel) {
                    PoleConnector connector = be.getConnector(face);
                    removeWiresByPlayer(context.getPlayer(), level, pos); // drops all pole wires (limitation)
                    be.removeConnector(face);
                    level.scheduleTick(pos, this, 1);
                    if (connector.item() != null)
                        popResource((ServerLevel) level, pos, new ItemStack(connector.item()));
                    level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        removeWiresByPlayer(context.getPlayer(), context.getLevel(), context.getClickedPos());
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    // ---- sneak + empty hand: remove label or crossarm, returning the item ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be))
            return InteractionResult.PASS;
        Direction face = hit.getDirection();

        if (be.hasLabel() && be.getLabelFace() == face) {
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                be.clearLabel();
                popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (be.getMount() == PoleMount.CROSSARM) {
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                removeWiresByPlayer(player, level, pos);
                be.clearMount();
                level.scheduleTick(pos, this, 1);
                popResource(sl, pos, new ItemStack(EUItems.CROSSARM.get()));
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ---- EE device + node lifecycle (mirrors SimpleElectricalDeviceBlock) ----

    @Override
    public SimulatedDeviceType<UtilityPoleDevice> getDevice() {
        return EUSimulatedDevices.UTILITY_POLE.get();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<Integer> nodes = new ArrayList<>(getNodePositions(level, pos, state).keySet());
        InfrastructureSavedData.load(level).registerOrUpdateNodes(pos, nodes);
        super.tick(state, level, pos, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl) {
            if (player.isCreative()) {
                InfrastructureSavedData sd = InfrastructureSavedData.load(sl);
                for (InWorldNodeData nodeData : sd.getNodesAt(pos))
                    for (InWorldNodeConnection connection : sd.getConnections(nodeData))
                        sd.removeConnection(connection);
            } else {
                removeWiresByPlayer(player, level, pos);
                if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be) {
                    if (be.getMount() == PoleMount.CROSSARM)
                        popResource(sl, pos, new ItemStack(EUItems.CROSSARM.get()));
                    else if (be.getMount() == PoleMount.CONNECTORS)
                        for (Direction face : PoleConnectorGeometry.FACES) {
                            PoleConnector connector = be.getConnector(face);
                            if (connector != PoleConnector.NONE && connector.item() != null)
                                popResource(sl, pos, new ItemStack(connector.item()));
                        }
                    if (be.hasLabel())
                        popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- nodes ----

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        Map<Integer, Vec3> nodes = new HashMap<>();
        if (!(level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be))
            return nodes;
        switch (be.getMount()) {
            case CROSSARM -> {
                for (int i = 0; i < 3; i++)
                    nodes.put(i, CrossarmGeometry.nodePosition(be.getCrossarmAxis(), be.getCrossarmOffset(), i));
            }
            case CONNECTORS -> {
                for (Direction face : PoleConnectorGeometry.FACES) {
                    PoleConnector connector = be.getConnector(face);
                    if (connector == PoleConnector.NONE)
                        continue;
                    int fi = PoleConnectorGeometry.faceIndex(face);
                    double[] heights = PoleConnectorGeometry.pinHeights(connector.nodeCount());
                    for (int pin = 0; pin < heights.length; pin++)
                        nodes.put(PoleConnectorGeometry.nodeId(fi, pin), PoleConnectorGeometry.nodePosition(face, heights[pin]));
                }
            }
            default -> {}
        }
        return nodes;
    }

    @Nullable
    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int id) {
        return getNodePositions(level, pos, state).get(id);
    }
}
