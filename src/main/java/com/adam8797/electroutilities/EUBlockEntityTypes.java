package com.adam8797.electroutilities;

import java.util.ArrayList;
import java.util.List;

import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.UtilityPoleRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.world.level.block.Block;

/**
 * Central registry holder for all block entity types.
 */
public class EUBlockEntityTypes {

    /** One block entity type shared by every wood's utility pole; stores/renders all pole features. */
    public static final BlockEntityEntry<UtilityPoleBlockEntity> UTILITY_POLE = CreateElectroUtilities.REGISTRATE
            .blockEntity("utility_pole", UtilityPoleBlockEntity::new)
            .validBlocks(poleSuppliers())
            .renderer(() -> UtilityPoleRenderer::new)
            .register();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static NonNullSupplier<? extends Block>[] poleSuppliers() {
        List<NonNullSupplier<? extends Block>> list = new ArrayList<>();
        EUBlocks.UTILITY_POLES.values().forEach(list::add);
        return list.toArray(new NonNullSupplier[0]);
    }

    public static void register() {
        // Class-load triggers the static initializer above.
    }
}
