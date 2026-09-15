package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry for a pole's crossarm: the three connector node positions and the beam bounds, shared by
 * {@link UtilityPoleBlock#getNodePositions} and the renderer so pins and nodes stay aligned.
 *
 * <p>The arm runs along its axis from -0.5 to 1.5 blocks (half a block past the block each side); the
 * three connectors sit at along = {-0.5, 0.5, 1.5}. The offset (-1/0/+1) shifts the whole arm by half
 * a block along its axis.
 */
public final class CrossarmGeometry {

    private CrossarmGeometry() {}

    /** Node (pin top) height, block units. */
    public static final double NODE_Y = 18.0 / 16.0;
    /** Base along-positions of the three pins before offset. */
    private static final double[] ALONG = { -0.5, 0.5, 1.5 };

    public static double shift(int offset) {
        return offset * 0.5;
    }

    /** Along-axis position of pin {@code i} (0..2) for the given offset. */
    public static double along(int i, int offset) {
        return ALONG[i] + shift(offset);
    }

    /** Node position for pin {@code i} of a crossarm with the given axis (X/Z) and offset (-1/0/+1). */
    public static Vec3 nodePosition(Direction.Axis axis, int offset, int i) {
        double a = along(i, offset);
        return axis == Direction.Axis.X ? new Vec3(a, NODE_Y, 0.5) : new Vec3(0.5, NODE_Y, a);
    }
}
