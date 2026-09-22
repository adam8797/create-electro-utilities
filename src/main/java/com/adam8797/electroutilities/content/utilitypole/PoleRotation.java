package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The whole-pole rotation shared by every utility-pole feature: a single index {@code 0..7} giving the
 * pole's facing in 45° clockwise increments (viewed from above), anchored so index 0 = North.
 *
 * <p>Two different notions of "rotate by this" live here and must not be confused:
 * <ul>
 *   <li><b>Cell steps</b> ({@link #stepX}/{@link #stepZ}) — integer neighbour offsets used to place the
 *       crossarm arm blocks. Diagonals are {@code (±1, ±1)} (the diagonal neighbour cell), so these are
 *       <em>not</em> unit length; they are for stepping between block positions only.</li>
 *   <li><b>True rotation</b> ({@link #rotateXZ}) — a length-preserving rotation of a block-local point
 *       about the vertical centre axis, used for connector node positions and rendering.</li>
 * </ul>
 */
public final class PoleRotation {

    private PoleRotation() {}

    public static final int COUNT = 8;

    /** Clockwise-from-north unit cell steps, indexed by rotation 0..7 (N, NE, E, SE, S, SW, W, NW). */
    private static final int[] STEP_X = { 0, 1, 1, 1, 0, -1, -1, -1 };
    private static final int[] STEP_Z = { -1, -1, 0, 1, 1, 1, 0, -1 };

    public static int wrap(int rot) {
        return ((rot % COUNT) + COUNT) % COUNT;
    }

    /** Whether this rotation is a diagonal (odd) step; the post renders as a diamond when true. */
    public static boolean isDiagonal(int rot) {
        return (wrap(rot) & 1) == 1;
    }

    public static int stepX(int rot) {
        return STEP_X[wrap(rot)];
    }

    public static int stepZ(int rot) {
        return STEP_Z[wrap(rot)];
    }

    /** Rotation angle in degrees (clockwise from above), for {@link com.mojang.math.Axis} pose rotations. */
    public static float degrees(int rot) {
        return 45.0f * wrap(rot);
    }

    /**
     * Rotates a block-local point about the vertical centre axis {@code (0.5, y, 0.5)} by this rotation,
     * clockwise (viewed from above). Length-preserving — for node positions and rendered geometry.
     */
    public static Vec3 rotateXZ(Vec3 local, int rot) {
        double theta = Math.toRadians(degrees(rot));
        double cos = Math.cos(theta), sin = Math.sin(theta);
        double dx = local.x - 0.5, dz = local.z - 0.5;
        // Clockwise in (x east, z south): (x,z) -> (x cos - z sin, x sin + z cos); 90° gives (x,z)->(-z,x).
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        return new Vec3(0.5 + rx, local.y, 0.5 + rz);
    }

    /** The rotation index whose cell step matches this integer delta's signs, or -1 if it isn't a line. */
    public static int fromDelta(int dx, int dz) {
        int sx = Integer.signum(dx), sz = Integer.signum(dz);
        for (int r = 0; r < COUNT; r++)
            if (STEP_X[r] == sx && STEP_Z[r] == sz)
                return r;
        return -1;
    }

    /**
     * The world compass slot (0..7) a click resolves to, among this rotation's four connector/label slots
     * (at {@code rot, rot+2, rot+4, rot+6}). At an even (cardinal) rotation the clicked block face maps
     * straight to its slot. At an odd (45°) rotation the two slots flank the clicked face, so the hit
     * point's angle within the face picks the nearer diagonal. {@code hitX}/{@code hitZ} are block-local.
     */
    public static int slotFor(Direction clickedFace, double hitX, double hitZ, int rot) {
        int c = cardinalIndex(clickedFace); // 0/2/4/6, or -1 for a vertical face
        if (c < 0)
            return -1;
        if (!isDiagonal(rot))
            return c; // even rotation: the clicked face is itself a slot
        double dx = hitX - 0.5, dz = hitZ - 0.5;
        double deg = Math.toDegrees(Math.atan2(dx, -dz)); // clockwise from north (−Z)
        // The clicked cardinal face sits between the two diagonal slots c-1 and c+1; pick the nearer.
        double toPrev = angularDistance(deg, (c - 1) * 45.0);
        double toNext = angularDistance(deg, (c + 1) * 45.0);
        return wrap(toNext <= toPrev ? c + 1 : c - 1);
    }

    private static double angularDistance(double a, double b) {
        double d = Math.abs(a - b) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    /** The base (rot-0) Direction for one of the four cardinal slots, given a world rotation index. */
    public static Direction baseCardinal(int worldSlot, int rot) {
        int base = wrap(worldSlot - rot);
        return switch (base) {
            case 0 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            case 4 -> Direction.SOUTH;
            case 6 -> Direction.WEST;
            default -> null; // not aligned to a slot
        };
    }

    /** Compass index (0..7) for a base cardinal Direction (N=0, E=2, S=4, W=6). */
    public static int cardinalIndex(Direction face) {
        return switch (face) {
            case NORTH -> 0;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 6;
            default -> -1;
        };
    }
}
