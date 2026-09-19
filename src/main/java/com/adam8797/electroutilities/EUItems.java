package com.adam8797.electroutilities;

import java.util.EnumMap;
import java.util.Map;

import com.adam8797.electroutilities.content.utilitypole.WoodSet;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;

/**
 * Central registry holder for all items: the "tool" items that configure a utility pole (using them on a
 * pole sets its state, see {@code UtilityPoleBlock}), plus the per-wood "treated wood" crafting
 * intermediate (log compacted with transformer oil) that poles are crafted from.
 */
public class EUItems {

    /** Per-wood crafting intermediate: a log compacted with transformer oil; combined vertically into poles. */
    public static final Map<WoodSet, ItemEntry<Item>> TREATED_WOOD = new EnumMap<>(WoodSet.class);

    static {
        for (WoodSet wood : WoodSet.values())
            TREATED_WOOD.put(wood, CreateElectroUtilities.REGISTRATE
                    .item(wood.id() + "_treated_wood", Item::new)
                    // Renders as a log-like cube (item/treated_wood parent); the log textures are tinted
                    // green at runtime via the tintindex-0 faces (see EUClient item colors).
                    .model((c, p) -> p.withExistingParent(c.getName(), CreateElectroUtilities.rl("item/treated_wood"))
                            .texture("side", wood.exteriorSide())
                            .texture("end", wood.exteriorEnd()))
                    .register());
    }

    /** Applied to a pole to add a crossarm (adopts the pole's wood). */
    public static final ItemEntry<Item> CROSSARM = CreateElectroUtilities.REGISTRATE
            .item("utility_pole_crossarm", Item::new)
            .model((c, p) -> p.withExistingParent(c.getName(), p.mcLoc("item/stick")))
            .register();

    /** Apply to a pole to open a sign-style label editor (one line, &lt;=5 chars); consumed on confirm. */
    public static final ItemEntry<Item> UTILITY_POLE_LABEL = CreateElectroUtilities.REGISTRATE
            .item("utility_pole_label", Item::new)
            .model((c, p) -> p.withExistingParent(c.getName(), p.mcLoc("item/name_tag")))
            .register();

    public static void register() {
        // Class-load triggers the static initializers above.
    }
}
