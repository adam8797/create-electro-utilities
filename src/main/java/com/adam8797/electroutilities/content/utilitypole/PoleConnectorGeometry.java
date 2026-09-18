package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Shared geometry for pole-embedded connectors, valid for any pole orientation. Connectors mount on
 * the four faces perpendicular to the pole's axis (e.g. N/E/S/W for a vertical pole, or U/D/N/S for a
 * pole lying along X). Each face can hold up to three connectors, spread along the pole's axis and
 * sitting at the block edge (so the node stays within the pole's own block and is clickable).
 */
public final class PoleConnectorGeometry {

    private PoleConnectorGeometry() {}

    /** All six directions; a connector may live on any face perpendicular to the pole axis. */
    public static final Direction[] FACES = Direction.values();
    public static final int MAX_PER_FACE = 3;

    /** Pole surface offset from block centre (8px post -> 4px = 0.25). */
    public static final double SURFACE = 0.25;
    /** Node distance from block centre along the face normal (0.5 = block edge, stays clickable). */
    public static final double NODE_OUT = 0.5;

    /** A face can host a connector only if it is perpendicular to the pole's axis. */
    public static boolean isValidFace(Direction.Axis poleAxis, Direction face) {
        return face.getAxis() != poleAxis;
    }

    public static int faceIndex(Direction face) {
        return face.get3DDataValue(); // 0..5
    }

    public static Direction face(int index) {
        return Direction.from3DDataValue(index);
    }

    /** Stable block-local node id for a face + pin index. */
    public static int nodeId(Direction face, int pin) {
        return face.get3DDataValue() * MAX_PER_FACE + pin;
    }

    /** Offsets along the pole axis for each pin of a connector of the given node count. */
    public static double[] pinSpreads(int count) {
        return switch (count) {
            case 1 -> new double[] { 0.0 };
            case 2 -> new double[] { -0.15, 0.15 };
            case 3 -> new double[] { -0.22, 0.0, 0.22 };
            default -> new double[0];
        };
    }

    public static Vec3 axisVec(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1, 0, 0);
            case Y -> new Vec3(0, 1, 0);
            case Z -> new Vec3(0, 0, 1);
        };
    }

    /** Connector node position (block-local): centre, out to the block edge along the face, spread along the axis. */
    public static Vec3 nodePosition(Direction face, Direction.Axis poleAxis, double spread) {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        return new Vec3(0.5, 0.5, 0.5).add(normal.scale(NODE_OUT)).add(axisVec(poleAxis).scale(spread));
    }
}
