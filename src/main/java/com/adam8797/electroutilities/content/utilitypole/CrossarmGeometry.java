package com.adam8797.electroutilities.content.utilitypole;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Geometry for the crossarm multiblock. A crossarm spans three consecutive slots along the pole's
 * {@linkplain PoleRotation rotation} line; the pole occupies one of them and the other two hold
 * {@link com.adam8797.electroutilities.content.utilitypole.CrossarmArmBlock}s, each carrying one
 * connector. The offset (-1/0/+1) chooses where the pole sits within the span: offset 0 = centre,
 * +1 = pole at the negative end (arms extend along the rotation), -1 = pole at the positive end.
 *
 * <p>Each of the three blocks carries its single connector at its own block centre, so every connector
 * is independently wireable. The beam sits near the top of each block, along the rotation line
 * (including the diagonal orientations).
 */
public final class CrossarmGeometry {

    private CrossarmGeometry() {}

    public static final double BEAM_Y1 = 11.0 / 16.0;
    public static final double BEAM_Y2 = 15.0 / 16.0;
    public static final double PERP1 = 6.0 / 16.0;
    public static final double PERP2 = 10.0 / 16.0;
    /** Base of the connector (top of the beam). */
    public static final double CONNECTOR_BASE_Y = BEAM_Y2;
    /** Node height: centre of the rendered insulator ball (beam top + 8/16). */
    public static final double NODE_Y = CONNECTOR_BASE_Y + 8.0 / 16.0;

    /** The connector node position within any slot block (block-local). */
    public static Vec3 nodeLocal() {
        return new Vec3(0.5, NODE_Y, 0.5);
    }

    /**
     * Interaction/outline box for the beam across a single slot, per rotation. Even rotations run the
     * beam along X or Z; diagonal rotations use a centred approximation (a box can't be turned 45°, so
     * the beam's extreme corners are not selectable — the insulator box on top is the main click target).
     */
    public static VoxelShape beamOutline(int rotation) {
        if (PoleRotation.isDiagonal(rotation))
            return Block.box(3, 11, 3, 13, 15, 13);
        return PoleRotation.stepX(rotation) != 0
                ? Block.box(0, 11, 6, 16, 15, 10)
                : Block.box(6, 11, 0, 10, 15, 16);
    }

    /** The k-index of each of the three slots along the line for the given offset (0 = the pole). */
    public static int[] slotKs(int offset) {
        return new int[] { offset - 1, offset, offset + 1 };
    }

    /** World positions of the two arm blocks (the non-pole slots) for a pole/rotation/offset. */
    public static List<BlockPos> armPositions(BlockPos polePos, int rotation, int offset) {
        int sx = PoleRotation.stepX(rotation), sz = PoleRotation.stepZ(rotation);
        List<BlockPos> list = new ArrayList<>(2);
        for (int k : slotKs(offset))
            if (k != 0)
                list.add(polePos.offset(sx * k, 0, sz * k));
        return list;
    }
}
