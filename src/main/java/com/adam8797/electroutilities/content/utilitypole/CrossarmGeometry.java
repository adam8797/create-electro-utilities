package com.adam8797.electroutilities.content.utilitypole;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry for the crossarm multiblock. A crossarm spans three consecutive slots along its axis; the
 * pole occupies one of them and the other two hold {@link com.adam8797.electroutilities.content.utilitypole.CrossarmArmBlock}s,
 * each carrying one connector. The offset (-1/0/+1) chooses where the pole sits within the span:
 * offset 0 = centre, +1 = pole at the negative end (arms extend +axis), -1 = pole at the positive end.
 *
 * <p>Each of the three blocks carries its single connector at its own block centre, so every connector
 * is independently wireable. The beam sits near the top of each block.
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

    public static Direction positiveDir(Direction.Axis axis) {
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
    }

    /** The k-index of each of the three slots along the axis for the given offset (0 = the pole). */
    public static int[] slotKs(int offset) {
        return new int[] { offset - 1, offset, offset + 1 };
    }

    /** World positions of the two arm blocks (the non-pole slots) for a pole/axis/offset. */
    public static List<BlockPos> armPositions(BlockPos polePos, Direction.Axis axis, int offset) {
        Direction dir = positiveDir(axis);
        List<BlockPos> list = new ArrayList<>(2);
        for (int k : slotKs(offset))
            if (k != 0)
                list.add(polePos.relative(dir, k));
        return list;
    }
}
