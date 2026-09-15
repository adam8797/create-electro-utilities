package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Shared geometry for pole-embedded connectors so the electrical node positions
 * ({@link UtilityPoleBlock#getNodePositions}) and the rendered pins
 * ({@code UtilityPoleConnectorRenderer}) always agree.
 *
 * <p>Connectors mount on the four horizontal faces (indices N=0, E=1, S=2, W=3). Each face can hold
 * up to three vertically-stacked pins.
 */
public final class PoleConnectorGeometry {

    private PoleConnectorGeometry() {}

    public static final Direction[] FACES = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    public static final int MAX_PER_FACE = 3;

    /** Pole surface offset from block centre (8px post -> 4px = 0.25). */
    public static final double SURFACE = 0.25;
    /** Node (insulator ball) distance from block centre, along the outward face normal. */
    public static final double NODE_OUT = 0.75;

    public static int faceIndex(Direction face) {
        return switch (face) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> -1;
        };
    }

    public static Direction face(int index) {
        return FACES[index];
    }

    /** Stable block-local node id for a given face + pin index. */
    public static int nodeId(int faceIndex, int pin) {
        return faceIndex * MAX_PER_FACE + pin;
    }

    /** Vertical positions (block units) of the pins for a connector of the given node count. */
    public static double[] pinHeights(int count) {
        return switch (count) {
            case 1 -> new double[] { 0.5 };
            case 2 -> new double[] { 0.375, 0.625 };
            case 3 -> new double[] { 0.28, 0.5, 0.72 };
            default -> new double[0];
        };
    }

    /** Node (insulator ball) position for a pin on a face at a given height, in block-local coords. */
    public static Vec3 nodePosition(Direction face, double height) {
        double x = 0.5, z = 0.5;
        switch (face) {
            case NORTH -> z = 0.5 - NODE_OUT;
            case SOUTH -> z = 0.5 + NODE_OUT;
            case EAST -> x = 0.5 + NODE_OUT;
            case WEST -> x = 0.5 - NODE_OUT;
            default -> {}
        }
        return new Vec3(x, height, z);
    }
}
