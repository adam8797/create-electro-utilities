package com.adam8797.electroutilities;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;

/**
 * Central registry holder for all items. These are the "tool" items that configure a utility pole:
 * using them on a pole sets the pole's state (see {@code UtilityPoleBlock}).
 */
public class EUItems {

    /** Applied to a pole to add a crossarm (adopts the pole's wood). */
    public static final ItemEntry<Item> CROSSARM = CreateElectroUtilities.REGISTRATE
            .item("utility_pole_crossarm", Item::new)
            .model((c, p) -> p.withExistingParent(c.getName(), p.mcLoc("item/stick")))
            .register();

    /** Pressed from a brass nugget; rename in an anvil, then apply to a pole to label it (<=5 chars). */
    public static final ItemEntry<Item> UTILITY_POLE_LABEL = CreateElectroUtilities.REGISTRATE
            .item("utility_pole_label", Item::new)
            .model((c, p) -> p.withExistingParent(c.getName(), p.mcLoc("item/name_tag")))
            .register();

    public static void register() {
        // Class-load triggers the static initializers above.
    }
}
