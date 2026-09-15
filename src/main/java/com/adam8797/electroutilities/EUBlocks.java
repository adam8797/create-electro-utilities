package com.adam8797.electroutilities;

import java.util.EnumMap;
import java.util.Map;

import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlock;
import com.adam8797.electroutilities.content.utilitypole.WoodSet;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * Central registry holder for all blocks. The utility pole is the only block; all features (crossarm,
 * connectors, label) are state on its block entity, applied via the items in {@link EUItems}.
 */
public class EUBlocks {

    /** One utility pole per vanilla wood. */
    public static final Map<WoodSet, BlockEntry<UtilityPoleBlock>> UTILITY_POLES = new EnumMap<>(WoodSet.class);

    static {
        for (WoodSet wood : WoodSet.values())
            UTILITY_POLES.put(wood, utilityPole(wood));
    }

    private static BlockEntry<UtilityPoleBlock> utilityPole(WoodSet wood) {
        return CreateElectroUtilities.REGISTRATE
                .block(wood.id() + "_utility_pole", p -> new UtilityPoleBlock(p, wood))
                .initialProperties(() -> Blocks.OAK_LOG)
                .properties(p -> p.mapColor(wood.mapColor()))
                .tag(BlockTags.MINEABLE_WITH_AXE)
                .blockstate((c, p) -> {
                    var model = p.models()
                            .withExistingParent(c.getName(), CreateElectroUtilities.rl("block/utility_pole_post"))
                            .texture("side", wood.exteriorSide())
                            .texture("end", wood.exteriorEnd())
                            .texture("particle", wood.exteriorSide());
                    p.axisBlock((RotatedPillarBlock) c.get(), model, model);
                })
                .item()
                .model((c, p) -> p.withExistingParent(c.getName(), CreateElectroUtilities.rl("block/" + c.getName())))
                .build()
                .register();
    }

    public static void register() {
        // Class-load triggers the static initializer above.
    }
}
