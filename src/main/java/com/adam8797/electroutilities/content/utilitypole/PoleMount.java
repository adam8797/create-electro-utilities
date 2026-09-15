package com.adam8797.electroutilities.content.utilitypole;

/**
 * The mutually-exclusive "mount" a utility pole is currently configured with. A pole is either bare,
 * carrying a crossarm (with its own three connectors), or carrying per-face connectors. A label is
 * independent of this and may be present in any mode.
 */
public enum PoleMount {
    NONE,
    CROSSARM,
    CONNECTORS;

    public static PoleMount byId(int id) {
        PoleMount[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
