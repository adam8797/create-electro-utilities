package com.adam8797.electroutilities.content.utilitypole;

import javax.annotation.Nullable;

import com.george_vi.electroenergetics.CEEBlocks;

import net.minecraft.world.item.Item;

/**
 * A connector that can be embedded on one face of a utility pole. Mirrors EE's single/double/triple
 * connectors (quad is intentionally excluded). {@link #nodeCount} determines how many electrical
 * nodes the face exposes; the pins are stacked vertically along the pole.
 */
public enum PoleConnector {
    NONE(0),
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3);

    private final int nodeCount;

    PoleConnector(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int nodeCount() {
        return nodeCount;
    }

    /** The EE connector item this maps to (for consuming on apply / dropping on removal). */
    @Nullable
    public Item item() {
        return switch (this) {
            case SINGLE -> CEEBlocks.CONNECTOR.get().asItem();
            case DOUBLE -> CEEBlocks.DOUBLE_CONNECTOR.get().asItem();
            case TRIPLE -> CEEBlocks.TRIPLE_CONNECTOR.get().asItem();
            case NONE -> null;
        };
    }

    /** Maps a held item to the connector it would place, or {@code null} if it isn't a supported connector. */
    @Nullable
    public static PoleConnector fromItem(Item item) {
        if (item == CEEBlocks.CONNECTOR.get().asItem())
            return SINGLE;
        if (item == CEEBlocks.DOUBLE_CONNECTOR.get().asItem())
            return DOUBLE;
        if (item == CEEBlocks.TRIPLE_CONNECTOR.get().asItem())
            return TRIPLE;
        return null;
    }

    public static PoleConnector byId(int id) {
        PoleConnector[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
