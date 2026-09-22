package com.adam8797.electroutilities.content.utilitypole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.adam8797.electroutilities.EUBlockEntityTypes;
import com.adam8797.electroutilities.EUBlocks;
import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.EUSimulatedDevices;
import com.george_vi.electroenergetics.config.CEEConfigs;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.device.ElectricalDeviceBlock;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.adam8797.electroutilities.net.OpenLabelEditorPayload;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.network.PacketDistributor;

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
    /** True when the pole's rotation is a 45° (odd) step: the post renders/collides as a diamond. */
    public static final BooleanProperty DIAGONAL = BooleanProperty.create("diagonal");

    /** Catnip placement-helper id, set once in {@link com.adam8797.electroutilities.CreateElectroUtilities}. */
    public static int placementHelperId;

    private static final VoxelShape SHAPE_Y = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_X = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape SHAPE_Z = box(4, 4, 0, 12, 12, 16);
    // Diamond footprint: the 8px post turned 45° reaches ~5.66px from centre, so a ~11px bounding box.
    private static final VoxelShape SHAPE_Y_DIAG = box(2.3, 0, 2.3, 13.7, 16, 13.7);

    private final WoodSet wood;

    public UtilityPoleBlock(Properties properties, WoodSet wood) {
        super(properties);
        this.wood = wood;
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(DIAGONAL, false)
                .setValue(WATERLOGGED, false));
    }

    public WoodSet getWood() {
        return wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DIAGONAL, WATERLOGGED);
    }

    // ---- shape ----

    private static VoxelShape poleShape(BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> state.getValue(DIAGONAL) ? SHAPE_Y_DIAG : SHAPE_Y;
        };
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape base = poleShape(state);
        // When a crossarm is present, render/interact with the beam over the pole's own slot, plus the
        // centre connector stalk (only when this pole is the topmost block, matching the node).
        if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be && be.getMount() == PoleMount.CROSSARM) {
            VoxelShape shape = Shapes.or(base, CrossarmGeometry.beamOutline(be.getRotation()));
            // Connector box tracks the rendered connector (x/z 5-11, y 15-25), poking above the block
            // so the insulator itself is clickable, not just the wood.
            if (isCrossarmTop(level, pos))
                shape = Shapes.or(shape, Block.box(5, 15, 5, 11, 25, 11));
            return shape;
        }
        return base;
    }

    /** The crossarm's centre (pole) connector only exists when nothing sits directly above the pole. */
    public static boolean isCrossarmTop(BlockGetter level, BlockPos pos) {
        return !(level.getBlockState(pos.above()).getBlock() instanceof UtilityPoleBlock);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return poleShape(state);
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
        // Utility poles are vertical only: ignore the clicked face's axis (RotatedPillarBlock's default).
        // Fresh poles start at rotation 0 (a square post), so DIAGONAL is false.
        return state.setValue(AXIS, Direction.Axis.Y).setValue(DIAGONAL, false).setValue(WATERLOGGED, waterlogged);
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

        if (item == EUItems.CROSSARM.get())
            return applyCrossarm(be, level, pos, player, stack);

        PoleConnector connector = PoleConnector.fromItem(item);
        if (connector != null)
            return applyConnector(be, level, pos, player, stack, hit, connector);

        if (item == EUItems.UTILITY_POLE_LABEL.get())
            return applyLabel(be, level, pos, player, hit);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Maps a click on the pole to the base (rot-0) slot face it targets, or null for a vertical face.
     * The hit point's angle around the pole centre picks the nearest of the four rotated slots, so a
     * click on a cardinal face at a 45° rotation resolves to the intended diagonal slot.
     */
    private static Direction slotFaceFromHit(BlockPos pos, BlockHitResult hit, int rotation) {
        return slotFaceFromClick(pos, hit.getDirection(), hit.getLocation(), rotation);
    }

    private static Direction slotFaceFromClick(BlockPos pos, Direction clickedFace, Vec3 loc, int rotation) {
        int worldSlot = PoleRotation.slotFor(clickedFace, loc.x - pos.getX(), loc.z - pos.getZ(), rotation);
        return worldSlot < 0 ? null : PoleRotation.baseCardinal(worldSlot, rotation);
    }

    /**
     * The base face of the occupied connector a side-face wrench is pointing at, or null to fall through
     * to a rotation. Picks the occupied slot whose (rotated) world direction is nearest the hit angle,
     * within half a slot (45°), so clicking a connector always removes it — even at a 45° rotation where
     * two diagonal slots flank the clicked face and a nearest-empty-slot check could miss the connector.
     */
    private static Direction connectorForClick(BlockPos pos, Direction clickedFace, Vec3 loc, UtilityPoleBlockEntity be) {
        if (clickedFace.getAxis().isVertical())
            return null; // wrenching the top rotates a connector pole (its escape hatch)
        int rot = be.getRotation();
        double clickDeg = Math.toDegrees(Math.atan2(loc.x - pos.getX() - 0.5, -(loc.z - pos.getZ() - 0.5)));
        Direction best = null;
        double bestDist = 45.0 + 1.0e-6; // within half the 90° slot spacing
        for (Direction f : PoleConnectorGeometry.FACES) {
            if (f.getAxis().isVertical() || be.getConnector(f) == PoleConnector.NONE)
                continue;
            double worldDeg = 45.0 * PoleRotation.wrap(PoleRotation.cardinalIndex(f) + rot);
            double diff = Math.abs(clickDeg - worldDeg) % 360.0;
            double dist = diff > 180.0 ? 360.0 - diff : diff;
            if (dist < bestDist) {
                bestDist = dist;
                best = f;
            }
        }
        return best;
    }

    private ItemInteractionResult applyCrossarm(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                                Player player, ItemStack stack) {
        if (be.getMount() != PoleMount.NONE)
            return ItemInteractionResult.FAIL; // exclusive: clear the current mount first
        // A crossarm orients along the pole's rotation. Respect a pole the player already turned; only a
        // fresh, unlabeled pole snaps to face the player (crossarm perpendicular to their look, as before).
        int rot = be.getRotation();
        if (rot == 0 && !be.hasLabel())
            rot = PoleRotation.cardinalIndex(player.getDirection().getClockWise());
        if (!canPlaceArms(level, pos, rot, 0))
            return ItemInteractionResult.FAIL; // no room for the arm blocks
        if (!level.isClientSide) {
            placeArms(level, pos, rot, 0, wood);
            be.setRotation(rot);
            be.setCrossarm(0);
            applyDiagonalState(level, pos, rot);
            level.scheduleTick(pos, this, 1);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            consume(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemInteractionResult applyConnector(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                                 Player player, ItemStack stack, BlockHitResult hit,
                                                 PoleConnector connector) {
        // Connectors mount only on the four side slots (not the pole's ends); the targeted slot is the
        // rotated slot nearest the hit point, stored on its base face.
        Direction slot = slotFaceFromHit(pos, hit, be.getRotation());
        if (be.getMount() == PoleMount.CROSSARM || slot == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (be.getConnector(slot) != PoleConnector.NONE)
            return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            be.setConnector(slot, connector);
            level.scheduleTick(pos, this, 1);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            consume(player, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Opens the label editor for a new (blank) label; the item is consumed on confirm, not here. */
    private ItemInteractionResult applyLabel(UtilityPoleBlockEntity be, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        Direction slot = slotFaceFromHit(pos, hit, be.getRotation());
        if (slot == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp, new OpenLabelEditorPayload(pos, slot, ""));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild)
            stack.shrink(1);
    }

    // ---- wrench: rotate the whole pole column; or (on a crossarm post body) cycle offset / pop a connector ----

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be) {
            // Wrenching a connector pops it; wrenching the pole itself rotates the whole column. Sliding
            // the crossarm is done by wrenching an arm block instead (see CrossarmArmBlock.onWrenched).
            if (be.getMount() == PoleMount.CONNECTORS) {
                // A wrench that lands on a connector removes it (cancelling the rotation); only a click
                // that isn't pointing at any connector falls through to rotate the column.
                Direction slot = connectorForClick(pos, face, context.getClickLocation(), be);
                if (slot != null) {
                    if (!level.isClientSide && level instanceof ServerLevel) {
                        PoleConnector connector = be.getConnector(slot);
                        removeWiresByPlayer(context.getPlayer(), level, pos); // drops all pole wires (limitation)
                        be.removeConnector(slot);
                        level.scheduleTick(pos, this, 1);
                        if (connector.item() != null)
                            popResource((ServerLevel) level, pos, new ItemStack(connector.item()));
                        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (!level.isClientSide)
                rotatePole(level, pos, be);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        // A crossarm is dismantled first: sneak-wrenching the pole removes the crossarm (mirroring the
        // empty-hand path) rather than uprooting the whole pole out from under it.
        if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be && be.getMount() == PoleMount.CROSSARM) {
            if (!level.isClientSide) {
                removeWiresByPlayer(player, level, pos);
                removeCrossarm(level, pos, player == null || !player.isCreative());
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        removeWiresByPlayer(player, level, pos);
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    // ---- empty hand: sneak removes a label/crossarm (returning the item); plain click re-opens the editor ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be))
            return InteractionResult.PASS;
        // The clicked side maps to a base slot through the pole's rotation, matching how the label is stored.
        Direction slot = slotFaceFromHit(pos, hit, be.getRotation());

        if (!player.isShiftKeyDown()) {
            // Plain (non-sneak) click on a labeled face re-opens the sign-style editor to change the text.
            if (be.hasLabel() && be.getLabelFace() == slot) {
                if (!level.isClientSide && player instanceof ServerPlayer sp)
                    PacketDistributor.sendToPlayer(sp, new OpenLabelEditorPayload(pos, slot, be.getLabelText()));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        if (be.hasLabel() && be.getLabelFace() == slot) {
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                be.clearLabel();
                popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (be.getMount() == PoleMount.CROSSARM) {
            if (!level.isClientSide) {
                removeWiresByPlayer(player, level, pos);
                removeCrossarm(level, pos, !player.isCreative());
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // When the block directly above changes, a crossarm's centre connector may appear/disappear.
        if (!level.isClientSide && level instanceof ServerLevel sl && neighborPos.equals(pos.above())
                && level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be && be.getMount() == PoleMount.CROSSARM) {
            if (!isCrossarmTop(level, pos))
                dropWiresAt(sl, pos); // centre node is now blocked: actually disconnect its wires
            level.scheduleTick(pos, this, 1); // re-register nodes (drop or restore the centre node)
        }
    }

    /**
     * Removes and drops every wire connected to any node at {@code pos} (no player context). Mirrors
     * EE's break behaviour: individual wire pieces (wiresPerSpool each), unless alternate wire
     * placement is configured, in which case a spool is returned.
     */
    private static void dropWiresAt(ServerLevel level, BlockPos pos) {
        InfrastructureSavedData sd = InfrastructureSavedData.load(level);
        for (InWorldNodeData nodeData : new ArrayList<>(sd.getNodesAt(pos)))
            for (InWorldNodeConnection connection : new ArrayList<>(sd.getConnections(nodeData))) {
                WireData wireData = sd.removeConnection(connection);
                if (wireData == null)
                    continue;
                ItemStack drop = CEEConfigs.server().alternateWirePlacement.get()
                        ? new ItemStack(wireData.wireType().getSpooledItem())
                        : new ItemStack(wireData.wireType().getDrops(), CEEConfigs.server().wiresPerSpool.get());
                popResource(level, pos, drop);
            }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl) {
            boolean creative = player.isCreative();
            if (creative) {
                InfrastructureSavedData sd = InfrastructureSavedData.load(sl);
                for (InWorldNodeData nodeData : sd.getNodesAt(pos))
                    for (InWorldNodeConnection connection : sd.getConnections(nodeData))
                        sd.removeConnection(connection);
            } else {
                removeWiresByPlayer(player, level, pos);
            }
            if (level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be) {
                if (be.getMount() == PoleMount.CROSSARM) {
                    removeCrossarm(level, pos, !creative); // removes arm blocks; drops the item in survival
                } else if (be.getMount() == PoleMount.CONNECTORS && !creative) {
                    for (Direction face : PoleConnectorGeometry.FACES) {
                        PoleConnector connector = be.getConnector(face);
                        if (connector != PoleConnector.NONE && connector.item() != null)
                            popResource(sl, pos, new ItemStack(connector.item()));
                    }
                }
                if (be.hasLabel() && !creative)
                    popResource(sl, pos, new ItemStack(EUItems.UTILITY_POLE_LABEL.get()));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- crossarm multiblock helpers ----

    private static boolean isOwnArm(BlockGetter level, BlockPos armPos, BlockPos polePos) {
        return level.getBlockState(armPos).getBlock() instanceof CrossarmArmBlock
                && level.getBlockEntity(armPos) instanceof CrossarmArmBlockEntity abe
                && abe.getPolePos().equals(polePos);
    }

    /**
     * Rotates the whole vertical run of poles this one belongs to by one 45° step, rebuilding every
     * crossarm's arms. All-or-nothing: if any crossarm in the column can't place its arms at the new
     * rotation, nothing rotates (so the column never splits into mixed orientations).
     */
    private void rotatePole(Level level, BlockPos clickedPos, UtilityPoleBlockEntity clickedBe) {
        int next = PoleRotation.wrap(clickedBe.getRotation() + 1);
        List<BlockPos> column = poleColumn(level, clickedPos);
        for (BlockPos p : column)
            if (level.getBlockEntity(p) instanceof UtilityPoleBlockEntity be && be.getMount() == PoleMount.CROSSARM
                    && !canPlaceArms(level, p, next, be.getCrossarmOffset()))
                return; // a crossarm somewhere in the column is boxed in: rotate nothing
        for (BlockPos p : column) {
            if (!(level.getBlockEntity(p) instanceof UtilityPoleBlockEntity be))
                continue;
            if (be.getMount() == PoleMount.CROSSARM) {
                WoodSet w = level.getBlockState(p).getBlock() instanceof UtilityPoleBlock pole ? pole.getWood() : wood;
                // Keeps the offset; carries the side connectors' wires to their new cells.
                rebuildCrossarm(level, p, be.getRotation(), be.getCrossarmOffset(), next, be.getCrossarmOffset(), w, be);
            } else {
                // Connectors/label live on the (unmoving) pole; re-registering repositions their wires.
                be.setRotation(next);
                applyDiagonalState(level, p, next);
                level.scheduleTick(p, this, 1);
            }
        }
        level.playSound(null, clickedPos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /** A wire lifted off a moving arm connector: which arm it was on, its far endpoint, and its data. */
    private record MovedWire(int armIndex, InWorldNode far, WireData data) {}

    /**
     * Rebuilds a crossarm's two arm blocks at a new rotation/offset, carrying the side connectors' wires
     * with them. EE keys a connection by node id + BlockPos, so moving the arm blocks would otherwise
     * drop their wires; instead we lift every arm wire off first (no drops), rebuild, then reconnect it
     * to the new arm node (remapping a far endpoint that is itself a moving arm, e.g. an inter-arm wire).
     * The centre connector lives on the unmoving pole, so its wires follow on their own.
     */
    private void rebuildCrossarm(Level level, BlockPos pole, int oldRot, int oldOffset,
                                 int newRot, int newOffset, WoodSet w, UtilityPoleBlockEntity be) {
        List<BlockPos> oldArms = CrossarmGeometry.armPositions(pole, oldRot, oldOffset);
        List<BlockPos> newArms = CrossarmGeometry.armPositions(pole, newRot, newOffset);

        // 1. Capture + detach every wire on the old arm nodes (server only), remembering the far end.
        List<MovedWire> moved = new ArrayList<>();
        InfrastructureSavedData sd = level instanceof ServerLevel sl ? InfrastructureSavedData.load(sl) : null;
        if (sd != null)
            for (int i = 0; i < oldArms.size(); i++)
                for (InWorldNodeData nd : new ArrayList<>(sd.getNodesAt(oldArms.get(i))))
                    for (InWorldNodeConnection c : new ArrayList<>(sd.getConnections(nd))) {
                        InWorldNode far = c.node1().equals(nd.node) ? c.node2() : c.node1();
                        WireData wd = sd.removeConnectionNoDrops(c);
                        if (wd != null)
                            moved.add(new MovedWire(i, far, wd));
                    }

        // 2. Rebuild the arm blocks at the new positions (removeCrossarm uses the current/old state).
        removeCrossarm(level, pole, false);
        placeArms(level, pole, newRot, newOffset, w);
        be.setRotation(newRot);
        be.setCrossarm(newOffset);
        applyDiagonalState(level, pole, newRot);
        level.scheduleTick(pole, this, 1);

        // 3. Register the new arm nodes (connect() throws on an unregistered or self node), then reconnect.
        if (sd != null && !moved.isEmpty()) {
            for (BlockPos armPos : newArms)
                sd.registerOrUpdateNodes(armPos, List.of(0));
            for (MovedWire m : moved) {
                InWorldNode src = new InWorldNode(0, newArms.get(m.armIndex));
                InWorldNode dst = remapArmEndpoint(m.far, oldArms, newArms);
                if (!src.equals(dst))
                    sd.connect(src, dst, m.data);
            }
        }
    }

    /**
     * Slides the crossarm at {@code polePos} by one offset step, carrying the side connectors' wires.
     * Invoked by wrenching an arm block ("wrench the extents to slide"); no-op if it can't fit.
     */
    public static void slideCrossarm(Level level, BlockPos polePos) {
        if (!(level.getBlockState(polePos).getBlock() instanceof UtilityPoleBlock poleBlock)
                || !(level.getBlockEntity(polePos) instanceof UtilityPoleBlockEntity be)
                || be.getMount() != PoleMount.CROSSARM)
            return;
        int rot = be.getRotation();
        int oldOffset = be.getCrossarmOffset();
        int next = oldOffset >= 1 ? -1 : oldOffset + 1;
        if (canPlaceArms(level, polePos, rot, next)) {
            poleBlock.rebuildCrossarm(level, polePos, rot, oldOffset, rot, next, poleBlock.getWood(), be);
            level.playSound(null, polePos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /** If a wire's far endpoint was one of the moving arm nodes, point it at that arm's new cell. */
    private static InWorldNode remapArmEndpoint(InWorldNode far, List<BlockPos> oldArms, List<BlockPos> newArms) {
        if (far.id() == 0)
            for (int i = 0; i < oldArms.size(); i++)
                if (far.sourcePos().equals(oldArms.get(i)))
                    return new InWorldNode(0, newArms.get(i));
        return far;
    }

    /** Every utility-pole block in the contiguous vertical run containing {@code pos}, bottom to top. */
    private static List<BlockPos> poleColumn(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).getBlock() instanceof UtilityPoleBlock)
            bottom = bottom.below();
        List<BlockPos> column = new ArrayList<>();
        for (BlockPos p = bottom; level.getBlockState(p).getBlock() instanceof UtilityPoleBlock; p = p.above())
            column.add(p);
        return column;
    }

    /** Mirrors a rotation's diagonal parity into the blockstate so the post renders square vs diamond. */
    private static void applyDiagonalState(Level level, BlockPos pos, int rotation) {
        BlockState cur = level.getBlockState(pos);
        if (cur.getBlock() instanceof UtilityPoleBlock) {
            boolean diagonal = PoleRotation.isDiagonal(rotation);
            if (cur.getValue(DIAGONAL) != diagonal)
                level.setBlock(pos, cur.setValue(DIAGONAL, diagonal), 3);
        }
    }

    /** True if the two arm slots for this rotation/offset are free (or already our own arm blocks). */
    private static boolean canPlaceArms(Level level, BlockPos polePos, int rotation, int offset) {
        for (BlockPos armPos : CrossarmGeometry.armPositions(polePos, rotation, offset))
            if (!level.getBlockState(armPos).canBeReplaced() && !isOwnArm(level, armPos, polePos))
                return false;
        return true;
    }

    private static void placeArms(Level level, BlockPos polePos, int rotation, int offset, WoodSet wood) {
        for (BlockPos armPos : CrossarmGeometry.armPositions(polePos, rotation, offset)) {
            level.setBlock(armPos, EUBlocks.CROSSARM_ARM.get().defaultBlockState(), 3);
            if (level.getBlockEntity(armPos) instanceof CrossarmArmBlockEntity abe)
                abe.configure(wood, polePos);
        }
    }

    /**
     * Tears down a crossarm: clears the pole's crossarm state first (so the arm blocks' own removal
     * hooks see no crossarm and don't recurse), removes the arm blocks, and optionally drops the item.
     */
    public static void removeCrossarm(Level level, BlockPos polePos, boolean drop) {
        if (!(level.getBlockEntity(polePos) instanceof UtilityPoleBlockEntity be) || be.getMount() != PoleMount.CROSSARM)
            return;
        int rotation = be.getRotation();
        int offset = be.getCrossarmOffset();
        be.clearMount();
        for (BlockPos armPos : CrossarmGeometry.armPositions(polePos, rotation, offset))
            if (isOwnArm(level, armPos, polePos))
                level.removeBlock(armPos, false);
        if (level.getBlockState(polePos).getBlock() instanceof UtilityPoleBlock poleBlock)
            level.scheduleTick(polePos, poleBlock, 1);
        if (drop && level instanceof ServerLevel sl)
            popResource(sl, polePos, new ItemStack(EUItems.CROSSARM.get()));
    }

    // ---- nodes ----

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        Map<Integer, Vec3> nodes = new HashMap<>();
        if (!(level.getBlockEntity(pos) instanceof UtilityPoleBlockEntity be))
            return nodes;
        switch (be.getMount()) {
            case CROSSARM -> {
                // The two side connectors live on the arm blocks; the pole hosts only the centre one,
                // and only when it is the topmost block (nothing above it to block the connector).
                if (isCrossarmTop(level, pos))
                    nodes.put(0, CrossarmGeometry.nodeLocal());
            }
            case CONNECTORS -> {
                Direction.Axis poleAxis = state.getValue(AXIS);
                int rot = be.getRotation();
                // Connectors are stored on their base (rot-0) face; their node id is rotation-independent,
                // so wires stay attached — only the reported position rotates with the pole.
                for (Direction face : PoleConnectorGeometry.FACES) {
                    PoleConnector connector = be.getConnector(face);
                    if (connector == PoleConnector.NONE)
                        continue;
                    double[] spreads = PoleConnectorGeometry.pinSpreads(connector.nodeCount());
                    for (int pin = 0; pin < spreads.length; pin++) {
                        Vec3 base = PoleConnectorGeometry.nodePosition(face, poleAxis, spreads[pin]);
                        nodes.put(PoleConnectorGeometry.nodeId(face, pin), PoleRotation.rotateXZ(base, rot));
                    }
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
